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
import java.util.HashMap;
import java.util.List;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Settings;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;
import com.carolinarollergirls.scoreboard.utils.ValWithId;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;

public class SettingsImpl extends ScoreBoardEventProviderImpl implements Settings {
    public SettingsImpl(ScoreBoard s) {
        sbm = s;
        children.put(Child.SETTING, new HashMap<String, ValueWithId>());
    }

    public String getProviderName() { return PropertyConversion.toFrontend(ScoreBoard.Child.SETTINGS); }
    public Class<Settings> getProviderClass() { return Settings.class; }
    public String getId() { return ""; }
    public ScoreBoardEventProvider getParent() { return sbm; }
    public List<Class<? extends Property>> getProperties() { return properties; }

    public void reset() { removeAll(Child.SETTING); }

    public String get(String k) {
	synchronized(coreLock) {
	    if (get(Child.SETTING, k) == null) { return null; }
	    return get(Child.SETTING, k).getValue();
	}
    }
    public void set(String k, String v) {
        synchronized (coreLock) {
            if (v == null) {
        	remove(Child.SETTING, k);
            } else {
        	add(Child.SETTING, new ValWithId(k, v));
            }
        }
    }

    protected ScoreBoard sbm = null;

    protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	add(Child.class);
    }};

    protected static Object coreLock = ScoreBoardImpl.getCoreLock();
}
