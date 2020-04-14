package com.carolinarollergirls.scoreboard.event;

import java.util.HashMap;
import java.util.Map;

public abstract class OrderedScoreBoardEventProviderImpl<C extends OrderedScoreBoardEventProvider<C>>
        extends ScoreBoardEventProviderImpl<C> implements OrderedScoreBoardEventProvider<C> {
    @SuppressWarnings("unchecked")
    public OrderedScoreBoardEventProviderImpl(ScoreBoardEventProvider parent, String id, AddRemoveProperty<C> type) {
        super(parent, id, type);
        if (!prevProperties.containsKey(providerClass)) {
            prevProperties.put(providerClass, new PermanentProperty<>(providerClass, "Previous", null));
            nextProperties.put(providerClass, new PermanentProperty<>(providerClass, "Next", null));
        }
        PREVIOUS = (PermanentProperty<C>) prevProperties.get(providerClass);
        NEXT = (PermanentProperty<C>) nextProperties.get(providerClass);
        addProperties(NUMBER, PREVIOUS, NEXT);
        addScoreBoardListener(new InverseReferenceUpdateListener<>((C) this, PREVIOUS, NEXT));
        addScoreBoardListener(new InverseReferenceUpdateListener<>((C) this, NEXT, PREVIOUS));
    }

    @Override
    public int getNumber() { return get(NUMBER); }

    @Override
    public C getPrevious() { return get(PREVIOUS); }
    @Override
    public boolean hasPrevious() { return getPrevious() != null; }
    @Override
    public void setPrevious(C prev) { set(PREVIOUS, prev); }
    @Override
    public C getNext() { return get(NEXT); }
    @Override
    public boolean hasNext() { return getNext() != null; }
    @Override
    public void setNext(C next) { set(NEXT, next); }

    protected static Map<Class<?>, PermanentProperty<?>> prevProperties = new HashMap<>();
    protected static Map<Class<?>, PermanentProperty<?>> nextProperties = new HashMap<>();
}
