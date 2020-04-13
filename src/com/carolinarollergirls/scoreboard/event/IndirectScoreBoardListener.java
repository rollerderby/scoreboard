package com.carolinarollergirls.scoreboard.event;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;

public class IndirectScoreBoardListener implements UnlinkableScoreBoardListener {
    public IndirectScoreBoardListener(ScoreBoardEventProvider indirectionElement, PermanentProperty indirectionProperty,
            Property watchedProperty, ScoreBoardListener listener) {
        indirectionListener = new ConditionalScoreBoardListener(indirectionElement, indirectionProperty, this);
        this.indirectionElement = indirectionElement;
        indirectionElement.addScoreBoardListener(indirectionListener);
        this.watchedProperty = watchedProperty;
        externalListener = listener;
        scoreBoardChange(new ScoreBoardEvent(indirectionElement, indirectionProperty,
                indirectionElement.get(indirectionProperty), null));
    }

    @Override
    public void scoreBoardChange(ScoreBoardEvent event) {
        if (event.getValue() == watchedElement) { return; }
        ScoreBoardEventProvider lastWatched = watchedElement;
        if (watchedProperty instanceof PermanentProperty) {
            PermanentProperty wp = (PermanentProperty) watchedProperty;
            Object last = wp.getDefaultValue();
            if (watchedElement != null) {
                last = watchedElement.get(wp);
                watchedElement.removeScoreBoardListener(listener);
            }
            watchedElement = (ScoreBoardEventProvider) event.getValue();
            if (watchedElement != null) {
                listener = new ConditionalScoreBoardListener(watchedElement, wp, externalListener);
                watchedElement.addScoreBoardListener(listener);
                externalListener
                        .scoreBoardChange(new ScoreBoardEvent(watchedElement, wp, watchedElement.get(wp), last));
            } else {
                listener = null;
                externalListener.scoreBoardChange(new ScoreBoardEvent(lastWatched, wp, wp.getDefaultValue(), last));
            }
        } else if (watchedProperty instanceof AddRemoveProperty) {
            AddRemoveProperty wp = (AddRemoveProperty) watchedProperty;
            if (watchedElement != null) {
                for (ValueWithId v : watchedElement.getAll(wp, wp.getType())) {
                    externalListener.scoreBoardChange(new ScoreBoardEvent(watchedElement, wp, v, true));
                }
                watchedElement.removeScoreBoardListener(listener);
            }
            watchedElement = (ScoreBoardEventProvider) event.getValue();
            if (watchedElement != null) {
                listener = new ConditionalScoreBoardListener(watchedElement, wp, externalListener);
                watchedElement.addScoreBoardListener(listener);
                for (ValueWithId v : watchedElement.getAll(wp, wp.getType())) {
                    externalListener.scoreBoardChange(new ScoreBoardEvent(watchedElement, wp, v, false));
                }
            } else {
                listener = null;
            }
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
    protected Property watchedProperty;
    protected ConditionalScoreBoardListener indirectionListener;
    protected ScoreBoardListener listener;
    protected ScoreBoardListener externalListener;
}
