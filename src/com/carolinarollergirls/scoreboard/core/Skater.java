package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.NumberedProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;

import java.util.List;

import com.carolinarollergirls.scoreboard.event.OrderedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public interface Skater extends ScoreBoardEventProvider {
    public int compareTo(Skater other);

    public Team getTeam();
    public String getName();
    public void setName(String id);
    public String getNumber();
    public void setNumber(String number);
    public Fielding getFielding(TeamJam teamJam);
    public Fielding getCurrentFielding();
    public void removeCurrentFielding();
    public void updateFielding(TeamJam teamJam);
    public Position getPosition();
    public void setPosition(Position position);
    public Role getRole();
    public Role getRole(TeamJam tj);
    public void setRole(Role role);
    public void setRoleToBase();
    public Role getBaseRole();
    public void setBaseRole(Role base);
    public void updateEligibility();
    public boolean isPenaltyBox();
    public void setPenaltyBox(boolean box);
    public String getFlags();
    public void setFlags(String flags);
    public Penalty getPenalty(String num);
    public List<Penalty> getUnservedPenalties();
    public boolean hasUnservedPenalties();

    public enum Value implements PermanentProperty {
        NAME(String.class, ""),
        ROSTER_NUMBER(String.class, ""),
        CURRENT_FIELDING(Fielding.class, null),
        CURRENT_BOX_SYMBOLS(String.class, ""),
        POSITION(Position.class, null),
        ROLE(Role.class, null),
        BASE_ROLE(Role.class, null),
        PENALTY_BOX(Boolean.class, false),
        FLAGS(String.class, "");

        private Value(Class<?> t, Object dv) { type = t; defaultValue = dv; }
        private final Class<?> type;
        private final Object defaultValue;
        @Override
        public Class<?> getType() { return type; }
        @Override
        public Object getDefaultValue() { return defaultValue; }
    }
    public enum Child implements AddRemoveProperty {
        FIELDING(Fielding.class);

        private Child(Class<? extends ValueWithId> t) { type = t; }
        private final Class<? extends ValueWithId> type;
        @Override
        public Class<? extends ValueWithId> getType() { return type; }
    }
    public enum NChild implements NumberedProperty {
        PENALTY(Penalty.class);

        private NChild(Class<? extends OrderedScoreBoardEventProvider<?>> t) { type = t; }
        private final Class<? extends OrderedScoreBoardEventProvider<?>> type;
        @Override
        public Class<? extends OrderedScoreBoardEventProvider<?>> getType() { return type; }
    }

    public static final String FO_EXP_ID = "0";
}
