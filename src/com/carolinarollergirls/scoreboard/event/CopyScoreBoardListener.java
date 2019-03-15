package com.carolinarollergirls.scoreboard.event;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Flag;

public class CopyScoreBoardListener implements ScoreBoardListener {
    CopyScoreBoardListener(ScoreBoardEventProvider targetElement, PermanentProperty targetProperty,
            boolean setCopyFlag) {
        this(targetElement, targetProperty);
        this.setCopyFlag = setCopyFlag;
    }
    CopyScoreBoardListener(ScoreBoardEventProvider targetElement, PermanentProperty targetProperty) {
        this.targetElement = targetElement;
        this.targetProperty = targetProperty;
    }

    public void scoreBoardChange(ScoreBoardEvent event) {
        if (targetElement != null) {
            targetElement.set(targetProperty, event.getValue(), Flag.COPY);
        }
    }
    
    protected ScoreBoardEventProvider targetElement;
    protected PermanentProperty targetProperty;
    protected boolean setCopyFlag = true;
}
