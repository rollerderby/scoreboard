package com.carolinarollergirls.scoreboard.event;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Source;

public abstract class CopyScoreBoardListener<T> implements ScoreBoardListener {
    CopyScoreBoardListener(ScoreBoardEventProvider guardElement, Value<Boolean> guardProperty) {
        this.guardElement = guardElement;
        this.guardProperty = guardProperty;
    }

    public abstract void scoreBoardChange(ScoreBoardEvent<T> event, Source source);
    public boolean isActive() { return (guardElement == null || guardElement.get(guardProperty)); }

    protected ScoreBoardEventProvider guardElement;
    protected Value<Boolean> guardProperty;
}
