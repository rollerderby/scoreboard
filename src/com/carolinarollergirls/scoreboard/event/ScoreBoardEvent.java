package com.carolinarollergirls.scoreboard.event;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.EventObject;
import java.util.Objects;

public class ScoreBoardEvent<T> extends EventObject implements Cloneable {
    public ScoreBoardEvent(ScoreBoardEventProvider sbeP, Value<T> p, T v, T prev) {
        this(sbeP, (Property<T>) p, v, prev);
    }

    public ScoreBoardEvent(ScoreBoardEventProvider sbeP, Property<T> p, T v, boolean r) {
        this(sbeP, p, v, null);
        remove = r;
    }

    private ScoreBoardEvent(ScoreBoardEventProvider sbeP, Property<T> p, T v, T prev) {
        super(sbeP);
        provider = sbeP;
        property = p;
        value = v;
        previousValue = prev;
        remove = false;
    }

    public ScoreBoardEventProvider getProvider() { return provider; }
    public Property<T> getProperty() { return property; }
    public T getValue() { return value; }
    public T getPreviousValue() { return previousValue; }
    public boolean isRemove() { return remove; }

    @Override
    public Object clone() {
        return new ScoreBoardEvent<>(getProvider(), getProperty(), getValue(), getPreviousValue());
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object o) {
        if (null == o) { return false; }
        try {
            return equals((ScoreBoardEvent<T>) o);
        } catch (ClassCastException ccE) {}
        try {
            return equals((ScoreBoardCondition<T>) o);
        } catch (ClassCastException ccE) {}
        return false;
    }
    public boolean equals(ScoreBoardEvent<T> e) {
        if (!Objects.equals(getProvider(), e.getProvider())) { return false; }
        if (!Objects.equals(getProperty(), e.getProperty())) { return false; }
        if (!Objects.equals(getValue(), e.getValue())) { return false; }
        if (!Objects.equals(getPreviousValue(), e.getPreviousValue())) { return false; }
        return true;
    }
    public boolean equals(ScoreBoardCondition<T> c) { return c.equals(this); }
    @Override
    public int hashCode() {
        return Objects.hash(provider, property, value, previousValue);
    }

    @Override
    public String toString() {
        return provider.getClass().getName() + ": " + property + "='" + value + "' (was '" + previousValue + "')";
    }

    protected ScoreBoardEventProvider provider;
    protected Property<T> property;
    protected T value;
    protected T previousValue;
    protected boolean remove;
}
