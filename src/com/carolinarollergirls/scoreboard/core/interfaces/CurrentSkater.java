package com.carolinarollergirls.scoreboard.core.interfaces;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.NumberedChild;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface CurrentSkater extends ScoreBoardEventProvider {
    public int compareTo(CurrentSkater other);

    public String getNumber();

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<Skater> SKATER = new Value<>(Skater.class, "Skater", null, props);

    public static final NumberedChild<CurrentPenalty> PENALTY =
        new NumberedChild<>(CurrentPenalty.class, "Penalty", props);
}
