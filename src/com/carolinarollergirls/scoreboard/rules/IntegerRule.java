package com.carolinarollergirls.scoreboard.rules;

public class IntegerRule extends RuleDefinition {
    public IntegerRule(String fullname, String description, int defaultValue) {
        super(Type.INTEGER, fullname, description, defaultValue);
    }

    @Override
    public boolean isValueValid(String v) {
        try {
            Integer.parseInt(v);
            return true;
        } catch (Exception e) { return false; }
    }
}
