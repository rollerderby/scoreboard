package com.carolinarollergirls.scoreboard.rules;


public class LongRule extends Rule {
    public LongRule(String fullname, String description, int defaultValue) {
        super("Long", fullname, description, new Long(defaultValue));
    }

    public boolean isValueValid(String v) {
        try {
            new Long(v);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
