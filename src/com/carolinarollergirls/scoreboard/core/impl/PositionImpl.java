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
        super(t, Value.ID, t.getId() + "_" + fp.toString(), Team.Child.POSITION, Position.class, Value.class, Command.class);
        floorPosition = fp;
        setCopy(Value.NAME, this, Value.SKATER, Skater.Value.NAME, true);
        setCopy(Value.NUMBER, this, Value.SKATER, Skater.Value.NUMBER, true);
        setCopy(Value.FLAGS, this, Value.SKATER, Skater.Value.FLAGS, true);
        setCopy(Value.SKATER, this, Value.CURRENT_FIELDING, Fielding.Value.SKATER, false);
        setCopy(Value.PENALTY_BOX, this, Value.CURRENT_FIELDING, Fielding.Value.PENALTY_BOX, false);
        setCopy(Value.CURRENT_BOX_SYMBOLS, this, Value.CURRENT_FIELDING, Fielding.Value.BOX_TRIP_SYMBOLS, true);
    }

    public String getProviderId() { return floorPosition.toString(); }

    public void execute(CommandProperty prop) {
        if (prop == Command.CLEAR) {
            set(Value.SKATER, null);
        }
    }

    public Team getTeam() { return (Team)parent; }

    public FloorPosition getFloorPosition() { return floorPosition; }

    public void reset() { setCurrentFielding(null); }
    
    public void updateCurrentFielding() {
        synchronized (coreLock) {
            setCurrentFielding(getTeam().getRunningOrUpcomingTeamJam().getFielding(floorPosition));
            if (getSkater() != null) { getSkater().set(Value.CURRENT_FIELDING, getCurrentFielding()); }
        }
    }

    public Skater getSkater() { return (Skater)get(Value.SKATER); }
    public void setSkater(Skater s) { set(Value.SKATER, s); }

    public Fielding getCurrentFielding() { return (Fielding)get(Value.CURRENT_FIELDING); }
    public void setCurrentFielding(Fielding f) { set(Value.CURRENT_FIELDING, f); }

    public boolean isPenaltyBox() { return (Boolean)get(Value.PENALTY_BOX); }
    public void setPenaltyBox(boolean box) { set(Value.PENALTY_BOX, box); }

    protected FloorPosition floorPosition;
}
