package com.carolinarollergirls.scoreboard.event;

public class AddRemoveProperty<T extends ValueWithId> extends Property<T> {
    public AddRemoveProperty(Class<T> type, String jsonName) {
        super(type, jsonName);
    }
}
