package com.carolinarollergirls.scoreboard.event;

public class Value<T> extends Property<T> {
    public Value(Class<T> type, String jsonName, T defaultValue) {
        super(type, jsonName);
        this.defaultValue = defaultValue;
    }
    public T getDefaultValue() { return defaultValue; }

    private T defaultValue;
}
