package com.carolinarollergirls.scoreboard;

public class SkaterNotFoundException extends RuntimeException
{
	public SkaterNotFoundException(String s) {
		super("Skater '"+s+"' not found");
		skater = s;
	}

	public String getSkater() { return skater; }

	protected String skater = "";
}
