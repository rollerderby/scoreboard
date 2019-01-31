package com.carolinarollergirls.scoreboard.core.impl;

import java.util.Arrays;
import java.util.UUID;

import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Penalty;
import com.carolinarollergirls.scoreboard.core.Period;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class JamImpl extends NumberedScoreBoardEventProviderImpl<Jam> implements Jam {
    public JamImpl(Period p, String j) {
	super(p, Value.NUMBER, Value.ID, Period.NChild.JAM, Jam.class, j, Value.class, Child.class);
	set(Value.ID, UUID.randomUUID().toString());
	writeProtectionOverride.put(Value.ID, Flag.FROM_AUTOSAVE);
	for (PermanentProperty prop : Arrays.asList(Value.DURATION, Value.PERIOD_CLOCK_ELAPSED_START,
		Value.PERIOD_CLOCK_ELAPSED_END, Value.WALLTIME_START, Value.WALLTIME_END)) {
	    set(prop, 0L);
	}
	addReference(new ElementReference(Child.PENALTY, Penalty.class, Penalty.Value.JAM));
	addReference(new PropertyReference(this, Value.PERIOD_NUMBER, parent, Period.Value.NUMBER, true, 0));
        add(Child.TEAM_JAM, new TeamJamImpl(this, Team.ID_1));
        add(Child.TEAM_JAM, new TeamJamImpl(this, Team.ID_2));
    }

    public Jam getPrevious(boolean create, boolean skipEmpty) {
	synchronized (coreLock) {
	    Jam prev = super.getPrevious(create, skipEmpty);
	    if (prev == null && getPeriod().hasPrevious(skipEmpty)) {
		prev = (Jam)getPeriod().getPrevious(false, skipEmpty).getLast(ownType);
	    }
	    return prev;
	}
    }
    public Jam getNext(boolean create, boolean skipEmpty) {
	synchronized (coreLock) {
	    Jam next = super.getNext(create, skipEmpty);
	    if (next == null && skipEmpty && getPeriod().hasNext(skipEmpty)) {
		next = (Jam)getPeriod().getNext(false, skipEmpty).getFirst(ownType);
	    }
	    return next;
	}
    }
    
    public Period getPeriod() { return (Period)parent; }
    public int getPeriodNumber() { return getPeriod().getNumber(); }
    public void moveToNextPeriod(int newNumber) {
	synchronized (coreLock) {
	    requestBatchStart();
	    Jam successor = super.getNext(false, true);
	    parent.remove(ownType, this);
	    parent = ((Period)parent).getNext(true, false);
	    if (scoreBoard.getRulesets().getBoolean(Rule.JAM_NUMBER_PER_PERIOD)) {
		set(Value.NUMBER, newNumber);
	    }
	    parent.insert(ownType, this);
	    if (successor != null) {
		successor.moveToNextPeriod(newNumber + 1);
	    }
	    requestBatchEnd();
	}
    }
    public void moveToPreviousPeriod() {
	synchronized (coreLock) {
	    requestBatchStart();
	    Jam predecessor = super.getPrevious(false, true);
	    if (predecessor != null) {
		predecessor.moveToPreviousPeriod();
	    }
	    int newNumber = getPrevious(false, true).getNumber() + 1;
	    parent.remove(ownType, this);
	    parent = ((Period)parent).getPrevious(true, false);
	    set(Value.NUMBER, newNumber);
	    parent.insert(ownType, this);
	    requestBatchEnd();
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
}
