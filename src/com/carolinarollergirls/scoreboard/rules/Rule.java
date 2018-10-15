package com.carolinarollergirls.scoreboard.rules;

import org.json.JSONException;
import org.json.JSONObject;

public class Rule {
    public Rule(String type, String fullname, String description, Object defaultValue) {
        this.type = type;
        this.fullname = fullname;
        this.defaultValue = defaultValue;
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
    public Object getDefaultValue() { return defaultValue; }
    public String getDescription() { return description; }

    public Object convertValue(String v) {
        return null;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", type);
        json.put("name", name);
        json.put("group", group);
        json.put("subgroup", subgroup);
        json.put("fullname", fullname);
        json.put("description", description);
        json.put("default_value", toHumanReadable(defaultValue));
        return json;
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
    protected Object defaultValue;
    protected String description;
}
