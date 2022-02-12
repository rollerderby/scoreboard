package com.carolinarollergirls.scoreboard.event;

import java.util.HashMap;
import java.util.Map;

public abstract class OrderedScoreBoardEventProviderImpl<C extends OrderedScoreBoardEventProvider<C>>
    extends ScoreBoardEventProviderImpl<C> implements OrderedScoreBoardEventProvider<C> {
    @SuppressWarnings("unchecked")
    public OrderedScoreBoardEventProviderImpl(ScoreBoardEventProvider parent, String id, Child<C> type) {
        super(parent, id, type);
        if (!prevProperties.containsKey(providerClass)) {
            prevProperties.put(providerClass, new Value<>(providerClass, "Previous", null, null));
            nextProperties.put(providerClass, new Value<>(providerClass, "Next", null, null));
        }
        PREVIOUS = (Value<C>) prevProperties.get(providerClass);
        NEXT = (Value<C>) nextProperties.get(providerClass);
        addProperties(NUMBER, PREVIOUS, NEXT);
        addWriteProtectionOverride(PREVIOUS, Source.NON_WS);
        addWriteProtectionOverride(NEXT, Source.NON_WS);
        addScoreBoardListener(new InverseReferenceUpdateListener<>((C) this, PREVIOUS, NEXT));
        addScoreBoardListener(new InverseReferenceUpdateListener<>((C) this, NEXT, PREVIOUS));
    }
    protected OrderedScoreBoardEventProviderImpl(OrderedScoreBoardEventProviderImpl<C> cloned,
                                                 ScoreBoardEventProvider root) {
        super(cloned, root);
    }

    @Override
    public int getNumber() {
        return get(NUMBER);
    }

    @Override
    public C getPrevious() {
        return get(PREVIOUS);
    }
    @Override
    public boolean hasPrevious() {
        return getPrevious() != null;
    }
    @Override
    public void setPrevious(C prev) {
        set(PREVIOUS, prev);
    }
    @Override
    public C getNext() {
        return get(NEXT);
    }
    @Override
    public boolean hasNext() {
        return getNext() != null;
    }
    @Override
    public void setNext(C next) {
        set(NEXT, next);
    }

    protected static Map<Class<?>, Value<?>> prevProperties = new HashMap<>();
    protected static Map<Class<?>, Value<?>> nextProperties = new HashMap<>();
}
