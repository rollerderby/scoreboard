package com.carolinarollergirls.scoreboard.event;

public abstract class ParentOrderedScoreBoardEventProviderImpl<C extends ParentOrderedScoreBoardEventProvider<C>>
        extends OrderedScoreBoardEventProviderImpl<C> implements ParentOrderedScoreBoardEventProvider<C> {
    protected ParentOrderedScoreBoardEventProviderImpl(OrderedScoreBoardEventProvider<?> parent, String subId,
            Child<C> type) {
        super(parent, parent.getId() + "_" + subId, type);
        ownType = type;
        this.parent = parent;
        this.subId = subId;
        setRecalculated(PREVIOUS).addSource(parent, prevProperties.get(parent.getProviderClass()));
        setRecalculated(NEXT).addSource(parent, nextProperties.get(parent.getProviderClass()));
        set(PREVIOUS, null, Source.RECALCULATE);
        set(NEXT, null, Source.RECALCULATE);
        setCopy(NUMBER, parent, NUMBER, true);
    }

    @Override
    public String getProviderId() { return subId; }

    @Override
    protected Object _computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == PREVIOUS && source != Source.INVERSE_REFERENCE) {
            if (parent.hasPrevious()) {
                return parent.getPrevious().get(ownType, subId);
            } else {
                return null;
            }
        }
        if (prop == NEXT && source != Source.INVERSE_REFERENCE) {
            if (parent.hasNext()) {
                return parent.getNext().get(ownType, subId);
            } else {
                return null;
            }
        }
        return super._computeValue(prop, value, last, source, flag);
    }

    @SuppressWarnings("hiding")
    protected OrderedScoreBoardEventProvider<?> parent;
    protected String subId;
}
