package com.carolinarollergirls.scoreboard.core.impl;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.ArrayList;
import java.util.List;

import com.carolinarollergirls.scoreboard.core.FloorPosition;
import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;

public class PositionImpl extends DefaultScoreBoardEventProvider implements Position {
    public PositionImpl(Team t, FloorPosition fp) {
        team = t;
        floorPosition = fp;
        values.put(Value.ID, fp.toString());
        reset();
    }

    public String getProviderName() { return "Position"; }
    public Class<Position> getProviderClass() { return Position.class; }
    public String getProviderId() { return getId(); }
    public ScoreBoardEventProvider getParent() { return team; }
    public List<Class<? extends Property>> getProperties() { return properties; }
    
    public Object valueFromString(PermanentProperty prop, String sValue) {
	synchronized (coreLock) {
	    if (prop == Value.SKATER) { return team.getSkater(sValue); }
	    return super.valueFromString(prop, sValue);
	}
    }
    
    public Object get(PermanentProperty prop) {
	synchronized (coreLock) {
	    if (prop == Value.ID || prop == Value.SKATER || prop == Value.PENALTY_BOX) {
		return super.get(prop);
	    }
	    Skater s = getSkater();
	    if (s != null &&
		    (prop == Value.NAME || prop == Value.NUMBER || prop == Value.FLAGS)) {
		return s.get(Skater.Value.valueOf(((Value)prop).name()));
	    }
	    return null;
	}
    }

    public boolean set(PermanentProperty prop, Object value, Flag flag) {
	synchronized (coreLock) {
	    boolean result = false;
	    if (prop == Value.SKATER && value instanceof String) {
		team.field(team.getSkater((String)value), this);
	    } else if (prop == Value.SKATER || (prop == Value.PENALTY_BOX && (value == Boolean.FALSE || getSkater() != null))) {
		requestBatchStart();
		result = super.set(prop, value, flag);
		if (result && prop == Value.PENALTY_BOX && get(Value.SKATER) != null) {
		    getSkater().set(Skater.Value.PENALTY_BOX, value);
		}
		if (result && prop == Value.SKATER) {
		    Skater s = (Skater)value;
		    scoreBoardChange(new ScoreBoardEvent(this, Value.NAME, s == null ? null : s.getName(), null));
		    scoreBoardChange(new ScoreBoardEvent(this, Value.NUMBER, s == null ? null : s.getNumber(), null));
		    scoreBoardChange(new ScoreBoardEvent(this, Value.FLAGS, s == null ? null : s.getFlags(), null));
		    setPenaltyBox(s == null ? false : s.isPenaltyBox());
		}
		requestBatchEnd();
	    }
	    return result;
	}
    }
    
    public void execute(CommandProperty prop) {
	if (prop == Command.CLEAR) {
	    team.field(null, this);
	}
    }
    
    public Team getTeam() { return team; }

    public String getId() { return (String)get(Value.ID); }

    public FloorPosition getFloorPosition() { return floorPosition; }

    public void reset() {
        synchronized (coreLock) {
            setSkater(null);
            setPenaltyBox(false);
        }
    }

    public Skater getSkater() { return (Skater)get(Value.SKATER); }
    public void setSkater(Skater s) { set(Value.SKATER, s); }

    public boolean isPenaltyBox() { return (Boolean)get(Value.PENALTY_BOX); }
    public void setPenaltyBox(boolean box) { set(Value.PENALTY_BOX, box); }

    protected Team team;
    protected FloorPosition floorPosition;

    protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	add(Value.class);
	add(Command.class);
    }};
}
