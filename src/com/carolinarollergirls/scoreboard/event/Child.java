package com.carolinarollergirls.scoreboard.event;

import java.util.Collection;

public class Child<T extends ValueWithId> extends Property<T> {
    public Child(Class<T> type, String jsonName, Collection<Property<?>> propsToAddTo) {
        super(type, jsonName, propsToAddTo);
    }
}
