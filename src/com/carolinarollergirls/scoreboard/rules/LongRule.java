package com.carolinarollergirls.scoreboard.rules;


public class LongRule extends RuleDefinition {
    public LongRule(String fullname, String description, int defaultValue) {
        super(Type.LONG, fullname, description, defaultValue);
    }

    @Override
    public boolean isValueValid(String v) {
        try {
            Long.valueOf(v);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
