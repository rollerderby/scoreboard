package com.carolinarollergirls.scoreboard.rules;


public class LongRule extends Rule {
	public LongRule(boolean onResetOnly, String group, String subgroup, String name, String description, int defaultValue) {
		super(onResetOnly, "Long", group, subgroup, name, description, new Long(defaultValue));
	}

	public Object convertValue(String v) {
		try {
			return new Long(v);
		} catch (Exception e) {
			return null;
		}
	}
}
