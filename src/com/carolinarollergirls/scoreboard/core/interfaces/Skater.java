package com.carolinarollergirls.scoreboard.core.interfaces;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.List;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.NumberedChild;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface Skater extends ScoreBoardEventProvider {
    public int compareTo(Skater other);

    public CurrentSkater getCurrentSkater();

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

    Value<String> NAME = new Value<>(String.class, "Name", "");
    Value<String> ROSTER_NUMBER = new Value<>(String.class, "RosterNumber", "");
    Value<Fielding> CURRENT_FIELDING = new Value<>(Fielding.class, "CurrentFielding", null);
    Value<String> CURRENT_BOX_SYMBOLS = new Value<>(String.class, "CurrentBoxSymbols", "");
    Value<Position> POSITION = new Value<>(Position.class, "Position", null);
    Value<Role> ROLE = new Value<>(Role.class, "Role", null);
    Value<Role> BASE_ROLE = new Value<>(Role.class, "BaseRole", null);
    Value<Boolean> PENALTY_BOX = new Value<>(Boolean.class, "PenaltyBox", false);
    Value<String> FLAGS = new Value<>(String.class, "Flags", "");

    Child<Fielding> FIELDING = new Child<>(Fielding.class, "Fielding");

    NumberedChild<Penalty> PENALTY = new NumberedChild<>(Penalty.class, "Penalty");

    public static final String FO_EXP_ID = "0";
}
