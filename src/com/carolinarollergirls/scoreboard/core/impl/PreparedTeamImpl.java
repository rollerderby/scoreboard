package com.carolinarollergirls.scoreboard.core.impl;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.core.PreparedTeam;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;

public class PreparedTeamImpl extends ScoreBoardEventProviderImpl<PreparedTeam> implements PreparedTeam {
    PreparedTeamImpl(ScoreBoard parent, String id) {
        super(parent, id, ScoreBoard.PREPARED_TEAM);
        addProperties(NAME, LOGO, ALTERNATE_NAME, COLOR, SKATER);
    }

    @Override
    public ScoreBoardEventProvider create(Child<?> prop, String id, Source source) {
        synchronized (coreLock) {
            if (prop == PreparedTeam.SKATER) {
                return new PreparedTeamSkaterImpl(this, id);
            }
            return null;
        }
    }

    public static class PreparedTeamSkaterImpl extends ScoreBoardEventProviderImpl<PreparedTeamSkater>
            implements PreparedTeamSkater {
        PreparedTeamSkaterImpl(PreparedTeam parent, String id) {
            super(parent, id, PreparedTeam.SKATER);
            addProperties(NAME, ROSTER_NUMBER, FLAGS);
        }
    }
}
