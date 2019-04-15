package com.carolinarollergirls.scoreboard.core.impl;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Settings;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public class SettingsImpl extends ScoreBoardEventProviderImpl implements Settings {
    public SettingsImpl(ScoreBoard s) {
        super (s, null, "", ScoreBoard.Child.SETTINGS, Settings.class, Child.class);
    }

    @Override
    public void reset() { removeAll(Child.SETTING); }

    @Override
    public String get(String k) {
        synchronized(coreLock) {
            if (get(Child.SETTING, k) == null) { return null; }
            return get(Child.SETTING, k).getValue();
        }
    }
    @Override
    public void set(String k, String v) {
        synchronized (coreLock) {
            if (v == null) {
                remove(Child.SETTING, k);
            } else {
                add(Child.SETTING, new ValWithId(k, v));
            }
        }
    }
}
