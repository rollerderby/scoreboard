package com.carolinarollergirls.scoreboard.event;

import java.util.Objects;
import java.util.UUID;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.NumberedProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;

public abstract class NumberedScoreBoardEventProviderImpl<T extends NumberedScoreBoardEventProvider<T>>
        extends OrderedScoreBoardEventProviderImpl<T> implements NumberedScoreBoardEventProvider<T> {

    @SafeVarargs
    protected NumberedScoreBoardEventProviderImpl(ScoreBoardEventProvider parent, int number,
            NumberedProperty type, Class<T> ownClass, Class<? extends Property>... props) {
        super(parent, UUID.randomUUID().toString(), type, ownClass, props);
        ownType = type;
        values.put(IValue.NUMBER, number);
        setNeighbors(getNumber());
    }

    @Override
    public String getProviderId() { return String.valueOf(getNumber()); }

    @Override
    public int compareTo(NumberedScoreBoardEventProvider<?> other) {
        if (other == null) { return -1; }
        if (getParent() == other.getParent()) {
            return getNumber() - other.getNumber();
        }
        if (getParent() == null) { return 1; }
        if (getParent() instanceof NumberedScoreBoardEventProvider<?> &&
                other.getParent() instanceof NumberedScoreBoardEventProvider<?>) {
            return ((NumberedScoreBoardEventProvider<?>)getParent()).compareTo(
                    (NumberedScoreBoardEventProvider<?>)other.getParent());
        }
        return getParent().compareTo(other.getParent());
    }
    
    @Override
    protected void unlink(boolean neighborsRemoved) {
        T next = null;
        if (!neighborsRemoved) {
            next = getNext();
            unlinkNeighbors();
        }
        super.unlink(neighborsRemoved);
        if (next != null && next.getNumber() == getNumber() + 1) {
            next.set(IValue.NUMBER, getNumber());
        }
    }

    @Override
    protected Object _computeValue(PermanentProperty prop, Object value, Object last, Flag flag) {
        value = super._computeValue(prop, value, last, flag);
        if (prop == IValue.NUMBER && last != null && !Objects.equals(value, last)) {
            parent.remove(ownType, this);
        }
        return value;
    }
    @Override
    protected void _valueChanged(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == IValue.NUMBER && last != null) {
            if (flag != Flag.INTERNAL) {
                if (hasNext() && getNext().getNumber() == (Integer)last + 1) {
                    getNext().set(IValue.NUMBER, (Integer)value + 1);
                } 
                if (hasPrevious() && getPrevious().getNumber() == (Integer)last - 1) {
                    getPrevious().set(IValue.NUMBER, (Integer)value - 1);
                }
            }
            parent.add(ownType, this);
        }
        super._valueChanged(prop, value, last, flag);
    }

    @Override
    public void setPrevious(T p) { set(IValue.PREVIOUS, p); }
    @Override
    public void setNext(T n) { set(IValue.NEXT, n); }

    @Override
    public void moveToNumber(int num)  {
        synchronized (coreLock) {
            if (num == getNumber()) { return; }
            unlinkNeighbors();
            setNeighbors(num);
            set(IValue.NUMBER, num);
        }
    }
    
    public void unlinkNeighbors() {
        if (hasPrevious()) {
            getPrevious().setNext(getNext());
        } else if (hasNext()) {
            getNext().setPrevious(getPrevious());
        }
    }
    @SuppressWarnings("unchecked")
    public void setNeighbors(int targetPosition) {
        if (parent.get(ownType, targetPosition) != null) {
            T replaced = (T)parent.get(ownType, targetPosition);
            if (targetPosition <= getNumber()) {
                setPrevious(replaced.getPrevious());
                setNext(replaced);
                replaced.set(IValue.NUMBER, 1, Flag.CHANGE);
            } else { 
                setNext(replaced.getNext());
                setPrevious(replaced);
                replaced.set(IValue.NUMBER, -1, Flag.CHANGE);
            }
        } else if (parent.getAll(ownType).size() == 0) {
            // do not set previous or next
        } else if (targetPosition > parent.getMaxNumber(ownType)) {
            T prev = (T) parent.getLast(ownType);
            setNext(prev.getNext());
            setPrevious(prev);
        } else if (targetPosition < parent.getMinNumber(ownType)) {
            T next = (T) parent.getFirst(ownType);
            setPrevious(next.getPrevious());
            setNext(next);
        } else {
            int n = targetPosition + 1;
            while (parent.get(ownType, n) == null) { n++; }
            T next = (T) parent.get(ownType, n);
            setPrevious(next.getPrevious());
            setNext(next);
        }
    }

    @SuppressWarnings("hiding")
    protected NumberedProperty ownType;
}
