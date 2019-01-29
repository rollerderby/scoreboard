package com.carolinarollergirls.scoreboard.core.impl;

import com.carolinarollergirls.scoreboard.core.Fielding;
import com.carolinarollergirls.scoreboard.core.FloorPosition;
import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;

public class TeamJamImpl extends ScoreBoardEventProviderImpl implements TeamJam {
    public TeamJamImpl(Jam j, String team_id) {
	super(j, Jam.Child.TEAM_JAM, TeamJam.class, Value.class, Child.class);
        jam = j;
        team = scoreBoard.getTeam(team_id);
        values.put(Value.LAST_SCORE, 0);
        values.put(Value.OS_OFFSET, 0);
        values.put(Value.JAM_SCORE, 0);
        values.put(Value.TOTAL_SCORE, 0);
        values.put(Value.NO_PIVOT, true);
        values.put(Value.STAR_PASS, false);
        values.put(Value.LEAD_JAMMER, Team.LEAD_NO_LEAD);
        for (ValueWithId p : team.getAll(Team.Child.POSITION)) {
            add(Child.FIELDING, new FieldingImpl(this, (Position)p));
        }
    }

    public String getId() { return jam.getId() + "_" + team.getId(); }
    public String getProviderId() { return team.getId(); }

    public boolean set(PermanentProperty prop, Object value, Flag flag) {
	synchronized (coreLock) {
	    if (prop == Value.TOTAL_SCORE && flag != Flag.CUSTOM) { return false; }
	    Number min = (value instanceof Integer && prop != Value.OS_OFFSET) ? 0 : null;
	    return super.set(prop, value, flag, min, null, 0);
	}
    }
    protected void valueChanged(PermanentProperty prop, Object value, Object last) {
	if (prop == Value.TOTAL_SCORE) {
	    if (jam.hasNext(true)) {
		getNext().set(Value.LAST_SCORE, value);
	    }
	} else if (prop == Value.JAM_SCORE || prop == Value.OS_OFFSET || prop == Value.LAST_SCORE) {
	    set(Value.TOTAL_SCORE, getLastScore() + getJamScore() + getOsOffset(), Flag.CUSTOM);
	}
    }
    
    public TeamJam getNext() {
	if (jam.hasNext(true)) {
	    return jam.getNext(false, true).getTeamJam(getTeam().getId());
	}
	return null;
    }
    public TeamJam getPrevious() {
	if (jam.hasPrevious(true)) {
	    return jam.getPrevious(false, true).getTeamJam(getTeam().getId());
	}
	return null;
    }

    public Team getTeam() { return team; }
    public int getPeriodNumber() { return jam.getPeriodNumber(); }
    public int getJamNumber() { return jam.getNumber(); }

    public boolean isRunningOrEnded() { return this == team.getRunningOrEndedTeamJam(); }
    public boolean isRunningOrUpcoming() { return this == team.getRunningOrUpcomingTeamJam(); }

    public int getLastScore() { return (Integer)get(Value.LAST_SCORE); }
    public void setLastScore(int l) { set(Value.LAST_SCORE, l); }

    public int getOsOffset() { return (Integer)get(Value.OS_OFFSET); }
    public void setOsOffset(int o) { set(Value.OS_OFFSET, o); }
    public void changeOsOffset(int c) { set(Value.OS_OFFSET, c, Flag.CHANGE); }

    public int getJamScore() { return (Integer)get(Value.JAM_SCORE); }
    public void setJamScore(int s) { set(Value.JAM_SCORE, s); }
    public void changeJamScore(int c) { set(Value.JAM_SCORE, c, Flag.CHANGE); }

    public int getTotalScore() { return (Integer)get(Value.TOTAL_SCORE); }

    public String getLeadJammer() { return (String)get(Value.LEAD_JAMMER); }
    public void setLeadJammer(String ls) { set(Value.LEAD_JAMMER, ls); }

    public boolean isStarPass() { return (Boolean)get(Value.STAR_PASS); }
    public void setStarPass(boolean sp) { set(Value.STAR_PASS, sp); }

    public boolean hasNoPivot() { return (Boolean)get(Value.NO_PIVOT); }
    public void setNoPivot(boolean np) { set(Value.NO_PIVOT, np); }

    public Fielding getFielding (FloorPosition fp) { return (Fielding)get(Child.FIELDING, fp.toString()); }

    private Jam jam;
    private Team team;
}
