package com.carolinarollergirls.scoreboard.event;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;

public abstract class OrderedScoreBoardEventProviderImpl<T extends OrderedScoreBoardEventProvider<T>>
        extends ScoreBoardEventProviderImpl implements OrderedScoreBoardEventProvider<T> {
    @SafeVarargs
    @SuppressWarnings("varargs")  // @SafeVarargs isn't working for some reason.
    public OrderedScoreBoardEventProviderImpl(ScoreBoardEventProvider parent, String id, 
            AddRemoveProperty type, Class<T> ownClass, Class<? extends Property>... props) {
        super(parent, IValue.ID, id, type, ownClass, append(props, IValue.class));
        addScoreBoardListener(new InverseReferenceUpdateListener(this, IValue.PREVIOUS, IValue.NEXT));
        addScoreBoardListener(new InverseReferenceUpdateListener(this, IValue.NEXT, IValue.PREVIOUS));
    }

    public int getNumber() { return (Integer)get(IValue.NUMBER); }

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
