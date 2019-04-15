package com.carolinarollergirls.scoreboard.event;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public class IndirectScoreBoardListener implements UnlinkableScoreBoardListener {
    public IndirectScoreBoardListener(ScoreBoardEventProvider indirectionElement, PermanentProperty indirectionProperty,
            PermanentProperty watchedProperty, ScoreBoardListener listener) {
        indirectionListener = new ConditionalScoreBoardListener(indirectionElement, indirectionProperty, this);
        this.indirectionElement = indirectionElement;
        indirectionElement.addScoreBoardListener(indirectionListener);
        this.watchedProperty = watchedProperty;
        externalListener = listener;
        scoreBoardChange(new ScoreBoardEvent(indirectionElement, indirectionProperty, indirectionElement.get(indirectionProperty), null));
    }

    @Override
    public void scoreBoardChange(ScoreBoardEvent event) {
        if (event.getValue() == watchedElement) { return; }
        ScoreBoardEventProvider lastWatched = watchedElement;
        Object last = watchedProperty.getDefaultValue();
        if (watchedElement != null) {
            last = watchedElement.get(watchedProperty);
            watchedElement.removeScoreBoardListener(listener);
        }
        watchedElement = (ScoreBoardEventProvider) event.getValue();
        if (watchedElement != null) {
            listener = new ConditionalScoreBoardListener(watchedElement, watchedProperty, externalListener);
            watchedElement.addScoreBoardListener(listener);
            externalListener.scoreBoardChange(new ScoreBoardEvent(watchedElement, watchedProperty,
                    watchedElement.get(watchedProperty), last));
        } else {
            listener = null;
            externalListener.scoreBoardChange(new ScoreBoardEvent(lastWatched, watchedProperty,
                    watchedProperty.getDefaultValue(), last));
        }
    }
    
    @Override
    public void unlink() {
        if (watchedElement != null) {
            watchedElement.removeScoreBoardListener(listener);
        }
        indirectionElement.removeScoreBoardListener(indirectionListener);
    }
    
    protected ScoreBoardEventProvider indirectionElement;
    protected ScoreBoardEventProvider watchedElement;
    protected PermanentProperty watchedProperty;
    protected ConditionalScoreBoardListener indirectionListener;
    protected ScoreBoardListener listener;
    protected ScoreBoardListener externalListener;
}
