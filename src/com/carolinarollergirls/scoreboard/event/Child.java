package com.carolinarollergirls.scoreboard.event;

public class Child<T extends ValueWithId> extends Property<T> {
    public Child(Class<T> type, String jsonName) { super(type, jsonName); }
}
