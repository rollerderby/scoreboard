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

public abstract class Trigger {
	public List<String> getFields() {
		return fields;
	}
	public abstract Boolean checkTrigger(TriggerManager.State state);


	private List<String> fields = new ArrayList<String>();
	protected void addField(String field) {
		if (!fields.contains(field))
			fields.add(field);
	}
	protected void addFields(List<String> fields) {
		for (String field : fields) 
			addField(field);
	}
}
