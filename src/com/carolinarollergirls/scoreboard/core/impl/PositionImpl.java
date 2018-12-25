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
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;

public class PositionImpl extends DefaultScoreBoardEventProvider implements Position {
    public PositionImpl(Team t, FloorPosition fp) {
        team = t;
        floorPosition = fp;
        id = fp.toString();
        reset();
    }

    public String getProviderName() { return "Position"; }
    public Class<Position> getProviderClass() { return Position.class; }
    public String getProviderId() { return getId(); }
    public ScoreBoardEventProvider getParent() { return team; }
    public List<Class<? extends Property>> getProperties() { return properties; }

    public Team getTeam() { return team; }

    public String getId() { return id; }

    public FloorPosition getFloorPosition() { return floorPosition; }

    public void reset() {
        synchronized (coreLock) {
            setSkater(null);
        }
    }

    public Skater getSkater() { return skater; }
    public void setSkater(Skater s) {
        synchronized (coreLock) {
            if (s == skater) { return; }
            Skater last = skater;
            skater = s;
            setPenaltyBox(s == null ? false : s.isPenaltyBox());
            scoreBoardChange(new ScoreBoardEvent(this, Value.SKATER, skater, last));
        }
    }

    public boolean isPenaltyBox() {
        return penaltyBox;
    }
    public void setPenaltyBox(boolean box) {
        synchronized (coreLock) {
            if (box != penaltyBox && (skater != null || !box)) {
                Boolean last = new Boolean(penaltyBox);
                penaltyBox = box;
                scoreBoardChange(new ScoreBoardEvent(this, Value.PENALTY_BOX, new Boolean(penaltyBox), last));
                if (skater != null) {
                    skater.setPenaltyBox(box);
                }
            }
        }
    }

    protected Team team;

    protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	add(Value.class);
	add(Command.class);
    }};

    protected static Object coreLock = ScoreBoardImpl.getCoreLock();

    protected String id;
    protected FloorPosition floorPosition;

    protected Skater skater = null;
    protected boolean penaltyBox = false;
    protected boolean settingSkaterPosition = false;
}
