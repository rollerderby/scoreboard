package com.carolinarollergirls.scoreboard.event;

import java.util.Objects;

public class ScoreBoardCondition<T> implements Cloneable {
    public ScoreBoardCondition(ScoreBoardEvent<T> e) { this(e.getProvider(), e.getProperty(), e.getValue()); }
    public ScoreBoardCondition(ScoreBoardEventProvider sbeP, Property<T> p) {
        this(sbeP.getProviderClass(), sbeP.getId(), p);
    }
    public ScoreBoardCondition(ScoreBoardEventProvider sbeP, Property<T> p, T v) {
        this(sbeP.getProviderClass(), sbeP.getId(), p, v);
    }
    public ScoreBoardCondition(Class<? extends ScoreBoardEventProvider> c, String id, Property<T> p) {
        this(c, id, p, null, true);
    }
    public ScoreBoardCondition(Class<? extends ScoreBoardEventProvider> c, String id, Property<T> p, T v) {
        this(c, id, p, v, false);
    }
    private ScoreBoardCondition(Class<? extends ScoreBoardEventProvider> c, String id, Property<T> p, T v, boolean av) {
        providerClass = c;
        providerId = id;
        property = p;
        value = v;
        anyValue = av;
    }

    public Class<? extends ScoreBoardEventProvider> getProviderClass() { return providerClass; }
    public String getProviderId() { return providerId; }
    public Property<T> getProperty() { return property; }
    public T getValue() { return value; }

    @Override
    public Object clone() {
        return new ScoreBoardCondition<>(getProviderClass(), getProviderId(), getProperty(), getValue());
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
        Class<? extends ScoreBoardEventProvider> pc =
            (null == e.getProvider() ? null : e.getProvider().getProviderClass());
        if (!Objects.equals(getProviderClass(), pc)) { return false; }
        String pi = (null == e.getProvider() ? null : e.getProvider().getId());
        if (!Objects.equals(getProviderId(), pi) && (getProviderId() != ANY_ID)) { return false; }
        if (!Objects.equals(getProperty(), e.getProperty())) { return false; }
        if (!Objects.equals(getValue(), e.getValue()) && !anyValue) { return false; }
        return true;
    }
    public boolean equals(ScoreBoardCondition<T> c) {
        if (!Objects.equals(getProviderClass(), c.getProviderClass())) { return false; }
        if (!Objects.equals(getProviderId(), c.getProviderId())) { return false; }
        if (!Objects.equals(getProperty(), c.getProperty())) { return false; }
        if (!Objects.equals(getValue(), c.getValue())) { return false; }
        return true;
    }
    @Override
    public int hashCode() {
        return Objects.hash(providerClass, providerId, property, value);
    }

    protected Class<? extends ScoreBoardEventProvider> providerClass;
    protected String providerId;
    protected Property<T> property;
    protected T value;
    protected boolean anyValue;

    public static final String ANY_ID = new String(ScoreBoardCondition.class.getName() + ".ANY_ID");
}
