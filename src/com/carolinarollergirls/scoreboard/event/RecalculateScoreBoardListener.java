package com.carolinarollergirls.scoreboard.event;

import java.util.HashMap;
import java.util.Map;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Source;

public class RecalculateScoreBoardListener<T> implements SelfRemovingScoreBoardListener {
    RecalculateScoreBoardListener(ScoreBoardEventProvider targetElement, Value<T> targetProperty) {
        this.targetElement = targetElement;
        this.targetProperty = targetProperty;
    }

    @Override
    public void scoreBoardChange(ScoreBoardEvent<?> event) {
        targetElement.set(targetProperty, targetElement.get(targetProperty), Source.RECALCULATE);
    }

    public RecalculateScoreBoardListener<T> addSource(ScoreBoardEventProvider element, Property<?> property) {
        ConditionalScoreBoardListener<?> l = new ConditionalScoreBoardListener<>(element, property, this);
        sources.put(l, element);
        element.addScoreBoardListener(l);
        return this;
    }
    public RecalculateScoreBoardListener<T>
    addIndirectSource(ScoreBoardEventProvider indirectionElement,
                      Value<? extends ScoreBoardEventProvider> indirectionProperty, Property<?> watchedProperty) {
        IndirectScoreBoardListener<?, ?> l =
            new IndirectScoreBoardListener<>(indirectionElement, indirectionProperty, watchedProperty, this);
        sources.put(l, null);
        return this;
    }

    @Override
    public void delete() {
        for (ScoreBoardListener l : sources.keySet()) {
            if (l instanceof IndirectScoreBoardListener) {
                ((IndirectScoreBoardListener<?, ?>) l).delete();
            } else {
                sources.get(l).removeScoreBoardListener(l);
            }
        }
    }

    protected Map<ScoreBoardListener, ScoreBoardEventProvider> sources = new HashMap<>();
    protected ScoreBoardEventProvider targetElement;
    protected Value<T> targetProperty;
}
