package com.carolinarollergirls.scoreboard.rules;

public class BooleanRule extends RuleDefinition {
    public BooleanRule(String fullname, String description, boolean defaultValue, String trueValue, String falseValue) {
        super(Type.BOOLEAN, fullname, description, Boolean.valueOf(defaultValue));
        addProperties(boolProps);
        set(TRUE_VALUE, trueValue);
        set(FALSE_VALUE, falseValue);
        addWriteProtection(TRUE_VALUE);
        addWriteProtection(FALSE_VALUE);
    }

    @Override
    public boolean isValueValid(String v) {
        try {
            Boolean.valueOf(v);
            return true;
        } catch (Exception e) { return false; }
    }

    public String toHumanReadable(Object v) {
        if (v == null) { return ""; }

        return Boolean.valueOf(v.toString()) ? getTrueValue() : getFalseValue();
    }

    public String getTrueValue() { return get(TRUE_VALUE); }
    public String getFalseValue() { return get(FALSE_VALUE); }
}
