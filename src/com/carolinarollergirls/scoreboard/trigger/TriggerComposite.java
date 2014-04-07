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

public class TriggerComposite extends Trigger {
	public TriggerComposite(Trigger[] t, Operator o, TriggerHandler th) {
		for (Trigger _t : t) {
			addFields(_t.getFields());
		}

		triggers = new ArrayList<Trigger>(Arrays.asList(t));
		op = o;
		triggerHandler = th;
	}
	public TriggerComposite(Collection<Trigger> t, Operator o, TriggerHandler th) {
		for (Trigger _t : t) {
			addFields(_t.getFields());
		}

		triggers = new ArrayList<Trigger>(t);
		op = o;
		triggerHandler = th;
	}

	public List<Trigger> getTriggers() { return triggers; }
	public Operator getOperator() { return op; }
	public TriggerHandler getTriggerHandler() { return triggerHandler; }

	public Boolean checkTrigger(TriggerManager.State state) {
		Boolean match = op == Operator.And;

		for (Trigger t : triggers) {
			Boolean tm = t.checkTrigger(state);

			if (op == Operator.And && tm == false) {
				match = false;
				break;
			} else if (op == Operator.Or && tm == true) {
				match = true;
				break;
			}
		}
		if (match && triggerHandler != null)
			triggerHandler.Trigger(this, state);
		return match;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		String op = "";
		switch (this.op) {
			case And: op = " AND "; break;
			case Or: op = " OR "; break;
		}

		sb.append('(');
		Iterator<Trigger> iter = triggers.iterator();
		if (iter.hasNext())
			sb.append(iter.next().toString());
		while (iter.hasNext()) {
			sb.append(op);
			sb.append(iter.next().toString());
		}
		sb.append(')');

		return sb.toString();
	}

	private final List<Trigger> triggers;
	private final Operator op;
	private final TriggerHandler triggerHandler;

	public enum Operator {
		And,
		Or
	}
}
