package com.carolinarollergirls.scoreboard.event;

public interface OrderedScoreBoardEventProvider<C extends OrderedScoreBoardEventProvider<C>>
        extends ScoreBoardEventProvider {
    public int getNumber();

    public C getPrevious();
    public boolean hasPrevious();
    public void setPrevious(C prev);
    public C getNext();
    public boolean hasNext();
    public void setNext(C next);

    public static final Value<Integer> NUMBER = new Value<>(Integer.class, "Number", 0);
}
