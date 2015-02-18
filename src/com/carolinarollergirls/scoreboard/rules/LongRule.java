package com.carolinarollergirls.scoreboard.rules;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LongRule extends Rule {
	public LongRule(String group, String subgroup, String name, String description, int defaultValue) {
		super("Long", group, subgroup, name, description, new Long(defaultValue));
	}

	public Object convertValue(String v) {
		try {
			return new Long(v);
		} catch (Exception e) {
			return null;
		}
	}
}
