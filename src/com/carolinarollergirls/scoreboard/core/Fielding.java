package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.ParentOrderedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public interface Fielding extends ParentOrderedScoreBoardEventProvider<Fielding> {
    public TeamJam getTeamJam();
    public Position getPosition();

    public boolean isCurrent();
    public Role getCurrentRole();

    public Skater getSkater();
    public void setSkater(Skater s);

    public boolean isInBox();
    public BoxTrip getCurrentBoxTrip();
    public void updateBoxTripSymbols();

    public enum Value implements PermanentProperty {
        SKATER,
        POSITION,
        PENALTY_BOX,
        CURRENT_BOX_TRIP,
        BOX_TRIP_SYMBOLS,
        BOX_TRIP_SYMBOLS_BEFORE_S_P,
        BOX_TRIP_SYMBOLS_AFTER_S_P,
        ANNOTATION
    }
    public enum Child implements AddRemoveProperty {
        BOX_TRIP
    }
}
