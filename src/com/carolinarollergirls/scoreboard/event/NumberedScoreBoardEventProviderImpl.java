package com.carolinarollergirls.scoreboard.event;

import java.util.Objects;
import java.util.UUID;

public abstract class NumberedScoreBoardEventProviderImpl<C extends NumberedScoreBoardEventProvider<C>>
        extends OrderedScoreBoardEventProviderImpl<C> implements NumberedScoreBoardEventProvider<C> {

    protected NumberedScoreBoardEventProviderImpl(ScoreBoardEventProvider parent, int number,
            NumberedChild<C> type) {
        super(parent, UUID.randomUUID().toString(), type);
        ownType = type;
        values.put(NUMBER, number);
        addWriteProtectionOverride(NUMBER, Source.RENUMBER);
        setNeighbors(getNumber());
    }
    protected NumberedScoreBoardEventProviderImpl(NumberedScoreBoardEventProviderImpl<C> cloned, ScoreBoardEventProvider root) {
        super(cloned, root);
        ownType = cloned.ownType;
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
        C next = null;
        if (source != Source.UNLINK) {
            next = getNext();
            unlinkNeighbors();
        }
        super.delete(source);
        if (next != null && next.getNumber() == getNumber() + 1) {
            next.set(NUMBER, getNumber(), Source.RENUMBER);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object _computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        value = super._computeValue(prop, value, last, source, flag);
        if (prop == NUMBER && last != null && !Objects.equals(value, last)) {
            parent.remove(ownType, (C) this, Source.RENUMBER);
        }
        return value;
    }
    @SuppressWarnings("unchecked")
    @Override
    protected <T> void _valueChanged(Value<T> prop, T value, T last, Source source, Flag flag) {
        if (prop == NUMBER && last != null) {
            if (flag != Flag.SPECIAL_CASE) {
                if (hasNext() && getNext().getNumber() == (Integer) last + 1) {
                    getNext().set(NUMBER, (Integer) value + 1, Source.RENUMBER);
                }
                if (hasPrevious() && getPrevious().getNumber() == (Integer) last - 1) {
                    getPrevious().set(NUMBER, (Integer) value - 1, Source.RENUMBER);
                }
            }
            parent.add(ownType, (C) this, Source.RENUMBER);
        }
        super._valueChanged(prop, value, last, source, flag);
    }

    @Override
    public void moveToNumber(int num) {
        synchronized (coreLock) {
            if (num == getNumber()) { return; }
            unlinkNeighbors();
            setNeighbors(num);
            set(NUMBER, num, Source.RENUMBER);
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
        if (parent.get(ownType, targetPosition) != null) {
            C replaced = parent.get(ownType, targetPosition);
            if (targetPosition <= getNumber()) {
                setPrevious(replaced.getPrevious());
                setNext(replaced);
                replaced.set(NUMBER, 1, Source.RENUMBER, Flag.CHANGE);
            } else {
                setNext(replaced.getNext());
                setPrevious(replaced);
                replaced.set(NUMBER, -1, Source.RENUMBER, Flag.CHANGE);
            }
        } else if (parent.numberOf(ownType) == 0) {
            // do not set previous or next
        } else if (targetPosition > parent.getMaxNumber(ownType)) {
            C prev = parent.getLast(ownType);
            setNext(prev.getNext());
            setPrevious(prev);
        } else if (targetPosition < parent.getMinNumber(ownType)) {
            C next = parent.getFirst(ownType);
            setPrevious(next.getPrevious());
            setNext(next);
        } else {
            int n = targetPosition + 1;
            while (parent.get(ownType, n) == null) { n++; }
            C next = parent.get(ownType, n);
            setPrevious(next.getPrevious());
            setNext(next);
        }
    }

    @SuppressWarnings("hiding")
    protected NumberedChild<C> ownType;
}
