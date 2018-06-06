package com.carolinarollergirls.scoreboard.rules;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class IntegerRule extends Rule {
	public IntegerRule(boolean onResetOnly, String group, String subgroup, String name, String description, int defaultValue) {
		super(onResetOnly, "Integer", group, subgroup, name, description, new Integer(defaultValue));
	}

	public Object convertValue(String v) {
		try {
			return new Integer(Integer.parseInt(v));
		} catch (Exception e) {
			return null;
		}
	}
}
