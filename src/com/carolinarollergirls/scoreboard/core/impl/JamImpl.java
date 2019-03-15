package com.carolinarollergirls.scoreboard.core.impl;

import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.TeamJam;

import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Penalty;
import com.carolinarollergirls.scoreboard.core.Period;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class JamImpl extends NumberedScoreBoardEventProviderImpl<Jam> implements Jam {
    public JamImpl(ScoreBoardEventProvider parent, Jam prev) {
        this(parent, prev.getNumber()+1);
        setPrevious(prev);
    }
    public JamImpl(ScoreBoardEventProvider p, int j) {
        super(p, j, Period.NChild.JAM, Jam.class, Value.class, Child.class, Command.class);
        setInverseReference(Child.PENALTY, Penalty.Value.JAM);
        periodNumberListener = setCopy(Value.PERIOD_NUMBER, parent,
                parent instanceof ScoreBoard ? ScoreBoard.Value.CURRENT_PERIOD_NUMBER : IValue.NUMBER,
                        true);
        add(Child.TEAM_JAM, new TeamJamImpl(this, Team.ID_1));
        add(Child.TEAM_JAM, new TeamJamImpl(this, Team.ID_2));
        addWriteProtection(Child.TEAM_JAM);
    }

    public void setParent(ScoreBoardEventProvider p) {
        if (parent == p) { return; }
        parent.removeScoreBoardListener(periodNumberListener);
        providers.remove(periodNumberListener);
        parent = p;
        periodNumberListener = setCopy(Value.PERIOD_NUMBER, parent,
                parent instanceof ScoreBoard ? ScoreBoard.Value.CURRENT_PERIOD_NUMBER : IValue.NUMBER,
                        true);
    }
    
    protected void unlink(boolean neighborsRemoved) {
        if (!neighborsRemoved) {
            if (parent instanceof Period && this == ((Period)parent).getCurrentJam()) {
                parent.set(Period.Value.CURRENT_JAM, getPrevious());
            }
            for (ValueWithId p : getAll(Child.PENALTY)) {
                ((Penalty)p).set(Penalty.Value.JAM, getNext());
            }
        }
        super.unlink(neighborsRemoved);
    }

    public void execute(CommandProperty prop) {
        synchronized (coreLock) {
            switch ((Command)prop) {
            case DELETE:
                if (scoreBoard.isInJam() && (parent == scoreBoard.getCurrentPeriod()) &&
                        (this == ((Period)parent).getCurrentJam())) {
                    break;
                }
                requestBatchStart();
                unlink();
                scoreBoard.updateTeamJams();
                requestBatchEnd();
                break;
            case INSERT_BEFORE:
                requestBatchStart();
                parent.add(ownType, new JamImpl(parent, getNumber()));
                scoreBoard.updateTeamJams();
                requestBatchEnd();
                break;
            }
        }
    }

    public long getDuration() { return (Long)get(Value.DURATION); }
    public void setDuration(long t) { set(Value.DURATION, t); }

    public long getPeriodClockElapsedStart() { return (Long)get(Value.PERIOD_CLOCK_ELAPSED_START); }
    public void setPeriodClockElapsedStart(long t) { set(Value.PERIOD_CLOCK_ELAPSED_START, t); }

    public long getPeriodClockElapsedEnd() { return (Long)get(Value.PERIOD_CLOCK_ELAPSED_END); }
    public void setPeriodClockElapsedEnd(long t) { set(Value.PERIOD_CLOCK_ELAPSED_END, t); }

    public long getWalltimeStart() { return (Long)get(Value.WALLTIME_START); }
    public void setWalltimeStart(long t) { set(Value.WALLTIME_START, t); }

    public long getWalltimeEnd() { return (Long)get(Value.WALLTIME_END); }
    public void setWalltimeEnd(long t) { set(Value.WALLTIME_END, t); }

    public TeamJam getTeamJam(String id) { return (TeamJam)get(Child.TEAM_JAM, id); }

    public void start() {
        synchronized (coreLock) {
            requestBatchStart();
            setPeriodClockElapsedStart(scoreBoard.getClock(Clock.ID_PERIOD).getTimeElapsed());
            setWalltimeStart(ScoreBoardClock.getInstance().getCurrentWalltime());
            requestBatchEnd();
        }
    }
    public void stop() {
        synchronized (coreLock) {
            requestBatchStart();
            set(Value.DURATION, scoreBoard.getClock(Clock.ID_JAM).getTimeElapsed());
            set(Value.PERIOD_CLOCK_ELAPSED_END, scoreBoard.getClock(Clock.ID_PERIOD).getTimeElapsed());
            set(Value.WALLTIME_END, ScoreBoardClock.getInstance().getCurrentWalltime());
            requestBatchEnd();
        }
    }

    private ScoreBoardListener periodNumberListener;
}
