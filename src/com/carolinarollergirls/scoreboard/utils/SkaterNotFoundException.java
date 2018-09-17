package com.carolinarollergirls.scoreboard.utils;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

public class SkaterNotFoundException extends RuntimeException
{
	public SkaterNotFoundException(String s) {
		super("Skater '"+s+"' not found");
		skater = s;
	}

	public String getSkater() { return skater; }

	protected String skater = "";
}
