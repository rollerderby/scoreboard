package com.carolinarollergirls.scoreboard.core.current;

import com.carolinarollergirls.scoreboard.core.game.GameImpl;
import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentClock;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentTeam;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.PreparedTeam;
import com.carolinarollergirls.scoreboard.core.interfaces.Rulesets;
import com.carolinarollergirls.scoreboard.core.interfaces.Rulesets.Ruleset;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.Value;

public class CurrentGameImpl extends ScoreBoardEventProviderImpl<CurrentGame> implements CurrentGame {
    public CurrentGameImpl(ScoreBoard sb) {
        super(sb, "", ScoreBoard.CURRENT_GAME);
        addProperties(GAME, Game.NAME, Game.CURRENT_PERIOD_NUMBER, Game.UPCOMING_JAM_NUMBER, Game.IN_PERIOD,
                Game.IN_JAM, Game.IN_OVERTIME, Game.OFFICIAL_SCORE, Game.CURRENT_TIMEOUT, Game.TIMEOUT_OWNER,
                Game.OFFICIAL_REVIEW, Game.NO_MORE_JAM, Game.RULESET, Game.RULESET_NAME, CLOCK, TEAM, Game.RULE,
                Game.LABEL, Game.START_JAM, Game.STOP_JAM, Game.TIMEOUT, Game.CLOCK_UNDO, Game.CLOCK_REPLACE,
                Game.START_OVERTIME, Game.OFFICIAL_TIMEOUT);
        setCopy(Game.NAME, this, GAME, Game.NAME, true);
        setCopy(Game.CURRENT_PERIOD_NUMBER, this, GAME, Game.CURRENT_PERIOD_NUMBER, true);
        setCopy(Game.UPCOMING_JAM_NUMBER, this, GAME, Game.UPCOMING_JAM_NUMBER, true);
        setCopy(Game.IN_PERIOD, this, GAME, Game.IN_PERIOD, true);
        setCopy(Game.IN_JAM, this, GAME, Game.IN_JAM, true);
        setCopy(Game.IN_OVERTIME, this, GAME, Game.IN_OVERTIME, true);
        setCopy(Game.OFFICIAL_SCORE, this, GAME, Game.OFFICIAL_SCORE, true);
        setCopy(Game.CURRENT_TIMEOUT, this, GAME, Game.CURRENT_TIMEOUT, true);
        setCopy(Game.TIMEOUT_OWNER, this, GAME, Game.TIMEOUT_OWNER, true);
        setCopy(Game.OFFICIAL_REVIEW, this, GAME, Game.OFFICIAL_REVIEW, true);
        setCopy(Game.NO_MORE_JAM, this, GAME, Game.NO_MORE_JAM, true);
        setCopy(Game.RULESET, this, GAME, Game.RULESET, true);
        setCopy(Game.RULESET_NAME, this, GAME, Game.RULESET_NAME, true);
        setCopy(Game.RULE, this, GAME, Game.RULE, true);
        setCopy(Game.LABEL, this, GAME, Game.LABEL, true);
        setCopy(Game.CURRENT_PERIOD_NUMBER, this, GAME, Game.CURRENT_PERIOD_NUMBER, true);
        add(TEAM, new CurrentTeamImpl(this, Team.ID_1));
        add(TEAM, new CurrentTeamImpl(this, Team.ID_2));
        addWriteProtection(TEAM);
        add(CLOCK, new CurrentClockImpl(this, Clock.ID_PERIOD));
        add(CLOCK, new CurrentClockImpl(this, Clock.ID_JAM));
        add(CLOCK, new CurrentClockImpl(this, Clock.ID_LINEUP));
        add(CLOCK, new CurrentClockImpl(this, Clock.ID_TIMEOUT));
        add(CLOCK, new CurrentClockImpl(this, Clock.ID_INTERMISSION));
        addWriteProtection(CLOCK);
    }

    @Override
    protected void valueChanged(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == GAME) {
            if (value != null) {
                Game g = (Game) value;
                for (CurrentTeam t : getAll(TEAM)) {
                    t.load(g.getTeam(t.getProviderId()));
                }
                for (CurrentClock c : getAll(CLOCK)) {
                    c.load(g.getClock(c.getProviderId()));
                }
            }
        }
    }

    @Override
    public void execute(Command prop, Source source) { get(GAME).execute(prop, source); }

    @Override
    public void postAutosaveUpdate() {
        synchronized (coreLock) {
            if (get(GAME) == null) {
                // autosave did not contain a current game - create one ad hoc
                PreparedTeam t1 = scoreBoard.getOrCreate(ScoreBoard.PREPARED_TEAM, "Black");
                t1.set(Team.TEAM_NAME, "Black");
                t1.set(Team.UNIFORM_COLOR, "Black");
                PreparedTeam t2 = scoreBoard.getOrCreate(ScoreBoard.PREPARED_TEAM, "White");
                t2.set(Team.TEAM_NAME, "White");
                t1.set(Team.UNIFORM_COLOR, "White");
                Ruleset rs = scoreBoard.getRulesets().getRuleset(Rulesets.ROOT_ID);
                Game g = new GameImpl(scoreBoard, t1, t2, rs);
                scoreBoard.add(ScoreBoard.GAME, g);
                load(g);
            }
        }
    }

    @Override
    public void load(Game g) { set(GAME, g); }
}
