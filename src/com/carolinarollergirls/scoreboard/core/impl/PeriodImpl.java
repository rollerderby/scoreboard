package com.carolinarollergirls.scoreboard.core.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Period;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public class PeriodImpl extends DefaultScoreBoardEventProvider implements Period {
    public PeriodImpl(ScoreBoard s, int p) {
        children.put(Child.JAM, new HashMap<String, ValueWithId>());
        scoreBoard = s;
        period = p;
        values.put(Value.RUNNING, false);
        values.put(Value.WALLTIME_START, 0L);
        values.put(Value.WALLTIME_END, 0L);
    }

    public String getProviderName() { return PropertyConversion.toFrontend(ScoreBoard.Child.PERIOD); }
    public Class<Period> getProviderClass() { return Period.class; }
    public String getId() { return String.valueOf(period); }
    public ScoreBoardEventProvider getParent() { return scoreBoard; }
    public List<Class<? extends Property>> getProperties() { return properties; }
    
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
    
    public ValueWithId create(AddRemoveProperty prop, String id) {
        return new JamImpl(this, Integer.valueOf(id));
    }

    public int getPeriodNumber() { return period; }

    public void ensureAtLeastNJams(int n) {
        synchronized (coreLock) {
    	requestBatchStart();
    	for (int i = getAll(Child.JAM).size(); i < n; i++) {
    	    get(Child.JAM, String.valueOf(i+1), true);
    	}
    	requestBatchEnd();
        }
    }

    public void truncateAfterNJams(int n) {
        synchronized (coreLock) {
            requestBatchStart();
            for (int i = getAll(Child.JAM).size(); i > n; i--) {
                remove(Child.JAM, getJam(i));
            }
            requestBatchEnd();
        }
    }
    
    public boolean isRunning() { return (Boolean)get(Value.RUNNING); }

    public Jam getJam(int j) { return (Jam)get(Child.JAM, String.valueOf(j)); }

    public void start() {
	set(Value.RUNNING, true);
    }
    public void stop() {
	set(Value.RUNNING, false);
    }

    private ScoreBoard scoreBoard;
    private int period;

    protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
        add(Child.class);
    }};
}
