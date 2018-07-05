package com.carolinarollergirls.scoreboard.defaults;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;

public abstract class DefaultScoreBoardEventProvider implements ScoreBoardEventProvider,ScoreBoardListener
{
	public abstract String getProviderName();
	public abstract Class<? extends ScoreBoardEventProvider> getProviderClass();
	public abstract String getProviderId();

	public void scoreBoardChange(ScoreBoardEvent event) {
		manager.addScoreBoardEvent(this, event);
	}

	protected void requestBatchStart() {
		scoreBoardChange(new ScoreBoardEvent(this, ScoreBoardEvent.BATCH_START, Boolean.TRUE, Boolean.TRUE));
	}

	protected void requestBatchEnd() {
		scoreBoardChange(new ScoreBoardEvent(this, ScoreBoardEvent.BATCH_END, Boolean.TRUE, Boolean.TRUE));
	}

	public void addScoreBoardListener(ScoreBoardListener listener) {
		manager.addProviderListener(this, listener);
	}
	public void removeScoreBoardListener(ScoreBoardListener listener) {
		manager.removeProviderListener(this, listener);
	}

	private ScoreBoardEventProviderManager manager = ScoreBoardEventProviderManager.getSingleton();
}
