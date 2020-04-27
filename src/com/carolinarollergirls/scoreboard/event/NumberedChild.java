package com.carolinarollergirls.scoreboard.event;

public class NumberedChild<T extends OrderedScoreBoardEventProvider<T>> extends Child<T> {
    public NumberedChild(Class<T> type, String jsonName) {
        super(type, jsonName);
    }
}
