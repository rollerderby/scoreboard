package com.carolinarollergirls.scoreboard.event;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public abstract class DefaultScoreBoardEventProvider implements ScoreBoardEventProvider,ScoreBoardListener {
    public abstract String getProviderName();
    public abstract Class<? extends ScoreBoardEventProvider> getProviderClass();
    public abstract String getProviderId();
    public String getId() { return getProviderId(); }
    public String getValue() {return getId(); }

    public void scoreBoardChange(ScoreBoardEvent event) {
        dispatch(event);
    }

    protected void dispatch(ScoreBoardEvent event) {
        // Synchronously send events to listeners.
        synchronized(scoreBoardEventListeners) {
            for (ScoreBoardListener l : scoreBoardEventListeners) {
                l.scoreBoardChange(event);
            }
        }
    }

    protected void requestBatchStart() {
        scoreBoardChange(new ScoreBoardEvent(this, BatchEvent.START, Boolean.TRUE, Boolean.TRUE));
    }

    protected void requestBatchEnd() {
        scoreBoardChange(new ScoreBoardEvent(this, BatchEvent.END, Boolean.TRUE, Boolean.TRUE));
    }

    public void addScoreBoardListener(ScoreBoardListener listener) {
        synchronized(scoreBoardEventListeners) {
            scoreBoardEventListeners.add(listener);
        }
    }
    public void removeScoreBoardListener(ScoreBoardListener listener) {
        synchronized(scoreBoardEventListeners) {
            scoreBoardEventListeners.remove(listener);
        }
    }

    public Object valueFromString(PermanentProperty prop, String sValue) {
	Object value = sValue;
	Object old = get(prop);
	if (old instanceof Boolean) { 
	    value = Boolean.valueOf(sValue); 
	}
	if (old instanceof Integer) {
	    value = Integer.valueOf(sValue);
	}
	if (old instanceof Long) {
	    value = Long.valueOf(sValue);
	}
	return value;
    }
    
    public Object get(PermanentProperty prop) {
	synchronized (coreLock) {
	    return values.get(prop);
	}
    }
    public boolean set(PermanentProperty prop, Object value) { return set(prop, value, null); }
    public boolean set(PermanentProperty prop, Object value, Flag flag) { return set(prop, value, flag, null, null, 0); }
    public boolean set(PermanentProperty prop, Object value, Flag flag, Number min, Number max, long tolerance) {
	synchronized (coreLock) {
	    Object last = values.get(prop);
	    if (flag == Flag.CHANGE) {
		if (last instanceof Integer) {
		    value = (Integer)last + (Integer)value;
		} else if (last instanceof Long) {
		    value = (Long)last + (Long)value;
		}
	    }
	    if (min instanceof Integer && (Integer)value < (Integer)min) {
		value = min;
	    }
	    if (min instanceof Long && (Long)value < (Long)min - tolerance) {
		value = min;
	    }
	    if (max instanceof Integer && (Integer)value > (Integer)max) {
		value = max;
	    }
	    if (max instanceof Long && (Long)value > (Long)max + tolerance) {
		value = max;
	    }
	    if (Objects.equals(value, last)) { return false; }
	    values.put(prop, value);
	    scoreBoardChange(new ScoreBoardEvent(this, prop, value, last));
	    return true;
	}
    }

    protected static Object coreLock = new Object();

    protected Set<ScoreBoardListener> scoreBoardEventListeners = new LinkedHashSet<ScoreBoardListener>();
    protected Map<PermanentProperty, Object> values = new HashMap<PermanentProperty, Object>();

    public enum BatchEvent implements ScoreBoardEvent.PermanentProperty {
	START,
	END;
    }
}
