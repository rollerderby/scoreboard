package com.carolinarollergirls.scoreboard.event;

public interface NumberedScoreBoardEventProvider<T extends NumberedScoreBoardEventProvider<T>> extends ScoreBoardEventProvider {
    public T getPrevious();
    public T getPrevious(boolean create);
    public T getPrevious(boolean create, boolean skipEmpty);
    public boolean hasPrevious(boolean skipEmpty);
    public T getNext();
    public T getNext(boolean create, boolean skipEmpty);
    public boolean hasNext(boolean skipEmpty);
    
    public int getNumber();
    public void setNumber(int num);
    public void setNumber(int num, boolean removeSilent);
}
