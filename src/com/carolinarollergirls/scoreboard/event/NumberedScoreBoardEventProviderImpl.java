package com.carolinarollergirls.scoreboard.event;

import java.util.Objects;
import java.util.UUID;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.NumberedProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;

public abstract class NumberedScoreBoardEventProviderImpl<T extends NumberedScoreBoardEventProvider<T>>
        extends OrderedScoreBoardEventProviderImpl<T> implements NumberedScoreBoardEventProvider<T> {

    @SafeVarargs
    protected NumberedScoreBoardEventProviderImpl(ScoreBoardEventProvider parent, int number, NumberedProperty type,
            Class<T> ownClass, Class<? extends Property>... props) {
        super(parent, UUID.randomUUID().toString(), type, ownClass, props);
        ownType = type;
        values.put(IValue.NUMBER, number);
        addWriteProtectionOverride(IValue.NUMBER, Source.RENUMBER);
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
        if (getParent() instanceof NumberedScoreBoardEventProvider<?>
                && other.getParent() instanceof NumberedScoreBoardEventProvider<?>) {
            return ((NumberedScoreBoardEventProvider<?>) getParent())
                    .compareTo((NumberedScoreBoardEventProvider<?>) other.getParent());
        }
        return getParent().compareTo(other.getParent());
    }

    @Override
    public void delete(Source source) {
        T next = null;
        if (source != Source.UNLINK) {
            next = getNext();
            unlinkNeighbors();
        }
        super.delete(source);
        if (next != null && next.getNumber() == getNumber() + 1) {
            next.set(IValue.NUMBER, getNumber(), Source.RENUMBER);
        }
    }

    @Override
    protected Object _computeValue(PermanentProperty prop, Object value, Object last, Source source, Flag flag) {
        value = super._computeValue(prop, value, last, source, flag);
        if (prop == IValue.NUMBER && last != null && !Objects.equals(value, last)) {
            parent.remove(ownType, this, Source.RENUMBER);
        }
        return value;
    }
    @Override
    protected void _valueChanged(PermanentProperty prop, Object value, Object last, Source source, Flag flag) {
        if (prop == IValue.NUMBER && last != null) {
            if (flag != Flag.SPECIAL_CASE) {
                if (hasNext() && getNext().getNumber() == (Integer) last + 1) {
                    getNext().set(IValue.NUMBER, (Integer) value + 1, Source.RENUMBER);
                }
                if (hasPrevious() && getPrevious().getNumber() == (Integer) last - 1) {
                    getPrevious().set(IValue.NUMBER, (Integer) value - 1, Source.RENUMBER);
                }
            }
            parent.add(ownType, this, Source.RENUMBER);
        }
        super._valueChanged(prop, value, last, source, flag);
    }

    @Override
    public void setPrevious(T p) { set(IValue.PREVIOUS, p); }
    @Override
    public void setNext(T n) { set(IValue.NEXT, n); }

    @Override
    public void moveToNumber(int num) {
        synchronized (coreLock) {
            if (num == getNumber()) { return; }
            unlinkNeighbors();
            setNeighbors(num);
            set(IValue.NUMBER, num, Source.RENUMBER);
        }
    }

    public void unlinkNeighbors() {
        if (hasPrevious()) {
            getPrevious().setNext(getNext());
        } else if (hasNext()) {
            getNext().setPrevious(getPrevious());
        }
    }
    public void setNeighbors(int targetPosition) {
        if (parent.get(ownType, getProviderClass(), targetPosition) != null) {
            T replaced = parent.get(ownType, getProviderClass(), targetPosition);
            if (targetPosition <= getNumber()) {
                setPrevious(replaced.getPrevious());
                setNext(replaced);
                replaced.set(IValue.NUMBER, 1, Source.RENUMBER, Flag.CHANGE);
            } else {
                setNext(replaced.getNext());
                setPrevious(replaced);
                replaced.set(IValue.NUMBER, -1, Source.RENUMBER, Flag.CHANGE);
            }
        } else if (parent.numberOf(ownType) == 0) {
            // do not set previous or next
        } else if (targetPosition > parent.getMaxNumber(ownType)) {
            T prev = parent.getLast(ownType, getProviderClass());
            setNext(prev.getNext());
            setPrevious(prev);
        } else if (targetPosition < parent.getMinNumber(ownType)) {
            T next = parent.getFirst(ownType, getProviderClass());
            setPrevious(next.getPrevious());
            setNext(next);
        } else {
            int n = targetPosition + 1;
            while (parent.get(ownType, getProviderClass(), n) == null) { n++; }
            T next = parent.get(ownType, getProviderClass(), n);
            setPrevious(next.getPrevious());
            setNext(next);
        }
    }

    @SuppressWarnings("hiding")
    protected NumberedProperty ownType;
}
