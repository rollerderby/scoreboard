package com.carolinarollergirls.scoreboard.event;

public abstract class Property<T> {
    public Property(Class<T> type, String jsonName) {
        this.type = type;
        this.jsonName = jsonName;
    }

    @Override
    public String toString() { return jsonName; }

    public Class<T> getType() { return type; }
    public String getJsonName() { return jsonName; }

    private Class<T> type;
    private String jsonName;
}
