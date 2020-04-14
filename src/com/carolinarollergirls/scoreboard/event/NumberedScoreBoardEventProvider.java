package com.carolinarollergirls.scoreboard.event;

public interface NumberedScoreBoardEventProvider<C extends NumberedScoreBoardEventProvider<C>>
        extends OrderedScoreBoardEventProvider<C> {
    public int compareTo(NumberedScoreBoardEventProvider<?> other);

    public void moveToNumber(int num);
}
