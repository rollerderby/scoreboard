package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.event.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public interface Settings extends ScoreBoardEventProvider {
    public void reset();

    public String get(String k);
    // Setting to null deletes a setting.
    public void set(String k, String v);

    AddRemoveProperty<ValWithId> SETTING = new AddRemoveProperty<>(ValWithId.class, "Setting");
}
