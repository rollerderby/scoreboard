package com.carolinarollergirls.scoreboard.core.interfaces;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface Position extends ScoreBoardEventProvider {
    public void updateCurrentFielding();

    public Team getTeam();
    public FloorPosition getFloorPosition();
    public Skater getSkater();
    public void setSkater(Skater s);
    public Fielding getCurrentFielding();
    public void setCurrentFielding(Fielding f);
    public boolean isPenaltyBox();
    public void setPenaltyBox(boolean box);

    Value<Fielding> CURRENT_FIELDING = new Value<>(Fielding.class, "CurrentFielding", null);
    Value<String> CURRENT_BOX_SYMBOLS = new Value<>(String.class, "CurrentBoxSymbols", "");
    Value<String> ANNOTATION = new Value<>(String.class, "Annotation", "");
    Value<Skater> SKATER = new Value<>(Skater.class, "Skater", null);
    Value<String> NAME = new Value<>(String.class, "Name", "");
    Value<String> ROSTER_NUMBER = new Value<>(String.class, "RosterNumber", "");
    Value<String> FLAGS = new Value<>(String.class, "Flags", "");
    Value<Boolean> PENALTY_BOX = new Value<>(Boolean.class, "PenaltyBox", false);

    Command CLEAR = new Command("Clear");
}
