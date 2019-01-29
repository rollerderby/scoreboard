package com.carolinarollergirls.scoreboard.event;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.NumberedProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;

public abstract class NumberedScoreBoardEventProviderImpl<T extends NumberedScoreBoardEventProvider<T>> extends ScoreBoardEventProviderImpl implements NumberedScoreBoardEventProvider<T> {
    @SafeVarargs
    protected NumberedScoreBoardEventProviderImpl(ScoreBoardEventProvider parent, NumberedProperty type,
	    PermanentProperty numProp, Class<? extends ScoreBoardEventProvider> ownClass, String id, Class<? extends Property>... props) {
	super(parent, type, ownClass, props);
	ownType = type;
	numberProperty = numProp;
	set(numProp, Integer.parseInt(id));
    }
    
    public String getProviderId() { return String.valueOf(getNumber()); }
    
    public T getPrevious() { return getPrevious(false, false); }
    public T getPrevious(boolean create) { return getPrevious(create, false); }
    @SuppressWarnings("unchecked")
    public T getPrevious(boolean create, boolean skipEmpty) {
	synchronized (coreLock) {
	    int num = getNumber()-1;
	    T prev = (T)parent.get(ownType, String.valueOf(num), create);
	    int min = parent.getMinNumber(ownType);
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
	    int max = parent.getMaxNumber(ownType);
	    while (next == null && skipEmpty && num <= max) {
		num++;
		next = (T)parent.get(ownType, String.valueOf(num));
	    }
	    return next;
	}
    }
    public boolean hasNext(boolean skipEmpty) { return getNext(false, skipEmpty) != null; }
    
    public int getNumber() { return (Integer)get(numberProperty); }
    public void setNumber(int num) { setNumber(num, false); }
    public void setNumber(int num, boolean removeSilent) {
	synchronized (coreLock) {
	    requestBatchStart();
	    if (removeSilent) {
		parent.removeSilent(ownType, this);
	    } else {
		parent.remove(ownType, this);
	    }
	    set(numberProperty, num);
	    parent.insert(ownType, this);
	    requestBatchEnd();
	}
    }
    
    protected NumberedProperty ownType;
    protected PermanentProperty numberProperty;
}
