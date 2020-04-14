package com.carolinarollergirls.scoreboard.event;

public class IndirectScoreBoardListener<T, U> implements SelfRemovingScoreBoardListener {
    public IndirectScoreBoardListener(ScoreBoardEventProvider indirectionElement,
            PermanentProperty<U> indirectionProperty, Property<T> watchedProperty, ScoreBoardListener listener) {
        indirectionListener = new ConditionalScoreBoardListener<>(indirectionElement, indirectionProperty, this);
        this.indirectionElement = indirectionElement;
        indirectionElement.addScoreBoardListener(indirectionListener);
        this.watchedProperty = watchedProperty;
        externalListener = listener;
        scoreBoardChange(new ScoreBoardEvent<>(indirectionElement, indirectionProperty,
                indirectionElement.get(indirectionProperty), null));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void scoreBoardChange(ScoreBoardEvent<?> event) {
        if (event.getValue() == watchedElement) { return; }
        ScoreBoardEventProvider lastWatched = watchedElement;
        if (watchedProperty instanceof PermanentProperty) {
            PermanentProperty<T> wp = (PermanentProperty<T>) watchedProperty;
            T last = wp.getDefaultValue();
            if (watchedElement != null) {
                last = watchedElement.get(wp);
                watchedElement.removeScoreBoardListener(listener);
            }
            watchedElement = (ScoreBoardEventProvider) event.getValue();
            if (watchedElement != null) {
                listener = new ConditionalScoreBoardListener<>(watchedElement, wp, externalListener);
                watchedElement.addScoreBoardListener(listener);
                externalListener
                        .scoreBoardChange(new ScoreBoardEvent<>(watchedElement, wp, watchedElement.get(wp), last));
            } else {
                listener = null;
                externalListener.scoreBoardChange(new ScoreBoardEvent<>(lastWatched, wp, wp.getDefaultValue(), last));
            }
        } else if (watchedProperty instanceof AddRemoveProperty) {
            AddRemoveProperty<?> wp = (AddRemoveProperty<?>) watchedProperty;
            if (watchedElement != null) {
                for (ValueWithId v : watchedElement.getAll(wp)) {
                    externalListener
                            .scoreBoardChange(new ScoreBoardEvent<>(watchedElement, watchedProperty, (T) v, true));
                }
                watchedElement.removeScoreBoardListener(listener);
            }
            watchedElement = (ScoreBoardEventProvider) event.getValue();
            if (watchedElement != null) {
                listener = new ConditionalScoreBoardListener<>(watchedElement, wp, externalListener);
                watchedElement.addScoreBoardListener(listener);
                for (ValueWithId v : watchedElement.getAll(wp)) {
                    externalListener
                            .scoreBoardChange(new ScoreBoardEvent<>(watchedElement, watchedProperty, (T) v, false));
                }
            } else {
                listener = null;
            }
        }
    }

    @Override
    public void delete() {
        if (watchedElement != null) {
            watchedElement.removeScoreBoardListener(listener);
        }
        indirectionElement.removeScoreBoardListener(indirectionListener);
    }

    protected ScoreBoardEventProvider indirectionElement;
    protected ScoreBoardEventProvider watchedElement;
    protected Property<T> watchedProperty;
    protected ConditionalScoreBoardListener<U> indirectionListener;
    protected ScoreBoardListener listener;
    protected ScoreBoardListener externalListener;
}
