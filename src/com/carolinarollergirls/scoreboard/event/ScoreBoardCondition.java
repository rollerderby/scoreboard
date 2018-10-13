package com.carolinarollergirls.scoreboard.event;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Objects;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;

public class ScoreBoardCondition implements Cloneable {
    public ScoreBoardCondition(ScoreBoardEvent e) {
        this(e.getProvider(), e.getProperty(), e.getValue());
    }
    public ScoreBoardCondition(ScoreBoardEventProvider sbeP, String p, Object v) {
        this(sbeP.getProviderClass(), sbeP.getProviderId(), p, v);
    }
    public ScoreBoardCondition(Class<? extends ScoreBoardEventProvider> c, String id, String p, Object v) {
        providerClass = c;
        providerId = id;
        property = p;
        value = v;
    }

    public Class<? extends ScoreBoardEventProvider> getProviderClass() { return providerClass; }
    public String getProviderId() { return providerId; }
    public String getProperty() { return property; }
    public Object getValue() { return value; }

    public Object clone() { return new ScoreBoardCondition(getProviderClass(), getProviderId(), getProperty(), getValue()); }

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
        Class<? extends ScoreBoardEventProvider> pc = (null == e.getProvider() ? null : e.getProvider().getProviderClass());
        if (!ScoreBoardManager.ObjectsEquals(getProviderClass(), pc)) {
            return false;
        }
        String pi = (null == e.getProvider() ? null : e.getProvider().getProviderId());
        if (!ScoreBoardManager.ObjectsEquals(getProviderId(), pi) && (getProviderId() != ANY_ID)) {
            return false;
        }
        if (!ScoreBoardManager.ObjectsEquals(getProperty(), e.getProperty())) {
            return false;
        }
        if (!ScoreBoardManager.ObjectsEquals(getValue(), e.getValue()) && (getValue() != ANY_VALUE)) {
            return false;
        }
        return true;
    }
    public boolean equals(ScoreBoardCondition c) {
        if (!ScoreBoardManager.ObjectsEquals(getProviderClass(), c.getProviderClass())) {
            return false;
        }
        if (!ScoreBoardManager.ObjectsEquals(getProviderId(), c.getProviderId())) {
            return false;
        }
        if (!ScoreBoardManager.ObjectsEquals(getProperty(), c.getProperty())) {
            return false;
        }
        if (!ScoreBoardManager.ObjectsEquals(getValue(), c.getValue())) {
            return false;
        }
        return true;
    }
    public int hashCode() {
        return Objects.hash(providerClass, providerId, property, value);
    }

    protected Class<? extends ScoreBoardEventProvider> providerClass;
    protected String providerId;
    protected String property;
    protected Object value;

    public static final String ANY_ID = new String(ScoreBoardCondition.class.getName()+".ANY_ID");
    public static final Object ANY_VALUE = new Object();
}
