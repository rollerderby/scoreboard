package com.carolinarollergirls.scoreboard.core.impl;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.core.Fielding;
import com.carolinarollergirls.scoreboard.core.FloorPosition;
import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;

public class PositionImpl extends ScoreBoardEventProviderImpl implements Position {
    public PositionImpl(Team t, FloorPosition fp) {
	super(t, Value.ID, Team.Child.POSITION, Position.class, Value.class, Command.class);
        team = t;
        floorPosition = fp;
        set(Value.ID, team.getId() + "_" + fp.toString());
        writeProtectionOverride.put(Value.CURRENT_FIELDING, Flag.INTERNAL);
        addReference(new IndirectPropertyReference(this, Value.NAME, this, Value.SKATER, Skater.Value.NAME, true, ""));
        addReference(new IndirectPropertyReference(this, Value.NUMBER, this, Value.SKATER, Skater.Value.NUMBER, true, ""));
        addReference(new IndirectPropertyReference(this, Value.FLAGS, this, Value.SKATER, Skater.Value.FLAGS, true, ""));
        addReference(new IndirectPropertyReference(this, Value.SKATER, this, Value.CURRENT_FIELDING, Fielding.Value.SKATER, false, null));
        addReference(new IndirectPropertyReference(this, Value.PENALTY_BOX, this, Value.CURRENT_FIELDING, Fielding.Value.PENALTY_BOX, false, false));
    }

    public String getProviderId() { return floorPosition.toString(); }

    public void execute(CommandProperty prop) {
	if (prop == Command.CLEAR) {
	    set(Value.SKATER, null);
	}
    }
    
    public Team getTeam() { return team; }

    public FloorPosition getFloorPosition() { return floorPosition; }

    public void updateCurrentFielding() {
        synchronized (coreLock) {
            setCurrentFielding(team.getRunningOrUpcomingTeamJam().getFielding(floorPosition));
            if (getSkater() != null) { getSkater().set(Value.CURRENT_FIELDING, getCurrentFielding(), Flag.INTERNAL); }
        }
    }

    public Skater getSkater() { return (Skater)get(Value.SKATER); }
    public void setSkater(Skater s) { set(Value.SKATER, s); }

    public Fielding getCurrentFielding() { return (Fielding)get(Value.CURRENT_FIELDING); }
    public void setCurrentFielding(Fielding f) { set(Value.CURRENT_FIELDING, f, Flag.INTERNAL); }

    public boolean isPenaltyBox() { return (Boolean)get(Value.PENALTY_BOX); }
    public void setPenaltyBox(boolean box) { set(Value.PENALTY_BOX, box); }

    protected Team team;
    protected FloorPosition floorPosition;
}
