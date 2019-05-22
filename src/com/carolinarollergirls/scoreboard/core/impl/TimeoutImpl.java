package com.carolinarollergirls.scoreboard.core.impl;

import java.util.UUID;

import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Period;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.Timeout;
import com.carolinarollergirls.scoreboard.core.TimeoutOwner;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.OrderedScoreBoardEventProvider.IValue;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class TimeoutImpl extends ScoreBoardEventProviderImpl implements Timeout {
    public TimeoutImpl(Period p, String id) {
        super(p, Value.ID, id, Period.Child.TIMEOUT, Timeout.class, Value.class, Command.class);
        initReferences();
        if (id == "noTimeout") {
            set(Value.RUNNING, false);
            addWriteProtection(Value.RUNNING);
            addWriteProtection(Value.OWNER);
            addWriteProtection(Value.REVIEW);
            addWriteProtection(Value.RETAINED_REVIEW);
            addWriteProtection(Value.PRECEDING_JAM);
        }
    }
    public TimeoutImpl(Jam precedingJam) {
        super(precedingJam.getParent(), Value.ID, UUID.randomUUID().toString(),
                Period.Child.TIMEOUT, Timeout.class, Value.class, Command.class);
        initReferences();
        set(Value.PRECEDING_JAM, precedingJam);
        set(Value.WALLTIME_START, ScoreBoardClock.getInstance().getCurrentWalltime());
        set(Value.PERIOD_CLOCK_ELAPSED_START, scoreBoard.getClock(Clock.ID_PERIOD).getTimeElapsed());
    }
    
    private void initReferences() {
        set(Value.OWNER, Owners.NONE);
        setInverseReference(Value.PRECEDING_JAM, Jam.Child.TIMEOUTS_AFTER);
        setCopy(Value.PRECEDING_JAM_NUMBER, this, Value.PRECEDING_JAM, IValue.NUMBER, true);
    }
    
    @Override
    public int compareTo(Timeout other) {
        int result = 0;
        if (get(Value.PRECEDING_JAM) != null && other.get(Value.PRECEDING_JAM) != null) {
            result = ((Jam)get(Value.PRECEDING_JAM)).compareTo((Jam)other.get(Value.PRECEDING_JAM));
        }
        if (result == 0) {
            result = (int) ((Long)get(Value.WALLTIME_START) - (Long)other.get(Value.WALLTIME_START));
        }
        return result;
    }
    
    @Override
    protected void valueChanged(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == Value.OWNER) {
            if (last instanceof Team) {
                ((Team) last).remove(Team.Child.TIME_OUT, this);
            }
            if (value instanceof Team) {
                ((Team) value).add(Team.Child.TIME_OUT, this);
            }
        }
        if (prop == Value.REVIEW && getOwner() instanceof Team) {
            ((Team)getOwner()).recountTimeouts();
        }
        if (prop == Value.RETAINED_REVIEW && getOwner() instanceof Team) {
            ((Team)getOwner()).recountTimeouts();
        }
        if (prop == Value.PRECEDING_JAM && value != null &&
                ((Jam)value).getParent() != getParent()) {
            getParent().remove(Period.Child.TIMEOUT, this);
            parent = ((Jam)value).getParent();
            getParent().add(Period.Child.TIMEOUT, this);
        }
        if (prop == Value.PRECEDING_JAM && getOwner() instanceof Team) {
            ((Team) getOwner()).recountTimeouts();
        }
    }

    @Override
    protected void unlink(boolean neighborsRemoved) {
        if (get(Value.OWNER) instanceof Team) {
            ((Team)get(Value.OWNER)).remove(Team.Child.TIME_OUT, this);
        }
        super.unlink(neighborsRemoved);
    }
    
    @Override
    public void execute(CommandProperty prop) {
        synchronized (coreLock) {
            switch((Command)prop) {
            case DELETE:
                if (!isRunning()) {
                    unlink();
                }
                break;
            }
        }
    }
    
    @Override
    public void stop() {
        set(Value.RUNNING, false);
        set(Value.DURATION, scoreBoard.getClock(Clock.ID_TIMEOUT).getTimeElapsed());
        set(Value.WALLTIME_END, ScoreBoardClock.getInstance().getCurrentWalltime());
        set(Value.PERIOD_CLOCK_ELAPSED_END, scoreBoard.getClock(Clock.ID_PERIOD).getTimeElapsed());
    }

    @Override
    public TimeoutOwner getOwner() { return (TimeoutOwner)get(Value.OWNER); }
    @Override
    public boolean isReview() { return (Boolean)get(Value.REVIEW); }
    @Override
    public boolean isRetained() { return (Boolean)get(Value.RETAINED_REVIEW); }
    @Override
    public boolean isRunning() { return (Boolean)get(Value.RUNNING); }
}
