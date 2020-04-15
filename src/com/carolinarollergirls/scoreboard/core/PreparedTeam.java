package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

// Roster for teams for loading in for games.
public interface PreparedTeam extends ScoreBoardEventProvider {
    Value<String> NAME = new Value<>(String.class, "Name", "");
    Value<String> LOGO = new Value<>(String.class, "Logo", "");

    Child<ValWithId> ALTERNATE_NAME = new Child<>(ValWithId.class, "AlternateName");
    Child<ValWithId> COLOR = new Child<>(ValWithId.class, "Color");
    Child<PreparedTeamSkater> SKATER = new Child<>(PreparedTeamSkater.class, "Skater");

    public static interface PreparedTeamSkater extends ScoreBoardEventProvider {

        @SuppressWarnings("hiding")
        Value<String> NAME = new Value<>(String.class, "Name", "");
        Value<String> ROSTER_NUMBER = new Value<>(String.class, "RosterNumber", "");
        Value<String> NUMBER_OLD = new Value<>(String.class, "Number", ""); // for compatibility with older autosaves
                                                                            // and exports
        Value<String> FLAGS = new Value<>(String.class, "Flags", "");
    }
}
