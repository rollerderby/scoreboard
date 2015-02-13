package com.carolinarollergirls.scoreboard.rules;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class StringRule extends Rule {
	public StringRule(String group, String subgroup, String name, String description, String defaultValue) {
		super(group, subgroup, name, description, defaultValue);
	}

	public Object convertValue(String v) {
		return v;
	}
}
