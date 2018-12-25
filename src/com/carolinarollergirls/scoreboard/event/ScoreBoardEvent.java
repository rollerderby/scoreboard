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

public class ScoreBoardEvent extends EventObject implements Cloneable {
    public ScoreBoardEvent(ScoreBoardEventProvider sbeP, PermanentProperty p, Object v, Object prev) {
        this(sbeP, (Property)p, v, prev);
    }

    public ScoreBoardEvent(ScoreBoardEventProvider sbeP, AddRemoveProperty p, ValueWithId v, ValueWithId prev) {
        this(sbeP, (Property)p, v, prev);
    }

    private ScoreBoardEvent(ScoreBoardEventProvider sbeP, Property p, Object v, Object prev) {
        super(sbeP);
        provider = sbeP;
        property = p;
        value = v;
        previousValue = prev;
    } 
    
    public ScoreBoardEventProvider getProvider() { return provider; }
    public Property getProperty() { return property; }
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
        if (!Objects.equals(getProvider(), e.getProvider())) {
            return false;
        }
        if (!Objects.equals(getProperty(), e.getProperty())) {
            return false;
        }
        if (!Objects.equals(getValue(), e.getValue())) {
            return false;
        }
        if (!Objects.equals(getPreviousValue(), e.getPreviousValue())) {
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
    protected Property property;
    protected Object value;
    protected Object previousValue;

    public interface Property {}
    public interface PermanentProperty extends Property {}
    public interface AddRemoveProperty extends Property {}
    public interface CommandProperty extends Property {}
    
    public interface ValueWithId {
	public String getId();
	public String getValue();
    }
}
