package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public interface ScoringTrip extends NumberedScoreBoardEventProvider<ScoringTrip> {
    public int getScore();
    
    public enum Value implements PermanentProperty {
        SCORE(Integer.class, 0),
        AFTER_S_P(Boolean.class, false),
        CURRENT(Boolean.class, false),
        DURATION(Long.class, 0L),
        JAM_CLOCK_START(Long.class, 0L),
        JAM_CLOCK_END(Long.class, 0L),
        ANNOTATION(String.class, "");

        private Value(Class<?> t, Object dv) { type = t; defaultValue = dv; }
        private final Class<?> type;
        private final Object defaultValue;
        @Override
        public Class<?> getType() { return type; }
        @Override
        public Object getDefaultValue() { return defaultValue; }
    }
    public enum Command implements CommandProperty {
        INSERT_BEFORE,
        REMOVE;
        
        @Override
        public Class<Boolean> getType() { return Boolean.class; }
    }
}
