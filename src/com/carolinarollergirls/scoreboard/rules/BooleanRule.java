package com.carolinarollergirls.scoreboard.rules;

public class BooleanRule extends RuleDefinition {
    public BooleanRule(String fullname, String description, boolean defaultValue, String trueValue, String falseValue) {
        super(Type.BOOLEAN, fullname, description, Boolean.valueOf(defaultValue));

        values.put(Value.TRUE_VALUE, trueValue);
        values.put(Value.FALSE_VALUE, falseValue);
    }

    @Override
    public boolean isValueValid(String v) {
        try {
            Boolean.valueOf(v);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String toHumanReadable(Object v) {
        if (v == null) {
            return "";
        }

        return Boolean.valueOf(v.toString()) ? getTrueValue() : getFalseValue();
    }

    public String getTrueValue() {
        return (String)get(Value.TRUE_VALUE);
    }
    public String getFalseValue() {
        return (String)get(Value.FALSE_VALUE);
    }
}
