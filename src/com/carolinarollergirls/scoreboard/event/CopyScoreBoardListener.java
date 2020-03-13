package com.carolinarollergirls.scoreboard.event;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Source;

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

    @Override
    public void scoreBoardChange(ScoreBoardEvent event) {
        scoreBoardChange(event, Source.COPY);
    }
    // used when sending updates from the copy to the master value 
    public void scoreBoardChange(ScoreBoardEvent event, Source source) {
        if (targetElement != null) {
            targetElement.set(targetProperty, event.getValue(), source);
        }
    }
    
    protected ScoreBoardEventProvider targetElement;
    protected PermanentProperty targetProperty;
    protected boolean setCopyFlag = true;
}
