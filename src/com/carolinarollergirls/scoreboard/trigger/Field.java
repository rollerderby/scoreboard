package com.carolinarollergirls.scoreboard.trigger;
/**
  * Copyright (C) 2014 Michael Mitton <mmitton@gmail.com>
  *
  * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
  * The CRG ScoreBoard is licensed under either the GNU General Public
  * License version 3 (or later), or the Apache License 2.0, at your option.
  * See the file COPYING for details.
  */

import java.util.*;

public class Field {
	protected Field(String n, String v, String pv, Action a) {
		name = n;
		value = v;
		previousValue = pv;
		action = a;
	}

	public Action getAction() { return action; }
	public String getName() { return name; }
	public String getValue() { return value; }
	public String getPreviousValue() { return previousValue; }

	private final String name;
	private final String value;
	private final String previousValue;
	private final Action action;

	public enum Action {
		Add,
		Change,
		Remove,
		Unchanged
	}
}
