package com.carolinarollergirls.scoreboard.rules;


public class IntegerRule extends Rule {
    public IntegerRule(boolean onResetOnly, String fullname, String description, int defaultValue) {
        super(onResetOnly, "Integer", fullname, description, new Integer(defaultValue));
    }

    public Object convertValue(String v) {
        try {
            return new Integer(Integer.parseInt(v));
        } catch (Exception e) {
            return null;
        }
    }
}
