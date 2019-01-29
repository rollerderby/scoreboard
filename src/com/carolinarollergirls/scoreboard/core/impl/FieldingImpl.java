package com.carolinarollergirls.scoreboard.core.impl;

import com.carolinarollergirls.scoreboard.core.Fielding;
import com.carolinarollergirls.scoreboard.core.FloorPosition;
import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.Role;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public class FieldingImpl extends ScoreBoardEventProviderImpl implements Fielding {
    public FieldingImpl(TeamJam teamJam, Position position) {
	super(teamJam, TeamJam.Child.FIELDING, Fielding.class, Value.class);
	this.teamJam = teamJam;
	values.put(Value.POSITION, position);
	writeProtectionOverride.put(Value.POSITION, false);
	setPenaltyBox(false);
    }

    public String getId() { return teamJam.getId() + "_" + getPosition().toString(); }
    public String getProviderId() { return getPosition().getProviderId(); }

    public Object valueFromString(PermanentProperty prop, String sValue) {
	if (prop == Value.SKATER) { return teamJam.getTeam().getSkater(sValue); }
	return super.valueFromString(prop, sValue);
    }
    public boolean set(PermanentProperty prop, Object value, Flag flag) {
	synchronized (coreLock) {
	    if (prop == Value.PENALTY_BOX && getSkater() == null && (Boolean)value) { return false; }
	    return super.set(prop, value, flag);
	}
    }
    protected void valueChanged(PermanentProperty prop, Object value, Object last) {
	if (prop == Value.SKATER) {
	    if (last != null) {
		((Skater)last).remove(Skater.Child.FIELDING, this);
	    }
	    if(value != null) {
		((Skater)value).add(Skater.Child.FIELDING, this);
	    }
	} else if (prop == Value.PENALTY_BOX && isCurrent() && (Boolean)value &&
		getPosition().getFloorPosition() == FloorPosition.JAMMER &&
		scoreBoard.isInJam() && teamJam.getLeadJammer().equals(Team.LEAD_LEAD)) {
	    teamJam.setLeadJammer(Team.LEAD_LOST_LEAD);
	}
    }

    public TeamJam getTeamJam() { return teamJam; }
    public Position getPosition() { return (Position)get(Value.POSITION); }

    public boolean isCurrent() { return teamJam.isRunningOrUpcoming(); }
    
    public Role getCurrentRole() { return getPosition().getFloorPosition().getRole(teamJam); }
    
    public Skater getSkater() { return (Skater)get(Value.SKATER); }
    public void setSkater(Skater s) { set(Value.SKATER, s); }

    public boolean getPenaltyBox() { return (Boolean)get(Value.PENALTY_BOX); }
    public void setPenaltyBox(boolean p) { set(Value.PENALTY_BOX, p); }

    private TeamJam teamJam;
}
