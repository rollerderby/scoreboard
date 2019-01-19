package com.carolinarollergirls.scoreboard.core.impl;

import java.util.HashMap;

import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Period;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.NumberedProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class PeriodImpl extends NumberedScoreBoardEventProviderImpl<Period> implements Period {
    public PeriodImpl(ScoreBoard s, String p) {
	super(s, ScoreBoard.NChild.PERIOD, Period.class, p, Value.class, NChild.class);
        children.put(NChild.JAM, new HashMap<String, ValueWithId>());
        values.put(Value.CURRENT_JAM_NUMBER, 0);
        values.put(Value.RUNNING, false);
        values.put(Value.WALLTIME_START, 0L);
        values.put(Value.WALLTIME_END, 0L);
    }

    public String getId() { return getProviderId(); }
    
    public boolean set(PermanentProperty prop, Object value, Flag flag) {
	synchronized (coreLock) {
	    if (prop == Value.DURATION) { return false; }
	    requestBatchStart();
	    Object last = get(prop);
	    boolean result = super.set(prop, value, flag);
	    if (result) {
		if (prop == Value.RUNNING) {
		    if (!(Boolean)value) {
			set(Value.WALLTIME_END, ScoreBoardClock.getInstance().getCurrentWalltime());
		    } else if ((Long)get(Value.WALLTIME_START) == 0L) {
			set(Value.WALLTIME_START, ScoreBoardClock.getInstance().getCurrentWalltime());
		    }
		    scoreBoardChange(new ScoreBoardEvent(scoreBoard, ScoreBoard.Value.IN_PERIOD, value, last));
		} else if (prop == Value.CURRENT_JAM_NUMBER && this == scoreBoard.getCurrentPeriod()) {
		    scoreBoardChange(new ScoreBoardEvent(scoreBoard.getClock(Clock.ID_JAM), Clock.Value.NUMBER, value, last));
		} else if (prop == Value.WALLTIME_END ||
			(prop == Value.WALLTIME_START && (Long)get(Value.WALLTIME_END) != 0L)) {
		    scoreBoardChange(new ScoreBoardEvent(this, Value.DURATION, get(Value.DURATION), 0L));
		}
	    }
	    requestBatchEnd();
	    return result;
	}
    }
    public Object get(PermanentProperty prop) {
	synchronized (coreLock) {
	    if (prop == Value.DURATION) {
		if (isRunning() || (Long)get(Value.WALLTIME_END) == 0) {
		    return 0;
		}
		return (Long)get(Value.WALLTIME_END) - (Long)get(Value.WALLTIME_START);
	    }
	    return super.get(prop);
	}
    }
    
    public boolean insert(NumberedProperty prop, NumberedScoreBoardEventProvider<?> item) {
	synchronized (coreLock) {
	    requestBatchStart();
	    boolean result = super.add(prop, item);
	    if (result && prop == NChild.JAM && item.getNumber() <= getCurrentJamNumber()) {
		set(Value.CURRENT_JAM_NUMBER, 1, Flag.CHANGE);
	    }
	    requestBatchEnd();
	    return result;
	}
    }
    public boolean remove(NumberedProperty prop, NumberedScoreBoardEventProvider<?> item, boolean renumber) {
	synchronized (coreLock) {
	    requestBatchStart();
	    boolean result = super.remove(prop, item, renumber);
	    if (result && prop == NChild.JAM && renumber && item.getNumber() < getCurrentJamNumber()) {
		set(Value.CURRENT_JAM_NUMBER, -1, Flag.CHANGE);
	    }
	    requestBatchEnd();
	    return result;
	}
    }
    public ValueWithId create(AddRemoveProperty prop, String id) {
	synchronized (coreLock) {
	    int num = Integer.parseInt(id);
	    if (prop == NChild.JAM && (num > 0 || (num == 0 && number == 0))) {
		return new JamImpl(this, id);
	    }
	    return null;
	}
    }
    
    public PeriodSnapshot snapshot() {
	synchronized (coreLock) {
	    return new PeriodSnapshotImpl(this);
	}
    }
    public void restoreSnapshot(PeriodSnapshot s) {
	synchronized (coreLock) {
            if (s.getId() != getId()) {	return; }
            set(Value.CURRENT_JAM_NUMBER, s.getCurrentJamNumber());
	}
    }

    public void truncateAfterCurrentJam() {
        synchronized (coreLock) {
            requestBatchStart();
            Jam j = getCurrentJam().getNext(false, true);
            while (j != null) {
        	remove(NChild.JAM, j);
        	j = j.getNext(false, true);
            }
            requestBatchEnd();
        }
    }
    
    public boolean isRunning() { return (Boolean)get(Value.RUNNING); }

    public Jam getJam(int j) { return (Jam)get(NChild.JAM, String.valueOf(j), true); }
    public Jam getCurrentJam() {
	if (getCurrentJamNumber() == 0 && number > 0) {
	    return (Jam)getPrevious().getLast(NChild.JAM);
	} else {
	    return getJam(getCurrentJamNumber());
	}	    
    }
    public int getCurrentJamNumber() { 
	int num = (Integer)get(Value.CURRENT_JAM_NUMBER);
	if (num == 0 && number > 0 && !scoreBoard.getRulesets().getBoolean(Rule.JAM_NUMBER_PER_PERIOD)) {
	    num = getPrevious().getLast(NChild.JAM).getNumber();
	}
	return num;
    }

    public void startJam() {
	synchronized (coreLock) {
	    requestBatchStart();
	    set(Value.RUNNING, true);
	    set(Value.CURRENT_JAM_NUMBER, 1, Flag.CHANGE);
	    getCurrentJam().start();
	    getCurrentJam().getNext(true, false);
	    requestBatchEnd();
	}
    }
    public void stopJam() {
	synchronized (coreLock) {
	    requestBatchStart();
	    getCurrentJam().stop();
	    requestBatchEnd();
	}
    }

    public static class PeriodSnapshotImpl implements PeriodSnapshot {
        private PeriodSnapshotImpl(Period period) {
            id = period.getId();
            currentJamNumber = period.getCurrentJamNumber();
        }
        
        public String getId() { return id; }
        public int getCurrentJamNumber() { return currentJamNumber; }
        
        private String id;
        private int currentJamNumber;
    }
}
