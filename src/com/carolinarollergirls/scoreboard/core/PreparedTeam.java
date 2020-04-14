package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.event.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

// Roster for teams for loading in for games.
public interface PreparedTeam extends ScoreBoardEventProvider {
    PermanentProperty<String> NAME = new PermanentProperty<>(String.class, "Name", "");
    PermanentProperty<String> LOGO = new PermanentProperty<>(String.class, "Logo", "");

    AddRemoveProperty<ValWithId> ALTERNATE_NAME = new AddRemoveProperty<>(ValWithId.class, "AlternateName");
    AddRemoveProperty<ValWithId> COLOR = new AddRemoveProperty<>(ValWithId.class, "Color");
    AddRemoveProperty<PreparedTeamSkater> SKATER = new AddRemoveProperty<>(PreparedTeamSkater.class, "Skater");

    public static interface PreparedTeamSkater extends ScoreBoardEventProvider {

        @SuppressWarnings("hiding")
        PermanentProperty<String> NAME = new PermanentProperty<>(String.class, "Name", "");
        PermanentProperty<String> ROSTER_NUMBER = new PermanentProperty<>(String.class, "RosterNumber", "");
        PermanentProperty<String> FLAGS = new PermanentProperty<>(String.class, "Flags", "");
    }
}
