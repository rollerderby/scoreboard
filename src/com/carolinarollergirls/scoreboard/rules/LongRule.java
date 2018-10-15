package com.carolinarollergirls.scoreboard.rules;


public class LongRule extends Rule {
    public LongRule(String fullname, String description, int defaultValue) {
        super("Long", fullname, description, new Long(defaultValue));
    }

    public Object convertValue(String v) {
        try {
            return new Long(v);
        } catch (Exception e) {
            return null;
        }
    }
}
