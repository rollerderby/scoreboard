package com.carolinarollergirls.scoreboard.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.carolinarollergirls.scoreboard.core.interfaces.FloorPosition;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Role;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.TimeoutOwner;
import com.carolinarollergirls.scoreboard.rules.RuleDefinition;
import com.carolinarollergirls.scoreboard.utils.Logger;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public abstract class ScoreBoardEventProviderImpl<C extends ScoreBoardEventProvider>
    implements ScoreBoardEventProvider, ScoreBoardListener {
    @SuppressWarnings("unchecked")
    protected ScoreBoardEventProviderImpl(ScoreBoardEventProvider parent, String id, Child<C> type) {
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
        addWriteProtectionOverride(READONLY, Source.ANY_INTERNAL);
    }
    protected ScoreBoardEventProviderImpl(ScoreBoardEventProviderImpl<C> cloned, ScoreBoardEventProvider root) {
        providerClass = cloned.providerClass;
        providerName = cloned.providerName;
        ownType = cloned.ownType;
        parent = toCloneIfInTree(cloned.parent, root);
        scoreBoard = toCloneIfInTree(cloned.scoreBoard, root);
        PREVIOUS = cloned.PREVIOUS;
        NEXT = cloned.NEXT;
        addProperties(ID);
        set(ID, "cloned-" + cloned.getId(), Source.OTHER);
        cloneProperties(cloned, root);
    }

    @Override
    public String getId() {
        return get(ID);
    }
    @Override
    public String getProviderName() {
        return providerName;
    }
    @Override
    public Class<? extends ScoreBoardEventProvider> getProviderClass() {
        return providerClass;
    }
    @Override
    public String getProviderId() {
        return getId();
    }
    @Override
    public String getValue() {
        return getId();
    }
    @Override
    public String toString() {
        return getId();
    }
    @Override
    public Collection<Property<?>> getProperties() {
        return properties.values();
    }
    @Override
    public Property<?> getProperty(String jsonName) {
        return properties.get(jsonName);
    }
    @Override
    public ScoreBoardEventProvider getParent() {
        return parent;
    }
    @Override
    public boolean isAncestorOf(ScoreBoardEventProvider other) {
        ScoreBoardEventProvider comp = other;
        while (comp != null) {
            if (comp == this) { return true; }
            comp = comp.getParent();
        }
        return false;
    }

    @Override
    public void scoreBoardChange(ScoreBoardEvent<?> event) {
        dispatch(event);
    }

    protected void dispatch(ScoreBoardEvent<?> event) {
        // Synchronously send events to listeners.
        // need to copy the list as some listeners may add or remove listeners
        synchronized (scoreBoardEventListeners) {
            for (ScoreBoardListener l : new ArrayList<>(scoreBoardEventListeners)) { l.scoreBoardChange(event); }
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
            } finally { requestBatchEnd(); }
        }
    }

    @Override
    public void addScoreBoardListener(ScoreBoardListener listener) {
        synchronized (scoreBoardEventListeners) { scoreBoardEventListeners.add(listener); }
    }
    @Override
    public void removeScoreBoardListener(ScoreBoardListener listener) {
        synchronized (scoreBoardEventListeners) { scoreBoardEventListeners.remove(listener); }
    }

    @Override
    public int compareTo(ScoreBoardEventProvider other) {
        if (other == null) { return -1; }
        if (getParent() == other.getParent()) { return 0; }
        if (getParent() == null) { return 1; }
        if (getParent() instanceof NumberedScoreBoardEventProvider<?> && other.getParent() instanceof
                                                                             NumberedScoreBoardEventProvider<?>) {
            return ((NumberedScoreBoardEventProvider<?>) getParent())
                .compareTo((NumberedScoreBoardEventProvider<?>) other.getParent());
        }
        return getParent().compareTo(other.getParent());
    }

    @Override
    public void delete() {
        delete(Source.OTHER);
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void delete(Source source) {
        if (get(READONLY) && source != Source.UNLINK) { return; }
        for (Property prop : properties.values()) {
            if (prop instanceof Child) {
                for (ValueWithId item : getAll((Child<?>) prop)) {
                    if (item instanceof ScoreBoardEventProvider &&
                        ((ScoreBoardEventProvider) item).getParent() == this) {
                        ((ScoreBoardEventProvider) item).delete(Source.UNLINK);
                    } else {
                        remove((Child) prop, item, Source.UNLINK);
                    }
                }
            } else if (prop instanceof Value && ScoreBoardEventProvider.class.isAssignableFrom(prop.getType())) {
                set((Value) prop, null, Source.UNLINK);
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
        elements.get(providerClass).remove(get(ID));
    }

    public void addWriteProtection(Property<?> prop) { addWriteProtectionOverride(prop, null); }
    public void addWriteProtectionOverride(Property<?> prop, Source override) {
        checkProperty(prop);
        writeProtectionOverride.put(prop, override);
    }
    public boolean isWritable(Property<?> prop, Source source) {
        checkProperty(prop);
        if (source == Source.UNLINK && prop != ID && prop != READONLY &&
            prop != OrderedScoreBoardEventProvider.NUMBER && prop != PREVIOUS && prop != NEXT) {
            return true;
        }
        if (get(READONLY)) { return false; }
        if (!writeProtectionOverride.containsKey(prop)) { return true; }
        if (writeProtectionOverride.get(prop) == null || source == null) { return false; }
        if (writeProtectionOverride.get(prop) == Source.ANY_INTERNAL) { return source.isInternal(); }
        if (writeProtectionOverride.get(prop) == Source.ANY_FILE) { return source.isFile(); }
        if (writeProtectionOverride.get(prop) == Source.NON_WS) { return source.isFile() || source.isInternal(); }
        if (writeProtectionOverride.get(prop) == source) { return true; }
        return false;
    }
    public <T extends ValueWithId> boolean isWritable(Child<T> prop, String id, Source source) {
        checkProperty(prop);
        if (source == Source.UNLINK || source == Source.RENUMBER) { return true; }
        T oldItem = get(prop, id);
        if (oldItem instanceof ScoreBoardEventProvider) {
            if (oldItem != null && ((ScoreBoardEventProvider) oldItem).get(READONLY)) { return false; }
        }
        return isWritable(prop, source);
    }

    /**
     * Make targetProperty a copy of sourceProperty on sourceElement
     */
    protected <T> ScoreBoardListener setCopy(Value<T> targetProperty, ScoreBoardEventProvider sourceElement,
                                             Value<T> sourceProperty, boolean readonly) {
        return setCopy(targetProperty, sourceElement, sourceProperty, readonly, null, READONLY);
    }
    protected <T> ScoreBoardListener setCopy(Value<T> targetProperty, ScoreBoardEventProvider sourceElement,
                                             Value<T> sourceProperty, boolean readonly, Value<Boolean> guardProperty) {
        return setCopy(targetProperty, sourceElement, sourceProperty, readonly, this, guardProperty);
    }
    protected <T> ScoreBoardListener setCopy(Value<T> targetProperty, ScoreBoardEventProvider sourceElement,
                                             Value<T> sourceProperty, boolean readonly,
                                             ScoreBoardEventProvider guardElement, Value<Boolean> guardProperty) {
        checkProperty(targetProperty);
        sourceElement.checkProperty(sourceProperty);
        ScoreBoardListener l = new ConditionalScoreBoardListener<>(
            sourceElement, sourceProperty,
            new CopyValueScoreBoardListener<>(this, targetProperty, guardElement, guardProperty));
        sourceElement.addScoreBoardListener(l);
        providers.put(l, sourceElement);
        if (readonly) {
            addWriteProtectionOverride(targetProperty, Source.COPY);
        } else {
            reverseCopyListeners.put(targetProperty, new CopyValueScoreBoardListener<>(sourceElement, sourceProperty,
                                                                                       guardElement, guardProperty));
        }
        if (guardElement == null || guardElement.get(guardProperty)) {
            set(targetProperty, sourceElement.get(sourceProperty), Source.COPY);
        }
        return l;
    }
    protected <T extends ValueWithId> ScoreBoardListener setCopy(Child<T> targetProperty,
                                                                 ScoreBoardEventProvider sourceElement,
                                                                 Child<T> sourceProperty, boolean readonly) {
        return setCopy(targetProperty, sourceElement, sourceProperty, readonly, null, READONLY);
    }
    protected <T extends ValueWithId> ScoreBoardListener setCopy(Child<T> targetProperty,
                                                                 ScoreBoardEventProvider sourceElement,
                                                                 Child<T> sourceProperty, boolean readonly,
                                                                 Value<Boolean> guardProperty) {
        return setCopy(targetProperty, sourceElement, sourceProperty, readonly, this, guardProperty);
    }
    protected <T extends ValueWithId> ScoreBoardListener setCopy(Child<T> targetProperty,
                                                                 ScoreBoardEventProvider sourceElement,
                                                                 Child<T> sourceProperty, boolean readonly,
                                                                 ScoreBoardEventProvider guardElement,
                                                                 Value<Boolean> guardProperty) {
        checkProperty(targetProperty);
        sourceElement.checkProperty(sourceProperty);
        ScoreBoardListener l = new ConditionalScoreBoardListener<>(
            sourceElement, sourceProperty,
            new CopyChildScoreBoardListener<>(this, targetProperty, guardElement, guardProperty));
        sourceElement.addScoreBoardListener(l);
        providers.put(l, sourceElement);
        if (readonly) {
            addWriteProtectionOverride(targetProperty, Source.COPY);
        } else {
            reverseCopyListeners.put(targetProperty, new CopyChildScoreBoardListener<>(sourceElement, sourceProperty,
                                                                                       guardElement, guardProperty));
        }
        if ((guardElement == null || guardElement.get(guardProperty))) {
            for (T element : sourceElement.getAll(sourceProperty)) { add(targetProperty, element); }
        }
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
    protected <T, U> ScoreBoardListener setCopy(final Value<T> targetProperty,
                                                ScoreBoardEventProvider indirectionElement,
                                                Value<U> indirectionProperty, final Value<T> sourceProperty,
                                                boolean readonly) {
        return setCopy(targetProperty, indirectionElement, indirectionProperty, sourceProperty, readonly, null,
                       READONLY);
    }
    protected <T, U> ScoreBoardListener setCopy(final Value<T> targetProperty,
                                                ScoreBoardEventProvider indirectionElement,
                                                Value<U> indirectionProperty, final Value<T> sourceProperty,
                                                boolean readonly, final Value<Boolean> guardProperty) {
        return setCopy(targetProperty, indirectionElement, indirectionProperty, sourceProperty, readonly, this,
                       guardProperty);
    }
    protected <T, U> ScoreBoardListener setCopy(final Value<T> targetProperty,
                                                ScoreBoardEventProvider indirectionElement,
                                                Value<U> indirectionProperty, final Value<T> sourceProperty,
                                                boolean readonly, final ScoreBoardEventProvider guardElement,
                                                final Value<Boolean> guardProperty) {
        checkProperty(targetProperty);
        indirectionElement.checkProperty(indirectionProperty);
        ScoreBoardListener l = new IndirectScoreBoardListener<>(
            indirectionElement, indirectionProperty, sourceProperty,
            new CopyValueScoreBoardListener<>(this, targetProperty, guardElement, guardProperty));
        providers.put(l, null);
        if (readonly) {
            addWriteProtectionOverride(targetProperty, Source.COPY);
        } else {
            ScoreBoardListener reverseListener =
                new ConditionalScoreBoardListener<>(indirectionElement, indirectionProperty, new ScoreBoardListener() {
                    @Override
                    public void scoreBoardChange(ScoreBoardEvent<?> event) {
                        reverseCopyListeners.put(targetProperty, new CopyValueScoreBoardListener<>(
                                                                     (ScoreBoardEventProvider) event.getValue(),
                                                                     sourceProperty, guardElement, guardProperty));
                    }
                });
            indirectionElement.addScoreBoardListener(reverseListener);
            reverseListener.scoreBoardChange(new ScoreBoardEvent<>(indirectionElement, indirectionProperty,
                                                                   indirectionElement.get(indirectionProperty), null));
        }
        return l;
    }
    protected <T extends ValueWithId, U> ScoreBoardListener setCopy(final Child<T> targetProperty,
                                                                    ScoreBoardEventProvider indirectionElement,
                                                                    Value<U> indirectionProperty,
                                                                    final Child<T> sourceProperty, boolean readonly) {
        return setCopy(targetProperty, indirectionElement, indirectionProperty, sourceProperty, readonly, null,
                       READONLY);
    }
    protected <T extends ValueWithId, U> ScoreBoardListener setCopy(final Child<T> targetProperty,
                                                                    ScoreBoardEventProvider indirectionElement,
                                                                    Value<U> indirectionProperty,
                                                                    final Child<T> sourceProperty, boolean readonly,
                                                                    final Value<Boolean> guardProperty) {
        return setCopy(targetProperty, indirectionElement, indirectionProperty, sourceProperty, readonly, this,
                       guardProperty);
    }
    protected <T extends ValueWithId, U>
        ScoreBoardListener setCopy(final Child<T> targetProperty, ScoreBoardEventProvider indirectionElement,
                                   Value<U> indirectionProperty, final Child<T> sourceProperty, boolean readonly,
                                   final ScoreBoardEventProvider guardElement, final Value<Boolean> guardProperty) {
        checkProperty(targetProperty);
        indirectionElement.checkProperty(indirectionProperty);
        ScoreBoardListener l = new IndirectScoreBoardListener<>(
            indirectionElement, indirectionProperty, sourceProperty,
            new CopyChildScoreBoardListener<>(this, targetProperty, guardElement, guardProperty));
        providers.put(l, null);
        if (readonly) {
            addWriteProtectionOverride(targetProperty, Source.COPY);
        } else {
            ScoreBoardListener reverseListener =
                new ConditionalScoreBoardListener<>(indirectionElement, indirectionProperty, new ScoreBoardListener() {
                    @Override
                    public void scoreBoardChange(ScoreBoardEvent<?> event) {
                        reverseCopyListeners.put(targetProperty, new CopyChildScoreBoardListener<>(
                                                                     (ScoreBoardEventProvider) event.getValue(),
                                                                     sourceProperty, guardElement, guardProperty));
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
    protected RecalculateScoreBoardListener<?> setRecalculated(Value<?> targetProperty) {
        checkProperty(targetProperty);
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
        checkProperty(localProperty);
        @SuppressWarnings("unchecked")
        InverseReferenceUpdateListener<T, C> l =
            new InverseReferenceUpdateListener<>((C) this, localProperty, remoteProperty);
        addScoreBoardListener(l);
        return l;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T valueFromString(Value<T> prop, String sValue) {
        synchronized (coreLock) {
            @SuppressWarnings("rawtypes")
            Class type = prop.getType();
            if (type == TimeoutOwner.class) { return (T) scoreBoard.getTimeoutOwner(sValue); }
            if (sValue == null) { return prop.getDefaultValue(); }
            if ("".equals(sValue) && !(type == String.class)) { return prop.getDefaultValue(); }
            if (type == RuleDefinition.Type.class) { return prop.getDefaultValue(); }
            if (type == Role.class) { return (T) Role.fromString(sValue); }
            if (type == FloorPosition.class) { return (T) FloorPosition.fromString(sValue); }
            if (type == Game.State.class) { return (T) Game.State.fromString(sValue); }
            if (type == Boolean.class) { return (T) Boolean.valueOf(sValue); }
            if (type == Integer.class) { return (T) Integer.valueOf(sValue); }
            if (type == Long.class) { return (T) Long.valueOf(sValue); }
            if (prop == PREVIOUS || prop == NEXT) { return (T) getElement(providerClass, sValue); }
            if (ScoreBoardEventProvider.class.isAssignableFrom(type)) { return (T) getElement(type, sValue); }
            if (type != String.class) {
                Logger.printMessage("Conversion to " + type.getSimpleName() + " used by " + prop.getJsonName() +
                                    " missing in ScoreBoardEventProvider.valueFromString()");
                return prop.getDefaultValue();
            }
            return (T) sValue;
        }
    }
    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Value<T> prop) {
        synchronized (coreLock) {
            if (!values.containsKey(prop)) { return prop.getDefaultValue(); }
            return (T) values.get(prop);
        }
    }
    @Override
    public <T> boolean set(Value<T> prop, T value) {
        return set(prop, value, Source.OTHER, null);
    }
    @Override
    public <T> boolean set(Value<T> prop, T value, Flag flag) {
        return set(prop, value, Source.OTHER, flag);
    }
    @Override
    public <T> boolean set(Value<T> prop, T value, Source source) {
        return set(prop, value, source, null);
    }
    @SuppressWarnings("unchecked")
    @Override
    public <T> boolean set(Value<T> prop, T value, Source source, Flag flag) {
        synchronized (coreLock) {
            if (prop == null) { return false; }
            if (prop == ID && source.isFile()) {
                // register ID as an alias so other elements from file are properly redirected
                elements.get(providerClass).put((String) value, this);
                return false;
            }
            if (!isWritable(prop, source)) { return false; }
            T last = get(prop);
            value = (T) _computeValue(prop, value, last, source, flag);
            if (reverseCopyListeners.containsKey(prop) && reverseCopyListeners.get(prop).isActive() &&
                source != Source.COPY) {
                reverseCopyListeners.get(prop).scoreBoardChange(new ScoreBoardEvent<>(this, prop, value, last), source);
                return false;
            }
            if (Objects.equals(value, last)) { return false; }
            values.put(prop, value);
            _valueChanged(prop, value, last, source, flag);
            return true;
        }
    }
    protected Object _computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (flag == Flag.CHANGE) {
            if (last instanceof Integer) {
                value = (Integer) last + (Integer) value;
            } else if (last instanceof Long) {
                value = (Long) last + (Long) value;
            }
        }
        return computeValue(prop, value, last, source, flag);
    }
    protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) { return value; }
    protected <T> void _valueChanged(Value<T> prop, T value, T last, Source source, Flag flag) {
        if (prop == ID) { elements.get(providerClass).put((String) value, this); }
        scoreBoardChange(new ScoreBoardEvent<>(this, prop, value, last));
        valueChanged(prop, value, last, source, flag);
    }
    protected void valueChanged(Value<?> prop, Object value, Object last, Source source, Flag flag) {}

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ValueWithId> T childFromString(Child<T> prop, String id, String sValue) {
        synchronized (coreLock) {
            if (prop.getType() == ValWithId.class) { return (T) new ValWithId(id, sValue); }
            return getElement(prop.getType(), sValue);
        }
    }
    @SuppressWarnings("unchecked")
    @Override
    public <T extends ValueWithId> T get(Child<T> prop, String id) {
        if (children.get(prop) == null) { return null; }
        return (T) children.get(prop).get(id);
    }
    @Override
    public <T extends OrderedScoreBoardEventProvider<T>> T get(NumberedChild<T> prop, Integer num) {
        return get(prop, String.valueOf(num));
    }
    @Override
    public <T extends ScoreBoardEventProvider> T getOrCreate(Child<T> prop, String id) {
        return getOrCreate(prop, id, Source.OTHER);
    }
    @SuppressWarnings("unchecked")
    @Override
    public <T extends ScoreBoardEventProvider> T getOrCreate(Child<T> prop, String id, Source source) {
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
    public <T extends OrderedScoreBoardEventProvider<T>> T getOrCreate(NumberedChild<T> prop, Integer num) {
        return getOrCreate(prop, String.valueOf(num), Source.OTHER);
    }
    @Override
    public <T extends OrderedScoreBoardEventProvider<T>> T getOrCreate(NumberedChild<T> prop, Integer num,
                                                                       Source source) {
        return getOrCreate(prop, String.valueOf(num), source);
    }
    @Override
    public <T extends OrderedScoreBoardEventProvider<T>> T getFirst(NumberedChild<T> prop) {
        synchronized (coreLock) { return get(prop, minIds.get(prop)); }
    }
    @Override
    public <T extends OrderedScoreBoardEventProvider<T>> T getLast(NumberedChild<T> prop) {
        synchronized (coreLock) { return get(prop, maxIds.get(prop)); }
    }
    @SuppressWarnings("unchecked")
    @Override
    public <T extends ValueWithId> Collection<T> getAll(Child<T> prop) {
        synchronized (coreLock) {
            checkProperty(prop);
            return new HashSet<>((Collection<? extends T>) children.get(prop).values());
        }
    }
    @Override
    public int numberOf(Child<?> prop) {
        synchronized (coreLock) {
            if (!children.containsKey(prop)) { return 0; }
            return children.get(prop).size();
        }
    }
    protected <T extends ValueWithId> boolean mayAdd(Child<T> prop, T item, Source source) { return true; }
    @Override
    public <T extends ValueWithId> boolean add(Child<T> prop, T item) {
        return add(prop, item, Source.OTHER);
    }
    @SuppressWarnings("unchecked")
    @Override
    public <T extends ValueWithId> boolean add(Child<T> prop, T item, Source source) {
        synchronized (coreLock) {
            if (item == null) { return false; }
            String id = item.getId();
            if (item instanceof ScoreBoardEventProvider && ((ScoreBoardEventProvider) item).getParent() == this) {
                id = ((ScoreBoardEventProvider) item).getProviderId();
            }
            if (!isWritable(prop, id, source)) { return false; }
            if (!mayAdd(prop, item, source)) { return false; }
            if (reverseCopyListeners.containsKey(prop) && reverseCopyListeners.get(prop).isActive() &&
                source != Source.COPY) {
                reverseCopyListeners.get(prop).scoreBoardChange(new ScoreBoardEvent<>(this, prop, item, false), source);
                return false;
            }
            Map<String, ValueWithId> map = children.get(prop);
            if (map.containsKey(id) && map.get(id).equals(item)) { return false; }
            map.put(id, item);
            _itemAdded(prop, item, source);
            return true;
        }
    }
    protected <T extends ValueWithId> void _itemAdded(Child<T> prop, T item, Source source) {
        if (item instanceof ScoreBoardEventProvider && ((ScoreBoardEventProvider) item).getParent() == this) {
            ((ScoreBoardEventProvider) item).addScoreBoardListener(this);
        }
        if (prop instanceof NumberedChild) {
            int num = ((OrderedScoreBoardEventProvider<?>) item).getNumber();
            if (minIds.get(prop) == null || num < minIds.get(prop)) { minIds.put((NumberedChild<?>) prop, num); }
            if (maxIds.get(prop) == null || num > maxIds.get(prop)) { maxIds.put((NumberedChild<?>) prop, num); }
        }
        scoreBoardChange(new ScoreBoardEvent<>(this, prop, item, false));
        itemAdded(prop, item, source);
    }
    protected void itemAdded(Child<?> prop, ValueWithId item, Source source) {}
    @Override
    public ScoreBoardEventProvider create(Child<? extends ScoreBoardEventProvider> prop, String id, Source source) {
        return null;
    }
    protected <T extends ValueWithId> boolean mayRemove(Child<T> prop, T item, Source source) { return true; }
    @Override
    public <T extends ValueWithId> boolean remove(Child<T> prop, String id) {
        return remove(prop, get(prop, id), Source.OTHER);
    }
    @Override
    public <T extends ValueWithId> boolean remove(Child<T> prop, String id, Source source) {
        return remove(prop, get(prop, id), source);
    }
    @Override
    public <T extends ValueWithId> boolean remove(Child<T> prop, T item) {
        return remove(prop, item, Source.OTHER);
    }
    @SuppressWarnings("unchecked")
    @Override
    public <T extends ValueWithId> boolean remove(Child<T> prop, T item, Source source) {
        synchronized (coreLock) {
            if (item == null) { return false; }
            String id = item.getId();
            if (item instanceof ScoreBoardEventProvider && ((ScoreBoardEventProvider) item).getParent() == this) {
                id = ((ScoreBoardEventProvider) item).getProviderId();
            }
            if (!isWritable(prop, id, source)) { return false; }
            if (!mayRemove(prop, item, source)) { return false; }
            if (reverseCopyListeners.containsKey(prop) && reverseCopyListeners.get(prop).isActive() &&
                source != Source.COPY) {
                reverseCopyListeners.get(prop).scoreBoardChange(new ScoreBoardEvent<>(this, prop, item, true), source);
                return false;
            }
            if (children.get(prop).get(id) == item) {
                children.get(prop).remove(id);
                _itemRemoved(prop, item, source);
                return true;
            }
            return false;
        }
    }
    protected <T extends ValueWithId> void _itemRemoved(Child<T> prop, T item, Source source) {
        if (item instanceof ScoreBoardEventProvider) {
            ((ScoreBoardEventProvider) item).removeScoreBoardListener(this);
        }
        if (prop instanceof NumberedChild) {
            NumberedChild<?> nprop = (NumberedChild<?>) prop;
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
    protected void itemRemoved(Child<?> prop, ValueWithId item, Source source) {}
    @Override
    public <T extends ValueWithId> void removeAll(Child<T> prop) {
        removeAll(prop, Source.OTHER);
    }
    @Override
    public <T extends ValueWithId> void removeAll(Child<T> prop, Source source) {
        synchronized (coreLock) {
            if (isWritable(prop, source)) {
                for (T item : getAll(prop)) { remove(prop, item, source); }
            }
        }
    }
    @Override
    public Integer getMinNumber(NumberedChild<?> prop) {
        return minIds.get(prop);
    }
    @Override
    public Integer getMaxNumber(NumberedChild<?> prop) {
        return maxIds.get(prop);
    }

    @Override
    public void execute(Command prop) {
        execute(prop, Source.OTHER);
    }
    @Override
    public void execute(Command prop, Source source) {}

    @Override
    public ScoreBoard getScoreBoard() {
        return scoreBoard;
    }

    public static Object getCoreLock() { return coreLock; }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ValueWithId> T getElement(Class<T> type, String id) {
        try {
            return (T) elements.get(type).get(id);
        } catch (NullPointerException e) { return null; }
    }

    @Override
    public void checkProperty(Property<?> prop) {
        if (!(properties.get(prop.getJsonName()) == prop)) {
            throw new IllegalArgumentException(prop.getJsonName() + " is not a property of " +
                                               this.getClass().getName());
        }
    }

    protected void addProperties(Property<?>... props) { addProperties(Arrays.asList(props)); }
    protected void addProperties(Collection<Property<?>> props) {
        for (Property<?> prop : props) {
            if (properties.containsKey(prop.getJsonName())) {
                throw new IllegalArgumentException(this.getClass().getName() +
                                                   " can't contain multiple properties wit JSON name " +
                                                   prop.getJsonName());
            }
            properties.put(prop.getJsonName(), prop);
            if (prop instanceof Child) { children.put((Child<?>) prop, new HashMap<String, ValueWithId>()); }
        }
    }

    @SuppressWarnings("unchecked")
    protected <T extends ScoreBoardEventProvider> T toCloneIfInTree(T source, ScoreBoardEventProvider root) {
        T result = source;
        if (root.isAncestorOf(source)) {
            T clone = (T) elements.get(source.getProviderClass()).get("cloned-" + source.getId());
            result = clone == null ? (T) source.clone(root) : clone;
        }
        return result;
    }

    protected void cloneProperties(ScoreBoardEventProviderImpl<C> cloned, ScoreBoardEventProvider root) {
        for (Property<?> prop : cloned.getProperties()) {
            if (prop == ID) { continue; }
            properties.put(prop.getJsonName(), prop);
            if (prop instanceof Value && cloned.values.containsKey(prop)) {
                Object value = cloned.values.get(prop);
                if (value instanceof ScoreBoardEventProvider) {
                    value = toCloneIfInTree((ScoreBoardEventProvider) value, root);
                }
                values.put((Value<?>) prop, value);
            } else if (prop instanceof Child) {
                Map<String, ValueWithId> newMap = new HashMap<>();
                for (ValueWithId child : cloned.children.get(prop).values()) {
                    String id = "";
                    if (child instanceof ValWithId) {
                        child = new ValWithId(child.getId(), child.getValue());
                        id = child.getId();
                    } else if (child instanceof ScoreBoardEventProvider) {
                        child = toCloneIfInTree((ScoreBoardEventProvider) child, root);
                        ScoreBoardEventProvider c = (ScoreBoardEventProvider) child;
                        id = c.getParent() == this ? c.getProviderId() : c.getId();
                    }
                    newMap.put(id, child);
                }
                if (prop instanceof NumberedChild<?>) {
                    minIds.put((NumberedChild<?>) prop, cloned.minIds.get(prop));
                    maxIds.put((NumberedChild<?>) prop, cloned.maxIds.get(prop));
                }
                children.put((Child<?>) prop, newMap);
            }
            if (cloned.writeProtectionOverride.containsKey(prop)) {
                writeProtectionOverride.put(prop, cloned.writeProtectionOverride.get(prop));
            }
        }
    }

    @Override
    public void cleanupAliases() {
        synchronized (coreLock) {
            for (Map<String, ScoreBoardEventProvider> list : elements.values()) {
                list.entrySet().removeIf(o -> (o.getValue() == null || !o.getKey().equals(o.getValue().getId())));
            }
        }
    }

    protected static Object coreLock = new Object();

    protected ScoreBoard scoreBoard;
    protected ScoreBoardEventProvider parent;
    protected Child<C> ownType;
    protected String providerName;
    protected Class<C> providerClass;

    protected Map<String, Property<?>> properties = new HashMap<>();

    protected Set<ScoreBoardListener> scoreBoardEventListeners = new LinkedHashSet<>();
    protected Map<ScoreBoardListener, ScoreBoardEventProvider> providers = new HashMap<>();

    protected Map<Value<?>, Object> values = new HashMap<>();
    protected Map<Property<?>, Source> writeProtectionOverride = new HashMap<>();
    @SuppressWarnings("rawtypes")
    protected Map<Property<?>, CopyScoreBoardListener> reverseCopyListeners = new HashMap<>();

    protected Map<Child<?>, Map<String, ValueWithId>> children = new HashMap<>();
    protected Map<NumberedChild<?>, Integer> minIds = new HashMap<>();
    protected Map<NumberedChild<?>, Integer> maxIds = new HashMap<>();

    protected static Map<Class<? extends ScoreBoardEventProvider>, Map<String, ScoreBoardEventProvider>> elements =
        new HashMap<>();

    public Value<C> PREVIOUS;
    public Value<C> NEXT;

    public final static Value<Boolean> BATCH_START = new Value<>(Boolean.class, "", true, null);
    public final static Value<Boolean> BATCH_END = new Value<>(Boolean.class, "", false, null);
}
