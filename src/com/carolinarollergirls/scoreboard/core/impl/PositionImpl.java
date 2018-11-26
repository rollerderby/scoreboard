package com.carolinarollergirls.scoreboard.core.impl;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.SkaterNotFoundException;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;

public class PositionImpl extends DefaultScoreBoardEventProvider implements Position {
    public PositionImpl(Team t, String i) {
        team = t;
        id = i;
        reset();
    }

    public String getProviderName() { return "Position"; }
    public Class<Position> getProviderClass() { return Position.class; }
    public String getProviderId() { return getId(); }

    public Team getTeam() { return team; }

    public String getId() { return id; }

    public Position getPosition() { return this; }

    public void reset() {
        synchronized (coreLock) {
            clear();
        }
    }

    public Skater getSkater() { return skater; }
    public void setSkater(String skaterId) throws SkaterNotFoundException {
        synchronized (coreLock) {
            if (skaterId == null || skaterId.equals("")) {
                clear();
            } else {
                getTeam().getSkater(skaterId).setPosition(getId());
            }
        }
    }
    public void _setSkater(String skaterId) throws SkaterNotFoundException {
        synchronized (coreLock) {
            Skater newSkater = getTeam().getSkater(skaterId);
            clear();
            Skater last = skater;
            skater = newSkater;
            _setPenaltyBox(newSkater.isPenaltyBox());
            scoreBoardChange(new ScoreBoardEvent(getPosition(), EVENT_SKATER, skater, last));
        }
    }
    public void clear() {
        synchronized (coreLock) {
            try { skater.setPosition(ID_BENCH); }
            catch ( NullPointerException npE ) { /* Was no skater in this position */ }
        }
    }
    public void _clear() {
        synchronized (coreLock) {
            if (null != skater) {
                Skater last = skater;
                skater = null;
                scoreBoardChange(new ScoreBoardEvent(getPosition(), EVENT_SKATER, skater, last));
            }
            _setPenaltyBox(false);
        }
    }
    public boolean getPenaltyBox() {
        return penaltyBox;
    }
    public void setPenaltyBox(boolean box) {
        synchronized (coreLock) {
            try { skater.setPenaltyBox(box); }
            catch ( NullPointerException npE ) { /* Was no skater in this position */ }
        }
    }
    public void _setPenaltyBox(boolean box) {
        synchronized (coreLock) {
            if (box != penaltyBox) {
                Boolean last = new Boolean(penaltyBox);
                penaltyBox = box;
                scoreBoardChange(new ScoreBoardEvent(getPosition(), EVENT_PENALTY_BOX, new Boolean(penaltyBox), last));
            }
        }
    }

    protected Team team;

    protected static Object coreLock = ScoreBoardImpl.getCoreLock();

    protected String id;

    protected Skater skater = null;
    protected boolean penaltyBox = false;
    protected boolean settingSkaterPosition = false;
}
