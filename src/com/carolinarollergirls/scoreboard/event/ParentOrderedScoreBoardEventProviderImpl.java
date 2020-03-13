package com.carolinarollergirls.scoreboard.event;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;

public abstract class ParentOrderedScoreBoardEventProviderImpl<T extends ParentOrderedScoreBoardEventProvider<T>>
extends OrderedScoreBoardEventProviderImpl<T> implements ParentOrderedScoreBoardEventProvider<T> {
    @SafeVarargs
    protected ParentOrderedScoreBoardEventProviderImpl(OrderedScoreBoardEventProvider<?> parent, String subId,
            AddRemoveProperty type, Class<T> ownClass, Class<? extends Property>... props) {
        super(parent, parent.getId() + "_" + subId, type, ownClass, props);
        ownType = type;
        this.subId = subId;
        setRecalculated(IValue.PREVIOUS).addSource(parent, IValue.PREVIOUS);
        setRecalculated(IValue.NEXT).addSource(parent, IValue.NEXT);
        set(IValue.PREVIOUS, null, Source.RECALCULATE);
        set(IValue.NEXT, null, Source.RECALCULATE);
        setCopy(IValue.NUMBER, parent, IValue.NUMBER, true);
    }

    @Override
    public String getProviderId() { return subId; }

    @Override
    protected Object _computeValue(PermanentProperty prop, Object value, Object last, Source source, Flag flag) {
        if (prop == IValue.PREVIOUS && source != Source.INVERSE_REFERENCE) {
            if (((OrderedScoreBoardEventProvider<?>) parent).hasPrevious()) {
                return ((OrderedScoreBoardEventProvider<?>) getParent()).getPrevious().get(ownType, subId);
            } else {
                return null;
            }
        }
        if (prop == IValue.NEXT && source != Source.INVERSE_REFERENCE) {
            if (((OrderedScoreBoardEventProvider<?>) parent).hasNext()) {
                return ((OrderedScoreBoardEventProvider<?>) getParent()).getNext().get(ownType, subId);
            } else {
                return null;
            }
        }
        return super._computeValue(prop, value, last, source, flag);
    }

    @SuppressWarnings("hiding")
    protected AddRemoveProperty ownType;
    protected String subId;
}
