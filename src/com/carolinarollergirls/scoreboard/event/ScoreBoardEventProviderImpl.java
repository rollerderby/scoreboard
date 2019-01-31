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

import com.carolinarollergirls.scoreboard.core.FloorPosition;
import com.carolinarollergirls.scoreboard.core.Role;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.TimeoutOwner;
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
    protected ScoreBoardEventProviderImpl(ScoreBoardEventProvider parent, PermanentProperty idProp,
	    AddRemoveProperty type, Class<? extends ScoreBoardEventProvider> ownClass, Class<? extends Property>... props) {
	this.parent = parent;
	if (parent != null) {
	    scoreBoard = parent.getScoreBoard();
	} else if (this instanceof ScoreBoard) {
	    scoreBoard = (ScoreBoard)this;
	}
	idProperty = idProp;
	if (type == null) {
	    providerName = "ScoreBoard";
	} else {
	    providerName = PropertyConversion.toFrontend(type);
	}
	this.providerClass = ownClass;
	if (elements.get(ownClass) == null) { elements.put(ownClass, new HashMap<String, ScoreBoardEventProvider>()); }
	properties = Arrays.asList(props);
	for (Class<? extends Property> propertySet : properties) {
	    for (Property prop : propertySet.getEnumConstants()) {
		if (prop instanceof AddRemoveProperty) {
		    children.put((AddRemoveProperty)prop, new HashMap<String, ValueWithId>());
		} else if (prop instanceof PermanentProperty) {
		    referencesFrom.put((PermanentProperty)prop, new HashSet<PropertyReference>());
		    referenceRelays.put((PermanentProperty)prop, new HashSet<PropertyReference>());
		}
	    }
	}
    }
    
    public String getId() { 
	if (idProperty == null) {
	    return "";
	} else {
	    return String.valueOf(get(idProperty)); }
	}
    public String getProviderName() { return providerName; }
    public Class<? extends ScoreBoardEventProvider> getProviderClass() { return providerClass; }
    public String getProviderId() { return getId(); }
    public String getValue() { return getId(); }
    public String toString() { return getId(); }
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

    protected void addReference(ElementReference reference) {
	elementReference.put(reference.getLocalProperty(), reference);
    }
    protected void addReference(PropertyReference reference) {
	if (reference instanceof IndirectPropertyReference) {
	    IndirectPropertyReference ref = (IndirectPropertyReference)reference;
	    ((ScoreBoardEventProviderImpl)ref.getReferenceElement()).referenceRelays.get(ref.getReferenceProperty()).add(ref);
	}
	ScoreBoardEventProviderImpl source = (ScoreBoardEventProviderImpl)reference.getSourceElement();
	if (reference.isReadOnly()) {
	    source.writeProtectionOverride.put(reference.getSourceProperty(), null);
	}
	source.referenceTo.put(reference.getSourceProperty(), reference);
	if (reference.getTargetElement() != null) {
	    ((ScoreBoardEventProviderImpl)reference.getTargetElement()).addReferenceSource(reference);
	}
    }
    protected void addReferenceSource(PropertyReference reference) {
	referencesFrom.get(reference.getTargetProperty()).add(reference);
    }
    protected void removeReferenceSource(PropertyReference reference) {
	referencesFrom.get(reference.getTargetProperty()).remove(reference);
    }
    public Object valueFromString(PermanentProperty prop, String sValue) {
	synchronized (coreLock) {
	    Object old;
	    ElementReference ref = elementReference.get(prop);
	    if (ref != null) {
		return getElement(ref.getRemoteClass(), sValue);
	    }
	    PropertyReference reference = referenceTo.get(prop);
	    if (reference != null) {
		if (reference.getTargetElement() != null) {
		    return reference.getTargetElement().valueFromString(reference.getTargetProperty(), sValue);
		} else {
		    old = reference.getDefaultValue();
		}
	    } else {
		old = get(prop);
	    }
	    Object value = sValue;
	    if (old instanceof Role) {
		value = Role.fromString(sValue);
	    }
	    if (old instanceof FloorPosition) {
		value = FloorPosition.fromString(sValue);
	    }
	    if (old instanceof TimeoutOwner) {
		value = scoreBoard.getTimeoutOwner(sValue);
	    }
	    if (old instanceof Boolean) { 
		value = Boolean.valueOf(sValue); 
	    }
	    if (old instanceof Integer) {
		value = Integer.valueOf(sValue);
	    }
	    if (old instanceof Long) {
		value = Long.valueOf(sValue);
	    }
	    if ("".equals(sValue) && !(old instanceof String)) {
		value = null;
	    }
	    return value;
	}
    }
    public Object get(PermanentProperty prop) {
	synchronized (coreLock) {
	    PropertyReference reference = referenceTo.get(prop);
	    if (reference != null) {
		if (reference.getTargetElement() == null) { return reference.getDefaultValue(); }
		return reference.getTargetElement().get(reference.getTargetProperty());
	    }
	    return values.get(prop);
	}
    }
    public boolean set(PermanentProperty prop, Object value) { return set(prop, value, null); }
    public boolean set(PermanentProperty prop, Object value, Flag flag) { return set(prop, value, flag, null, null, 0); }
    public boolean set(PermanentProperty prop, Object value, Flag flag, Number min, Number max, long tolerance) {
	synchronized (coreLock) {
	    boolean foreign = true;
	    for (Class<? extends Property> pc : properties) {
		if (pc.isAssignableFrom(prop.getClass())) { foreign = false; }
	    }
	    if (foreign) { return false; }
	    PropertyReference reference = referenceTo.get(prop);
	    if (reference != null) {
		if (reference.isReadOnly() || reference.getTargetElement() == null || flag == Flag.FROM_AUTOSAVE) {
		    return false;
		} else {
		    return reference.getTargetElement().set(reference.getTargetProperty(), value, flag);
		}
	    }
	    if (writeProtectionOverride.containsKey(prop) && (writeProtectionOverride.get(prop) == null ||
		    writeProtectionOverride.get(prop) != flag)) {
		return false;
	    }
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
	    if (prop == idProperty) {
		elements.get(providerClass).remove(String.valueOf(last));
		elements.get(providerClass).put(String.valueOf(value), this);
		writeProtectionOverride.put(prop, null);
	    }
	    _valueChanged(prop, value, last);
	    return true;
	}
    }
    protected void _valueChanged(PermanentProperty prop, Object value, Object last) {
	synchronized (coreLock) {
	    requestBatchStart();
	    scoreBoardChange(new ScoreBoardEvent(this, prop, value, last));
	    ElementReference reference = elementReference.get(prop);
	    if (reference != null) {
		ScoreBoardEventProvider lastRemote = (ScoreBoardEventProvider)last;
		ScoreBoardEventProvider newRemote = (ScoreBoardEventProvider)value;
		if (reference.getRemoteProperty() instanceof PermanentProperty) {
		    if (lastRemote != null && lastRemote.get((PermanentProperty)reference.getRemoteProperty()) == this) {
			lastRemote.set((PermanentProperty)reference.getRemoteProperty(), null);
		    }
		    if (newRemote != null) {
			newRemote.set((PermanentProperty)reference.getRemoteProperty(), this);
		    }
		} else if (reference.getRemoteProperty() instanceof AddRemoveProperty) {
		    if (lastRemote != null) {
			lastRemote.remove((AddRemoveProperty)reference.getRemoteProperty(), this);
		    }
		    if (newRemote != null) {
			newRemote.add((AddRemoveProperty)reference.getRemoteProperty(), this);
		    }
		}
	    }
	    for (PropertyReference ref : referencesFrom.get(prop)) {
		((ScoreBoardEventProviderImpl)ref.getSourceElement())._valueChanged(ref.getSourceProperty(), value, last);
	    }
	    for (PropertyReference ref : referenceRelays.get(prop)) {
		Object oldValue = ref.getDefaultValue(); 
		Object newValue = ref.getDefaultValue();
		if (last != null) {
		    oldValue = ((ScoreBoardEventProvider)last).get(ref.getTargetProperty());
		    ((ScoreBoardEventProviderImpl)last).removeReferenceSource(ref);
		}
		if (value != null) {
		    newValue = ((ScoreBoardEventProvider)value).get(ref.getTargetProperty());
		    ((ScoreBoardEventProviderImpl)value).addReferenceSource(ref);
		}
		if (!Objects.equals(oldValue, newValue)) {
		    ((ScoreBoardEventProviderImpl)ref.getSourceElement())._valueChanged(
			    ref.getSourceProperty(), newValue, oldValue);
		}
	    }
	    valueChanged(prop, value, last);
	    requestBatchEnd();
	}
    }
    protected void valueChanged(PermanentProperty prop, Object value, Object last) {}
    
    public ValueWithId childFromString(AddRemoveProperty prop, String id, String sValue) {
	synchronized (coreLock) {
	    ElementReference ref = elementReference.get(prop);
	    if (ref != null) { return getElement(ref.getRemoteClass(), sValue); }
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
	    if (item == null) { return false; }
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
	    ElementReference reference = elementReference.get(prop);
	    if (reference != null) {
		ScoreBoardEventProvider remote = (ScoreBoardEventProvider)item;
		if (reference.getRemoteProperty() instanceof PermanentProperty) {
		    remote.set((PermanentProperty)reference.getRemoteProperty(), this);
		} else if (reference.getRemoteProperty() instanceof AddRemoveProperty) {
		    remote.add((AddRemoveProperty)reference.getRemoteProperty(), this);
		}
	    }
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
	    ElementReference reference = elementReference.get(prop);
	    if (reference != null) {
		ScoreBoardEventProvider remote = (ScoreBoardEventProvider)item;
		if (reference.getRemoteProperty() instanceof PermanentProperty) {
		    if (remote != null && remote.get((PermanentProperty)reference.getRemoteProperty()) == this) {
			remote.set((PermanentProperty)reference.getRemoteProperty(), null);
		    }
		} else if (reference.getRemoteProperty() instanceof AddRemoveProperty) {
		    if (remote != null) {
			remote.remove((AddRemoveProperty)reference.getRemoteProperty(), this);
		    }
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

    public static Object getCoreLock() { return coreLock; }

    @SuppressWarnings("unchecked")
    public <T extends ScoreBoardEventProvider> T getElement(Class<T> type, String id) {
	try {
	    return (T) elements.get(type).get(id);
	} catch (NullPointerException e) { return null; }
    }
    
    protected static Object coreLock = new Object();
    
    protected ScoreBoard scoreBoard;
    protected ScoreBoardEventProvider parent;
    protected String providerName;
    protected Class<? extends ScoreBoardEventProvider> providerClass;
    protected PermanentProperty idProperty;
    
    protected List<Class<? extends Property>> properties;

    protected Set<ScoreBoardListener> scoreBoardEventListeners = new LinkedHashSet<ScoreBoardListener>();
    protected Map<PermanentProperty, Object> values = new HashMap<PermanentProperty, Object>();
    protected Map<PermanentProperty, Flag> writeProtectionOverride = new HashMap<PermanentProperty, Flag>(); 
    protected Map<PermanentProperty, PropertyReference> referenceTo = new HashMap<PermanentProperty, PropertyReference>();
    protected Map<PermanentProperty, Set<PropertyReference>> referencesFrom = new HashMap<PermanentProperty, Set<PropertyReference>>();
    protected Map<PermanentProperty, Set<PropertyReference>> referenceRelays = new HashMap<PermanentProperty, Set<PropertyReference>>();
    protected Map<Property, ElementReference> elementReference = new HashMap<Property, ElementReference>();
    protected Map<AddRemoveProperty, Map<String, ValueWithId>> children = new HashMap<AddRemoveProperty, Map<String, ValueWithId>>();
    protected Map<NumberedProperty, Integer> minIds = new HashMap<NumberedProperty, Integer>();
    protected Map<NumberedProperty, Integer> maxIds = new HashMap<NumberedProperty, Integer>();
    
    protected static Map<Class<? extends ScoreBoardEventProvider>, Map<String, ScoreBoardEventProvider>> elements =
	    new HashMap<Class<? extends ScoreBoardEventProvider>, Map<String, ScoreBoardEventProvider>>();

    public enum BatchEvent implements ScoreBoardEvent.PermanentProperty {
	START,
	END;
    }
}
