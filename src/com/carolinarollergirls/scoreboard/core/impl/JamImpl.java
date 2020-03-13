package com.carolinarollergirls.scoreboard.core.impl;

import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Penalty;
import com.carolinarollergirls.scoreboard.core.Period;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.core.Timeout;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class JamImpl extends NumberedScoreBoardEventProviderImpl<Jam> implements Jam {
    public JamImpl(ScoreBoardEventProvider parent, Jam prev) {
        this(parent, prev.getNumber() + 1);
        setPrevious(prev);
    }
    public JamImpl(ScoreBoardEventProvider p, int j) {
        super(p, j, Period.NChild.JAM, Jam.class, Value.class, Child.class, Command.class);
        setInverseReference(Child.PENALTY, Penalty.Value.JAM);
        setInverseReference(Child.TIMEOUTS_AFTER, Timeout.Value.PRECEDING_JAM);
        periodNumberListener = setCopy(Value.PERIOD_NUMBER, parent,
                parent instanceof ScoreBoard ? ScoreBoard.Value.CURRENT_PERIOD_NUMBER : IValue.NUMBER, true);
        add(Child.TEAM_JAM, new TeamJamImpl(this, Team.ID_1));
        add(Child.TEAM_JAM, new TeamJamImpl(this, Team.ID_2));
        addWriteProtection(Child.TEAM_JAM);
        setRecalculated(Value.STAR_PASS).addSource(getTeamJam(Team.ID_1), TeamJam.Value.STAR_PASS)
                .addSource(getTeamJam(Team.ID_2), TeamJam.Value.STAR_PASS);
    }

    @Override
    public void setParent(ScoreBoardEventProvider p) {
        if (parent == p) { return; }
        parent.removeScoreBoardListener(periodNumberListener);
        providers.remove(periodNumberListener);
        parent = p;
        periodNumberListener = setCopy(Value.PERIOD_NUMBER, parent,
                parent instanceof ScoreBoard ? ScoreBoard.Value.CURRENT_PERIOD_NUMBER : IValue.NUMBER, true);
    }

    @Override
    public void delete(Source source) {
        if (source != Source.UNLINK) {
            if (parent instanceof Period && this == ((Period) parent).getCurrentJam()) {
                parent.set(Period.Value.CURRENT_JAM, getPrevious());
            }
            for (ValueWithId p : getAll(Child.PENALTY)) {
                ((Penalty) p).set(Penalty.Value.JAM, getNext());
            }
            for (ValueWithId p : getAll(Child.TIMEOUTS_AFTER)) {
                ((Timeout) p).set(Timeout.Value.PRECEDING_JAM, getPrevious());
            }
        }
        super.delete(source);
    }

    @Override
    protected Object computeValue(PermanentProperty prop, Object value, Object last, Source source, Flag flag) {
        if (prop == Value.STAR_PASS) {
            return getTeamJam(Team.ID_1).isStarPass() || getTeamJam(Team.ID_2).isStarPass();
        }
        return value;
    }

    @Override
    public void execute(CommandProperty prop, Source source) {
        synchronized (coreLock) {
            switch ((Command) prop) {
            case DELETE:
                if (scoreBoard.isInJam() && (parent == scoreBoard.getCurrentPeriod())
                        && (this == ((Period) parent).getCurrentJam())) {
                    break;
                }
                delete(source);
                scoreBoard.updateTeamJams();
                break;
            case INSERT_BEFORE:
                if (parent instanceof Period) {
                    parent.add(ownType, new JamImpl(parent, getNumber()));
                    scoreBoard.updateTeamJams();
                } else if (!scoreBoard.isInJam()) {
                    Period currentPeriod = scoreBoard.getCurrentPeriod();
                    Jam newJam = new JamImpl(currentPeriod, getNumber());
                    currentPeriod.add(ownType, newJam);
                    currentPeriod.set(Period.Value.CURRENT_JAM, newJam);
                    set(IValue.NUMBER, 1, Source.RENUMBER, Flag.CHANGE);
                    scoreBoard.updateTeamJams();
                }
                break;
            }
        }
    }

    @Override
    public boolean isOvertimeJam() { return (Boolean) get(Value.OVERTIME); }

    @Override
    public long getDuration() { return (Long) get(Value.DURATION); }
    public void setDuration(long t) { set(Value.DURATION, t); }

    @Override
    public long getPeriodClockElapsedStart() { return (Long) get(Value.PERIOD_CLOCK_ELAPSED_START); }
    public void setPeriodClockElapsedStart(long t) { set(Value.PERIOD_CLOCK_ELAPSED_START, t); }

    @Override
    public long getPeriodClockElapsedEnd() { return (Long) get(Value.PERIOD_CLOCK_ELAPSED_END); }
    public void setPeriodClockElapsedEnd(long t) { set(Value.PERIOD_CLOCK_ELAPSED_END, t); }

    @Override
    public long getWalltimeStart() { return (Long) get(Value.WALLTIME_START); }
    public void setWalltimeStart(long t) { set(Value.WALLTIME_START, t); }

    @Override
    public long getWalltimeEnd() { return (Long) get(Value.WALLTIME_END); }
    public void setWalltimeEnd(long t) { set(Value.WALLTIME_END, t); }

    @Override
    public TeamJam getTeamJam(String id) { return (TeamJam) get(Child.TEAM_JAM, id); }

    @Override
    public void start() {
        synchronized (coreLock) {
            setPeriodClockElapsedStart(scoreBoard.getClock(Clock.ID_PERIOD).getTimeElapsed());
            setWalltimeStart(ScoreBoardClock.getInstance().getCurrentWalltime());
        }
    }
    @Override
    public void stop() {
        synchronized (coreLock) {
            set(Value.DURATION, scoreBoard.getClock(Clock.ID_JAM).getTimeElapsed());
            set(Value.PERIOD_CLOCK_ELAPSED_END, scoreBoard.getClock(Clock.ID_PERIOD).getTimeElapsed());
            set(Value.PERIOD_CLOCK_DISPLAY_END, scoreBoard.getClock(Clock.ID_PERIOD).getTime());
            set(Value.WALLTIME_END, ScoreBoardClock.getInstance().getCurrentWalltime());
        }
    }

    private ScoreBoardListener periodNumberListener;
}
