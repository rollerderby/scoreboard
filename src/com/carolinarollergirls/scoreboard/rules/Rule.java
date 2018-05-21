package com.carolinarollergirls.scoreboard.rules;

import org.json.JSONException;
import org.json.JSONObject;

public class Rule {
	public Rule(boolean onResetOnly, String type, String group, String subgroup, String name, String description, Object defaultValue) {
		this.onResetOnly = onResetOnly;
		this.type = type;
		this.group = group;
		this.subgroup = subgroup;
		this.name = name;
		this.defaultValue = defaultValue;
		this.description = description;

		StringBuilder sb = new StringBuilder();
		if (group != null) {
			sb.append(group);
			sb.append(".");
		}
		if (subgroup != null) {
			sb.append(subgroup);
			sb.append(".");
		}
		sb.append(name);
		fullname = sb.toString();
	}

	public boolean isResetOnly() { return onResetOnly; }
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
		json.put("default_value", toHumanReadable(defaultValue));
		return json;
	}

	public String toHumanReadable(Object v) {
		if (v == null)
			return "";
		return v.toString();
	}

	protected boolean onResetOnly;
	protected String type;
	protected String group;
	protected String subgroup;
	protected String name;
	protected String fullname;
	protected Object defaultValue;
	protected String description;
}
