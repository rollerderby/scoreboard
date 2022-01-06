package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface Media extends ScoreBoardEventProvider {
    public MediaFormat getFormat(String format);

    // Deletes a file off disk. True if successful.
    public boolean removeMediaFile(String format, String type, String id);

    public boolean validFileName(String fn);

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Child<MediaFormat> FORMAT = new Child<>(MediaFormat.class, "Format", props);

    public static interface MediaFormat extends ScoreBoardEventProvider {
        public String getFormat();
        public MediaType getType(String type);

        @SuppressWarnings("hiding")
        public static Collection<Property<?>> props = new ArrayList<>();

        public static final Child<MediaType> TYPE = new Child<>(MediaType.class, "Type", props);
    }

    public static interface MediaType extends ScoreBoardEventProvider {
        public String getFormat();
        public String getType();

        public MediaFile getFile(String id);
        public void addFile(MediaFile file);
        public void removeFile(MediaFile file);

        @SuppressWarnings("hiding")
        public static Collection<Property<?>> props = new ArrayList<>();

        public static final Child<MediaFile> FILE = new Child<>(MediaFile.class, "File", props);
    }

    public static interface MediaFile extends ScoreBoardEventProvider {
        public String getFormat();
        public String getType();
        @Override
        public String getId();
        public String getName();
        public void setName(String s);
        public String getSrc();

        @SuppressWarnings("hiding")
        public static Collection<Property<?>> props = new ArrayList<>();

        public static final Value<String> SRC = new Value<>(String.class, "Src", "", props);
        public static final Value<String> NAME = new Value<>(String.class, "Name", "", props);
    }
}
