package com.carolinarollergirls.scoreboard.utils;

import com.carolinarollergirls.scoreboard.event.ValueWithId;

public class ValWithId implements ValueWithId {
    public ValWithId(String i, String val) {
        id = i;
        value = val;
    }

    @Override
    public String getId() {
        return id;
    }
    @Override
    public String getValue() {
        return value;
    }

    private String id;
    private String value;
}
