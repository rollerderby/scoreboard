package com.carolinarollergirls.scoreboard.core.prepared;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.core.interfaces.PreparedTeam;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.Settings;
import com.carolinarollergirls.scoreboard.core.interfaces.Skater;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.Value;

public class PreparedTeamImpl extends ScoreBoardEventProviderImpl<PreparedTeam> implements PreparedTeam {
    public PreparedTeamImpl(ScoreBoard parent, String id) {
        super(parent, id, ScoreBoard.PREPARED_TEAM);
        addProperties(Team.FULL_NAME, Team.LEAGUE_NAME, Team.TEAM_NAME, Team.DISPLAY_NAME, Team.UNIFORM_COLOR,
                      Team.LOGO, Team.ALTERNATE_NAME, Team.COLOR, SKATER);
        setRecalculated(Team.FULL_NAME).addSource(this, Team.LEAGUE_NAME).addSource(this, Team.TEAM_NAME);
        setRecalculated(Team.DISPLAY_NAME)
            .addSource(this, Team.LEAGUE_NAME)
            .addSource(this, Team.TEAM_NAME)
            .addSource(scoreBoard.getSettings(), Settings.SETTING);
    }
    public PreparedTeamImpl(PreparedTeamImpl cloned, ScoreBoardEventProvider root) { super(cloned, root); }

    @Override
    public ScoreBoardEventProvider clone(ScoreBoardEventProvider root) {
        return new PreparedTeamImpl(this, root);
    }

    @Override
    protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == Team.FULL_NAME) {
            String league = get(Team.LEAGUE_NAME);
            String team = get(Team.TEAM_NAME);
            String in = value == null ? "" : (String) value;
            if (!"".equals(league)) {
                if (!"".equals(team)) {
                    if (league.equals(team)) {
                        return league;
                    } else {
                        return league + " - " + team;
                    }
                } else {
                    return league;
                }
            } else {
                if (!"".equals(team)) {
                    return team;
                } else if (!"".equals(in)) {
                    return in;
                } else {
                    return "Unnamed Team";
                }
            }
        }
        if (prop == Team.DISPLAY_NAME) {
            String setting = scoreBoard.getSettings().get(Team.SETTING_DISPLAY_NAME);
            if (Team.OPTION_TEAM_NAME.equals(setting) && !"".equals(get(Team.TEAM_NAME))) {
                return get(Team.TEAM_NAME);
            } else if (!Team.OPTION_FULL_NAME.equals(setting) && !"".equals(get(Team.LEAGUE_NAME))) {
                return get(Team.LEAGUE_NAME);
            } else {
                return get(Team.FULL_NAME);
            }
        }
        return value;
    }

    @Override
    public ScoreBoardEventProvider create(Child<?> prop, String id, Source source) {
        synchronized (coreLock) {
            if (prop == PreparedTeam.SKATER) { return new PreparedTeamSkaterImpl(this, id); }
            return null;
        }
    }

    public static class PreparedTeamSkaterImpl
        extends ScoreBoardEventProviderImpl<PreparedSkater> implements PreparedSkater {
        public PreparedTeamSkaterImpl(PreparedTeam parent, String id) {
            super(parent, id, PreparedTeam.SKATER);
            addProperties(Skater.NAME, Skater.ROSTER_NUMBER, Skater.FLAGS);
        }
        public PreparedTeamSkaterImpl(PreparedTeamSkaterImpl cloned, ScoreBoardEventProvider root) {
            super(cloned, root);
        }

        @Override
        public ScoreBoardEventProvider clone(ScoreBoardEventProvider root) {
            return new PreparedTeamSkaterImpl(this, root);
        }
    }
}
