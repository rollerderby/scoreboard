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
        set(IValue.PREVIOUS, null); //recalculate
        set(IValue.NEXT, null); //recalculate
        setCopy(IValue.NUMBER, parent, IValue.NUMBER, true);
    }

    public String getProviderId() { return subId; }

    protected Object _computeValue(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == IValue.PREVIOUS && flag != Flag.INVERSE_REFERENCE) {
            if (((OrderedScoreBoardEventProvider<?>) parent).hasPrevious()) {
                return ((OrderedScoreBoardEventProvider<?>) getParent()).getPrevious().get(ownType, subId);
            } else {
                return null;
            }
        }
        if (prop == IValue.NEXT && flag != Flag.INVERSE_REFERENCE) {
            if (((OrderedScoreBoardEventProvider<?>) parent).hasNext()) {
                return ((OrderedScoreBoardEventProvider<?>) getParent()).getNext().get(ownType, subId);
            } else {
                return null;
            }
        }
        return super._computeValue(prop, value, last, flag);
    }

    protected AddRemoveProperty ownType;
    protected String subId;
}
