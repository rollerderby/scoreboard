package com.carolinarollergirls.scoreboard.rules;


public class StringRule extends Rule {
	public StringRule(boolean onResetOnly, String fullname, String description, String defaultValue) {
		super(onResetOnly, "String", fullname, description, defaultValue);
	}

	public Object convertValue(String v) {
		return v;
	}
}
