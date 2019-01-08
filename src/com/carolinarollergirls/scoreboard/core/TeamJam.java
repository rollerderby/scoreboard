package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public interface TeamJam extends ScoreBoardEventProvider {
    public int getPeriodNumber();
    public int getJamNumber();
    public String getTeamId();

    public int getJamScore();
    public void setJamScore(int s);
    public int getTotalScore();
    public void setTotalScore(int s);
    public String getLeadJammer();
    public void setLeadJammer(String ls);
    public boolean getStarPass();
    public void setStarPass(boolean sp);
    public boolean getNoPivot();
    public void setNoPivot(boolean np);
    public int getTimeouts();
    public void setTimeouts(int t);
    public int getOfficialReviews();
    public void setOfficialReviews(int o);

    public Fielding getFielding(String sid);
    public void addFielding(String sid);
    public void removeFielding(String sid);
    public void removeFielding();

    public enum Value implements PermanentProperty {
        ID,
        JAM_SCORE,
        TOTAL_SCORE,
        LEAD_JAMMER,
        STAR_PASS,
        NO_PIVOT,
        TIMEOUTS,
        OFFICIAL_REVIEWS;
    }
    public enum Child implements AddRemoveProperty {
        FIELDING;
    }
}
