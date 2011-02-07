package com.carolinarollergirls.scoreboard.policy;

import java.util.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public abstract class AbstractSkaterPenaltyBoxChangePolicy extends AbstractSkaterChangePolicy
{
	public AbstractSkaterPenaltyBoxChangePolicy() {
		super();
		addSkaterProperty("PenaltyBox");
	}
	public AbstractSkaterPenaltyBoxChangePolicy(String id) {
		super(id);
		addSkaterProperty("PenaltyBox");
	}

	protected void skaterChange(Skater s, Object v) {
		skaterPenaltyBoxChange(s, ((Boolean)v).booleanValue());
	}

	protected abstract void skaterPenaltyBoxChange(Skater skater, boolean penaltyBox);
}
