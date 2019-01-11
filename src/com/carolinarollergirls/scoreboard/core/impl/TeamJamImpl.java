package com.carolinarollergirls.scoreboard.core.impl;

import java.util.HashMap;
import com.carolinarollergirls.scoreboard.core.Fielding;
import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;

public class TeamJamImpl extends ScoreBoardEventProviderImpl implements TeamJam {
    public TeamJamImpl(String team_id, Jam j) {
	super(j, Jam.Child.TEAM_JAM, TeamJam.class, Value.class, Child.class);
        children.put(Child.FIELDING, new HashMap<String, ValueWithId>());
        values.put(Value.ID, team_id);
        jam = j;
    }

    public String getId() { return getTeamId(); }

    public Object valueFromString(PermanentProperty prop, String sValue) {
        if (sValue == null) return null;
        if (prop == Value.STAR_PASS) { return Boolean.parseBoolean(sValue); }
        if (prop == Value.JAM_SCORE || prop == Value.TOTAL_SCORE || prop == Value.TIMEOUTS
    	    || prop == Value.OFFICIAL_REVIEWS) { return Integer.parseInt(sValue); }
        return sValue;
    }
    
    public boolean set(PermanentProperty prop, Object value, Flag flag) {
        if (!(prop instanceof Value) || prop == Value.ID) { return false; }
        return super.set(prop, value, flag);
    }
    
    public ValueWithId create(AddRemoveProperty prop, String id) {
        return new FieldingImpl(id, this);
    }

    public String getTeamId() { return (String)get(Value.ID); }
    public int getPeriodNumber() { return jam.getPeriodNumber(); }
    public int getJamNumber() { return jam.getNumber(); }

    public int getJamScore() { return (Integer)get(Value.JAM_SCORE); }
    public void setJamScore(int s) { set(Value.JAM_SCORE, s); }

    public int getTotalScore() { return (Integer)get(Value.TOTAL_SCORE); }
    public void setTotalScore(int s) { set(Value.TOTAL_SCORE, s); }

    public String getLeadJammer() { return (String)get(Value.LEAD_JAMMER); }
    public void setLeadJammer(String ls) { set(Value.LEAD_JAMMER, ls); }

    public boolean getStarPass() { return (Boolean)get(Value.STAR_PASS); }
    public void setStarPass(boolean sp) { set(Value.STAR_PASS, sp); }

    public boolean getNoPivot() { return (Boolean)get(Value.NO_PIVOT); }
    public void setNoPivot(boolean np) { set(Value.NO_PIVOT, np); }

    public int getTimeouts() { return (Integer)get(Value.TIMEOUTS); }
    public void setTimeouts(int t) { set(Value.TIMEOUTS, t); }

    public int getOfficialReviews() { return (Integer)get(Value.OFFICIAL_REVIEWS); }
    public void setOfficialReviews(int o) { set(Value.OFFICIAL_REVIEWS, o); }

    public Fielding getFielding (String sid) { return (Fielding)get(Child.FIELDING, sid); }
    public void addFielding(String sid) { get(Child.FIELDING, sid, true); }
    public void removeFielding(String sid) { remove(Child.FIELDING, sid); }
    public void removeFielding() { removeAll(Child.FIELDING); }

    private Jam jam;
}
