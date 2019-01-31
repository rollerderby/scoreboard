package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public interface TeamJam extends ScoreBoardEventProvider {
    public int getPeriodNumber();
    public int getJamNumber();
    public Jam getJam();
    public Team getTeam();

    public boolean isRunningOrEnded();
    public boolean isRunningOrUpcoming();
    
    public TeamJam getNext();
    public TeamJam getPrevious();
    
    public int getLastScore();
    public void setLastScore(int l);

    public int getOsOffset();
    public void setOsOffset(int o);
    public void changeOsOffset(int c);
    
    public int getJamScore();
    public void setJamScore(int s);
    public void changeJamScore(int c);
    
    public int getTotalScore();

    public String getLeadJammer();
    public void setLeadJammer(String ls);
    
    public boolean isStarPass();
    public void setStarPass(boolean sp);
    
    public boolean hasNoPivot();
    public void setNoPivot(boolean np);

    public Fielding getFielding(FloorPosition fp);

    public enum Value implements PermanentProperty {
	ID,
        LAST_SCORE,
        OS_OFFSET,
        JAM_SCORE,
        TOTAL_SCORE,
        LEAD_JAMMER,
        STAR_PASS,
        NO_PIVOT;
    }
    public enum Child implements AddRemoveProperty {
        FIELDING;
    }
}
