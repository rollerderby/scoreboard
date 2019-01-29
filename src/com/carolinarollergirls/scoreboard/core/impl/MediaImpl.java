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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collection;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import com.carolinarollergirls.scoreboard.core.Media;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;

public class MediaImpl extends ScoreBoardEventProviderImpl implements Media {
    public MediaImpl(ScoreBoard parent, File path) {
	super(parent, ScoreBoard.Child.MEDIA, Media.class, Child.class);
        setup(path.toPath().resolve("html"));
    }

    public String getId() { return ""; }

    private void setup(Path path) {
        this.path = path;
        addFormat("images", "fullscreen", "sponsor_banner", "teamlogo");
        addFormat("videos", "fullscreen");
        addFormat("customhtml", "fullscreen");

        // Create directories and register with inotify.
        try {
            watchService = FileSystems.getDefault().newWatchService();
            for (ValueWithId mf : getAll(Child.FORMAT)) {
        	String format = ((MediaFormat)mf).getFormat();
                for (ValueWithId mt : ((MediaFormat)mf).getAll(MediaFormat.Child.TYPE)) {
                    String type = ((MediaType)mt).getType();
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
            Collection<? extends ValueWithId> files = getFormat(format).getType(type).getAll(MediaType.Child.FILE);
            // Remove any files that aren't there or aren't files any more.
            for (ValueWithId fn : files) {
                if (!p.resolve(fn.getId()).toFile().isFile()) {
                    mediaFileDeleted(format, type, fn.getId());
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
            MediaType mt = getFormat(format).getType(type);
            if(mt.getFile(id) == null) {
                // URL paths always use forward slashes.
                String p = "/" + format + "/" + type + "/" + id;
                // Name is the filename without the extension.
                mt.addFile(new MediaFileImpl(mt, id, id.replaceFirst("\\.[^.]*$", ""), p));
            }
        }
    }

    private void mediaFileDeleted(String format, String type, String id) {
        synchronized (coreLock) {
            MediaType mt = getFormat(format).getType(type); 
            mt.removeFile(mt.getFile(id));
        }
    }

    private void addFormat(String format, String... types) {
        MediaFormatImpl child = new MediaFormatImpl(this, format);
	child.addScoreBoardListener(this);
	children.get(Child.FORMAT).put(format, child);
	scoreBoardChange(new ScoreBoardEvent(this, Child.FORMAT, child, false));
        for (String type : types) {
            child.addType(type);
        }
    }
    public MediaFormat getFormat(String format) { return (MediaFormat)get(Child.FORMAT, format); }

    public boolean removeMediaFile(String format, String type, String id) {
        synchronized (coreLock) {
            try {
        	// Check the directory is one the user is meant to be able to change.
        	if (getFormat(format).getType(type) != null) {
        	    // Delete the file, and let inotify take care of handling the change.
        	    return Files.deleteIfExists(path.resolve(format).resolve(type).resolve(id));
        	}
        	return false;
            } catch (Exception e) {
        	return false;
            }
        }
    }

    private Path path;
    private WatchService watchService;

    public class MediaFormatImpl extends ScoreBoardEventProviderImpl implements MediaFormat {
	MediaFormatImpl(Media parent, String format) {
	    super(parent, Media.Child.FORMAT, MediaFormat.class, Child.class);
	    this.format = format;
	}
	
        public String getId() { return format; }

        public String getFormat() { return format; }
        
        public MediaType getType(String type) { return (MediaType)get(Child.TYPE, type); }
	protected void addType(String type) {
	    synchronized (coreLock) {
		MediaType mt = new MediaTypeImpl(this, type);
		mt.addScoreBoardListener(this);
		children.get(Child.TYPE).put(type, mt);
		scoreBoardChange(new ScoreBoardEvent(this, Child.TYPE, mt, false));
	    }
	}
	
	private String format;
    }
    
    public class MediaTypeImpl extends ScoreBoardEventProviderImpl implements MediaType {
	MediaTypeImpl(MediaFormat parent, String type) {
	    super(parent, MediaFormat.Child.TYPE, MediaType.class, Child.class);
	    this.parent = parent;
	    this.type = type;
	}
	
        public String getId() { return type; }

        public String getFormat() { return parent.getFormat(); }
        
        public String getType() { return type; }
        
        public MediaFile getFile(String file) { return (MediaFile)get(Child.FILE, file); }
	public void addFile(MediaFile file) { add(Child.FILE, file); }
	public void removeFile(MediaFile file) { remove(Child.FILE, file); }
	
	private MediaFormat parent;
	private String type;
    }
    
    public class MediaFileImpl extends ScoreBoardEventProviderImpl implements MediaFile {
        MediaFileImpl(MediaType type, String id, String name, String src) {
            super(type, MediaType.Child.FILE, MediaFile.class, Value.class);
            this.type = type;
            values.put(Value.ID, id);
            writeProtectionOverride.put(Value.ID, false);
            values.put(Value.NAME, name);
            values.put(Value.SRC, src);
            writeProtectionOverride.put(Value.SRC, false);
        }

        public String getFormat() { synchronized (coreLock) { return type.getFormat() ;} }
        public String getType() { synchronized (coreLock) { return type.getType() ;} }
        public String getId() { synchronized (coreLock) { return (String)get(Value.ID) ;} }
        public String getName() { synchronized (coreLock) { return (String)get(Value.NAME) ;} }
        public void setName(String n) { synchronized (coreLock) { set(Value.NAME, n) ;} }
        public String getSrc() { synchronized (coreLock) { return (String)get(Value.SRC); } }

        private MediaType type;
    }
}
