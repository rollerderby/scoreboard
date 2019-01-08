package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public interface Fielding extends ScoreBoardEventProvider {
    public int getPeriodNumber();
    public int getJamNumber();
    public String getTeamId();
    public String getSkaterId();

    public boolean getPenaltyBox();
    public void setPenaltyBox(boolean p);
    public String getPosition();
    public void setPosition(String p);

    public enum Value implements PermanentProperty {
        ID,
        POSITION,
        PENALTY_BOX;
    }
}
