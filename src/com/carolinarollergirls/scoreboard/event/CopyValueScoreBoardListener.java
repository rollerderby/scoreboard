package com.carolinarollergirls.scoreboard.event;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Source;

public class CopyValueScoreBoardListener<T> extends CopyScoreBoardListener<T> {
    CopyValueScoreBoardListener(ScoreBoardEventProvider targetElement, Value<T> targetProperty,
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
        if (isActive() && targetElement != null) { targetElement.set(targetProperty, event.getValue(), source); }
    }

    protected ScoreBoardEventProvider targetElement;
    protected Value<T> targetProperty;
}
