package com.carolinarollergirls.scoreboard.core.impl;

import com.carolinarollergirls.scoreboard.core.Fielding;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public class FieldingImpl extends ScoreBoardEventProviderImpl implements Fielding {
    public FieldingImpl(String skater_id, TeamJam teamJam) {
	super(teamJam, TeamJam.Child.FIELDING, Fielding.class, Value.class);
	values.put(Value.ID, skater_id);
	this.team = teamJam;
	setPenaltyBox(false);
    }

    public String getId() { return getSkaterId(); }

    public Object valueFromString(PermanentProperty prop, String sValue) {
	if (sValue == null) return null;
	if (prop == Value.PENALTY_BOX) { return Boolean.parseBoolean(sValue); }
	return sValue;
    }

    public boolean set(PermanentProperty prop, Object value, Flag flag) {
	if (!(prop instanceof Value) || prop == Value.ID) { return false; }
	return super.set(prop, value, flag);
    }

    public String getSkaterId() { return (String)get(Value.ID); }
    public String getTeamId() { return team.getTeamId(); }
    public int getPeriodNumber() { return team.getPeriodNumber(); }
    public int getJamNumber() { return team.getJamNumber(); }

    public String getPosition() { return (String)get(Value.POSITION); }
    public void setPosition(String p) { set(Value.POSITION, p); }

    public boolean getPenaltyBox() { return (Boolean)get(Value.PENALTY_BOX); }
    public void setPenaltyBox(boolean p) { set(Value.PENALTY_BOX, p); }

    private TeamJam team;
}
