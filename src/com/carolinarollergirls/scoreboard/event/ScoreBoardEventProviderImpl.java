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
    protected ScoreBoardEventProviderImpl(ScoreBoardEventProvider parent, PermanentProperty idProp,
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
                updatedValues.put(prop, new HashSet<UpdateReference>());
                if (prop instanceof AddRemoveProperty) {
                    children.put((AddRemoveProperty)prop, new HashMap<String, ValueWithId>());
                } else if (prop instanceof PermanentProperty) {
                    referenceRelays.put((PermanentProperty)prop, new HashSet<IndirectValueReference>());
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

    public void unlink() { unlink(false); }
    protected void unlink(boolean neighborsRemoved) {
        for (Class<? extends Property> propertySet : properties) {
            for (Property prop : propertySet.getEnumConstants()) {
                if (prop instanceof AddRemoveProperty) {
                    for (ValueWithId v : getAll((AddRemoveProperty) prop)) {
                        ScoreBoardEventProviderImpl item = (ScoreBoardEventProviderImpl) v;
                        if (item.getParent() == this) {
                            item.unlink(neighborsRemoved || item instanceof NumberedScoreBoardEventProvider<?>);
                        }
                    }
                }
            }
        }
        for (ElementReference ref : elementReference.values()) {
            if (ref.getLocalProperty() instanceof PermanentProperty && get((PermanentProperty) ref.getLocalProperty()) != null) {
                ScoreBoardEventProvider remote = (ScoreBoardEventProvider)get((PermanentProperty) ref.getLocalProperty());
                if (ref.getRemoteProperty() instanceof PermanentProperty) {
                    remote.set((PermanentProperty) ref.getRemoteProperty(), ref.getAlternateTarget(this, neighborsRemoved));
                } else if (ref.getRemoteProperty() instanceof AddRemoveProperty) {
                    remote.remove((AddRemoveProperty) ref.getRemoteProperty(), this);
                }
            } else if (ref.getLocalProperty() instanceof AddRemoveProperty) {
                for (ValueWithId remote : getAll((AddRemoveProperty) ref.getLocalProperty())) {
                    if (ref.getRemoteProperty() instanceof PermanentProperty) {
                        ((ScoreBoardEventProvider)remote).set(
                                (PermanentProperty) ref.getRemoteProperty(), ref.getAlternateTarget(this, neighborsRemoved));
                    } else if (ref.getRemoteProperty() instanceof AddRemoveProperty) {
                        ((ScoreBoardEventProvider)remote).remove((AddRemoveProperty) ref.getRemoteProperty(), this);
                    }
                }
            }
        }
        for (UpdateReference ref : incomingUpdates) {
            if (ref.getWatchedElement() != null) {
                ((ScoreBoardEventProviderImpl) ref.getWatchedElement()).removeUpdateTarget(ref);
            }
            if (ref instanceof IndirectValueReference) {
                ((ScoreBoardEventProviderImpl)((IndirectValueReference)ref).getIndirectionElement()).referenceRelays.get(
                        ((IndirectValueReference)ref).getIndirectionProperty()).remove(ref);
            }
        }
        getParent().remove(ownType, this);
    }

    public void addWriteProtection(Property prop) { addWriteProtectionOverride(prop, null); }
    public void addWriteProtectionOverride(Property prop, Flag override) {
        writeProtectionOverride.put(prop, override);
    }

    protected void addReference(ElementReference reference) {
        elementReference.put(reference.getLocalProperty(), reference);
    }
    protected void addReference(UpdateReference reference) {
        if (reference instanceof IndirectValueReference) {
            IndirectValueReference ref = (IndirectValueReference)reference;
            ((ScoreBoardEventProviderImpl)ref.getIndirectionElement()).referenceRelays.get(ref.getIndirectionProperty()).add(ref);
        }
        if (reference instanceof ValueReference) {
            ValueReference ref= (ValueReference)reference;
            ScoreBoardEventProviderImpl updatedElement = (ScoreBoardEventProviderImpl)ref.getUpdatedElement();
            if (ref.isReadOnly()) {
                updatedElement.addWriteProtection(ref.getUpdatedProperty());
            }
            updatedElement.valueSources.put(ref.getUpdatedProperty(), ref);
        }
        if (reference.getWatchedElement() != null) {
            ((ScoreBoardEventProviderImpl)reference.getWatchedElement()).addUpdateTarget(reference);
        }
        ((ScoreBoardEventProviderImpl)reference.getUpdatedElement()).incomingUpdates.add(reference);
    }
    protected void addUpdateTarget(UpdateReference reference) {
        updatedValues.get(reference.getWatchedProperty()).add(reference);
    }
    protected void removeUpdateTarget(UpdateReference reference) {
        updatedValues.get(reference.getWatchedProperty()).remove(reference);
    }
    public Object valueFromString(PermanentProperty prop, String sValue, Flag flag) {
        synchronized (coreLock) {
            ElementReference ref = elementReference.get(prop);
            if (ref != null) {
                return getElement(ref.getRemoteClass(), sValue);
            }
            Object old;
            ValueReference reference = valueSources.get(prop);
            if (reference != null) {
                if (reference.getWatchedElement() != null) {
                    return reference.getWatchedElement().valueFromString(reference.getWatchedProperty(), sValue, flag);
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
            ValueReference reference = valueSources.get(prop);
            if (reference != null) {
                if (reference.getWatchedElement() == null) { return reference.getDefaultValue(); }
                return reference.getWatchedElement().get(reference.getWatchedProperty());
            }
            return values.get(prop);
        }
    }
    public boolean set(PermanentProperty prop, Object value) { return set(prop, value, null); }
    public boolean set(PermanentProperty prop, Object value, Flag flag) {
        synchronized (coreLock) {
            boolean foreign = true;
            for (Class<? extends Property> pc : properties) {
                if (pc.isAssignableFrom(prop.getClass())) { foreign = false; break; }
            }
            if (foreign) { return false; }
            ValueReference reference = valueSources.get(prop);
            if (reference != null) {
                if (reference.isReadOnly() || reference.getWatchedElement() == null || flag == Flag.FROM_AUTOSAVE) {
                    return false;
                } else {
                    return reference.getWatchedElement().set(reference.getWatchedProperty(), value, flag);
                }
            }
            if (writeProtectionOverride.containsKey(prop) && (writeProtectionOverride.get(prop) == null ||
                    writeProtectionOverride.get(prop) != flag)) {
                return false;
            }
            Object last = values.get(prop);
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
        scoreBoardChange(new ScoreBoardEvent(this, prop, value, last));
        if (prop == idProperty) {
            elements.get(providerClass).remove(String.valueOf(last));
            elements.get(providerClass).put(String.valueOf(value), this);
            if (! writeProtectionOverride.containsKey(prop)) {
                addWriteProtectionOverride(prop, Flag.FROM_AUTOSAVE);
            }
        }
        ElementReference reference = elementReference.get(prop);
        if (reference != null) {
            ScoreBoardEventProvider lastRemote = 
                    last instanceof ScoreBoardEventProvider ? (ScoreBoardEventProvider)last : null;
            ScoreBoardEventProvider newRemote = 
                    value instanceof ScoreBoardEventProvider ? (ScoreBoardEventProvider)value : null;
            if (reference.getRemoteProperty() instanceof PermanentProperty) {
                if (lastRemote != null && lastRemote.get((PermanentProperty)reference.getRemoteProperty()) == this) {
                    lastRemote.set((PermanentProperty)reference.getRemoteProperty(), null, Flag.INTERNAL);
                }
                if (newRemote != null) {
                    newRemote.set((PermanentProperty)reference.getRemoteProperty(), this, Flag.INTERNAL);
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
        for (UpdateReference ref : updatedValues.get(prop)) {
            if (ref instanceof ValueReference) {
                ((ScoreBoardEventProviderImpl)ref.getUpdatedElement())._valueChanged(ref.getUpdatedProperty(), value, last, flag);
            } else {
                ref.getUpdatedElement().set(ref.getUpdatedProperty(), ref.getUpdatedElement().get(ref.getUpdatedProperty()), Flag.INTERNAL);
            }
        }
        for (ValueReference ref : referenceRelays.get(prop)) {
            if (value instanceof ScoreBoardEventProvider || value == null) {
                ref.setWatchedElement((ScoreBoardEventProvider) value);
            }
        }
        valueChanged(prop, value, last, flag);
        requestBatchEnd();
    }
    protected void valueChanged(PermanentProperty prop, Object value, Object last, Flag flag) {}

    public ValueWithId childFromString(AddRemoveProperty prop, String id, String sValue) {
        synchronized (coreLock) {
            ElementReference ref = elementReference.get(prop);
            if (ref != null) { return getElement(ref.getRemoteClass(), sValue); }
            ValueWithId v = create(prop, id);
            if ( v != null) { return v; }
            return new ValWithId(id, sValue);
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
    public NumberedScoreBoardEventProvider<?> getFirst(NumberedProperty prop) {
        synchronized (coreLock) {
            return (NumberedScoreBoardEventProvider<?>)get(prop, minIds.get(prop));
        }
    }
    public NumberedScoreBoardEventProvider<?> getLast(NumberedProperty prop) {
        synchronized (coreLock) {
            return (NumberedScoreBoardEventProvider<?>)get(prop, maxIds.get(prop));
        }
    }
    public Collection<? extends ValueWithId> getAll(AddRemoveProperty prop) {
        synchronized (coreLock) {
            return new HashSet<ValueWithId>(children.get(prop).values());
        }
    }
    public boolean add(AddRemoveProperty prop, ValueWithId item) {
        synchronized (coreLock) {
            if (item == null || writeProtectionOverride.containsKey(prop)) { return false; }
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
            int num = ((NumberedScoreBoardEventProvider<?>)item).getNumber();
            if (minIds.get(prop) == null || num < minIds.get(prop)) { minIds.put((NumberedProperty)prop,  num); }
            if (maxIds.get(prop) == null || num > maxIds.get(prop)) { maxIds.put((NumberedProperty)prop,  num); }
        }
        scoreBoardChange(new ScoreBoardEvent(this, prop, item, false));
        if (item instanceof ParentOrderedScoreBoardEventProvider<?> && ((ScoreBoardEventProvider) item).getParent() == this) {
            ((ScoreBoardEventProvider) item).set(IValue.NEXT, null);
            ((ScoreBoardEventProvider) item).set(IValue.PREVIOUS, null);
        }
        ElementReference reference = elementReference.get(prop);
        if (reference != null) {
            ScoreBoardEventProvider remote = (ScoreBoardEventProvider)item;
            if (reference.getRemoteProperty() instanceof PermanentProperty) {
                remote.set((PermanentProperty)reference.getRemoteProperty(), this);
            } else if (reference.getRemoteProperty() instanceof AddRemoveProperty) {
                remote.add((AddRemoveProperty)reference.getRemoteProperty(), this);
            }
        }
        for (UpdateReference ref : updatedValues.get(prop)) {
            ref.getUpdatedElement().set(ref.getUpdatedProperty(), ref.getUpdatedElement().get(ref.getUpdatedProperty()), Flag.INTERNAL);
        }
        itemAdded(prop, item);
    }
    protected void itemAdded(AddRemoveProperty prop, ValueWithId item) {}
    public ValueWithId create(AddRemoveProperty prop, String id) { return null; }
    public boolean remove(AddRemoveProperty prop, String id) { return remove(prop, get(prop, id)); }
    public boolean remove(AddRemoveProperty prop, ValueWithId item) {
        synchronized (coreLock) {
            if (item == null || writeProtectionOverride.containsKey(prop)) { return false; }
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
        scoreBoardChange(new ScoreBoardEvent(this, prop, item, true));
        if (prop instanceof NumberedProperty) {
            NumberedProperty nprop = (NumberedProperty) prop;
            if (getAll(nprop).isEmpty()) {
                minIds.remove(nprop);
                maxIds.remove(nprop);
            } else {
                int num = ((NumberedScoreBoardEventProvider<?>)item).getNumber();
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
        for (UpdateReference ref : updatedValues.get(prop)) {
            ref.getUpdatedElement().set(ref.getUpdatedProperty(), ref.getUpdatedElement().get(ref.getUpdatedProperty()), Flag.INTERNAL);
        }
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

    @SuppressWarnings("unchecked")
    public <T extends ScoreBoardEventProvider> T getElement(Class<T> type, String id) {
        try {
            return (T) elements.get(type).get(id);
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

    protected Map<PermanentProperty, Object> values = new HashMap<PermanentProperty, Object>();
    protected Map<Property, Flag> writeProtectionOverride = new HashMap<Property, Flag>();
    protected Map<PermanentProperty, ValueReference> valueSources = new HashMap<PermanentProperty, ValueReference>();
    protected Map<Property, Set<UpdateReference>> updatedValues = new HashMap<Property, Set<UpdateReference>>();
    protected Map<PermanentProperty, Set<IndirectValueReference>> referenceRelays = new HashMap<PermanentProperty, Set<IndirectValueReference>>();
    protected Set<UpdateReference> incomingUpdates = new HashSet<UpdateReference>();
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

    public static class UpdateReference {
        public UpdateReference(ScoreBoardEventProvider updatedElement, PermanentProperty updatedProperty,
                ScoreBoardEventProvider watchedElement, Property watchedProperty) {
            this.updatedElement = updatedElement;
            this.updatedProperty = updatedProperty;
            this.watchedElement = watchedElement;
            this.watchedProperty = watchedProperty;
        }

        public ScoreBoardEventProvider getUpdatedElement() { return updatedElement; }
        public PermanentProperty getUpdatedProperty() { return updatedProperty; }
        public ScoreBoardEventProvider getWatchedElement() { return watchedElement; }
        public Property getWatchedProperty() { return watchedProperty; }
        public void setWatchedElement(ScoreBoardEventProvider we) {
            if (watchedElement == we) { return; }
            if (watchedElement != null) {
                ((ScoreBoardEventProviderImpl)watchedElement).removeUpdateTarget(this);
            }
            watchedElement = we;
            if (watchedElement != null) {
                ((ScoreBoardEventProviderImpl)watchedElement).addUpdateTarget(this);
            }
            updatedElement.set(updatedProperty, updatedElement.get(updatedProperty), Flag.INTERNAL);
        }

        protected ScoreBoardEventProvider updatedElement;
        protected PermanentProperty updatedProperty;
        protected ScoreBoardEventProvider watchedElement;
        protected Property watchedProperty;
    }

    public static class ValueReference extends UpdateReference {
        public ValueReference(ScoreBoardEventProvider updatedElement, PermanentProperty updatedProperty,
                ScoreBoardEventProvider watchedElement, PermanentProperty watchedProperty, boolean readonly,
                Object defaultValue) {
            super(updatedElement, updatedProperty, watchedElement, watchedProperty);
            this.readonly = readonly;
            this.defaultValue = defaultValue;
        }

        public PermanentProperty getWatchedProperty() { return (PermanentProperty) watchedProperty; }
        public boolean isReadOnly() { return readonly; }
        public Object getDefaultValue() { return defaultValue; }
        public void setWatchedElement(ScoreBoardEventProvider we) {
            if (watchedElement == we) { return; }
            Object oldValue = getDefaultValue(); 
            Object newValue = getDefaultValue();
            if (watchedElement != null) {
                oldValue = (watchedElement).get(getWatchedProperty());
                ((ScoreBoardEventProviderImpl)watchedElement).removeUpdateTarget(this);
            }
            watchedElement = we;
            if (watchedElement != null) {
                newValue = (watchedElement).get(getWatchedProperty());
                ((ScoreBoardEventProviderImpl)watchedElement).addUpdateTarget(this);
            }
            if (!Objects.equals(oldValue, newValue)) {
                ((ScoreBoardEventProviderImpl)getUpdatedElement())._valueChanged(
                        updatedProperty, newValue, oldValue, null);
            }
        }

        protected boolean readonly;
        protected Object defaultValue;
    }

    public static class IndirectValueReference extends ValueReference {
        public IndirectValueReference(ScoreBoardEventProvider updatedElement, PermanentProperty updatedProperty,
                ScoreBoardEventProvider indirectionElement, PermanentProperty indirectionProperty,
                PermanentProperty watchedProperty, boolean readonly, Object defaultValue) {
            super(updatedElement, updatedProperty, (ScoreBoardEventProvider) indirectionElement.get(indirectionProperty),
                    watchedProperty, readonly, defaultValue);
            this.indirectionElement = indirectionElement;
            this.indirectionProperty = indirectionProperty;
        }

        public ScoreBoardEventProvider getIndirectionElement() { return indirectionElement; }
        public PermanentProperty getIndirectionProperty() { return indirectionProperty; }

        private ScoreBoardEventProvider indirectionElement;
        private PermanentProperty indirectionProperty;
    }

    public static class ElementReference {
        public ElementReference(Property localProperty, Class<? extends ScoreBoardEventProvider> remoteClass,
                Property remoteProperty) {
            this(localProperty, remoteClass, remoteProperty, AlternateTargetPolicy.NONE);
        }
        public ElementReference(Property localProperty, Class<? extends ScoreBoardEventProvider> remoteClass,
                Property remoteProperty, AlternateTargetPolicy alternateTargetPolicy) {
            this.localProperty = localProperty;
            this.remoteClass = remoteClass;
            this.remoteProperty = remoteProperty;
            this.alternateTargetPolicy = alternateTargetPolicy;
        }

        public enum AlternateTargetPolicy {
            NONE,
            PREVIOUS,
            NEXT
        }

        public Property getLocalProperty() { return localProperty; }
        public Class<? extends ScoreBoardEventProvider> getRemoteClass() { return remoteClass; }
        public Property getRemoteProperty() { return remoteProperty; }
        public ScoreBoardEventProvider getAlternateTarget(ScoreBoardEventProvider current, boolean neighborsRemoved) {
            if (!neighborsRemoved && current instanceof OrderedScoreBoardEventProvider<?>) {
                if (alternateTargetPolicy == AlternateTargetPolicy.PREVIOUS) {
                    return ((OrderedScoreBoardEventProvider<?>)current).getPrevious();
                } else if (alternateTargetPolicy == AlternateTargetPolicy.NEXT) {
                    return ((OrderedScoreBoardEventProvider<?>)current).getNext();
                }
            }
            return null;
        }

        private Property localProperty;
        private Class<? extends ScoreBoardEventProvider> remoteClass;
        private Property remoteProperty;
        private AlternateTargetPolicy alternateTargetPolicy;
    }
}
