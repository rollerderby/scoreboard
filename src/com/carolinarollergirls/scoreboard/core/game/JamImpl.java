package com.carolinarollergirls.scoreboard.core.game;

import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Jam;
import com.carolinarollergirls.scoreboard.core.interfaces.Penalty;
import com.carolinarollergirls.scoreboard.core.interfaces.Period;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoringTrip;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.core.interfaces.TeamJam;
import com.carolinarollergirls.scoreboard.core.interfaces.Timeout;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.Logger;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class JamImpl extends NumberedScoreBoardEventProviderImpl<Jam> implements Jam {
    public JamImpl(ScoreBoardEventProvider parent, Jam prev) {
        this(parent, prev.getNumber() + 1);
        setPrevious(prev);
    }
    public JamImpl(ScoreBoardEventProvider p, int j) {
        super(p, j, Period.JAM);
        addProperties(props);
        setInverseReference(PENALTY, Penalty.JAM);
        setInverseReference(TIMEOUTS_AFTER, Timeout.PRECEDING_JAM);
        periodNumberListener =
            setCopy(PERIOD_NUMBER, parent, parent instanceof Game ? Game.CURRENT_PERIOD_NUMBER : Period.NUMBER, true);
        game = parent instanceof Game ? (Game) parent : ((Period) parent).getGame();
        add(TEAM_JAM, new TeamJamImpl(this, Team.ID_1));
        add(TEAM_JAM, new TeamJamImpl(this, Team.ID_2));
        addWriteProtection(TEAM_JAM);
        setRecalculated(STAR_PASS)
            .addSource(getTeamJam(Team.ID_1), TeamJam.STAR_PASS)
            .addSource(getTeamJam(Team.ID_2), TeamJam.STAR_PASS);
    }

    @Override
    public void setParent(ScoreBoardEventProvider p) {
        if (parent == p) { return; }
        parent.removeScoreBoardListener(periodNumberListener);
        providers.remove(periodNumberListener);
        parent = p;
        periodNumberListener =
            setCopy(PERIOD_NUMBER, parent, parent instanceof Game ? Game.CURRENT_PERIOD_NUMBER : Period.NUMBER, true);
    }

    @Override
    public void delete(Source source) {
        if (source != Source.UNLINK) {
            if (parent instanceof Period && this == ((Period) parent).getCurrentJam()) {
                parent.set(Period.CURRENT_JAM, getPrevious());
            }
            for (Penalty p : getAll(PENALTY)) { p.set(Penalty.JAM, getNext()); }
            for (Timeout t : getAll(TIMEOUTS_AFTER)) { t.set(Timeout.PRECEDING_JAM, getPrevious()); }
        }
        super.delete(source);
    }

    @Override
    protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == STAR_PASS) { return getTeamJam(Team.ID_1).isStarPass() || getTeamJam(Team.ID_2).isStarPass(); }
        return value;
    }
    @Override
    protected void valueChanged(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == INJURY_CONTINUATION && (Boolean) value == true && source.isFile()) {
            for (TeamJam tj : getAll(TEAM_JAM)) {
                ScoringTrip first = tj.getFirst(TeamJam.SCORING_TRIP);
                if (getPrevious().isImmediateScoring() || first.hasNext() && first.getNext().getNumber() > 2) {
                    // team started on a scoring trip but constructor added initial - remove again
                    tj.remove(TeamJam.SCORING_TRIP, first);
                }
            }
        }
    }

    @Override
    public void execute(Command prop, Source source) {
        synchronized (coreLock) {
            if (prop == DELETE) {
                if (game.isInJam() && (parent == game.getCurrentPeriod()) &&
                    (this == ((Period) parent).getCurrentJam())) {
                    Logger.printMessage("Refusing to delete current Jam.");
                    return;
                }
                if (getTeamJam(Team.ID_1).getJamScore() > 0 || getTeamJam(Team.ID_2).getJamScore() > 0) {
                    Logger.printMessage("Refusing to delete Jam with points. Remove points first.");
                    return;
                }
                if (get(STAR_PASS)) {
                    Logger.printMessage("Refusing to delete Jam with Star Pass. Remove SP first.");
                    return;
                }

                delete(source);
                game.updateTeamJams();
            } else if (prop == INSERT_BEFORE) {
                if (parent instanceof Period) {
                    parent.add(ownType, new JamImpl(parent, getNumber()));
                    game.updateTeamJams();
                } else if (!game.isInJam()) {
                    Period currentPeriod = game.getCurrentPeriod();
                    Jam newJam = new JamImpl(currentPeriod, getNumber());
                    currentPeriod.add(ownType, newJam);
                    currentPeriod.set(Period.CURRENT_JAM, newJam);
                    set(NUMBER, 1, Source.RENUMBER, Flag.CHANGE);
                    game.updateTeamJams();
                }
            } else if (prop == INSERT_TIMEOUT_AFTER) {
                Timeout newTo = new TimeoutImpl(this);
                newTo.set(Timeout.RUNNING, false);
                newTo.set(Timeout.WALLTIME_START, get(WALLTIME_END));
                newTo.set(Timeout.WALLTIME_END, get(WALLTIME_END));
                newTo.set(Timeout.PERIOD_CLOCK_ELAPSED_START, getNext().get(PERIOD_CLOCK_ELAPSED_START));
                newTo.set(Timeout.PERIOD_CLOCK_ELAPSED_END, get(PERIOD_CLOCK_ELAPSED_START));
                newTo.set(Timeout.PERIOD_CLOCK_END,
                          getPeriod().getGame().getClock(Clock.ID_PERIOD).get(Clock.MAXIMUM_TIME) -
                              get(PERIOD_CLOCK_ELAPSED_END));
                getPeriod().add(Period.TIMEOUT, newTo);
            }
        }
    }

    @Override
    public Period getPeriod() {
        return parent instanceof Game ? game.getCurrentPeriod() : (Period) parent;
    }

    @Override
    public boolean isOvertimeJam() {
        return get(OVERTIME);
    }

    @Override
    public boolean isInjuryContinuation() {
        return get(INJURY_CONTINUATION);
    }

    @Override
    public boolean isImmediateScoring() {
        return isOvertimeJam() || getPeriod().isSuddenScoring();
    }

    @Override
    public long getDuration() {
        return get(DURATION);
    }
    public void setDuration(long t) { set(DURATION, t); }

    @Override
    public long getPeriodClockElapsedStart() {
        return get(PERIOD_CLOCK_ELAPSED_START);
    }
    public void setPeriodClockElapsedStart(long t) { set(PERIOD_CLOCK_ELAPSED_START, t); }

    @Override
    public long getPeriodClockElapsedEnd() {
        return get(PERIOD_CLOCK_ELAPSED_END);
    }
    public void setPeriodClockElapsedEnd(long t) { set(PERIOD_CLOCK_ELAPSED_END, t); }

    @Override
    public long getWalltimeStart() {
        return get(WALLTIME_START);
    }
    public void setWalltimeStart(long t) { set(WALLTIME_START, t); }

    @Override
    public long getWalltimeEnd() {
        return get(WALLTIME_END);
    }
    public void setWalltimeEnd(long t) { set(WALLTIME_END, t); }

    @Override
    public TeamJam getTeamJam(String id) {
        return get(TEAM_JAM, id);
    }

    @Override
    public void start() {
        synchronized (coreLock) {
            setPeriodClockElapsedStart(game.getClock(Clock.ID_PERIOD).getTimeElapsed());
            setWalltimeStart(ScoreBoardClock.getInstance().getCurrentWalltime());
            if (isInjuryContinuation()) {
                if (getPrevious().isOvertimeJam()) { set(OVERTIME, true); }
                game.getClock(Clock.ID_JAM).set(Clock.MAXIMUM_TIME, -getPrevious().getDuration(), Flag.CHANGE);
                getTeamJam(Team.ID_1).setupInjuryContinuation();
                getTeamJam(Team.ID_2).setupInjuryContinuation();
            } else if (getPrevious().isInjuryContinuation()) {
                game.getClock(Clock.ID_JAM)
                    .set(Clock.MAXIMUM_TIME, game.getLong(Rule.JAM_DURATION), Source.RECALCULATE);
            }
        }
    }
    @Override
    public void stop() {
        synchronized (coreLock) {
            set(DURATION, game.getClock(Clock.ID_JAM).getTimeElapsed());
            set(PERIOD_CLOCK_ELAPSED_END, game.getClock(Clock.ID_PERIOD).getTimeElapsed());
            set(PERIOD_CLOCK_DISPLAY_END, game.getClock(Clock.ID_PERIOD).getTime());
            set(WALLTIME_END, ScoreBoardClock.getInstance().getCurrentWalltime());
            for (TeamJam tj : getAll(TEAM_JAM)) { tj.getCurrentScoringTrip().set(ScoringTrip.CURRENT, false); }
        }
    }

    private Game game;

    private ScoreBoardListener periodNumberListener;
}
