package com.carolinarollergirls.scoreboard.event;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Flag;

public class InverseReferenceUpdateListener extends ConditionalScoreBoardListener {
    public InverseReferenceUpdateListener(ScoreBoardEventProvider localElement, 
            Property localProperty, Property remoteProperty) {
        super(localElement, localProperty);
        this.remoteProperty = remoteProperty;
    }

    public void matchedScoreBoardChange(ScoreBoardEvent e) {
        ScoreBoardEventProvider newRemote =
                (ScoreBoardEventProvider) (e.isRemove() ? null : e.getValue());
        ScoreBoardEventProvider lastRemote =
                (ScoreBoardEventProvider) (e.isRemove() ? e.getValue() : e.getPreviousValue());
        if (remoteProperty instanceof PermanentProperty) {
            if (lastRemote != null &&
                    lastRemote.get((PermanentProperty)remoteProperty) == e.getProvider()) {
                lastRemote.set((PermanentProperty)remoteProperty, null, Flag.INVERSE_REFERENCE);
            }
            if (newRemote != null) {
                newRemote.set((PermanentProperty)remoteProperty, e.getProvider(), Flag.INVERSE_REFERENCE);
            }
        } else if (remoteProperty instanceof AddRemoveProperty) {
            if (lastRemote != null) {
                lastRemote.remove((AddRemoveProperty)remoteProperty, e.getProvider());
            }
            if (newRemote != null) {
                newRemote.add((AddRemoveProperty)remoteProperty, e.getProvider());
            }
        }
    }

    private Property remoteProperty;
}
