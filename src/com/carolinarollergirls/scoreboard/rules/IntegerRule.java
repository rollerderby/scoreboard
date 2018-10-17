package com.carolinarollergirls.scoreboard.rules;


public class IntegerRule extends Rule {
    public IntegerRule(String fullname, String description, int defaultValue) {
        super("Integer", fullname, description, new Integer(defaultValue));
    }

    public boolean isValueValid(String v) {
        try {
            Integer.parseInt(v);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
