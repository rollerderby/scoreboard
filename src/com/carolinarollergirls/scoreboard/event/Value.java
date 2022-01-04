package com.carolinarollergirls.scoreboard.event;

import java.util.Collection;

public class Value<T> extends Property<T> {
    public Value(Class<T> type, String jsonName, T defaultValue, Collection<Property<?>> propsToAddTo) {
        super(type, jsonName, propsToAddTo);
        this.defaultValue = defaultValue;
    }
    public T getDefaultValue() { return defaultValue; }

    private T defaultValue;
}
