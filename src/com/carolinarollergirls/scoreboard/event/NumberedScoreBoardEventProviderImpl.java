package com.carolinarollergirls.scoreboard.event;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.NumberedProperty;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;

public abstract class NumberedScoreBoardEventProviderImpl<T extends NumberedScoreBoardEventProvider<T>> extends ScoreBoardEventProviderImpl implements NumberedScoreBoardEventProvider<T> {
    protected NumberedScoreBoardEventProviderImpl(NumberedProperty type, ScoreBoardEventProvider parent, String id, int minNum) {
	ownType = type;
	this.parent = parent;
	number = Integer.parseInt(id);
	minimumNumber = minNum; 
    }
    
    public String getProviderName() { return PropertyConversion.toFrontend(ownType); }
    public String getProviderId() { return String.valueOf(getNumber()); }
    public ScoreBoardEventProvider getParent() { return parent; }
    
    public T getPrevious() { return getPrevious(false, false); }
    public T getPrevious(boolean create) { return getPrevious(create, false); }
    @SuppressWarnings("unchecked")
    public T getPrevious(boolean create, boolean skipEmpty) {
	synchronized (coreLock) {
	    if (getNumber() == minimumNumber) { return null; }
	    int num = getNumber()-1;
	    T prev = (T)parent.get(ownType, String.valueOf(num), create);
	    int min = Integer.parseInt(parent.getFirst(ownType).getId());
	    while (prev == null && skipEmpty && num > min) {
		num--;
		prev = (T)parent.get(ownType, String.valueOf(num));
	    }
	    return prev;
	}
    }
    public boolean hasPrevious(boolean skipEmpty) { return getPrevious(false, skipEmpty) != null; }
    public T getNext() { return getNext(false, false); }
    @SuppressWarnings("unchecked")
    public T getNext(boolean create, boolean skipEmpty) {
	synchronized (coreLock) {
	    int num = getNumber()+1;
	    T next = (T)parent.get(ownType, String.valueOf(num), create);
	    int max = Integer.parseInt(parent.getLast(ownType).getId());
	    while (next == null && skipEmpty && num <= max) {
		num++;
		next = (T)parent.get(ownType, String.valueOf(num));
	    }
	    return next;
	}
    }
    public boolean hasNext(boolean skipEmpty) { return getNext(false, skipEmpty) != null; }
    
    public int getNumber() { return number; }
    public void setNumber(int num) { setNumber(num, false); }
    public void setNumber(int num, boolean removeSilent) {
	synchronized (coreLock) {
	    requestBatchStart();
	    if (removeSilent) {
		parent.removeSilent(ownType, this);
	    } else {
		parent.remove(ownType, this);
	    }
	    number = num;
	    parent.insert(ownType, this);
	    requestBatchEnd();
	}
    }
    
    protected ScoreBoardEventProvider parent;
    protected NumberedProperty ownType;
    protected int number;
    protected int minimumNumber;
}
