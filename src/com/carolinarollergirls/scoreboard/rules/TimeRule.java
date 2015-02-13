package com.carolinarollergirls.scoreboard.rules;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TimeRule extends Rule {
	public TimeRule(String group, String subgroup, String name, String description, String defaultValue) {
		super(group, subgroup, name, description, null);
		this.defaultValue = convertValue(defaultValue);
	}

	public Object convertValue(String v) {
		Matcher m = p.matcher(v);
		if (m.matches()) {
			MatchResult mr = m.toMatchResult();
			long min = Long.parseLong(mr.group(1));
			long sec = Long.parseLong(mr.group(2));

			sec = sec + (min * 60);
			return new Long(sec * 1000);
		}

		try {
			return new Long(Long.parseLong(v));
		} catch (Exception e) {
			return null;
		}
	}

	public String toHumanReadable(Object v) {
		if (v == null)
			return "";

		Object v2 = convertValue(v.toString());
		if (v2 != null) {
			Long dv = (Long)v2 / 1000;
			return String.format("%d:%02d", dv / 60, dv % 60);
		}
		return v.toString();
	}

	private Pattern p = Pattern.compile("(\\d+):(\\d+)");
}
