package com.carolinarollergirls.scoreboard.event;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.NumberedProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public abstract class ScoreBoardEventProviderImpl implements ScoreBoardEventProvider,ScoreBoardListener {
    @SafeVarargs
    protected ScoreBoardEventProviderImpl(ScoreBoardEventProvider parent, AddRemoveProperty type,
	    Class<? extends ScoreBoardEventProvider> ownClass, Class<? extends Property>... props) {
	this.parent = parent;
	if (parent != null) {
	    scoreBoard = parent.getScoreBoard();
	} else if (this instanceof ScoreBoard) {
	    scoreBoard = (ScoreBoard)this;
	}
	if (type == null) {
	    providerName = "ScoreBoard";
	} else {
	    providerName = PropertyConversion.toFrontend(type);
	}
	this.providerClass = ownClass;
	properties = Arrays.asList(props);
    }
    
    public String getProviderName() { return providerName; }
    public Class<? extends ScoreBoardEventProvider> getProviderClass() { return providerClass; }
    public String getProviderId() { return getId(); }
    public String getValue() { return getProviderId(); }
    public String toString() { return getProviderId(); }
    public List<Class<? extends Property>> getProperties() { return properties; }
    public ScoreBoardEventProvider getParent() { return parent; }

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
    public NumberedScoreBoardEventProvider<?> getFirst(NumberedProperty prop) {
	synchronized (coreLock) {
	    return (NumberedScoreBoardEventProvider<?>)get(prop, String.valueOf(minIds.get(prop)));
	}
    }
    public NumberedScoreBoardEventProvider<?> getLast(NumberedProperty prop) {
	synchronized (coreLock) {
	    return (NumberedScoreBoardEventProvider<?>)get(prop, String.valueOf(maxIds.get(prop)));
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
	    String id = item.getId();
	    if (item instanceof ScoreBoardEventProvider && ((ScoreBoardEventProvider)item).getParent() == this) {
		id = ((ScoreBoardEventProvider)item).getProviderId();
	    }
	    if (map.containsKey(id) && map.get(id).equals(item)) { return false; }
	    map.put(id, item);
	    if (item instanceof ScoreBoardEventProvider && ((ScoreBoardEventProvider)item).getParent() == this) {
		((ScoreBoardEventProvider)item).addScoreBoardListener(this);
	    }
	    if (prop instanceof NumberedProperty) {
		int num = Integer.parseInt(id);
		if (minIds.get(prop) == null || minIds.get(prop) > num) {
		    minIds.put((NumberedProperty)prop,  num);
		}
		if (maxIds.get(prop) == null || maxIds.get(prop) < num) {
		    maxIds.put((NumberedProperty)prop,  num);
		}
	    }
	    scoreBoardChange(new ScoreBoardEvent(this, prop, item, false));
	    return true;
	}
    }
    public boolean insert(NumberedProperty prop, NumberedScoreBoardEventProvider<?> item) {
	synchronized (coreLock) {
	    if (item == null) { return false; }
	    if (get(prop, item.getProviderId()) == null) { return add(prop, item); }
	    requestBatchStart();
	    int newNum = Integer.parseInt(item.getProviderId());
	    NumberedScoreBoardEventProvider<?> curItem = 
		    (NumberedScoreBoardEventProvider<?>)get(prop, item.getProviderId());
	    while (curItem.hasNext(false)) {
		curItem = curItem.getNext(false, false);
	    }
	    int curNum = Integer.parseInt(curItem.getProviderId()) + 1;
	    while (curNum > newNum) {
		curItem.setNumber(curNum, true);
		curNum--;
		curItem = curItem.getPrevious(false, true);
	    }
	    add(prop, item);
	    requestBatchEnd();
	    return true;
	}
    }
    public ValueWithId create(AddRemoveProperty prop, String id) { return null; }
    public boolean remove(AddRemoveProperty prop, String id) { return remove(prop, get(prop, id)); }
    public boolean remove(AddRemoveProperty prop, ValueWithId item) {
	synchronized (coreLock) {
	    boolean result = removeSilent(prop, item);
	    if (result) { scoreBoardChange(new ScoreBoardEvent(this, prop, item, true)); }
	    return result;
	}
    }
    public boolean remove(NumberedProperty prop, String id, boolean renumber) {
	return remove(prop, (NumberedScoreBoardEventProvider<?>)get(prop, id), renumber);
    }
    public boolean remove(NumberedProperty prop, NumberedScoreBoardEventProvider<?> item, boolean renumber) {
	synchronized (coreLock) {
	    if (!renumber || !item.hasNext(false)) {
		return remove(prop, item);
	    }
	    requestBatchStart();
	    boolean result = removeSilent(prop, item);
	    if (result) {
		NumberedScoreBoardEventProvider<?> curItem = item.getNext(false, false);
		int curNumber = Integer.valueOf(item.getProviderId());
		while (curItem.hasNext(false)) {
		    curItem.setNumber(curNumber, true);
		    curNumber++;
		    curItem = curItem.getNext(false, false);
		}
		curItem.setNumber(curNumber, false);
	    }
	    requestBatchEnd();
	    return result;
	}
    }
    public boolean removeSilent(AddRemoveProperty prop, ValueWithId item) {
	synchronized (coreLock) {
	    Map<String, ValueWithId> map = children.get(prop);
	    if (item == null) { return false; }
	    String id = item.getId();
	    if (item instanceof ScoreBoardEventProvider && ((ScoreBoardEventProvider)item).getParent() == this) {
		id = ((ScoreBoardEventProvider)item).getProviderId();
	    }
	    if (!map.containsKey(id)) { return false; }
	    map.remove(id);
	    if (item instanceof ScoreBoardEventProvider) {
		((ScoreBoardEventProvider)item).removeScoreBoardListener(this);
	    }
	    if (prop instanceof NumberedProperty) {
		int num = Integer.parseInt(id);
		if (minIds.get(prop) != null && num == minIds.get(prop)) {
		    NumberedScoreBoardEventProvider<?> next = ((NumberedScoreBoardEventProvider<?>)item).getNext(false, true);
		    minIds.put((NumberedProperty)prop,  next == null ? null : Integer.parseInt(next.getProviderId()));
		}
		if (maxIds.get(prop) != null && maxIds.get(prop) == num) {
		    NumberedScoreBoardEventProvider<?> prev = ((NumberedScoreBoardEventProvider<?>)item).getPrevious(false, true);
		    maxIds.put((NumberedProperty)prop,  prev == null ? null : Integer.parseInt(prev.getProviderId()));
		}
	    }
	    return true;
	}
    }
    public void removeAll(AddRemoveProperty prop) {
	synchronized (coreLock) {
	    if (prop instanceof NumberedProperty) {
		minIds.remove(prop);
		maxIds.remove(prop);
	    }
	    for (ValueWithId item : getAll(prop)) {
		remove(prop, item);
	    }
	}
    }
    public int getMinNumber(NumberedProperty prop) { return minIds.get(prop); }
    public int getMaxNumber(NumberedProperty prop) { return maxIds.get(prop); }
    
    public void execute(CommandProperty prop) {  }
    
    public ScoreBoard getScoreBoard() { return scoreBoard; }

    protected static Object coreLock = new Object();
    
    protected ScoreBoard scoreBoard;
    protected ScoreBoardEventProvider parent;
    protected String providerName;
    protected Class<? extends ScoreBoardEventProvider> providerClass;
    
    protected List<Class<? extends Property>> properties;

    protected Set<ScoreBoardListener> scoreBoardEventListeners = new LinkedHashSet<ScoreBoardListener>();
    protected Map<PermanentProperty, Object> values = new HashMap<PermanentProperty, Object>();
    protected Map<AddRemoveProperty, Map<String, ValueWithId>> children = new HashMap<AddRemoveProperty, Map<String, ValueWithId>>();
    protected Map<NumberedProperty, Integer> minIds = new HashMap<NumberedProperty, Integer>();
    protected Map<NumberedProperty, Integer> maxIds = new HashMap<NumberedProperty, Integer>();

    public enum BatchEvent implements ScoreBoardEvent.PermanentProperty {
	START,
	END;
    }
}
