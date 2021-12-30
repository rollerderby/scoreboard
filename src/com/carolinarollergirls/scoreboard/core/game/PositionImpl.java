package com.carolinarollergirls.scoreboard.core.game;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.core.interfaces.Fielding;
import com.carolinarollergirls.scoreboard.core.interfaces.FloorPosition;
import com.carolinarollergirls.scoreboard.core.interfaces.Position;
import com.carolinarollergirls.scoreboard.core.interfaces.Skater;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;

public class PositionImpl extends ScoreBoardEventProviderImpl<Position> implements Position {
    public PositionImpl(Team t, FloorPosition fp) {
        super(t, t.getId() + "_" + fp.toString(), Team.POSITION);
        addProperties(CURRENT_FIELDING, CURRENT_BOX_SYMBOLS, CURRENT_PENALTIES, ANNOTATION, SKATER, NAME, ROSTER_NUMBER,
                      FLAGS, PENALTY_BOX, CLEAR);
        floorPosition = fp;
        setCopy(NAME, this, SKATER, Skater.NAME, true);
        setCopy(ROSTER_NUMBER, this, SKATER, Skater.ROSTER_NUMBER, true);
        setCopy(FLAGS, this, SKATER, Skater.FLAGS, true);
        setCopy(SKATER, this, CURRENT_FIELDING, Fielding.SKATER, false);
        setCopy(PENALTY_BOX, this, CURRENT_FIELDING, Fielding.PENALTY_BOX, false);
        setCopy(CURRENT_BOX_SYMBOLS, this, CURRENT_FIELDING, Fielding.BOX_TRIP_SYMBOLS, true);
        setCopy(CURRENT_PENALTIES, this, SKATER, Skater.CURRENT_PENALTIES, true);
        setCopy(ANNOTATION, this, CURRENT_FIELDING, Fielding.ANNOTATION, true);
    }
    public PositionImpl(PositionImpl cloned, ScoreBoardEventProvider root) {
        super(cloned, root);
        floorPosition = cloned.floorPosition;
    }

    @Override
    public ScoreBoardEventProvider clone(ScoreBoardEventProvider root) {
        return new PositionImpl(this, root);
    }

    @Override
    public String getProviderId() {
        return floorPosition.toString();
    }

    @Override
    public void execute(Command prop, Source source) {
        if (prop == CLEAR) { set(SKATER, null); }
    }

    @Override
    public Team getTeam() {
        return (Team) parent;
    }

    @Override
    public FloorPosition getFloorPosition() {
        return floorPosition;
    }

    @Override
    public void updateCurrentFielding() {
        synchronized (coreLock) {
            setCurrentFielding(getTeam().getRunningOrUpcomingTeamJam().getFielding(floorPosition));
        }
    }

    @Override
    public Skater getSkater() {
        return get(SKATER);
    }
    @Override
    public void setSkater(Skater s) {
        set(SKATER, s);
    }

    @Override
    public Fielding getCurrentFielding() {
        return get(CURRENT_FIELDING);
    }
    @Override
    public void setCurrentFielding(Fielding f) {
        set(CURRENT_FIELDING, f);
    }

    @Override
    public boolean isPenaltyBox() {
        return get(PENALTY_BOX);
    }
    @Override
    public void setPenaltyBox(boolean box) {
        set(PENALTY_BOX, box);
    }

    protected FloorPosition floorPosition;
}
