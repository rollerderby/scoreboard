package com.carolinarollergirls.scoreboard.core.impl;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Settings;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;

public class SettingsImpl extends DefaultScoreBoardEventProvider implements Settings {
    public SettingsImpl(ScoreBoard s) {
        sbm = s;
    }

    public String getProviderName() { return "Settings"; }
    public Class<Settings> getProviderClass() { return Settings.class; }
    public String getProviderId() { return ""; }
    public ScoreBoardEventProvider getParent() { return sbm; }
    public List<Class<? extends Property>> getProperties() { return properties; }

    public void reset() {
        synchronized (coreLock) {
            Map<String, String> old = new HashMap<String, String>(settings);
            settings.clear();
            for (String k : old.keySet()) {
                scoreBoardChange(new ScoreBoardEvent(this, Child.SETTING, new Setting(k, null), old.get(k)));
            }
        }
    }

    public Map<String, String> getAll() {
        synchronized (coreLock) {
            return Collections.unmodifiableMap(new HashMap<String, String>(settings));
        }
    }
    public String get(String k) {
        synchronized (coreLock) {
            return settings.get(k);
        }
    }
    public void set(String k, String v) {
        synchronized (coreLock) {
            String last = settings.get(k);
            if (v == null) {
                settings.remove(k);
            } else {
                settings.put(k, v);
            }
            scoreBoardChange(new ScoreBoardEvent(this, Child.SETTING, new Setting(k, v), last));
        }
    }

    protected ScoreBoard sbm = null;
    protected Map<String, String> settings = new HashMap<String, String>();

    protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	add(Child.class);
    }};

    protected static Object coreLock = ScoreBoardImpl.getCoreLock();
    
    public class Setting {

	public Setting(String k, String v) {
	    key = k;
	    value = v;
	}

	public String getId() { return key; }
	public String getValue() { return value; }
	
	private String key;
	private String value;
    }
}
