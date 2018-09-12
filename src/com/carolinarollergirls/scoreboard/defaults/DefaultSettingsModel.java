package com.carolinarollergirls.scoreboard.defaults;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.carolinarollergirls.scoreboard.Ruleset;
import com.carolinarollergirls.scoreboard.Settings;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.model.ScoreBoardModel;
import com.carolinarollergirls.scoreboard.model.SettingsModel;

public class DefaultSettingsModel extends SimpleScoreBoardEventProvider implements SettingsModel, Ruleset.RulesetReceiver {
	public DefaultSettingsModel(ScoreBoardModel s) {
		sbm = s;
		parent = s;
		reset();
	}

	public void applyRule(String rule, Object value) {
		synchronized (settingsLock) {
			if (ruleMapping.containsKey(rule)) {
				for (String map : ruleMapping.get(rule))
					set(map, String.valueOf(value));
			} else
				set(rule, String.valueOf(value));
		}
	}

	public String getProviderName() { return "Settings"; }
	public Class<Settings> getProviderClass() { return Settings.class; }
	public String getProviderId() { return ""; }

	public ScoreBoardEventProvider getParent() { return parent; }

	public void reset() {
		synchronized (settingsLock) {
			List<String> keys = new ArrayList<String>(settings.keySet());
			for (String k : keys) {
				set(k, null);
			}
		}
		sbm._getRuleset().apply(true, this);
	}

	public void addRuleMapping(String rule, String[] mapTo) {
		synchronized (settingsLock) {
			List<String> l = ruleMapping.get(rule);
			if (l == null) {
				l = new ArrayList<String>();
				ruleMapping.put(rule, l);
			}
			for (String map : mapTo)
				l.add(map);
		}
	}

	public Map<String, String> getAll() {
		synchronized (settingsLock) {
			return Collections.unmodifiableMap(new Hashtable<String, String>(settings));
		}
	}
	public String get(String k) {
		synchronized (settingsLock) {
			return settings.get(k);
		}
	}
	public boolean getBoolean(String k) {
		return Boolean.parseBoolean(get(k));
	}
	public long getLong(String k) {
		return Long.parseLong(get(k));
	}
	public void set(String k, String v) {
		synchronized (settingsLock) {
			String last = settings.get(k);
			if (v == null || v.equals(""))
				v = "";
			settings.put(k, v);
			scoreBoardChange(new ScoreBoardEvent(this, k, v, last));
		}
	}
	public void set(Map<String, String> s) {
		synchronized (settingsLock) {
			// Remove settings not in the new set
			for (String k : settings.keySet())
				if (!s.containsKey(k))
					set(k, null);

			// Set settings from new set
			for (String k : s.keySet())
				set(k, s.get(k));
		}
	}

	protected ScoreBoardModel sbm = null;
	protected Map<String, String> settings = new Hashtable<String, String>();
	protected Map<String, List<String>> ruleMapping = new Hashtable<String, List<String>>();
	protected Object settingsLock = new Object();
	protected ScoreBoardEventProvider parent = null;
}
