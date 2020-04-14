package com.carolinarollergirls.scoreboard.event;

public class NumberedProperty<T extends OrderedScoreBoardEventProvider<T>> extends AddRemoveProperty<T> {
    public NumberedProperty(Class<T> type, String jsonName) {
        super(type, jsonName);
    }
}
