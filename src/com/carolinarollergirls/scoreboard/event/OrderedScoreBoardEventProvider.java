package com.carolinarollergirls.scoreboard.event;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public interface OrderedScoreBoardEventProvider<T extends OrderedScoreBoardEventProvider<T>> extends ScoreBoardEventProvider {
    public int getNumber();

    public T getPrevious();
    public boolean hasPrevious();
    public T getNext();
    public boolean hasNext();

    public enum IValue implements PermanentProperty {
        ID,
        NUMBER,
        PREVIOUS,
        NEXT
    }
}
