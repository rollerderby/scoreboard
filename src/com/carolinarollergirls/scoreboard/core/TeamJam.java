package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.OrderedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ParentOrderedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.NumberedProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;

public interface TeamJam extends ParentOrderedScoreBoardEventProvider<TeamJam> {
    public Jam getJam();
    public Team getTeam();
    
    public TeamJam getOtherTeam();

    public boolean isRunningOrEnded();
    public boolean isRunningOrUpcoming();

    public int getLastScore();
    public void setLastScore(int l);

    public int getOsOffset();
    public void setOsOffset(int o);
    public void changeOsOffset(int c);

    public int getJamScore();
    public int getTotalScore();

    public ScoringTrip getCurrentScoringTrip();
    public void addScoringTrip();
    public void removeScoringTrip();
    
    public boolean isLost();
    public boolean isLead();
    public boolean isCalloff();
    public boolean isInjury();
    public boolean isDisplayLead();

    public boolean isStarPass();
    public ScoringTrip getStarPassTrip();

    public boolean hasNoNamedPivot();
    public void setNoNamedPivot(boolean np);

    public Fielding getFielding(FloorPosition fp);

    public enum Value implements PermanentProperty {
        CURRENT_TRIP(ScoringTrip.class, null),
        CURRENT_TRIP_NUMBER(Integer.class, 0),
        LAST_SCORE(Integer.class, 0),
        OS_OFFSET(Integer.class, 0),
        JAM_SCORE(Integer.class, 0),
        AFTER_S_P_SCORE(Integer.class, 0),
        TOTAL_SCORE(Integer.class, 0),
        LOST(Boolean.class, false),
        LEAD(Boolean.class, false),
        CALLOFF(Boolean.class, false),
        NO_INITIAL(Boolean.class, false),
        INJURY(Boolean.class, false),
        DISPLAY_LEAD(Boolean.class, false),
        STAR_PASS(Boolean.class, false),
        STAR_PASS_TRIP(ScoringTrip.class, null),
        NO_NAMED_PIVOT(Boolean.class, true),
        NO_PIVOT(Boolean.class, false);

        private Value(Class<?> t, Object dv) { type = t; defaultValue = dv; }
        private final Class<?> type;
        private final Object defaultValue;
        @Override
        public Class<?> getType() { return type; }
        @Override
        public Object getDefaultValue() { return defaultValue; }
    }
    public enum Child implements AddRemoveProperty {
        FIELDING(Fielding.class);

        private Child(Class<? extends ValueWithId> t) { type = t; }
        private final Class<? extends ValueWithId> type;
        @Override
        public Class<? extends ValueWithId> getType() { return type; }
    }
    public enum NChild implements NumberedProperty {
        SCORING_TRIP(ScoringTrip.class);

        private NChild(Class<? extends OrderedScoreBoardEventProvider<?>> t) { type = t; }
        private final Class<? extends OrderedScoreBoardEventProvider<?>> type;
        @Override
        public Class<? extends OrderedScoreBoardEventProvider<?>> getType() { return type; }
    }
}
