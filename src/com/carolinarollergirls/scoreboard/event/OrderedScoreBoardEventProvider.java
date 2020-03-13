package com.carolinarollergirls.scoreboard.event;

public interface OrderedScoreBoardEventProvider<T extends OrderedScoreBoardEventProvider<T>> extends ScoreBoardEventProvider {
    public int getNumber();

    public T getPrevious();
    public boolean hasPrevious();
    public T getNext();
    public boolean hasNext();
}
