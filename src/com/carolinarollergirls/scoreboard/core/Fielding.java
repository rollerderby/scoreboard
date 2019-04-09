package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.ParentOrderedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;

public interface Fielding extends ParentOrderedScoreBoardEventProvider<Fielding> {
    public TeamJam getTeamJam();
    public Position getPosition();

    public boolean isCurrent();
    public Role getCurrentRole();

    public Skater getSkater();
    public void setSkater(Skater s);

    public boolean isSitFor3();
    public boolean isInBox();
    public BoxTrip getCurrentBoxTrip();
    public void updateBoxTripSymbols();

    public enum Value implements PermanentProperty {
        SKATER(Skater.class, null),
        SKATER_NUMBER(String.class, "?"),
        NOT_FIELDED(Boolean.class, false),
        POSITION(Position.class, null),
        SIT_FOR_3(Boolean.class, false),
        PENALTY_BOX(Boolean.class, false),
        CURRENT_BOX_TRIP(BoxTrip.class, null),
        BOX_TRIP_SYMBOLS(String.class, ""),
        BOX_TRIP_SYMBOLS_BEFORE_S_P(String.class, ""),
        BOX_TRIP_SYMBOLS_AFTER_S_P(String.class, ""),
        ANNOTATION(String.class, "");

        private Value(Class<?> t, Object dv) { type = t; defaultValue = dv; }
        private final Class<?> type;
        private final Object defaultValue;
        public Class<?> getType() { return type; }
        public Object getDefaultValue() { return defaultValue; }
    }
    public enum Child implements AddRemoveProperty {
        BOX_TRIP(BoxTrip.class);

        private Child(Class<? extends ValueWithId> t) { type = t; }
        private final Class<? extends ValueWithId> type;
        public Class<? extends ValueWithId> getType() { return type; }
    }
    public enum Command implements CommandProperty {
        ADD_BOX_TRIP;
        
        public Class<Boolean> getType() { return Boolean.class; }
    }
}
