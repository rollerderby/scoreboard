package com.carolinarollergirls.scoreboard.event;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public abstract class DefaultScoreBoardEventProvider implements ScoreBoardEventProvider,ScoreBoardListener {
    public abstract String getProviderName();
    public abstract Class<? extends ScoreBoardEventProvider> getProviderClass();
    public abstract String getProviderId();
    public String getId() { return getProviderId(); }
    public String getValue() {return getProviderId(); }
    public String toString() { return getProviderId(); }

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
    
    public ValueWithId childFromString(AddRemoveProperty prop, String id, String sValue) {
	synchronized (coreLock) {
	    ValueWithId v = create(prop, id);
	    if ( v != null) { return v; }
	    return new ValWithId(id, sValue);
	}
    }
    public ValueWithId get(AddRemoveProperty prop, String id) { return get(prop, id, false); }
    public ValueWithId get(AddRemoveProperty prop, String id, boolean add) {
	synchronized (coreLock) {
	    Map<String, ValueWithId> map = children.get(prop);
	    ValueWithId result = map.get(id);
	    if (result == null && add) {
		result = create(prop, id);
		add(prop, result);
	    }
	    return result;
	}
    }
    public Collection<? extends ValueWithId> getAll(AddRemoveProperty prop) {
	synchronized (coreLock) {
	    return new HashSet<ValueWithId>(children.get(prop).values());
	}
    }
    public boolean add(AddRemoveProperty prop, ValueWithId item) {
	synchronized (coreLock) {
	    Map<String, ValueWithId> map = children.get(prop);
	    if (map.containsKey(item.getId()) && map.get(item.getId()).equals(item)) { return false; }
	    map.put(item.getId(), item);
	    if (item instanceof ScoreBoardEventProvider && ((ScoreBoardEventProvider)item).getParent() == this) {
		((ScoreBoardEventProvider)item).addScoreBoardListener(this);
	    }
	    scoreBoardChange(new ScoreBoardEvent(this, prop, item, false));
	    return true;
	}
    }
    public ValueWithId create(AddRemoveProperty prop, String id) { return null; }
    public boolean remove(AddRemoveProperty prop, String id) { return remove(prop, get(prop, id)); }
    public boolean remove(AddRemoveProperty prop, ValueWithId item) {
	synchronized (coreLock) {
	    Map<String, ValueWithId> map = children.get(prop);
	    if (item == null || !map.containsKey(item.getId())) { return false; }
	    map.remove(item.getId());
	    if (item instanceof ScoreBoardEventProvider) {
		((ScoreBoardEventProvider)item).removeScoreBoardListener(this);
	    }
	    scoreBoardChange(new ScoreBoardEvent(this, prop, item, true));
	    return true;
	}
    }
    public void removeAll(AddRemoveProperty prop) {
	synchronized (coreLock) {
	    for (ValueWithId item : getAll(prop)) {
		remove(prop, item);
	    }
	}
    }
    
    public void execute(CommandProperty prop) {
	
    }

    protected static Object coreLock = new Object();

    protected Set<ScoreBoardListener> scoreBoardEventListeners = new LinkedHashSet<ScoreBoardListener>();
    protected Map<PermanentProperty, Object> values = new HashMap<PermanentProperty, Object>();
    protected Map<AddRemoveProperty, Map<String, ValueWithId>> children = new HashMap<AddRemoveProperty, Map<String, ValueWithId>>();

    public enum BatchEvent implements ScoreBoardEvent.PermanentProperty {
	START,
	END;
    }
}
