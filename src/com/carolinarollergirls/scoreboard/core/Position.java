package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.event.CommandProperty;
import com.carolinarollergirls.scoreboard.event.PermanentProperty;
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

    PermanentProperty<Fielding> CURRENT_FIELDING = new PermanentProperty<>(Fielding.class, "CurrentFielding", null);
    PermanentProperty<String> CURRENT_BOX_SYMBOLS = new PermanentProperty<>(String.class, "CurrentBoxSymbols", "");
    PermanentProperty<String> ANNOTATION = new PermanentProperty<>(String.class, "Annotation", "");
    PermanentProperty<Skater> SKATER = new PermanentProperty<>(Skater.class, "Skater", null);
    PermanentProperty<String> NAME = new PermanentProperty<>(String.class, "Name", "");
    PermanentProperty<String> ROSTER_NUMBER = new PermanentProperty<>(String.class, "RosterNumber", "");
    PermanentProperty<String> FLAGS = new PermanentProperty<>(String.class, "Flags", "");
    PermanentProperty<Boolean> PENALTY_BOX = new PermanentProperty<>(Boolean.class, "PenaltyBox", false);

    CommandProperty CLEAR = new CommandProperty("Clear");
}
