package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.List;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public interface Skater extends ScoreBoardEventProvider {
    public void bench();
    public SkaterSnapshot snapshot();
    public void restoreSnapshot(SkaterSnapshot s);

    public Team getTeam();
    public String getId();
    public String getName();
    public void setName(String id);
    public String getNumber();
    public void setNumber(String number);
    public String getPosition();
    public void setPosition(String position) throws PositionNotFoundException;
    public boolean isPenaltyBox();
    public void setPenaltyBox(boolean box);
    public String getFlags();
    public void setFlags(String flags);
    public List<Penalty> getPenalties();
    public Penalty getFOEXPPenalty();
    // A null code removes the penalty.
    public void AddPenalty(String id, boolean foulout_explusion, int period, int jam, String code);

    public static final String EVENT_NAME = "Name";
    public static final String EVENT_NUMBER = "Number";
    public static final String EVENT_POSITION = "Position";
    public static final String EVENT_PENALTY_BOX = "PenaltyBox";
    public static final String EVENT_FLAGS = "Flags";

    public static final String EVENT_PENALTY = "Penalty";
    public static final String EVENT_REMOVE_PENALTY = "RemovePenalty";
    public static final String EVENT_PENALTY_FOEXP = "PenaltyFOEXP";
    public static final String EVENT_PENALTY_REMOVE_FOEXP = "RemovePenaltyFOEXP";
    public static final String EVENT_PENALTY_PERIOD = "Period";
    public static final String EVENT_PENALTY_JAM = "Jam";
    public static final String EVENT_PENALTY_CODE = "Code";

    public static interface Penalty extends ScoreBoardEventProvider {
        public String getId();
        public int getPeriod();
        public int getJam();
        public String getCode();

        public static final String RULE_FO_LIMIT = "Penalties.NumberToFoulout";
    }

    public static interface SkaterSnapshot	{
        public String getId();
        public String getPosition();
        public boolean isPenaltyBox();
    }
}
