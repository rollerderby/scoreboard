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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import com.carolinarollergirls.scoreboard.core.Media;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;

public class MediaImpl extends DefaultScoreBoardEventProvider implements Media {
    public MediaImpl(ScoreBoardEventProvider parent, File path) {
	this.parent = parent;
	children.put(Child.FORMAT, new HashMap<String, ValueWithId>());
        setup(path.toPath().resolve("html"));
    }

    public String getProviderName() { return PropertyConversion.toFrontend(ScoreBoard.Child.MEDIA); }
    public Class<Media> getProviderClass() { return Media.class; }
    public String getId() { return ""; }
    public ScoreBoardEventProvider getParent() { return parent; }
    public List<Class<? extends Property>> getProperties() { return properties; }

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

    protected ScoreBoardEventProvider parent;
    protected static List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	add(Child.class);
    }};

    public class MediaFormatImpl extends DefaultScoreBoardEventProvider implements MediaFormat {
	MediaFormatImpl(Media parent, String format) {
	    this.parent = parent;
	    this.format = format;
	    children.put(Child.TYPE, new HashMap<String, ValueWithId>());
	}
	
        public String getProviderName() { return PropertyConversion.toFrontend(Media.Child.FORMAT); }
        public Class<MediaFormat> getProviderClass() { return MediaFormat.class; }
        public String getId() { return format; }
        public ScoreBoardEventProvider getParent() { return parent; }
        public List<Class<? extends Property>> getProperties() { return properties; }

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
	
	private Media parent;
	private String format;
	private List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	    add(Child.class);
	}};
    }
    
    public class MediaTypeImpl extends DefaultScoreBoardEventProvider implements MediaType {
	MediaTypeImpl(MediaFormat parent, String type) {
	    this.parent = parent;
	    this.type = type;
	    children.put(Child.FILE, new HashMap<String, ValueWithId>());
	}
	
        public String getProviderName() { return PropertyConversion.toFrontend(MediaFormat.Child.TYPE); }
        public Class<MediaFormat> getProviderClass() { return MediaFormat.class; }
        public String getId() { return type; }
        public ScoreBoardEventProvider getParent() { return parent; }
        public List<Class<? extends Property>> getProperties() { return properties; }

        public String getFormat() { return parent.getFormat(); }
        
        public String getType() { return type; }
        
        public MediaFile getFile(String file) { return (MediaFile)get(Child.FILE, file); }
	public void addFile(MediaFile file) { add(Child.FILE, file); }
	public void removeFile(MediaFile file) { remove(Child.FILE, file); }
	
	private MediaFormat parent;
	private String type;
	private List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	    add(Child.class);
	}};
    }
    
    public class MediaFileImpl extends DefaultScoreBoardEventProvider implements MediaFile {
        MediaFileImpl(MediaType type, String id, String name, String src) {
            this.type = type;
            values.put(Value.ID, id);
            values.put(Value.NAME, name);
            values.put(Value.SRC, src);
        }

        public String getProviderName() { return PropertyConversion.toFrontend(MediaType.Child.FILE); }
        public Class<MediaFile> getProviderClass() { return MediaFile.class; }
        public String getProviderId() { return getId(); }
        public ScoreBoardEventProvider getParent() { return type; }
        public List<Class<? extends Property>> getProperties() { return properties; }
        
        public boolean set(PermanentProperty prop, Object value, Flag flag) {
            if (prop == Value.NAME) { return super.set(prop, value, flag); }
            return false;
        }

        public String getFormat() { synchronized (coreLock) { return type.getFormat() ;} }
        public String getType() { synchronized (coreLock) { return type.getType() ;} }
        public String getId() { synchronized (coreLock) { return (String)get(Value.ID) ;} }
        public String getName() { synchronized (coreLock) { return (String)get(Value.NAME) ;} }
        public void setName(String n) { synchronized (coreLock) { set(Value.NAME, n) ;} }
        public String getSrc() { synchronized (coreLock) { return (String)get(Value.SRC); } }

        private MediaType type;
        protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
            add(Value.class);
        }};
    }
}
