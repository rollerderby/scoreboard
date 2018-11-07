package com.carolinarollergirls.scoreboard.rules;

public class Rule {
    public Rule(String type, String fullname, String description, Object defaultValue) {
        this.type = type;
        this.fullname = fullname;
        this.defaultValue = defaultValue.toString();
        this.description = description;

    }

    public String getType() { return type; }
    public String getFullName() { return fullname; }
    public String getDefaultValue() { return defaultValue; }
    public String getDescription() { return description; }

    public boolean isValueValid(String v) {
        return false;
    }

    public String toHumanReadable(Object v) {
        if (v == null) {
            return "";
        }
        return v.toString();
    }

    protected String type;
    protected String group;
    protected String subgroup;
    protected String name;
    protected String fullname;
    protected String defaultValue;
    protected String description;
}
