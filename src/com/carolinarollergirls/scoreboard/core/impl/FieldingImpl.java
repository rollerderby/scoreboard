package com.carolinarollergirls.scoreboard.core.impl;

import java.util.ArrayList;
import java.util.List;

import com.carolinarollergirls.scoreboard.core.Fielding;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;

public class FieldingImpl extends DefaultScoreBoardEventProvider implements Fielding {
    public FieldingImpl(String skater_id, TeamJam teamJam) {
	values.put(Value.ID, skater_id);
	this.team = teamJam;
	setPenaltyBox(false);
    }

    public String getProviderName() { return PropertyConversion.toFrontend(TeamJam.Child.FIELDING); }
    public Class<Fielding> getProviderClass() { return Fielding.class; }
    public String getId() { return getSkaterId(); }
    public ScoreBoardEventProvider getParent() { return team; }
    public List<Class<? extends Property>> getProperties() { return properties; }

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

    protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	add(Value.class);
    }};
}
