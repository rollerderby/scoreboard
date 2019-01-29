package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public interface Fielding extends ScoreBoardEventProvider {
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
