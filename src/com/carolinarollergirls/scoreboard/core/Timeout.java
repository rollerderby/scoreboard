package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public interface Timeout extends ScoreBoardEventProvider {
    int compareTo(Timeout other);

    public void stop();
    
    public TimeoutOwner getOwner();
    public boolean isReview();
    public boolean isRetained();
    public boolean isRunning();
    
    public enum Value implements PermanentProperty {
        OWNER(TimeoutOwner.class, null),
        REVIEW(Boolean.class, false),
        RETAINED_REVIEW(Boolean.class, false),
        RUNNING(Boolean.class, true),
        PRECEDING_JAM(Jam.class, null),
        PRECEDING_JAM_NUMBER(Integer.class, 0),
        DURATION(Long.class, 0L),
        PERIOD_CLOCK_ELAPSED_START(Long.class, 0L),
        PERIOD_CLOCK_ELAPSED_END(Long.class, 0L),
        PERIOD_CLOCK_END(Long.class, 0L),
        WALLTIME_START(Long.class, 0L),
        WALLTIME_END(Long.class, 0L);

        private Value(Class<?> t, Object dv) { type = t; defaultValue = dv; }
        private final Class<?> type;
        private final Object defaultValue;
        @Override
        public Class<?> getType() { return type; }
        @Override
        public Object getDefaultValue() { return defaultValue; }
    }
    public enum Command implements CommandProperty {
        DELETE;
        
        @Override
        public Class<Boolean> getType() { return Boolean.class; }
    }

    public enum Owners implements TimeoutOwner {
        NONE(""),
        OTO("O");

        Owners(String id) {
            this.id = id;
        }

        @Override
        public String getId() { return id; }
        @Override
        public String toString() { return id; }

        private String id;
    }
}
