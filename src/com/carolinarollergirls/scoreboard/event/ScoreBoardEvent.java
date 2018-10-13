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

import com.carolinarollergirls.scoreboard.ScoreBoardManager;

public class ScoreBoardEvent extends EventObject implements Cloneable {
    public ScoreBoardEvent(ScoreBoardEventProvider sbeP, String p, Object v, Object prev) {
        super(sbeP);
        provider = sbeP;
        property = p;
        value = v;
        previousValue = prev;
    }

    public ScoreBoardEventProvider getProvider() { return provider; }
    public String getProperty() { return property; }
    public Object getValue() { return value; }
    public Object getPreviousValue() { return previousValue; }

    public Object clone() { return new ScoreBoardEvent(getProvider(), getProperty(), getValue(), getPreviousValue()); }

    public boolean equals(Object o) {
        if (null == o) {
            return false;
        }
        try { return equals((ScoreBoardEvent)o); }
        catch ( ClassCastException ccE ) { }
        try { return equals((ScoreBoardCondition)o); }
        catch ( ClassCastException ccE ) { }
        return false;
    }
    public boolean equals(ScoreBoardEvent e) {
        if (!ScoreBoardManager.ObjectsEquals(getProvider(), e.getProvider())) {
            return false;
        }
        if (!ScoreBoardManager.ObjectsEquals(getProperty(), e.getProperty())) {
            return false;
        }
        if (!ScoreBoardManager.ObjectsEquals(getValue(), e.getValue())) {
            return false;
        }
        if (!ScoreBoardManager.ObjectsEquals(getPreviousValue(), e.getPreviousValue())) {
            return false;
        }
        return true;
    }
    public boolean equals(ScoreBoardCondition c) {
        return c.equals(this);
    }
    public int hashCode() {
        return Objects.hash(provider, property, value, previousValue);
    }

    public String toString() {
        return provider.getClass().getName() + ": " + property + "='" + value + "' (was '" + previousValue + "')";
    }

    protected ScoreBoardEventProvider provider;
    protected String property;
    protected Object value;
    protected Object previousValue;

    public static final String BATCH_START = "Batch Start";
    public static final String BATCH_END = "Batch End";
}
