package com.carolinarollergirls.scoreboard.rules;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BooleanRule extends Rule {
	public BooleanRule(String group, String subgroup, String name, String description, boolean defaultValue, String trueValue, String falseValue) {
		super(group, subgroup, name, description, new Boolean(defaultValue));

		this.trueValue = trueValue;
		this.falseValue = falseValue;
	}

	public Object convertValue(String v) {
		if (trueValue.equalsIgnoreCase(v))
			return new Boolean(true);
		if (falseValue.equalsIgnoreCase(v))
			return new Boolean(false);

		try {
			return new Boolean(v);
		} catch (Exception e) {
			return null;
		}
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject json = super.toJSON();
		json.put("trueValue", trueValue);
		json.put("falseValue", falseValue);
		return json;
	}

	public String toHumanReadable(Object v) {
		if (v == null)
			return "";

		Object v2 = convertValue(v.toString());
		if (v2 != null) {
			Boolean b = (Boolean)v2;
			return b ? trueValue : falseValue;
		}
		return v.toString();
	}

	private String trueValue;
	private String falseValue;
}
