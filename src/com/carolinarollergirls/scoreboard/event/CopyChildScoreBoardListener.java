package com.carolinarollergirls.scoreboard.event;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Source;

public class CopyChildScoreBoardListener<T extends ValueWithId> extends CopyScoreBoardListener<T> {
    CopyChildScoreBoardListener(ScoreBoardEventProvider targetElement, Child<T> targetProperty,
                                ScoreBoardEventProvider guardElement, Value<Boolean> guardProperty) {
        super(guardElement, guardProperty);
        this.targetElement = targetElement;
        this.targetProperty = targetProperty;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void scoreBoardChange(ScoreBoardEvent<?> event) {
        scoreBoardChange((ScoreBoardEvent<T>) event, Source.COPY);
    }
    // used when sending updates from the copy to the master value
    @Override
    @SuppressWarnings("unchecked")
    public void scoreBoardChange(ScoreBoardEvent<T> event, Source source) {
        if (isActive() && targetElement != null) {
            if (event.isRemove()) {
                targetElement.remove(targetProperty, event.getValue().getId(), source);
            } else {
                targetElement.add(targetProperty, event.getValue(), source);
            }
        }
    }

    protected ScoreBoardEventProvider targetElement;
    protected Child<T> targetProperty;
}
