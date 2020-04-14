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

import com.carolinarollergirls.scoreboard.event.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.NumberedProperty;
import com.carolinarollergirls.scoreboard.event.PermanentProperty;
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

    PermanentProperty<String> NAME = new PermanentProperty<>(String.class, "Name", "");
    PermanentProperty<String> ROSTER_NUMBER = new PermanentProperty<>(String.class, "RosterNumber", "");
    PermanentProperty<Fielding> CURRENT_FIELDING = new PermanentProperty<>(Fielding.class, "Fielding", null);
    PermanentProperty<String> CURRENT_BOX_SYMBOLS = new PermanentProperty<>(String.class, "CurrentBoxSymbols", "");
    PermanentProperty<Position> POSITION = new PermanentProperty<>(Position.class, "Position", null);
    PermanentProperty<Role> ROLE = new PermanentProperty<>(Role.class, "Role", null);
    PermanentProperty<Role> BASE_ROLE = new PermanentProperty<>(Role.class, "BaseRole", null);
    PermanentProperty<Boolean> PENALTY_BOX = new PermanentProperty<>(Boolean.class, "PenaltyBox", false);
    PermanentProperty<String> FLAGS = new PermanentProperty<>(String.class, "Flags", "");

    AddRemoveProperty<Fielding> FIELDING = new AddRemoveProperty<>(Fielding.class, "Fielding");

    NumberedProperty<Penalty> PENALTY = new NumberedProperty<>(Penalty.class, "Penalty");

    public static final String FO_EXP_ID = "0";
}
