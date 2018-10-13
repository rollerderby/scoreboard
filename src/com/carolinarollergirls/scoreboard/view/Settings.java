package com.carolinarollergirls.scoreboard.view;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Map;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public interface Settings extends ScoreBoardEventProvider {
    public Map<String, String> getAll();
    public String get(String k);
    public boolean getBoolean(String k);
    public int getInt(String k);
    public long getLong(String k);
    public ScoreBoardEventProvider getParent();

    public static final String EVENT_SETTING = "Setting";
}
