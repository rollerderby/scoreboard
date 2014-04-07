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

public class TriggerString extends Trigger {
	public TriggerString(String n, Operator o, String v, TriggerHandler th) {
		addField(n);
		name = n;
		op = o;
		value = v;
		triggerHandler = th;
	}

	public String getName() { return name; }
	public Operator getOperator() { return op; }
	public String getValue() { return value; }
	public TriggerHandler getTriggerHandler() { return triggerHandler; }

	public Boolean checkTrigger(TriggerManager.State state) {
		Boolean match = false;
		Field field = state.getField(name);
		if (field == null)
			return false;

		String v = field.getValue();
		if (v == null)
			match = op == Operator.IsNull;
		else {
			int c = v.compareTo(value);
			switch (op) {
				case Equals: match = c == 0; break;
				case LessThan: match = c > 0; break;
				case LessThanEqualTo: match = c >= 0; break;
				case GreaterThan: match = c > 0; break;
				case GreaterThanEqualTo: match = c >= 0; break;
				case NotEquals: match = !v.equals(value); break;
				case IsNotNull: match = true;
			}
		}
		if (match && triggerHandler != null)
			triggerHandler.Trigger(this, state);
		return match;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		switch (op) {
			case Equals: sb.append(" = "); break;
			case LessThan: sb.append(" < "); break;
			case LessThanEqualTo: sb.append(" <= "); break;
			case GreaterThan: sb.append(" > "); break;
			case GreaterThanEqualTo: sb.append(" >= "); break;
			case NotEquals: sb.append(" != "); break;
			case IsNull: sb.append(" IsNull"); break;
			case IsNotNull: sb.append(" IsNotNull"); break;
		}
		if (op != Operator.IsNull && op != Operator.IsNotNull) {
			sb.append('"');
			sb.append(value);
			sb.append('"');
		}
		return sb.toString();
	}

	private final String name;
	private final Operator op;
	private final String value;
	private final TriggerHandler triggerHandler;

	public enum Operator {
		Equals,
		LessThan,
		LessThanEqualTo,
		GreaterThan,
		GreaterThanEqualTo,
		NotEquals,
		IsNull,
		IsNotNull
	}
}
