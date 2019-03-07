package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.ParentOrderedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public interface Fielding extends ParentOrderedScoreBoardEventProvider<Fielding> {
    public TeamJam getTeamJam();
    public Position getPosition();

    public boolean isCurrent();
    public Role getCurrentRole();

    public Skater getSkater();
    public void setSkater(Skater s);

    public boolean getPenaltyBox();
    public void setPenaltyBox(boolean p);

    public enum Value implements PermanentProperty {
        SKATER,
        POSITION,
        PENALTY_BOX;
    }
}
