package com.carolinarollergirls.scoreboard.event;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.ArrayList;
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
import com.carolinarollergirls.scoreboard.event.OrderedScoreBoardEventProvider.IValue;
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
    protected ScoreBoardEventProviderImpl(ScoreBoardEventProvider parent, PermanentProperty idProp, String id,
            AddRemoveProperty type, Class<? extends ScoreBoardEventProvider> ownClass, Class<? extends Property>... props) {
        this.parent = parent;
        if (parent != null) {
            scoreBoard = parent.getScoreBoard();
        } else if (this instanceof ScoreBoard) {
            scoreBoard = (ScoreBoard)this;
        }
        idProperty = idProp;
        ownType = type;
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
                }
            }
        }
        set(idProperty, id);
        addWriteProtection(idProp);
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
        // need to copy the list as some listeners may add or remove listeners
        synchronized(scoreBoardEventListeners) {
            for (ScoreBoardListener l : new ArrayList<ScoreBoardListener>(scoreBoardEventListeners)) {
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

    public void unlink() { unlink(false); }
    protected void unlink(boolean neighborsRemoved) {
        for (Class<? extends Property> propertySet : properties) {
            for (Property prop : propertySet.getEnumConstants()) {
                if (prop instanceof AddRemoveProperty) {
                    for (ValueWithId v : getAll((AddRemoveProperty) prop)) {
                        ScoreBoardEventProviderImpl item = (ScoreBoardEventProviderImpl) v;
                        if (item.getParent() == this) {
                            item.unlink(neighborsRemoved || item instanceof NumberedScoreBoardEventProvider<?>);
                        } else {
                            remove((AddRemoveProperty) prop, item);
                        }
                    }
                } else if (prop instanceof PermanentProperty &&
                        ScoreBoardEventProvider.class.isAssignableFrom(prop.getType())) {
                    set((PermanentProperty) prop, null);
                }
            }
        }
        for (ScoreBoardListener l : providers.keySet()) {
            if (l instanceof UnlinkableScoreBoardListener) {
                ((UnlinkableScoreBoardListener) l).unlink();
            } else {
                providers.get(l).removeScoreBoardListener(l);
            }
        }
        getParent().remove(ownType, this);
    }

    public void addWriteProtection(Property prop) { addWriteProtectionOverride(prop, null); }
    public void addWriteProtectionOverride(Property prop, Flag override) {
        writeProtectionOverride.put(prop, override);
    }
    public boolean isWritable(Property prop, Flag flag) {
        if (!writeProtectionOverride.containsKey(prop)) { return true; }
        if (writeProtectionOverride.get(prop) == null) { return false; }
        if (writeProtectionOverride.get(prop) == flag) { return true; }
        return false;
    }
    
    protected ScoreBoardListener setCopy(PermanentProperty targetProperty, ScoreBoardEventProvider sourceElement,
            PermanentProperty sourceProperty, boolean readonly) {
        ScoreBoardListener l = new ConditionalScoreBoardListener(sourceElement, sourceProperty,
                new CopyScoreBoardListener(this, targetProperty));
        sourceElement.addScoreBoardListener(l);
        providers.put(l, sourceElement);
        if (readonly) {
            addWriteProtectionOverride(targetProperty, Flag.COPY);
        } else {
            reverseCopyListeners.put(targetProperty, 
                    new CopyScoreBoardListener(sourceElement, sourceProperty, false));
        }
        set(targetProperty, sourceElement.get(sourceProperty), Flag.COPY);
        return l;
    }
    protected ScoreBoardListener setCopy(final PermanentProperty targetProperty, ScoreBoardEventProvider indirectionElement,
            PermanentProperty indirectionProperty, final PermanentProperty sourceProperty, boolean readonly) {
        ScoreBoardListener l = new IndirectScoreBoardListener(indirectionElement, indirectionProperty, sourceProperty,
                new CopyScoreBoardListener(this, targetProperty));
        providers.put(l, null);
        if (readonly) {
            addWriteProtectionOverride(targetProperty, Flag.COPY);
        } else {
            ScoreBoardListener reverseListener = 
            new ConditionalScoreBoardListener(indirectionElement, indirectionProperty,
                    new ScoreBoardListener() {
                        public void scoreBoardChange(ScoreBoardEvent event) {
                            reverseCopyListeners.put(targetProperty,
                                    new CopyScoreBoardListener((ScoreBoardEventProvider) event.getValue(),
                                            sourceProperty, false));
                        }
                    });
            indirectionElement.addScoreBoardListener(reverseListener);
            reverseListener.scoreBoardChange(new ScoreBoardEvent(indirectionElement, indirectionProperty,
                    indirectionElement.get(indirectionProperty), null));
        }
        return l;
    }
    protected RecalculateScoreBoardListener setRecalculated(PermanentProperty targetProperty) {
        RecalculateScoreBoardListener l = new RecalculateScoreBoardListener(this, targetProperty);
        providers.put(l, null);
        return l;
    }
    protected InverseReferenceUpdateListener setInverseReference(Property localProperty, Property remoteProperty) {
        InverseReferenceUpdateListener l = new InverseReferenceUpdateListener(this, localProperty, remoteProperty);
        addScoreBoardListener(l);
        return l;
    }

    public Object valueFromString(PermanentProperty prop, String sValue, Flag flag) {
        synchronized (coreLock) {
            if (sValue == null) { return null; }
            if (prop == IValue.PREVIOUS || prop == IValue.NEXT) {
                return getElement(providerClass, sValue);
            }
            @SuppressWarnings("rawtypes")
            Class type = prop.getType();
            if (ScoreBoardEventProvider.class.isAssignableFrom(type)) {
                return getElement(type, sValue);
            }
            Object value = sValue;
            if (type == Role.class) {
                value = Role.fromString(sValue);
            }
            if (type == FloorPosition.class) {
                value = FloorPosition.fromString(sValue);
            }
            if (type == TimeoutOwner.class) {
                value = scoreBoard.getTimeoutOwner(sValue);
            }
            if (type == Boolean.class) { 
                value = Boolean.valueOf(sValue); 
            }
            if (type == Integer.class) {
                value = Integer.valueOf(sValue);
            }
            if (type == Long.class) {
                value = Long.valueOf(sValue);
            }
            if ("".equals(sValue) && !(type == String.class)) {
                value = null;
            }
            return value;
        }
    }
    public Object get(PermanentProperty prop) {
        synchronized (coreLock) {
            if (!values.containsKey(prop)) return prop.getDefaultValue();
            return values.get(prop);
        }
    }
    public boolean set(PermanentProperty prop, Object value) { return set(prop, value, null); }
    public boolean set(PermanentProperty prop, Object value, Flag flag) {
        synchronized (coreLock) {
            if (prop == null) { return false; }
            boolean foreign = true;
            for (Class<? extends Property> pc : properties) {
                if (pc.isAssignableFrom(prop.getClass())) { foreign = false; break; }
            }
            if (foreign) { return false; }
            if (value != null && !prop.getType().isAssignableFrom(value.getClass())) { return false; }
            if (prop == idProperty && flag == Flag.FROM_AUTOSAVE) {
                // register ID as an alias so other elements from autosave are properly redirected
                elements.get(providerClass).put((String) value, this);
                return false;
            }
            if (!isWritable(prop, flag)) { return false; }
            Object last = get(prop);
            if (reverseCopyListeners.containsKey(prop) && flag != Flag.COPY) {
                reverseCopyListeners.get(prop).scoreBoardChange(new ScoreBoardEvent(
                        this, prop, value, last));
                return false;
            }
            value = _computeValue(prop, value, last, flag);
            if (Objects.equals(value, last)) { return false; }
            values.put(prop, value);
            _valueChanged(prop, value, last, flag);
            return true;
        }
    }
    protected Object _computeValue(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (flag == Flag.CHANGE) {
            if (last instanceof Integer) {
                value = (Integer)last + (Integer)value;
            } else if (last instanceof Long) {
                value = (Long)last + (Long)value;
            }
        }
        return computeValue(prop, value, last, flag);
    }
    protected Object computeValue(PermanentProperty prop, Object value, Object last, Flag flag) { return value; }
    protected void _valueChanged(PermanentProperty prop, Object value, Object last, Flag flag) {
        requestBatchStart();
        if (prop == idProperty) {
            elements.get(providerClass).put((String)value, this);
        }
        scoreBoardChange(new ScoreBoardEvent(this, prop, value, last));
        valueChanged(prop, value, last, flag);
        requestBatchEnd();
    }
    protected void valueChanged(PermanentProperty prop, Object value, Object last, Flag flag) {}

    public ValueWithId childFromString(AddRemoveProperty prop, String id, String sValue) {
        synchronized (coreLock) {
            if (prop.getType() == ValWithId.class) {
                return new ValWithId(id, sValue);
            }
            return (ValueWithId) getElement(prop.getType(), sValue);
        }
    }
    public ValueWithId get(AddRemoveProperty prop, String id) {
        if(children.get(prop) == null) { return null; }
        return children.get(prop).get(id);
    }
    public ValueWithId get(NumberedProperty prop, Integer num) { return get(prop, String.valueOf(num)); }
    public ValueWithId getOrCreate(AddRemoveProperty prop, String id) {
        synchronized (coreLock) {
            ValueWithId result = get(prop, id);
            if (result == null) {
                result = create(prop, id);
                add(prop, result);
            }
            return result;
        }
    }
    public ValueWithId getOrCreate(NumberedProperty prop, Integer num) { return getOrCreate(prop, String.valueOf(num)); }
    public OrderedScoreBoardEventProvider<?> getFirst(NumberedProperty prop) {
        synchronized (coreLock) {
            return (OrderedScoreBoardEventProvider<?>)get(prop, minIds.get(prop));
        }
    }
    public OrderedScoreBoardEventProvider<?> getLast(NumberedProperty prop) {
        synchronized (coreLock) {
            return (OrderedScoreBoardEventProvider<?>)get(prop, maxIds.get(prop));
        }
    }
    public Collection<? extends ValueWithId> getAll(AddRemoveProperty prop) {
        synchronized (coreLock) {
            return new HashSet<ValueWithId>(children.get(prop).values());
        }
    }
    public boolean add(AddRemoveProperty prop, ValueWithId item) {
        synchronized (coreLock) {
            if (item == null || !isWritable(prop, null)) { return false; }
            if (item != null && !prop.getType().isAssignableFrom(item.getClass())) { return false; }
            Map<String, ValueWithId> map = children.get(prop);
            String id = item.getId();
            if (item instanceof ScoreBoardEventProvider && ((ScoreBoardEventProvider)item).getParent() == this) {
                id = ((ScoreBoardEventProvider)item).getProviderId();
            }
            if (map.containsKey(id) && map.get(id).equals(item)) { return false; }
            map.put(id, item);
            _itemAdded(prop, item);
            return true;
        }
    }
    protected void _itemAdded(AddRemoveProperty prop, ValueWithId item) {
        if (item instanceof ScoreBoardEventProvider && ((ScoreBoardEventProvider)item).getParent() == this) {
            ((ScoreBoardEventProvider)item).addScoreBoardListener(this);
        }
        if (prop instanceof NumberedProperty) {
            int num = ((OrderedScoreBoardEventProvider<?>)item).getNumber();
            if (minIds.get(prop) == null || num < minIds.get(prop)) { minIds.put((NumberedProperty)prop,  num); }
            if (maxIds.get(prop) == null || num > maxIds.get(prop)) { maxIds.put((NumberedProperty)prop,  num); }
        }
        scoreBoardChange(new ScoreBoardEvent(this, prop, item, false));
        itemAdded(prop, item);
    }
    protected void itemAdded(AddRemoveProperty prop, ValueWithId item) {}
    public ValueWithId create(AddRemoveProperty prop, String id) { return null; }
    public boolean remove(AddRemoveProperty prop, String id) { return remove(prop, get(prop, id)); }
    public boolean remove(AddRemoveProperty prop, ValueWithId item) {
        synchronized (coreLock) {
            if (item == null || !isWritable(prop, null)) { return false; }
            String id = item.getId();
            if (item instanceof ScoreBoardEventProvider && ((ScoreBoardEventProvider)item).getParent() == this) {
                id = ((ScoreBoardEventProvider)item).getProviderId();
            }
            if (children.get(prop).remove(id, item)) {
                _itemRemoved(prop, item);
            }
            return true;
        }
    }
    protected void _itemRemoved(AddRemoveProperty prop, ValueWithId item) {
        requestBatchStart();
        if (item instanceof ScoreBoardEventProvider) {
            ((ScoreBoardEventProvider)item).removeScoreBoardListener(this);
        }
        if (prop instanceof NumberedProperty) {
            NumberedProperty nprop = (NumberedProperty) prop;
            if (getAll(nprop).isEmpty()) {
                minIds.remove(nprop);
                maxIds.remove(nprop);
            } else {
                int num = ((OrderedScoreBoardEventProvider<?>)item).getNumber();
                if (num == getMaxNumber(nprop)) {
                    while (get(nprop, num) == null) { num --; }
                    maxIds.put(nprop, num);
                }
                if (num == getMinNumber(nprop)) {
                    while (get(nprop, num) == null) { num ++; }
                    minIds.put(nprop, num);
                }
            }
        }
        scoreBoardChange(new ScoreBoardEvent(this, prop, item, true));
        itemRemoved(prop, item);
        requestBatchEnd();
    }
    protected void itemRemoved(AddRemoveProperty prop, ValueWithId item) {}
    public void removeAll(AddRemoveProperty prop) {
        synchronized (coreLock) {
            for (ValueWithId item : getAll(prop)) {
                remove(prop, item);
            }
        }
    }
    public Integer getMinNumber(NumberedProperty prop) { return minIds.get(prop); }
    public Integer getMaxNumber(NumberedProperty prop) { return maxIds.get(prop); }

    public void execute(CommandProperty prop) {  }

    public ScoreBoard getScoreBoard() { return scoreBoard; }

    public static Object getCoreLock() { return coreLock; }

    public ScoreBoardEventProvider getElement(Class<?> type, String id) {
        try {
            return elements.get(type).get(id);
        } catch (NullPointerException e) { return null; }
    }

    protected static Object coreLock = new Object();

    protected ScoreBoard scoreBoard;
    protected ScoreBoardEventProvider parent;
    protected AddRemoveProperty ownType;
    protected String providerName;
    protected Class<? extends ScoreBoardEventProvider> providerClass;
    protected PermanentProperty idProperty;

    protected List<Class<? extends Property>> properties;

    protected Set<ScoreBoardListener> scoreBoardEventListeners = new LinkedHashSet<ScoreBoardListener>();
    protected Map<ScoreBoardListener, ScoreBoardEventProvider> providers = new HashMap<ScoreBoardListener, ScoreBoardEventProvider>();
    
    protected Map<PermanentProperty, Object> values = new HashMap<PermanentProperty, Object>();
    protected Map<Property, Flag> writeProtectionOverride = new HashMap<Property, Flag>();
    protected Map<PermanentProperty, ScoreBoardListener> reverseCopyListeners = new HashMap<PermanentProperty, ScoreBoardListener>();

    protected Map<AddRemoveProperty, Map<String, ValueWithId>> children = new HashMap<AddRemoveProperty, Map<String, ValueWithId>>();
    protected Map<NumberedProperty, Integer> minIds = new HashMap<NumberedProperty, Integer>();
    protected Map<NumberedProperty, Integer> maxIds = new HashMap<NumberedProperty, Integer>();

    protected static Map<Class<? extends ScoreBoardEventProvider>, Map<String, ScoreBoardEventProvider>> elements =
            new HashMap<Class<? extends ScoreBoardEventProvider>, Map<String, ScoreBoardEventProvider>>();

    public enum BatchEvent implements ScoreBoardEvent.PermanentProperty {
        START(true),
        END(false);
        
        private BatchEvent(Boolean v) { defaultValue = v; }
        private final Boolean defaultValue;        
        public Class<Boolean> getType() { return Boolean.class; }
        public Boolean getDefaultValue() { return defaultValue; }
    }
}
