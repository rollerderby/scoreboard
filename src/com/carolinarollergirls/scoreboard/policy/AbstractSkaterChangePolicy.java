package com.carolinarollergirls.scoreboard.policy;

import java.util.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public abstract class AbstractSkaterChangePolicy extends DefaultPolicyModel
{
	public AbstractSkaterChangePolicy() {
		super();
	}
	public AbstractSkaterChangePolicy(String id) {
		super(id);
	}

	protected void addSkaterProperty(String p) {
		filterScoreBoardListener.addProperty(Skater.class, FilterScoreBoardListener.ANY_ID, p);
	}

	public void setScoreBoardModel(ScoreBoardModel sbM) {
		super.setScoreBoardModel(sbM);
		sbM.addScoreBoardListener(filterScoreBoardListener);
	}

	protected abstract void skaterChange(Skater skater, Object value);

	protected FilterScoreBoardListener filterScoreBoardListener = new FilterScoreBoardListener() {
			public void filteredScoreBoardChange(ScoreBoardEvent event) {
				if (isEnabled()) {
					synchronized (changeLock) {
						skaterChange((Skater)event.getProvider(), event.getValue());
					}
				}
			}
		};

	protected Object changeLock = new Object();
}
