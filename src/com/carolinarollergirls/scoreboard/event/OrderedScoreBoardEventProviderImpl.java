package com.carolinarollergirls.scoreboard.event;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;

public abstract class OrderedScoreBoardEventProviderImpl<T extends OrderedScoreBoardEventProvider<T>>
        extends ScoreBoardEventProviderImpl implements OrderedScoreBoardEventProvider<T> {
    @SafeVarargs
    public OrderedScoreBoardEventProviderImpl(ScoreBoardEventProvider parent, PermanentProperty idProp,
            AddRemoveProperty type, Class<? extends ScoreBoardEventProvider> ownClass,
            Class<? extends Property>... props) {
        super(parent, idProp, type, ownClass, append(props, IValue.class));
        addReference(new ElementReference(IValue.NEXT, ownClass, IValue.PREVIOUS));
        addReference(new ElementReference(IValue.PREVIOUS, ownClass, IValue.NEXT));
    }

    @SuppressWarnings("unchecked")
    public T getPrevious() { return (T)get(IValue.PREVIOUS); }
    public boolean hasPrevious() { return getPrevious() != null; }
    @SuppressWarnings("unchecked")
    public T getNext() { return (T)get(IValue.NEXT); }
    public boolean hasNext() { return getNext() != null; }

    static <U> U[] append(U[] arr, U lastElement) {
        final int N = arr.length;
        arr = java.util.Arrays.copyOf(arr, N+1);
        arr[N] = lastElement;
        return arr;
    }

}
