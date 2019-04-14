package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public interface Penalty extends NumberedScoreBoardEventProvider<Penalty> {
    public int compareTo(Penalty other);

    public int getPeriodNumber();
    public int getJamNumber();
    public Jam getJam();
    public String getCode();
    
    public boolean isServed();

    public enum Value implements PermanentProperty {
        TIME(Long.class, 0L),
        JAM(Jam.class, null),
        PERIOD_NUMBER(Integer.class, 0),
        JAM_NUMBER(Integer.class, 0),
        CODE(String.class, ""),
        SERVING(Boolean.class, false),
        SERVED(Boolean.class, false),
        BOX_TRIP(BoxTrip.class, null);

        private Value(Class<?> t, Object dv) { type = t; defaultValue = dv; }
        private final Class<?> type;
        private final Object defaultValue;
        @Override
        public Class<?> getType() { return type; }
        @Override
        public Object getDefaultValue() { return defaultValue; }
    }
    public enum Command implements CommandProperty {
        REMOVE;
        
        @Override
        public Class<Boolean> getType() { return Boolean.class; }
    }
}
