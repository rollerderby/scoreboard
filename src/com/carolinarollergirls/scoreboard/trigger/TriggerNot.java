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

public class TriggerNot extends Trigger {
	public TriggerNot(Trigger t, TriggerHandler th) {
		addFields(t.getFields());

		trigger = t;
		triggerHandler = th;
	}

	public Trigger getTrigger() { return trigger; }
	public TriggerHandler getTriggerHandler() { return triggerHandler; }

	public Boolean checkTrigger(TriggerManager.State state) {
		Boolean match = !trigger.checkTrigger(state);

		if (match && triggerHandler != null)
			triggerHandler.Trigger(this, state);
		return match;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("Not(");
		sb.append(trigger.toString());
		sb.append(')');

		return sb.toString();
	}

	private final Trigger trigger;
	private final TriggerHandler triggerHandler;
}
