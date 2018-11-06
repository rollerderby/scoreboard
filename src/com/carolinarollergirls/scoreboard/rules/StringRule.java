package com.carolinarollergirls.scoreboard.rules;


public class StringRule extends Rule {
    public StringRule(String fullname, String description, String defaultValue) {
        super("String", fullname, description, defaultValue);
    }

    public boolean isValueValid(String v) {
        return v != null;
    }
}
