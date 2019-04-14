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

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;

public class ScoreBoardCondition implements Cloneable {
    public ScoreBoardCondition(ScoreBoardEvent e) {
        this(e.getProvider(), e.getProperty(), e.getValue());
    }
    public ScoreBoardCondition(ScoreBoardEventProvider sbeP, Property p, Object v) {
        this(sbeP.getProviderClass(), sbeP.getId(), p, v);
    }
    public ScoreBoardCondition(Class<? extends ScoreBoardEventProvider> c, String id, Property p, Object v) {
        providerClass = c;
        providerId = id;
        property = p;
        value = v;
    }

    public Class<? extends ScoreBoardEventProvider> getProviderClass() { return providerClass; }
    public String getProviderId() { return providerId; }
    public Property getProperty() { return property; }
    public Object getValue() { return value; }

    @Override
    public Object clone() { return new ScoreBoardCondition(getProviderClass(), getProviderId(), getProperty(), getValue()); }

    @Override
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
        if (!Objects.equals(getProviderClass(), pc)) {
            return false;
        }
        String pi = (null == e.getProvider() ? null : e.getProvider().getId());
        if (!Objects.equals(getProviderId(), pi) && (getProviderId() != ANY_ID)) {
            return false;
        }
        if (!Objects.equals(getProperty(), e.getProperty())) {
            return false;
        }
        if (!Objects.equals(getValue(), e.getValue()) && (getValue() != ANY_VALUE)) {
            return false;
        }
        return true;
    }
    public boolean equals(ScoreBoardCondition c) {
        if (!Objects.equals(getProviderClass(), c.getProviderClass())) {
            return false;
        }
        if (!Objects.equals(getProviderId(), c.getProviderId())) {
            return false;
        }
        if (!Objects.equals(getProperty(), c.getProperty())) {
            return false;
        }
        if (!Objects.equals(getValue(), c.getValue())) {
            return false;
        }
        return true;
    }
    @Override
    public int hashCode() {
        return Objects.hash(providerClass, providerId, property, value);
    }

    protected Class<? extends ScoreBoardEventProvider> providerClass;
    protected String providerId;
    protected Property property;
    protected Object value;

    public static final String ANY_ID = new String(ScoreBoardCondition.class.getName()+".ANY_ID");
    public static final Object ANY_VALUE = new Object();
}
