package com.carolinarollergirls.scoreboard.event;

public abstract class ReferenceOrderedScoreBoardEventProviderImpl<C extends ReferenceOrderedScoreBoardEventProvider<C>>
    extends OrderedScoreBoardEventProviderImpl<C> implements ReferenceOrderedScoreBoardEventProvider<C> {
    protected ReferenceOrderedScoreBoardEventProviderImpl(ScoreBoardEventProvider parent,
                                                          OrderedScoreBoardEventProvider<?> reference, Child<C> type) {
        super(parent, reference.getId(), type);
        this.reference = reference;
        ownType = type;
        setRecalculated(PREVIOUS).addSource(reference, prevProperties.get(reference.getProviderClass()));
        setRecalculated(NEXT).addSource(reference, nextProperties.get(reference.getProviderClass()));
        set(PREVIOUS, null, Source.RECALCULATE);
        set(NEXT, null, Source.RECALCULATE);
        setCopy(NUMBER, reference, NUMBER, true);
    }

    @Override
    public String getProviderId() {
        return reference.getProviderId();
    }

    @Override
    protected Object _computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == PREVIOUS && source != Source.INVERSE_REFERENCE) {
            if (reference.hasPrevious()) {
                return parent.get(ownType, reference.getPrevious().getProviderId());
            } else {
                return null;
            }
        }
        if (prop == NEXT && source != Source.INVERSE_REFERENCE) {
            if (reference.hasNext()) {
                return parent.get(ownType, reference.getNext().getProviderId());
            } else {
                return null;
            }
        }
        return super._computeValue(prop, value, last, source, flag);
    }

    private OrderedScoreBoardEventProvider<?> reference;
}
