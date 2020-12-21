package com.carolinarollergirls.scoreboard.core.interfaces;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface CurrentClock extends ScoreBoardEventProvider {
    public void load(Clock c);

    Value<Clock> CLOCK = new Value<>(Clock.class, "Clock", null);
}
