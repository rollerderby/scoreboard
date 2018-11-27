package com.carolinarollergirls.scoreboard.rules;


public class IntegerRule extends AbstractRule {
    public IntegerRule(String fullname, String description, int defaultValue) {
        super(Type.INTEGER, fullname, description, new Integer(defaultValue));
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
