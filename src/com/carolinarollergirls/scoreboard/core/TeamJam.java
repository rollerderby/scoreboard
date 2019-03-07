package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.ParentOrderedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.NumberedProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

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

    public boolean hasNoPivot();
    public void setNoPivot(boolean np);

    public Fielding getFielding(FloorPosition fp);

    public enum Value implements PermanentProperty {
        CURRENT_TRIP,
        CURRENT_TRIP_NUMBER,
        LAST_SCORE,
        OS_OFFSET,
        JAM_SCORE,
        TOTAL_SCORE,
        LOST,
        LEAD,
        CALLOFF,
        NO_INITIAL,
        INJURY,
        DISPLAY_LEAD,
        STAR_PASS,
        STAR_PASS_TRIP,
        NO_PIVOT
    }
    public enum Child implements AddRemoveProperty {
        FIELDING
    }
    public enum NChild implements NumberedProperty {
        SCORING_TRIP
    }
}
