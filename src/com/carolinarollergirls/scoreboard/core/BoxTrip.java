package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;

public interface BoxTrip extends ScoreBoardEventProvider {
    public int compareTo(BoxTrip other);
    
    public void end();
    
    public boolean isCurrent();
    public Fielding getCurrentFielding();
    public Fielding getStartFielding();
    public boolean startedBetweenJams();
    public boolean startedAfterSP();
    public Fielding getEndFielding();
    public boolean endedBetweenJams();
    public boolean endedAfterSP();
    
    public enum Value implements PermanentProperty {
        ID(String.class, ""),
        IS_CURRENT(Boolean.class, false),
        CURRENT_FIELDING(Fielding.class, null),
        START_FIELDING(Fielding.class, null),
        START_JAM_NUMBER(Integer.class, 0),
        START_BETWEEN_JAMS(Boolean.class, false),
        START_AFTER_S_P(Boolean.class, false),
        END_FIELDING(Fielding.class, null),
        END_JAM_NUMBER(Integer.class, 0),
        END_BETWEEN_JAMS(Boolean.class, false),
        END_AFTER_S_P(Boolean.class, false),
        WALLTIME_START(Long.class, 0L),
        WALLTIME_END(Long.class, 0L),
        JAM_CLOCK_START(Long.class, 0L),
        JAM_CLOCK_END(Long.class, 0L),
        DURATION(Long.class, 0L);

        private Value(Class<?> t, Object dv) { type = t; defaultValue = dv; }
        private final Class<?> type;
        private final Object defaultValue;
        public Class<?> getType() { return type; }
        public Object getDefaultValue() { return defaultValue; }
    }
    public enum Child implements AddRemoveProperty {
        FIELDING(Fielding.class),
        PENALTY(Penalty.class);

        private Child(Class<? extends ValueWithId> t) { type = t; }
        private final Class<? extends ValueWithId> type;
        public Class<? extends ValueWithId> getType() { return type; }
    }
    public enum Command implements CommandProperty {
        START_EARLIER,
        START_LATER,
        END_EARLIER,
        END_LATER,
        DELETE;
        
        public Class<Boolean> getType() { return Boolean.class; }
    }
}
