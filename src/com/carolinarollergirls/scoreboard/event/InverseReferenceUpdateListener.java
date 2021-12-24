package com.carolinarollergirls.scoreboard.event;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Source;

public class InverseReferenceUpdateListener<T, U extends ScoreBoardEventProvider>
    extends ConditionalScoreBoardListener<T> {
    public InverseReferenceUpdateListener(U localElement, Property<T> localProperty, Property<U> remoteProperty) {
        super(localElement, localProperty);
        this.remoteProperty = remoteProperty;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void matchedScoreBoardChange(ScoreBoardEvent<?> e) {
        ScoreBoardEventProvider newRemote = (ScoreBoardEventProvider) (e.isRemove() ? null : e.getValue());
        ScoreBoardEventProvider lastRemote =
            (ScoreBoardEventProvider) (e.isRemove() ? e.getValue() : e.getPreviousValue());
        if (remoteProperty instanceof Value) {
            if (lastRemote != null && lastRemote.get((Value<U>) remoteProperty) == e.getProvider()) {
                lastRemote.set((Value<U>) remoteProperty, null, Source.INVERSE_REFERENCE);
            }
            if (newRemote != null) {
                newRemote.set((Value<U>) remoteProperty, (U) e.getProvider(), Source.INVERSE_REFERENCE);
            }
        } else if (remoteProperty instanceof Child) {
            if (lastRemote != null) {
                lastRemote.remove((Child<U>) remoteProperty, (U) e.getProvider(), Source.INVERSE_REFERENCE);
            }
            if (newRemote != null) {
                newRemote.add((Child<U>) remoteProperty, (U) e.getProvider(), Source.INVERSE_REFERENCE);
            }
        }
    }

    private Property<U> remoteProperty;
}
