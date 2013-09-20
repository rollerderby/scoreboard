package com.carolinarollergirls.scoreboard.policy;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public class LineupClockResetsJamTimePolicy extends AbstractClockRunningChangePolicy
{
	public LineupClockResetsJamTimePolicy() {
		super(ID, DESCRIPTION);
	}

	public void setScoreBoardModel(ScoreBoardModel sbm) {
		super.setScoreBoardModel(sbm);
		addClock(Clock.ID_LINEUP);
	}

	public void reset() {
		super.reset();

		setEnabled(false);
	}

	public void clockRunningChange(Clock clock, boolean running) {
		boolean atEnd = clock.getTime() == (clock.isCountDirectionDown() ? clock.getMinimumTime() : clock.getMaximumTime());
		if (!running && atEnd) {
			ClockModel jC = getScoreBoardModel().getClockModel(Clock.ID_JAM);
			if (!jC.isRunning())
				jC.reset();
		}
	}

	public static final String ID = "Lineup Clock Resets Jam Time";
	public static final String DESCRIPTION = "This resets the Jam clock based on the Lineup clock.	When the Lineup clock ends (i.e. it stops and its time is at minimum or maximum depending on count direction), the Jam clock time is reset.	 Note that this will not reset the Jam clock time if the Jam clock is running.";
}
