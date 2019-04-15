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

    @Override
    public void scoreBoardChange(ScoreBoardEvent event) {
        scoreBoardChange(event, Flag.COPY);
    }
    // used when sending updates from the copy to the master value 
    public void scoreBoardChange(ScoreBoardEvent event, Flag flag) {
        if (targetElement != null) {
            targetElement.set(targetProperty, event.getValue(), flag);
        }
    }
    
    protected ScoreBoardEventProvider targetElement;
    protected PermanentProperty targetProperty;
    protected boolean setCopyFlag = true;
}
