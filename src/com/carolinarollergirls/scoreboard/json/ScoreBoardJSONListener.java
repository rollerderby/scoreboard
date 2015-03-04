package com.carolinarollergirls.scoreboard.json;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Hashtable;
import java.util.Map;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.SettingsModel;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.jetty.WS;

/**
 * Converts a ScoreBoardEvent into a representative JSON Update
 */
public class ScoreBoardJSONListener implements ScoreBoardListener
{
	public ScoreBoardJSONListener(ScoreBoard sb) {
		sb.addScoreBoardListener(this);
	}

	public void scoreBoardChange(ScoreBoardEvent event) {
		synchronized (this) {
			ScoreBoardEventProvider p = event.getProvider();
			String provider = p.getProviderName();
			String prop = event.getProperty();
			if (prop.equals(ScoreBoardEvent.BATCH_START)) {
				batch++;
				return;
			}
			if (prop.equals(ScoreBoardEvent.BATCH_END)) {
				if (batch == 0)
					return;
				if (--batch == 0)
					sendUpdates();
				return;
			}

			Object v = event.getValue();
			if (p instanceof ScoreBoard)
				update("ScoreBoard", prop, v);
			else if (p instanceof Team)
				update("ScoreBoard.Team(" + ((Team)p).getId() + ")", prop, v);
			else if (p instanceof Clock)
				update("ScoreBoard.Clock(" + ((Clock)p).getId() + ")", prop, v);
			else if (p instanceof Policy)
				update("ScoreBoard.Policy(" + ((Policy)p).getId() + ")", prop, v);
			else if (p instanceof Policy.Parameter) {
				Policy.Parameter param = (Policy.Parameter)p;
				Policy pol = param.getPolicy();
				update("ScoreBoard.Policy(" + pol.getId() + ")", param.getName(), v);
			} else if (p instanceof Settings) {
				Settings s = (Settings)p;
				String prefix = null;
				if (s.getParent() instanceof ScoreBoard)
					prefix = "ScoreBoard";
				if (prefix == null)
					ScoreBoardManager.printMessage(provider + " update of unknown kind.  prop: " + prop + ", v: " + v);
				else
					update(prefix, "Setting(" + prop + ")", v);
			} else if (p instanceof Team.AlternateName) {
				Team.AlternateName an = (Team.AlternateName)p;
				update("ScoreBoard.Team(" + an.getTeam().getId() + ")", "AlternateName(" + an.getId() + ")", v);
			} else if (p instanceof Team.Color) {
				Team.Color c = (Team.Color)p;
				update("ScoreBoard.Team(" + c.getTeam().getId() + ")", "Color(" + c.getId() + ")", v);
			} else
				ScoreBoardManager.printMessage(provider + " update of unknown kind.  prop: " + prop + ", v: " + v);

			if (batch == 0)
				sendUpdates();
		}
	}

	private void sendUpdates() {
		synchronized (this) {
			if (updateMap.size() == 0)
				return;
			WS.update(updateMap);
			updateMap.clear();
		}
	}

	private void update(String prefix, String prop, Object v) {
		if (v instanceof String)
			updateMap.put(prefix + "." + prop, v);
		else if (v instanceof Integer)
			updateMap.put(prefix + "." + prop, v);
		else if (v instanceof Long)
			updateMap.put(prefix + "." + prop, v);
		else if (v instanceof Boolean)
			updateMap.put(prefix + "." + prop, v);
		else {
			ScoreBoardManager.printMessage(prefix + " update of unknown type.  prop: " + prop + ", v: " + v + " v.getClass(): " + v.getClass());
		}
	}

	private Map<String, Object> updateMap = new Hashtable<String, Object>();
	private long batch = 0;
}
