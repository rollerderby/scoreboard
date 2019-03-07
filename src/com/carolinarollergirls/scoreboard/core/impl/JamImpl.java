package com.carolinarollergirls.scoreboard.core.impl;

import java.util.Arrays;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Penalty;
import com.carolinarollergirls.scoreboard.core.Period;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl.ElementReference.AlternateTargetPolicy;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class JamImpl extends NumberedScoreBoardEventProviderImpl<Jam> implements Jam {
    public JamImpl(ScoreBoardEventProvider parent, Jam prev) {
        this(parent, String.valueOf(prev.getNumber()+1));
        setPrevious(prev);
    }
    public JamImpl(ScoreBoardEventProvider p, String j) {
        super(p, j, Period.NChild.JAM, Jam.class, Value.class, Child.class, Command.class);
        for (PermanentProperty prop : Arrays.asList(Value.DURATION, Value.PERIOD_CLOCK_ELAPSED_START,
                Value.PERIOD_CLOCK_ELAPSED_END, Value.WALLTIME_START, Value.WALLTIME_END)) {
            set(prop, 0L);
        }
        addReference(new ElementReference(Child.PENALTY, Penalty.class, Penalty.Value.JAM, AlternateTargetPolicy.NEXT));
        if (parent instanceof Period) {
            periodNumberReference.setWatchedElement(parent);
        } else if (scoreBoard.get(ScoreBoard.Value.CURRENT_PERIOD) instanceof Period) {
            periodNumberReference.setWatchedElement(scoreBoard.getCurrentPeriod());
        } /* else we are restoring from autosave and CurrentPeriod isn't properly set yet.
             Reference will be updated after restore is complete.*/
        addReference(periodNumberReference);
        add(Child.TEAM_JAM, new TeamJamImpl(this, Team.ID_1));
        add(Child.TEAM_JAM, new TeamJamImpl(this, Team.ID_2));
        addWriteProtection(Child.TEAM_JAM);
    }

    public void setParent(ScoreBoardEventProvider p) {
        parent = p;
        periodNumberReference.setWatchedElement(parent instanceof Period ? parent : scoreBoard.getCurrentPeriod());
    }
    
    protected void unlink(boolean neighborsRemoved) {
        if (parent instanceof Period && this == ((Period)parent).getCurrentJam() && !neighborsRemoved) {
            parent.set(Period.Value.CURRENT_JAM, getPrevious());
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
                parent.add(ownType, new JamImpl(parent, getProviderId()));
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

    private ValueReference periodNumberReference = new ValueReference(this, Value.PERIOD_NUMBER, null, IValue.NUMBER, true, 0);
}
