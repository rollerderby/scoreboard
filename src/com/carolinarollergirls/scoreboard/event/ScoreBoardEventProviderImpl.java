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
import com.carolinarollergirls.scoreboard.rules.RuleDefinition;
import com.carolinarollergirls.scoreboard.utils.Logger;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public abstract class ScoreBoardEventProviderImpl<C extends ScoreBoardEventProvider>
        implements ScoreBoardEventProvider, ScoreBoardListener {
    @SuppressWarnings("unchecked")
    protected ScoreBoardEventProviderImpl(ScoreBoardEventProvider parent, String id, AddRemoveProperty<C> type) {
        this.parent = parent;
        if (parent != null) {
            scoreBoard = parent.getScoreBoard();
        } else if (this instanceof ScoreBoard) {
            scoreBoard = (ScoreBoard) this;
        }
        ownType = type;
        if (type == null) {
            providerName = "ScoreBoard";
            providerClass = (Class<C>) ScoreBoard.class;
        } else {
            providerName = type.getJsonName();
            providerClass = type.getType();
        }
        if (elements.get(providerClass) == null) {
            elements.put(providerClass, new HashMap<String, ScoreBoardEventProvider>());
        }
        addProperties(ID, READONLY);

        set(ID, id, Source.OTHER);
        addWriteProtection(ID);
    }

    @Override
    public String getId() { return get(ID); }
    @Override
    public String getProviderName() { return providerName; }
    @Override
    public Class<? extends ScoreBoardEventProvider> getProviderClass() { return providerClass; }
    @Override
    public String getProviderId() { return getId(); }
    @Override
    public String getValue() { return getId(); }
    @Override
    public String toString() { return getId(); }
    @Override
    public List<Property<?>> getProperties() { return properties; }
    @Override
    public ScoreBoardEventProvider getParent() { return parent; }

    @Override
    public void scoreBoardChange(ScoreBoardEvent<?> event) {
        dispatch(event);
    }

    protected void dispatch(ScoreBoardEvent<?> event) {
        // Synchronously send events to listeners.
        // need to copy the list as some listeners may add or remove listeners
        synchronized (scoreBoardEventListeners) {
            for (ScoreBoardListener l : new ArrayList<>(scoreBoardEventListeners)) {
                l.scoreBoardChange(event);
            }
        }
    }

    protected void requestBatchStart() {
        scoreBoardChange(new ScoreBoardEvent<>(this, BATCH_START, Boolean.TRUE, Boolean.TRUE));
    }

    protected void requestBatchEnd() {
        scoreBoardChange(new ScoreBoardEvent<>(this, BATCH_END, Boolean.TRUE, Boolean.TRUE));
    }

    @Override
    public void runInBatch(Runnable r) {
        synchronized (coreLock) {
            requestBatchStart();
            try {
                r.run();
            } finally {
                requestBatchEnd();
            }
        }
    }

    @Override
    public void addScoreBoardListener(ScoreBoardListener listener) {
        synchronized (scoreBoardEventListeners) {
            scoreBoardEventListeners.add(listener);
        }
    }
    @Override
    public void removeScoreBoardListener(ScoreBoardListener listener) {
        synchronized (scoreBoardEventListeners) {
            scoreBoardEventListeners.remove(listener);
        }
    }

    @Override
    public int compareTo(ScoreBoardEventProvider other) {
        if (other == null) { return -1; }
        if (getParent() == other.getParent()) { return 0; }
        if (getParent() == null) { return 1; }
        if (getParent() instanceof NumberedScoreBoardEventProvider<?>
                && other.getParent() instanceof NumberedScoreBoardEventProvider<?>) {
            return ((NumberedScoreBoardEventProvider<?>) getParent())
                    .compareTo((NumberedScoreBoardEventProvider<?>) other.getParent());
        }
        return getParent().compareTo(other.getParent());
    }

    @Override
    public void delete() { delete(Source.OTHER); }
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void delete(Source source) {
        if (get(READONLY) && source != Source.UNLINK) { return; }
        for (Property prop : properties) {
            if (prop instanceof AddRemoveProperty) {
                for (ValueWithId item : getAll((AddRemoveProperty<?>) prop)) {
                    if (item instanceof ScoreBoardEventProvider
                            && ((ScoreBoardEventProvider) item).getParent() == this) {
                        ((ScoreBoardEventProvider) item).delete(Source.UNLINK);
                    } else {
                        remove((AddRemoveProperty) prop, item, Source.UNLINK);
                    }
                }
            } else if (prop instanceof PermanentProperty
                    && ScoreBoardEventProvider.class.isAssignableFrom(prop.getType())) {
                set((PermanentProperty) prop, null, Source.UNLINK);
            }
        }
        for (ScoreBoardListener l : providers.keySet()) {
            if (l instanceof SelfRemovingScoreBoardListener) {
                ((SelfRemovingScoreBoardListener) l).delete();
            } else {
                providers.get(l).removeScoreBoardListener(l);
            }
        }
        getParent().remove(ownType, (C) this, Source.UNLINK);
    }

    public void addWriteProtection(Property<?> prop) { addWriteProtectionOverride(prop, null); }
    public void addWriteProtectionOverride(Property<?> prop, Source override) {
        writeProtectionOverride.put(prop, override);
    }
    public boolean isWritable(Property<?> prop, Source source) {
        if (source == Source.UNLINK && prop != ID && prop != READONLY && prop != OrderedScoreBoardEventProvider.NUMBER
                && prop != PREVIOUS && prop != NEXT) {
            return true;
        }
        if (get(READONLY)) { return false; }
        if (!writeProtectionOverride.containsKey(prop)) { return true; }
        if (writeProtectionOverride.get(prop) == null || source == null) { return false; }
        if (writeProtectionOverride.get(prop) == Source.ANY_INTERNAL) { return source.isInternal(); }
        if (writeProtectionOverride.get(prop) == Source.ANY_FILE) { return source.isFile(); }
        if (writeProtectionOverride.get(prop) == source) { return true; }
        return false;
    }
    public <T extends ValueWithId> boolean isWritable(AddRemoveProperty<T> prop, String id, Source source) {
        if (source == Source.UNLINK || source == Source.RENUMBER) { return true; }
        T oldItem = get(prop, id);
        if (oldItem instanceof ScoreBoardEventProvider) {
            if (oldItem != null && ((ScoreBoardEventProvider) oldItem).get(READONLY)) {
                return false;
            }
        }
        return isWritable(prop, source);
    }

    /**
     * Make targetProperty a copy of sourceProperty on sourceElement
     */
    protected <T> ScoreBoardListener setCopy(PermanentProperty<T> targetProperty, ScoreBoardEventProvider sourceElement,
            PermanentProperty<T> sourceProperty, boolean readonly) {
        ScoreBoardListener l = new ConditionalScoreBoardListener<>(sourceElement, sourceProperty,
                new CopyScoreBoardListener<>(this, targetProperty));
        sourceElement.addScoreBoardListener(l);
        providers.put(l, sourceElement);
        if (readonly) {
            addWriteProtectionOverride(targetProperty, Source.COPY);
        } else {
            reverseCopyListeners.put(targetProperty, new CopyScoreBoardListener<>(sourceElement, sourceProperty));
        }
        set(targetProperty, sourceElement.get(sourceProperty), Source.COPY);
        return l;
    }
    /**
     * Make targetProperty a copy of sourceProperty on the element that
     * indirectionProperty on indirectionElement points to and update the reference
     * if indirectionProperty changes.
     *
     * Example: calling setCopy(Value.CURRENT_JAM_NUMBER, this, Value.CURRENT_JAM,
     * IValue.NUMBER) in a Period object ensures that Value.CURRENT_PERIOD_NUMBER
     * always contains the number of the current Jam, whereas calling the above
     * method setCopy(Value.CURRENT_JAM_NUMBER, get(Value.CURRENT_JAM),
     * IValue.NUMBER) would attach it to the number of the Jam that was current at
     * the time of calling.
     */
    protected <T, U> ScoreBoardListener setCopy(final PermanentProperty<T> targetProperty,
            ScoreBoardEventProvider indirectionElement, PermanentProperty<U> indirectionProperty,
            final PermanentProperty<T> sourceProperty, boolean readonly) {
        ScoreBoardListener l = new IndirectScoreBoardListener<>(indirectionElement, indirectionProperty, sourceProperty,
                new CopyScoreBoardListener<>(this, targetProperty));
        providers.put(l, null);
        if (readonly) {
            addWriteProtectionOverride(targetProperty, Source.COPY);
        } else {
            ScoreBoardListener reverseListener = new ConditionalScoreBoardListener<>(indirectionElement,
                    indirectionProperty, new ScoreBoardListener() {
                        @Override
                        public void scoreBoardChange(ScoreBoardEvent<?> event) {
                            reverseCopyListeners.put(targetProperty, new CopyScoreBoardListener<>(
                                    (ScoreBoardEventProvider) event.getValue(), sourceProperty));
                        }
                    });
            indirectionElement.addScoreBoardListener(reverseListener);
            reverseListener.scoreBoardChange(new ScoreBoardEvent<>(indirectionElement, indirectionProperty,
                    indirectionElement.get(indirectionProperty), null));
        }
        return l;
    }
    /**
     * recalculate targetProperty whenever one of the sources added to the listener
     * is changed
     */
    protected RecalculateScoreBoardListener<?> setRecalculated(PermanentProperty<?> targetProperty) {
        RecalculateScoreBoardListener<?> l = new RecalculateScoreBoardListener<>(this, targetProperty);
        providers.put(l, null);
        return l;
    }
    /**
     * Make sure remoteProperty on the Element(s) pointed to by localProperty points
     * back to this element
     */
    protected <T> InverseReferenceUpdateListener<T, C> setInverseReference(Property<T> localProperty,
            Property<C> remoteProperty) {
        @SuppressWarnings("unchecked")
        InverseReferenceUpdateListener<T, C> l = new InverseReferenceUpdateListener<>((C) this, localProperty,
                remoteProperty);
        addScoreBoardListener(l);
        return l;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T valueFromString(PermanentProperty<T> prop, String sValue) {
        synchronized (coreLock) {
            @SuppressWarnings("rawtypes")
            Class type = prop.getType();
            if (type == TimeoutOwner.class) { return (T) scoreBoard.getTimeoutOwner(sValue); }
            if (sValue == null) { return prop.getDefaultValue(); }
            if ("".equals(sValue) && !(type == String.class)) { return prop.getDefaultValue(); }
            if (type == RuleDefinition.Type.class) { return prop.getDefaultValue(); }
            if (type == Role.class) { return (T) Role.fromString(sValue); }
            if (type == FloorPosition.class) { return (T) FloorPosition.fromString(sValue); }
            if (type == Boolean.class) { return (T) Boolean.valueOf(sValue); }
            if (type == Integer.class) { return (T) Integer.valueOf(sValue); }
            if (type == Long.class) { return (T) Long.valueOf(sValue); }
            if (prop == PREVIOUS || prop == NEXT) {
                return (T) getElement(providerClass, sValue);
            }
            if (ScoreBoardEventProvider.class.isAssignableFrom(type)) {
                return (T) getElement(type, sValue);
            }
            if (type != String.class) {
                Logger.printMessage("Conversion to " + type.getSimpleName() + " used by " + prop.getJsonName()
                        + " missing in ScoreBoardEventProvider.valueFromString()");
                return prop.getDefaultValue();
            }
            return (T) sValue;
        }
    }
    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(PermanentProperty<T> prop) {
        synchronized (coreLock) {
            if (!values.containsKey(prop)) { return prop.getDefaultValue(); }
            return (T) values.get(prop);
        }
    }
    @Override
    public <T> boolean set(PermanentProperty<T> prop, T value) { return set(prop, value, Source.OTHER, null); }
    @Override
    public <T> boolean set(PermanentProperty<T> prop, T value, Flag flag) {
        return set(prop, value, Source.OTHER, flag);
    }
    @Override
    public <T> boolean set(PermanentProperty<T> prop, T value, Source source) { return set(prop, value, source, null); }
    @SuppressWarnings("unchecked")
    @Override
    public <T> boolean set(PermanentProperty<T> prop, T value, Source source, Flag flag) {
        synchronized (coreLock) {
            if (prop == null) { return false; }
            if (!properties.contains(prop)) {
                throw new IllegalArgumentException(
                        prop.getJsonName() + " is not a property of " + this.getClass().getName());
            }
            if (prop == ID && source.isFile()) {
                // register ID as an alias so other elements from file are properly redirected
                elements.get(providerClass).put((String) value, this);
                return false;
            }
            if (!isWritable(prop, source)) { return false; }
            T last = get(prop);
            value = (T) _computeValue(prop, value, last, source, flag);
            if (reverseCopyListeners.containsKey(prop) && source != Source.COPY) {
                reverseCopyListeners.get(prop).scoreBoardChange(new ScoreBoardEvent<>(this, prop, value, last), source);
                return false;
            }
            if (Objects.equals(value, last)) { return false; }
            values.put(prop, value);
            _valueChanged(prop, value, last, source, flag);
            return true;
        }
    }
    protected Object _computeValue(PermanentProperty<?> prop, Object value, Object last, Source source, Flag flag) {
        if (flag == Flag.CHANGE) {
            if (last instanceof Integer) {
                value = (Integer) last + (Integer) value;
            } else if (last instanceof Long) {
                value = (Long) last + (Long) value;
            }
        }
        return computeValue(prop, value, last, source, flag);
    }
    protected Object computeValue(PermanentProperty<?> prop, Object value, Object last, Source source, Flag flag) {
        return value;
    }
    protected <T> void _valueChanged(PermanentProperty<T> prop, T value, T last, Source source, Flag flag) {
        if (prop == ID) {
            elements.get(providerClass).put((String) value, this);
        }
        scoreBoardChange(new ScoreBoardEvent<>(this, prop, value, last));
        valueChanged(prop, value, last, source, flag);
    }
    protected void valueChanged(PermanentProperty<?> prop, Object value, Object last, Source source, Flag flag) {}

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ValueWithId> T childFromString(AddRemoveProperty<T> prop, String id, String sValue) {
        synchronized (coreLock) {
            if (prop.getType() == ValWithId.class) {
                return (T) new ValWithId(id, sValue);
            }
            return getElement(prop.getType(), sValue);
        }
    }
    @SuppressWarnings("unchecked")
    @Override
    public <T extends ValueWithId> T get(AddRemoveProperty<T> prop, String id) {
        if (children.get(prop) == null) { return null; }
        return (T) children.get(prop).get(id);
    }
    @Override
    public <T extends OrderedScoreBoardEventProvider<T>> T get(NumberedProperty<T> prop, Integer num) {
        return get(prop, String.valueOf(num));
    }
    @Override
    public <T extends ScoreBoardEventProvider> T getOrCreate(AddRemoveProperty<T> prop, String id) {
        return getOrCreate(prop, id, Source.OTHER);
    }
    @SuppressWarnings("unchecked")
    @Override
    public <T extends ScoreBoardEventProvider> T getOrCreate(AddRemoveProperty<T> prop, String id, Source source) {
        synchronized (coreLock) {
            T result = get(prop, id);
            if (result == null) {
                result = (T) create(prop, id, source);
                add(prop, result, source);
            }
            return result;
        }
    }
    @Override
    public <T extends OrderedScoreBoardEventProvider<T>> T getOrCreate(NumberedProperty<T> prop, Integer num) {
        return getOrCreate(prop, String.valueOf(num), Source.OTHER);
    }
    @Override
    public <T extends OrderedScoreBoardEventProvider<T>> T getOrCreate(NumberedProperty<T> prop, Integer num,
            Source source) {
        return getOrCreate(prop, String.valueOf(num), source);
    }
    @Override
    public <T extends OrderedScoreBoardEventProvider<T>> T getFirst(NumberedProperty<T> prop) {
        synchronized (coreLock) {
            return get(prop, minIds.get(prop));
        }
    }
    @Override
    public <T extends OrderedScoreBoardEventProvider<T>> T getLast(NumberedProperty<T> prop) {
        synchronized (coreLock) {
            return get(prop, maxIds.get(prop));
        }
    }
    @SuppressWarnings("unchecked")
    @Override
    public <T extends ValueWithId> Collection<T> getAll(AddRemoveProperty<T> prop) {
        synchronized (coreLock) {
            return new HashSet<>((Collection<? extends T>) children.get(prop).values());
        }
    }
    @Override
    public int numberOf(AddRemoveProperty<?> prop) {
        synchronized (coreLock) {
            if (!children.containsKey(prop)) { return 0; }
            return children.get(prop).size();
        }
    }
    @Override
    public <T extends ValueWithId> boolean add(AddRemoveProperty<T> prop, T item) {
        return add(prop, item, Source.OTHER);
    }
    @Override
    public <T extends ValueWithId> boolean add(AddRemoveProperty<T> prop, T item, Source source) {
        synchronized (coreLock) {
            if (!properties.contains(prop)) {
                throw new IllegalArgumentException(
                        prop.getJsonName() + " is not a property of " + this.getClass().getName());
            }
            if (item == null || !isWritable(prop, item.getId(), source)) { return false; }
            Map<String, ValueWithId> map = children.get(prop);
            String id = item.getId();
            if (item instanceof ScoreBoardEventProvider && ((ScoreBoardEventProvider) item).getParent() == this) {
                id = ((ScoreBoardEventProvider) item).getProviderId();
            }
            if (map.containsKey(id) && map.get(id).equals(item)) { return false; }
            map.put(id, item);
            _itemAdded(prop, item, source);
            return true;
        }
    }
    protected <T extends ValueWithId> void _itemAdded(AddRemoveProperty<T> prop, T item, Source source) {
        if (item instanceof ScoreBoardEventProvider && ((ScoreBoardEventProvider) item).getParent() == this) {
            ((ScoreBoardEventProvider) item).addScoreBoardListener(this);
        }
        if (prop instanceof NumberedProperty) {
            int num = ((OrderedScoreBoardEventProvider<?>) item).getNumber();
            if (minIds.get(prop) == null || num < minIds.get(prop)) { minIds.put((NumberedProperty<?>) prop, num); }
            if (maxIds.get(prop) == null || num > maxIds.get(prop)) { maxIds.put((NumberedProperty<?>) prop, num); }
        }
        scoreBoardChange(new ScoreBoardEvent<>(this, prop, item, false));
        itemAdded(prop, item, source);
    }
    protected void itemAdded(AddRemoveProperty<?> prop, ValueWithId item, Source source) {}
    @Override
    public ScoreBoardEventProvider create(AddRemoveProperty<?> prop, String id, Source source) {
        return null;
    }
    @Override
    public <T extends ValueWithId> boolean remove(AddRemoveProperty<T> prop, String id) {
        return remove(prop, get(prop, id), Source.OTHER);
    }
    @Override
    public <T extends ValueWithId> boolean remove(AddRemoveProperty<T> prop, String id, Source source) {
        return remove(prop, get(prop, id), source);
    }
    @Override
    public <T extends ValueWithId> boolean remove(AddRemoveProperty<T> prop, T item) {
        return remove(prop, item, Source.OTHER);
    }
    @Override
    public <T extends ValueWithId> boolean remove(AddRemoveProperty<T> prop, T item, Source source) {
        synchronized (coreLock) {
            if (item == null || !isWritable(prop, source)) { return false; }
            String id = item.getId();
            if (item instanceof ScoreBoardEventProvider && ((ScoreBoardEventProvider) item).getParent() == this) {
                id = ((ScoreBoardEventProvider) item).getProviderId();
            }
            if (children.get(prop).get(id) == item) {
                children.get(prop).remove(id);
                _itemRemoved(prop, item, source);
                return true;
            }
            return false;
        }
    }
    protected <T extends ValueWithId> void _itemRemoved(AddRemoveProperty<T> prop, T item, Source source) {
        if (item instanceof ScoreBoardEventProvider) {
            ((ScoreBoardEventProvider) item).removeScoreBoardListener(this);
        }
        if (prop instanceof NumberedProperty) {
            NumberedProperty<?> nprop = (NumberedProperty<?>) prop;
            if (numberOf(nprop) == 0) {
                minIds.remove(nprop);
                maxIds.remove(nprop);
            } else {
                int num = ((OrderedScoreBoardEventProvider<?>) item).getNumber();
                if (num == getMaxNumber(nprop)) {
                    while (get(nprop, num) == null) { num--; }
                    maxIds.put(nprop, num);
                }
                if (num == getMinNumber(nprop)) {
                    while (get(nprop, num) == null) { num++; }
                    minIds.put(nprop, num);
                }
            }
        }
        scoreBoardChange(new ScoreBoardEvent<>(this, prop, item, true));
        itemRemoved(prop, item, source);
    }
    protected void itemRemoved(AddRemoveProperty<?> prop, ValueWithId item, Source source) {}
    @Override
    public <T extends ValueWithId> void removeAll(AddRemoveProperty<T> prop) { removeAll(prop, Source.OTHER); }
    @Override
    public <T extends ValueWithId> void removeAll(AddRemoveProperty<T> prop, Source source) {
        synchronized (coreLock) {
            if (isWritable(prop, source)) {
                for (T item : getAll(prop)) {
                    remove(prop, item, source);
                }
            }
        }
    }
    @Override
    public Integer getMinNumber(NumberedProperty<?> prop) { return minIds.get(prop); }
    @Override
    public Integer getMaxNumber(NumberedProperty<?> prop) { return maxIds.get(prop); }

    @Override
    public void execute(CommandProperty prop) { execute(prop, Source.OTHER); }
    @Override
    public void execute(CommandProperty prop, Source source) {}

    @Override
    public ScoreBoard getScoreBoard() { return scoreBoard; }

    public static Object getCoreLock() { return coreLock; }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ValueWithId> T getElement(Class<T> type, String id) {
        try {
            return (T) elements.get(type).get(id);
        } catch (NullPointerException e) {
            return null;
        }
    }

    protected void addProperties(Property<?>... props) {
        for (Property<?> prop : props) {
            properties.add(prop);
            if (prop instanceof AddRemoveProperty) {
                children.put((AddRemoveProperty<?>) prop, new HashMap<String, ValueWithId>());
            }
        }
    }

    protected static Object coreLock = new Object();

    protected ScoreBoard scoreBoard;
    protected ScoreBoardEventProvider parent;
    protected AddRemoveProperty<C> ownType;
    protected String providerName;
    protected Class<C> providerClass;

    protected List<Property<?>> properties = new ArrayList<>();

    protected Set<ScoreBoardListener> scoreBoardEventListeners = new LinkedHashSet<>();
    protected Map<ScoreBoardListener, ScoreBoardEventProvider> providers = new HashMap<>();

    protected Map<PermanentProperty<?>, Object> values = new HashMap<>();
    protected Map<Property<?>, Source> writeProtectionOverride = new HashMap<>();
    @SuppressWarnings("rawtypes")
    protected Map<PermanentProperty<?>, CopyScoreBoardListener> reverseCopyListeners = new HashMap<>();

    protected Map<AddRemoveProperty<?>, Map<String, ValueWithId>> children = new HashMap<>();
    protected Map<NumberedProperty<?>, Integer> minIds = new HashMap<>();
    protected Map<NumberedProperty<?>, Integer> maxIds = new HashMap<>();

    protected static Map<Class<? extends ScoreBoardEventProvider>, Map<String, ScoreBoardEventProvider>> elements = new HashMap<>();

    public PermanentProperty<C> PREVIOUS;
    public PermanentProperty<C> NEXT;

    public final static PermanentProperty<Boolean> BATCH_START = new PermanentProperty<>(Boolean.class, "", true);
    public final static PermanentProperty<Boolean> BATCH_END = new PermanentProperty<>(Boolean.class, "", false);
}
