package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public interface Position extends ScoreBoardEventProvider {
    public void reset();
    public void updateCurrentFielding();

    public Team getTeam();
    public FloorPosition getFloorPosition();
    public Skater getSkater();
    public void setSkater(Skater s);
    public Fielding getCurrentFielding();
    public void setCurrentFielding(Fielding f);
    public boolean isPenaltyBox();
    public void setPenaltyBox(boolean box);

    public enum Value implements PermanentProperty {
        ID(String.class, ""),
        CURRENT_FIELDING(Fielding.class, null),
        CURRENT_BOX_SYMBOLS(String.class, ""),
        SKATER(Skater.class, null),
        NAME(String.class, ""),
        NUMBER(String.class, ""),
        FLAGS(String.class, ""),
        PENALTY_BOX(Boolean.class, false);

        private Value(Class<?> t, Object dv) { type = t; defaultValue = dv; }
        private final Class<?> type;
        private final Object defaultValue;
        public Class<?> getType() { return type; }
        public Object getDefaultValue() { return defaultValue; }
    }
    public enum Command implements CommandProperty {
        CLEAR;
        
        public Class<Boolean> getType() { return Boolean.class; }
    }
}
