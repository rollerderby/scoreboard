package com.carolinarollergirls.scoreboard.core.prepared;

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
        addProperties(props);
        addProperties(Team.preparedProps);
        setRecalculated(Team.FULL_NAME).addSource(this, Team.LEAGUE_NAME).addSource(this, Team.TEAM_NAME);
        setRecalculated(Team.DISPLAY_NAME)
            .addSource(this, Team.LEAGUE_NAME)
            .addSource(this, Team.TEAM_NAME)
            .addSource(this, Team.FULL_NAME)
            .addSource(scoreBoard.getSettings(), Settings.SETTING);
        set(Team.FULL_NAME, "");
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
    public ScoreBoardEventProvider create(Child<? extends ScoreBoardEventProvider> prop, String id, Source source) {
        synchronized (coreLock) {
            if (prop == PreparedTeam.SKATER) { return new PreparedTeamSkaterImpl(this, id); }
            return null;
        }
    }

    public static class PreparedTeamSkaterImpl
        extends ScoreBoardEventProviderImpl<PreparedSkater> implements PreparedSkater {
        public PreparedTeamSkaterImpl(PreparedTeam parent, String id) {
            super(parent, id, PreparedTeam.SKATER);
            addProperties(Skater.preparedProps);
        }
    }
}
