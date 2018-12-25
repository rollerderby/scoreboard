package com.carolinarollergirls.scoreboard.event;

import java.util.List;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;

/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

public interface ScoreBoardEventProvider extends ValueWithId{
    public String getProviderName();
    public Class<? extends ScoreBoardEventProvider> getProviderClass();
    public String getProviderId();
    public ScoreBoardEventProvider getParent();
    public List<Class<? extends Property>> getProperties();

    public void addScoreBoardListener(ScoreBoardListener listener);
    public void removeScoreBoardListener(ScoreBoardListener listener);
}
