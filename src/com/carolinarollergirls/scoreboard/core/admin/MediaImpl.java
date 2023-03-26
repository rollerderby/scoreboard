package com.carolinarollergirls.scoreboard.core.admin;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.core.interfaces.Media;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.utils.BasePath;

public class MediaImpl extends ScoreBoardEventProviderImpl<Media> implements Media {
    public MediaImpl(ScoreBoard parent) {
        super(parent, "", ScoreBoard.MEDIA);
        addProperties(props);
        setup(BasePath.get().toPath().resolve("html"));
    }

    private void setup(Path path) {
        this.path = path;
        addFormat("images", "fullscreen", "sponsor_banner", "teamlogo");
        addFormat("videos", "fullscreen");
        addFormat("custom", "nso", "settings", "view", "overlay");
        addFormat("game-data", "json", "xlsx");

        // Create directories and register with inotify.
        try {
            watchService = FileSystems.getDefault().newWatchService();
            for (MediaFormat mf : getAll(FORMAT)) {
                String format = mf.getFormat();
                for (MediaType mt : mf.getAll(MediaFormat.TYPE)) {
                    String type = mt.getType();
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
            @Override
            public void run() {
                while (true) {
                    WatchKey key;
                    try {
                        key = watchService.take();
                    } catch (InterruptedException x) { return; }
                    Path dir = (Path) key.watchable();
                    String format = dir.getName(dir.getNameCount() - 2).toString();
                    String type = dir.getName(dir.getNameCount() - 1).toString();

                    synchronized (coreLock) {
                        try {
                            requestBatchStart();

                            for (WatchEvent<?> event : key.pollEvents()) {
                                WatchEvent.Kind<?> kind = event.kind();
                                if (kind == OVERFLOW) {
                                    mediaTypeRefresh(format, type);
                                    continue;
                                }
                                Path filename = (Path) event.context();
                                if (kind == ENTRY_CREATE) {
                                    mediaFileCreated(format, type, filename.toString());
                                } else if (kind == ENTRY_DELETE) {
                                    mediaFileDeleted(format, type, filename.toString());
                                }
                            }
                            key.reset();
                        } finally { requestBatchEnd(); }
                    }
                }
            }
        };
        thread.start();
    }

    private void mediaTypeRefresh(String format, String type) {
        synchronized (coreLock) {
            Path p = path.resolve(format).resolve(type);
            Collection<MediaFile> files = getFormat(format).getType(type).getAll(MediaType.FILE);
            // Remove any files that aren't there any more.
            for (MediaFile fn : files) {
                if (!p.resolve(fn.getId()).toFile().exists()) { mediaFileDeleted(format, type, fn.getId()); }
            }
            // Add any files that are there.
            for (File f : p.toFile().listFiles()) { mediaFileCreated(format, type, f.getName()); }
        }
    }

    @Override
    public boolean validFileName(String fn) {
        return !fn.matches("(^\\.)|(\\.[dD][bB]$)|\\\\|/");
    }

    private void mediaFileCreated(String format, String type, String id) {
        synchronized (coreLock) {
            MediaType mt = getFormat(format).getType(type);
            if (mt.getFile(id) == null) {
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
        add(FORMAT, child);
        for (String type : types) { child.addType(type); }
    }
    @Override
    public MediaFormat getFormat(String format) {
        return get(FORMAT, format);
    }

    @Override
    public boolean removeMediaFile(String format, String type, String id) {
        synchronized (coreLock) {
            try {
                // Check the directory is one the user is meant to be able to change.
                if (getFormat(format).getType(type) != null) {
                    // Delete the file, and let inotify take care of handling the change.
                    Files.walkFileTree(path.resolve(format).resolve(type).resolve(id), new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                    return true;
                }
                return false;
            } catch (Exception e) { return false; }
        }
    }

    private Path path;
    private WatchService watchService;

    public class MediaFormatImpl extends ScoreBoardEventProviderImpl<MediaFormat> implements MediaFormat {
        MediaFormatImpl(Media parent, String format) {
            super(parent, format, Media.FORMAT);
            addProperties(props);
            this.format = format;
        }

        @Override
        public String getFormat() {
            return format;
        }

        @Override
        public MediaType getType(String type) {
            return get(TYPE, type);
        }
        protected void addType(String type) { add(TYPE, new MediaTypeImpl(this, type)); }

        private String format;
    }

    public class MediaTypeImpl extends ScoreBoardEventProviderImpl<MediaType> implements MediaType {
        MediaTypeImpl(MediaFormat parent, String type) {
            super(parent, parent.getId() + "_" + type, MediaFormat.TYPE);
            addProperties(props);
            this.parent = parent;
            this.type = type;
        }

        @Override
        public String getProviderId() {
            return type;
        }

        @Override
        public String getFormat() {
            return parent.getFormat();
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public MediaFile getFile(String file) {
            return get(FILE, file);
        }
        @Override
        public void addFile(MediaFile file) {
            add(FILE, file);
        }
        @Override
        public void removeFile(MediaFile file) {
            remove(FILE, file);
        }

        @SuppressWarnings("hiding")
        private MediaFormat parent;
        private String type;
    }

    public class MediaFileImpl extends ScoreBoardEventProviderImpl<MediaFile> implements MediaFile {
        MediaFileImpl(MediaType type, String id, String name, String src) {
            super(type, id, MediaType.FILE);
            addProperties(props);
            this.type = type;
            set(NAME, name);
            set(SRC, src);
            addWriteProtection(SRC);
        }

        @Override
        public String getFormat() {
            synchronized (coreLock) { return type.getFormat(); }
        }
        @Override
        public String getType() {
            synchronized (coreLock) { return type.getType(); }
        }
        @Override
        public String getName() {
            synchronized (coreLock) { return get(NAME); }
        }
        @Override
        public void setName(String n) {
            synchronized (coreLock) { set(NAME, n); }
        }
        @Override
        public String getSrc() {
            synchronized (coreLock) { return get(SRC); }
        }

        private MediaType type;
    }
}
