package com.carolinarollergirls.scoreboard.defaults;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.model.MediaModel;
import com.carolinarollergirls.scoreboard.view.Media;

public class DefaultMediaModel extends DefaultScoreBoardEventProvider implements MediaModel {
    public DefaultMediaModel(File path) {
        setup(path.toPath().resolve("html"));
    }

    public String getProviderName() { return "Media"; }
    public Class<Media> getProviderClass() { return Media.class; }
    public String getProviderId() { return ""; }

    private void setup(Path path) {
        this.path = path;
        files.put("images", new HashMap<String, Map<String, MediaFileModel>>());
        files.get("images").put("fullscreen", new HashMap<String, MediaFileModel>());
        files.get("images").put("sponsor_banner", new HashMap<String, MediaFileModel>());
        files.get("images").put("teamlogo", new HashMap<String, MediaFileModel>());
        files.put("videos", new HashMap<String, Map<String, MediaFileModel>>());
        files.get("videos").put("fullscreen", new HashMap<String, MediaFileModel>());
        files.put("customhtml", new HashMap<String, Map<String, MediaFileModel>>());
        files.get("customhtml").put("fullscreen", new HashMap<String, MediaFileModel>());

        // Create directories and register with inotify.
        try {
            watchService = FileSystems.getDefault().newWatchService();
            for (String format : files.keySet()) {
                for (String type : files.get(format).keySet()) {
                    Path p = path.resolve(format).resolve(type);
                    p.toFile().mkdirs();
                    p.register(watchService, ENTRY_CREATE, ENTRY_DELETE, OVERFLOW);
                    mediaTypeRefresh(format, type);
                }
            }
        } catch (Exception e) {
            // This should never fail.
            throw new RuntimeException(e);
        }

        Thread thread = new Thread() {
            public void run() {
                while (true) {
                    WatchKey key;
                    try {
                        key = watchService.take();
                    } catch (InterruptedException x) {
                        return;
                    }
                    Path dir = (Path)key.watchable();
                    String format = dir.getName(dir.getNameCount() - 2).toString();
                    String type = dir.getName(dir.getNameCount() - 1).toString();

                    for (WatchEvent<?> event: key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        if (kind == OVERFLOW) {
                            mediaTypeRefresh(format, type);
                            continue;
                        }
                        Path filename = (Path)event.context();
                        if (kind == ENTRY_CREATE) {
                            // Ignore directories.
                            if (dir.resolve(filename).toFile().isFile()) {
                                mediaFileCreated(format, type, filename.toString());
                            }
                        } else if (kind == ENTRY_DELETE) {
                            mediaFileDeleted(format, type, filename.toString());
                        }
                    }
                    key.reset();
                }
            }
        };
        thread.start();
    }

    private void mediaTypeRefresh(String format, String type) {
        synchronized (coreLock) {
            Path p = path.resolve(format).resolve(type);
            Map<String, MediaFileModel> map = files.get(format).get(type);
            // Remove any files that aren't there or aren't files any more.
            for (String fn : map.keySet()) {
                if (!p.resolve(fn).toFile().isFile()) {
                    mediaFileDeleted(format, type, fn);
                }
            }
            // Add any files that are there.
            for (File f : p.toFile().listFiles()) {
                if (f.isFile()) {
                    mediaFileCreated(format, type, f.getName());
                }
            }
        }
    }

    public boolean validFileName(String fn) {
        return !fn.matches("(^\\.)|(\\.[dD][bB]$)|\\\\|/");
    }

    private void mediaFileCreated(String format, String type, String id) {
        synchronized (coreLock) {
            Map<String, MediaFileModel> map = files.get(format).get(type);
            if(!map.containsKey(id)) {
                // URL paths always use forward slashes.
                String p = "/" + format + "/" + type + "/" + id;
                // Name is the filename without the extension.
                MediaFileModel mf = new DefaultMediaFileModel(format, type, id, id.replaceFirst("\\.[^.]*$", ""), p);
                mf.addScoreBoardListener(this);
                map.put(id, mf);
                scoreBoardChange(new ScoreBoardEvent(mf, MediaFile.EVENT_FILE, mf, null));
            }
        }
    }

    private void mediaFileDeleted(String format, String type, String id) {
        synchronized (coreLock) {
            Map<String, MediaFileModel> map = files.get(format).get(type);
            MediaFile mf = map.get(id);
            if (mf != null) {
                map.remove(id);
                scoreBoardChange(new ScoreBoardEvent(this, EVENT_REMOVE_FILE, mf, null));
            }
        }
    }

    public Set<String> getFormats() {
        synchronized (coreLock) {
            return Collections.unmodifiableSet(files.keySet());
        }
    }

    public Set<String> getTypes(String format) {
        synchronized (coreLock) {
            return Collections.unmodifiableSet(files.get(format).keySet());
        }
    }

    public Map<String, MediaFile> getMediaFiles(String format, String type) {
        return Collections.unmodifiableMap(new HashMap<String, MediaFile>(getMediaFileModels(format, type)));
    }

    public Map<String, MediaFileModel> getMediaFileModels(String format, String type) {
        Map<String, Map<String, MediaFileModel>> fm = files.get(format);
        if (fm == null) {
            return null;
        }
        Map<String, MediaFileModel> tm = fm.get(type);
        if (tm == null) {
            return null;
        }
        return Collections.unmodifiableMap(tm);
    }

    public boolean removeMediaFile(String format, String type, String id) {
        synchronized (coreLock) {
            // Check the directory is one the user is meant to be able to change.
            if (files.containsKey(format) && files.get(format).containsKey(type)) {
                // Delete the file, and let inotify take care of handling the change.
                try {
                    return Files.deleteIfExists(path.resolve(format).resolve(type).resolve(id));
                } catch (IOException e) {
                    return false;
                }
            }
            return false;
        }
    }

    private Map<String, Map<String, Map<String, MediaFileModel>>> files = new HashMap<String, Map<String, Map<String, MediaFileModel>>>();
    private Path path;
    private WatchService watchService;

    private static Object coreLock = DefaultScoreBoardModel.getCoreLock();

    public class DefaultMediaFileModel extends DefaultScoreBoardEventProvider implements MediaFileModel {
        DefaultMediaFileModel(String format, String type, String id, String name, String src) {
            this.format = format;
            this.type = type;
            this.id = id;
            this.name = name;
            this.src = src;
        }

        public String getProviderName() { return "MediaFile"; }
        public Class<MediaFile> getProviderClass() { return MediaFile.class; }
        public String getProviderId() { return getId(); }

        public String getFormat() { synchronized (coreLock) { return format ;} }
        public String getType() { synchronized (coreLock) { return type ;} }
        public String getId() { synchronized (coreLock) { return id ;} }
        public String getName() { synchronized (coreLock) { return name ;} }
        public void setName(String n) {
            synchronized (coreLock) {
                name = n;
                scoreBoardChange(new ScoreBoardEvent(this, EVENT_FILE, this, null));
            };
        }
        public String getSrc() { synchronized (coreLock) { return src ;} }

        private String format;
        private String type;
        private String id;
        private String name;
        private String src;
    }
}
