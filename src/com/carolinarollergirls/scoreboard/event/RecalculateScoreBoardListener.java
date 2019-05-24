package com.carolinarollergirls.scoreboard.event;

import java.util.HashMap;
import java.util.Map;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Flag;

public class RecalculateScoreBoardListener implements UnlinkableScoreBoardListener {
    RecalculateScoreBoardListener(ScoreBoardEventProvider targetElement, PermanentProperty targetProperty) {
        this.targetElement = targetElement;
        this.targetProperty = targetProperty;
    }

    @Override
    public void scoreBoardChange(ScoreBoardEvent event) {
        targetElement.set(targetProperty, targetElement.get(targetProperty), Flag.RECALCULATE);
    }
    
    public RecalculateScoreBoardListener addSource(ScoreBoardEventProvider element, Property property) {
        ConditionalScoreBoardListener l = new ConditionalScoreBoardListener(element, property, this);
        sources.put(l, element);
        element.addScoreBoardListener(l);
        return this;
    }
    public RecalculateScoreBoardListener addIndirectSource(ScoreBoardEventProvider indirectionElement,
            PermanentProperty indirectionProperty, Property watchedProperty) {
        IndirectScoreBoardListener l = new
                IndirectScoreBoardListener(indirectionElement, indirectionProperty, watchedProperty, this);
        sources.put(l, null);
        return this;
    }
    
    @Override
    public void unlink() {
        for (ScoreBoardListener l : sources.keySet()) {
            if (l instanceof IndirectScoreBoardListener) {
                ((IndirectScoreBoardListener) l).unlink();
            } else {
                sources.get(l).removeScoreBoardListener(l);
            }
        }
    }
    
    protected Map<ScoreBoardListener, ScoreBoardEventProvider> sources = new HashMap<>();
    protected ScoreBoardEventProvider targetElement;
    protected PermanentProperty targetProperty;
}
