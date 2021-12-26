package com.carolinarollergirls.scoreboard.core.admin;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.Settings;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public class SettingsImpl extends ScoreBoardEventProviderImpl<Settings> implements Settings {
    public SettingsImpl(ScoreBoard s) {
        super(s, "", ScoreBoard.SETTINGS);
        addProperties(SETTING);
    }
    public SettingsImpl(SettingsImpl cloned, ScoreBoardEventProvider root) { super(cloned, root); }

    @Override
    public ScoreBoardEventProvider clone(ScoreBoardEventProvider root) { return new SettingsImpl(this, root); }

    @Override
    public void reset() { removeAll(SETTING); }

    @Override
    public String get(String k) {
        synchronized (coreLock) {
            if (get(SETTING, k) == null) { return null; }
            return get(SETTING, k).getValue();
        }
    }
    @Override
    public void set(String k, String v) {
        synchronized (coreLock) {
            if (v == null) {
                remove(SETTING, k);
            } else {
                add(SETTING, new ValWithId(k, v));
            }
        }
    }
}
