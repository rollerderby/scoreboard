package com.carolinarollergirls.scoreboard.rules;

public class BooleanRule extends Rule {
    public BooleanRule(String fullname, String description, boolean defaultValue, String trueValue, String falseValue) {
        super("Boolean", fullname, description, new Boolean(defaultValue));

        this.trueValue = trueValue;
        this.falseValue = falseValue;
    }

    public boolean isValueValid(String v) {
        try {
            new Boolean(v);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String toHumanReadable(Object v) {
        if (v == null) {
            return "";
        }

        return new Boolean(v.toString()) ? trueValue : falseValue;
    }

    public String getTrueValue() {
        return trueValue;
    }
    public String getFalseValue() {
        return falseValue;
    }

    private String trueValue;
    private String falseValue;
}
