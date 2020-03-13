package com.carolinarollergirls.scoreboard.event;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;

public abstract class OrderedScoreBoardEventProviderImpl<T extends OrderedScoreBoardEventProvider<T>>
        extends ScoreBoardEventProviderImpl implements OrderedScoreBoardEventProvider<T> {
    @SafeVarargs
    @SuppressWarnings("varargs")  // @SafeVarargs isn't working for some reason.
    public OrderedScoreBoardEventProviderImpl(ScoreBoardEventProvider parent, String id, 
            AddRemoveProperty type, Class<T> ownClass, Class<? extends Property>... props) {
        super(parent, id, type, ownClass, props);
        addScoreBoardListener(new InverseReferenceUpdateListener(this, IValue.PREVIOUS, IValue.NEXT));
        addScoreBoardListener(new InverseReferenceUpdateListener(this, IValue.NEXT, IValue.PREVIOUS));
    }

    @Override
    public int getNumber() { return (Integer)get(IValue.NUMBER); }

    @Override
    @SuppressWarnings("unchecked")
    public T getPrevious() { return (T)get(IValue.PREVIOUS); }
    @Override
    public boolean hasPrevious() { return getPrevious() != null; }
    @Override
    @SuppressWarnings("unchecked")
    public T getNext() { return (T)get(IValue.NEXT); }
    @Override
    public boolean hasNext() { return getNext() != null; }
}
