package com.carolinarollergirls.scoreboard.defaults;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.LinkedHashSet;
import java.util.Set;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;

public abstract class SimpleScoreBoardEventProvider implements ScoreBoardEventProvider,ScoreBoardListener
{
	public abstract String getProviderName();
	public abstract Class<? extends ScoreBoardEventProvider> getProviderClass();
	public abstract String getProviderId();

	public void scoreBoardChange(ScoreBoardEvent event) {
		// Synchronously send events to listeners.
		synchronized(scoreBoardEventListeners) {
			for (ScoreBoardListener l : scoreBoardEventListeners) {
				l.scoreBoardChange(event);
			}
		}
	}

	protected void requestBatchStart() {
		scoreBoardChange(new ScoreBoardEvent(this, ScoreBoardEvent.BATCH_START, Boolean.TRUE, Boolean.TRUE));
	}

	protected void requestBatchEnd() {
		scoreBoardChange(new ScoreBoardEvent(this, ScoreBoardEvent.BATCH_END, Boolean.TRUE, Boolean.TRUE));
	}

	public void addScoreBoardListener(ScoreBoardListener listener) {
		synchronized(scoreBoardEventListeners) {
			scoreBoardEventListeners.add(listener);
		}
	}
	public void removeScoreBoardListener(ScoreBoardListener listener) {
		synchronized(scoreBoardEventListeners) {
			scoreBoardEventListeners.remove(listener);
		}
	}

	private Set<ScoreBoardListener> scoreBoardEventListeners = new LinkedHashSet<ScoreBoardListener>();
}
