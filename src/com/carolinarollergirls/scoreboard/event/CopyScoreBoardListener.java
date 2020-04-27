package com.carolinarollergirls.scoreboard.event;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Source;

public class CopyScoreBoardListener<T> implements ScoreBoardListener {
    CopyScoreBoardListener(ScoreBoardEventProvider targetElement, Value<T> targetProperty) {
        this.targetElement = targetElement;
        this.targetProperty = targetProperty;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void scoreBoardChange(ScoreBoardEvent<?> event) {
        scoreBoardChange((ScoreBoardEvent<T>) event, Source.COPY);
    }
    // used when sending updates from the copy to the master value
    @SuppressWarnings("unchecked")
    public void scoreBoardChange(ScoreBoardEvent<T> event, Source source) {
        if (targetElement != null) {
            targetElement.set(targetProperty, event.getValue(), source);
        }
    }

    protected ScoreBoardEventProvider targetElement;
    protected Value<T> targetProperty;
}
