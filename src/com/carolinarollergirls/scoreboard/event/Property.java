package com.carolinarollergirls.scoreboard.event;

import java.util.Collection;

public abstract class Property<T> {
    public Property(Class<T> type, String jsonName, Collection<Property<?>> propsToAddTo) {
        this.type = type;
        this.jsonName = jsonName;
        if (propsToAddTo != null) { propsToAddTo.add(this); }
    }

    @Override
    public String toString() {
        return jsonName;
    }

    public Class<T> getType() { return type; }
    public String getJsonName() { return jsonName; }

    private Class<T> type;
    private String jsonName;
}
