package com.carolinarollergirls.scoreboard.event;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;

public abstract class ParentOrderedScoreBoardEventProviderImpl<T extends ParentOrderedScoreBoardEventProvider<T>>
extends OrderedScoreBoardEventProviderImpl<T> implements ParentOrderedScoreBoardEventProvider<T> {
    @SafeVarargs
    protected ParentOrderedScoreBoardEventProviderImpl(OrderedScoreBoardEventProvider<?> parent, String subId,
            AddRemoveProperty type, Class<T> ownClass, Class<? extends Property>... props) {
        super(parent, type, ownClass, props);
        ownType = type;
        this.subId = subId;
        set(IValue.ID, parent.getId() + "_" + subId);
        addReference(new UpdateReference(this, IValue.ID, parent, IValue.ID));
        addReference(new UpdateReference(this, IValue.PREVIOUS, parent, IValue.PREVIOUS));
        addReference(new UpdateReference(this, IValue.NEXT, parent, IValue.NEXT));
        addReference(new ValueReference(this, IValue.NUMBER, parent, IValue.NUMBER, true, 0));
    }

    public String getProviderId() { return subId; }

    protected Object _computeValue(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == IValue.ID) {
            return parent.getId() + "_" + subId;
        }
        if (prop == IValue.PREVIOUS) {
            if (((OrderedScoreBoardEventProvider<?>) parent).hasPrevious()) {
                return ((OrderedScoreBoardEventProvider<?>) getParent()).getPrevious().get(ownType, subId);
            } else {
                return null;
            }
        }
        if (prop == IValue.NEXT) {
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
