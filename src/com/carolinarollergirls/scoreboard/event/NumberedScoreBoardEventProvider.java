package com.carolinarollergirls.scoreboard.event;

public interface NumberedScoreBoardEventProvider<T extends NumberedScoreBoardEventProvider<T>>
        extends OrderedScoreBoardEventProvider<T> {
    public int getNumber();
    public void moveToNumber(int num);

    public void setPrevious(T p);
    public void setNext(T n);
}
