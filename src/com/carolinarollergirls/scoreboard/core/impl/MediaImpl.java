package com.carolinarollergirls.scoreboard.core.impl;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import com.carolinarollergirls.scoreboard.core.Media;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;

public class MediaImpl extends DefaultScoreBoardEventProvider implements Media {
    public MediaImpl(ScoreBoardEventProvider parent, File path) {
	this.parent = parent;
        setup(path.toPath().resolve("html"));
    }

    public String getProviderName() { return "Media"; }
    public Class<Media> getProviderClass() { return Media.class; }
    public String getProviderId() { return ""; }
    public ScoreBoardEventProvider getParent() { return parent; }
    public List<Class<? extends Property>> getProperties() { return properties; }

    private void setup(Path path) {
        this.path = path;
        formats.put("images", new MediaFormatImpl(this, "images"));
        formats.get("images").addType("fullscreen");
        formats.get("images").addType("sponsor_banner");
        formats.get("images").addType("teamlogo");
        formats.put("videos", new MediaFormatImpl(this, "videos"));
        formats.get("videos").addType("fullscreen");
        formats.put("customhtml", new MediaFormatImpl(this, "customhtml"));
        formats.get("customhtml").addType("fullscreen");
        for (MediaFormat mf : formats.values()) {
            mf.addScoreBoardListener(this);
        }

        // Create directories and register with inotify.
        try {
            watchService = FileSystems.getDefault().newWatchService();
            for (String format : formats.keySet()) {
                for (String type : formats.get(format).getTypes()) {
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
            Map<String, MediaFile> map = formats.get(format).getType(type).getFiles();
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
            MediaType mt = formats.get(format).getType(type);
            if(!mt.getFiles().containsKey(id)) {
                // URL paths always use forward slashes.
                String p = "/" + format + "/" + type + "/" + id;
                // Name is the filename without the extension.
                mt.addFile(new MediaFileImpl(mt, id, id.replaceFirst("\\.[^.]*$", ""), p));
            }
        }
    }

    private void mediaFileDeleted(String format, String type, String id) {
        synchronized (coreLock) {
            formats.get(format).getType(type).removeFile(id);
        }
    }

    public Set<String> getFormats() {
        synchronized (coreLock) {
            return Collections.unmodifiableSet(formats.keySet());
        }
    }

    public Set<String> getTypes(String format) {
        synchronized (coreLock) {
            if (formats.containsKey(format)) {
        	return Collections.unmodifiableSet(formats.get(format).getTypes());
            } else {
        	return new HashSet<String>();
            }
        }
    }

    public Map<String, MediaFile> getMediaFiles(String format, String type) {
        MediaFormat f = formats.get(format);
        if (f == null) {
            return null;
        }
        MediaType t = f.getType(type);
        if (t == null) {
            return null;
        }
        return t.getFiles();
    }

    public boolean removeMediaFile(String format, String type, String id) {
        synchronized (coreLock) {
            // Check the directory is one the user is meant to be able to change.
            if (formats.containsKey(format) && formats.get(format).getTypes().contains(type)) {
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

    private Map<String, MediaFormat> formats = new HashMap<String, MediaFormat>();
    private Path path;
    private WatchService watchService;

    protected ScoreBoardEventProvider parent;
    protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	add(Child.class);
    }};

    private static Object coreLock = ScoreBoardImpl.getCoreLock();

    public class MediaFormatImpl extends DefaultScoreBoardEventProvider implements MediaFormat {
	MediaFormatImpl(Media parent, String format) {
	    this.parent = parent;
	    this.format = format;
	}
	
        public String getProviderName() { return format; }
        public Class<MediaFormat> getProviderClass() { return MediaFormat.class; }
        public String getProviderId() { return ""; }
        public ScoreBoardEventProvider getParent() { return parent; }
        public List<Class<? extends Property>> getProperties() { return properties; }

        public String getFormat() {
            return format;
        }
        
	public Set<String> getTypes() {
	    return Collections.unmodifiableSet(types.keySet());
	}
        public MediaType getType(String type) {
            return types.get(type);
        }
	public void addType(String type) {
	    if (!types.containsKey(type)) {
		MediaType mt = new MediaTypeImpl(this, type);
		types.put(type, mt);
		mt.addScoreBoardListener(this);
                scoreBoardChange(new ScoreBoardEvent(this, Child.TYPE, type, null));
	    }
	}
	
	private Media parent;
	private String format;
	private Map<String, MediaType> types = new HashMap<String, MediaType>();
	private List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	    add(Child.class);
	}};
    }
    
    public class MediaTypeImpl extends DefaultScoreBoardEventProvider implements MediaType {
	MediaTypeImpl(MediaFormat parent, String type) {
	    this.parent = parent;
	    this.type = type;
	}
	
        public String getProviderName() { return type; }
        public Class<MediaFormat> getProviderClass() { return MediaFormat.class; }
        public String getProviderId() { return ""; }
        public ScoreBoardEventProvider getParent() { return parent; }
        public List<Class<? extends Property>> getProperties() { return properties; }

        public String getFormat() {
            return parent.getFormat();
        }
        
        public String getType() {
            return type;
        }
        
	public Map<String, MediaFile> getFiles() {
	    return Collections.unmodifiableMap(files);
	}
        public MediaFile getFile(String file) {
            return files.get(file);
        }
	public void addFile(MediaFile file) {
	    if (!files.containsKey(file.getId())) {
		files.put(file.getId(), file);
		file.addScoreBoardListener(this);
                scoreBoardChange(new ScoreBoardEvent(this, Child.FILE, file, null));
	    }
	}
	
	public void removeFile(String id) {
	    MediaFile file = files.get(id);
	    if (file != null) {
		files.remove(id);
                scoreBoardChange(new ScoreBoardEvent(this, Child.FILE, null, file));
	    }
	}
	
	private MediaFormat parent;
	private String type;
	private Map<String, MediaFile> files = new HashMap<String, MediaFile>();
	private List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	    add(Child.class);
	}};
    }
    
    public class MediaFileImpl extends DefaultScoreBoardEventProvider implements MediaFile {
        MediaFileImpl(MediaType type, String id, String name, String src) {
            this.type = type;
            this.id = id;
            this.name = name;
            this.src = src;
        }

        public String getProviderName() { return PropertyConversion.toFrontend(MediaType.Child.FILE); }
        public Class<MediaFile> getProviderClass() { return MediaFile.class; }
        public String getProviderId() { return getId(); }
        public ScoreBoardEventProvider getParent() { return type; }
        public List<Class<? extends Property>> getProperties() { return properties; }

        public String getFormat() { synchronized (coreLock) { return type.getFormat() ;} }
        public String getType() { synchronized (coreLock) { return type.getType() ;} }
        public String getId() { synchronized (coreLock) { return id ;} }
        public String getName() { synchronized (coreLock) { return name ;} }
        public void setName(String n) {
            synchronized (coreLock) {
                name = n;
                scoreBoardChange(new ScoreBoardEvent(parent, MediaType.Child.FILE, this, null));
            };
        }
        public String getSrc() { synchronized (coreLock) { return src ;} }

        private MediaType type;
        private String id;
        private String name;
        private String src;
        protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
            add(Child.class);
        }};
    }
}
