package com.carolinarollergirls.scoreboard.rules;

public class StringRule extends RuleDefinition {
    public StringRule(String fullname, String description, String defaultValue) {
        super(Type.STRING, fullname, description, defaultValue);
    }

    @Override
    public boolean isValueValid(String v) {
        return v != null;
    }
}
