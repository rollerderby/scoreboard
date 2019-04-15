package com.carolinarollergirls.scoreboard.rules;


public class LongRule extends RuleDefinition {
    public LongRule(String fullname, String description, int defaultValue) {
        super(Type.LONG, fullname, description, new Long(defaultValue));
    }

    @Override
    public boolean isValueValid(String v) {
        try {
            new Long(v);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
