package com.carolinarollergirls.scoreboard.rules;

public class Rule {
    public Rule(String type, String fullname, String description, Object defaultValue) {
        this.type = type;
        this.fullname = fullname;
        this.defaultValue = defaultValue.toString();
        this.description = description;

        name = "";
        group = "";
        subgroup = "";
        String[] parts = fullname.split("[.]");
        if (parts.length > 0) {
            name = parts[parts.length - 1];
        }
        if (parts.length > 1) {
            group = parts[0];
        }
        if (parts.length > 2) {
            subgroup = fullname.substring(fullname.indexOf(".")+1, fullname.lastIndexOf("."));
        }
    }

    public String getType() { return type; }
    public String getName() { return name; }
    public String getGroup() { return group; }
    public String getSubgroup() { return subgroup; }
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
