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
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public interface Skater extends ScoreBoardEventProvider {
    public SkaterSnapshot snapshot();
    public void restoreSnapshot(SkaterSnapshot s);

    public Team getTeam();
    public String getId();
    public String getName();
    public void setName(String id);
    public String getNumber();
    public void setNumber(String number);
    public Position getPosition();
    public void setPosition(Position position);
    public Role getRole();
    public void setRole(Role role);
    public void setRoleToBase();
    public Role getBaseRole();
    public void setBaseRole(Role base);
    public boolean isPenaltyBox();
    public void setPenaltyBox(boolean box);
    public String getFlags();
    public void setFlags(String flags);
    // A null code removes the penalty.
    public void penalty(String id, String number, int period, int jam, String code);
    public void sortPenalties();

    public enum Value implements PermanentProperty {
	NAME,
	NUMBER,
	POSITION,
	ROLE,
	BASE_ROLE,
	PENALTY_BOX,
	FLAGS;
    }
    public enum Child implements AddRemoveProperty {
	PENALTY;
    }
    
    public static final String FO_EXP_ID = "FO_EXP";

    public static interface Penalty extends ScoreBoardEventProvider {
        public String getUuid();
        public int getPeriod();
        public int getJam();
        public String getCode();

        public enum Value implements PermanentProperty {
            ID,
            PERIOD,
            JAM,
            NUMBER,
            CODE;
        }
    }

    public static interface SkaterSnapshot	{
        public String getId();
        public Position getPosition();
        public Role getRole();
        public Role getBaseRole();
        public boolean isPenaltyBox();
    }
}
