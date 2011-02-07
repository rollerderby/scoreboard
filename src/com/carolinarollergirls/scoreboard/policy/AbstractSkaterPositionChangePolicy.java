package com.carolinarollergirls.scoreboard.policy;

import java.util.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public abstract class AbstractSkaterPositionChangePolicy extends AbstractSkaterChangePolicy
{
	public AbstractSkaterPositionChangePolicy() {
		super();
		addSkaterProperty("Position");
	}
	public AbstractSkaterPositionChangePolicy(String id) {
		super(id);
		addSkaterProperty("Position");
	}

	protected void skaterChange(Skater s, Object v) {
		skaterPositionChange(s, v.toString());
	}

	protected abstract void skaterPositionChange(Skater skater, String position);
}
