package com.carolinarollergirls.scoreboard.defaults;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.carolinarollergirls.scoreboard.CustomSettings;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.model.CustomSettingsModel;
import com.carolinarollergirls.scoreboard.model.ScoreBoardModel;

public class DefaultCustomSettingsModel extends DefaultScoreBoardEventProvider implements CustomSettingsModel {
	public DefaultCustomSettingsModel(ScoreBoardModel s) {
		sbm = s;
	}

	public String getProviderName() { return "CustomSettings"; }
	public Class<CustomSettings> getProviderClass() { return CustomSettings.class; }
	public String getProviderId() { return ""; }

	public void reset() {
		synchronized (settingsLock) {
      Map<String, String> old = new HashMap<String, String>(settings);
      settings.clear();
      for (String k : old.keySet()) {
        scoreBoardChange(new ScoreBoardEvent(this, k, null, old.get(k)));
      }
		}
	}

	public Map<String, String> getAll() {
		synchronized (settingsLock) {
			return Collections.unmodifiableMap(new HashMap<String, String>(settings));
		}
	}
	public String get(String k) {
		synchronized (settingsLock) {
			return settings.get(k);
		}
	}
	public void set(String k, String v) {
		synchronized (settingsLock) {
			String last = settings.get(k);
			if (v == null) {
        settings.remove(k);
      } else {
        settings.put(k, v);
      }
			scoreBoardChange(new ScoreBoardEvent(this, k, v, last));
		}
	}

	protected ScoreBoardModel sbm = null;
	protected Map<String, String> settings = new HashMap<String, String>();
	protected Object settingsLock = new Object();
}
