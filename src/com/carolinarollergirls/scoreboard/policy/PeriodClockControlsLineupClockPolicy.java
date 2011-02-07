package com.carolinarollergirls.scoreboard.policy;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public class PeriodClockControlsLineupClockPolicy extends AbstractClockRunningChangePolicy
{
	public PeriodClockControlsLineupClockPolicy() {
		super();
		setDescription("This controls the Lineup clock based on the Period clock.  When the Period clock stops and its time is equal to its minimum (if counting down, or its maximum if counting up), and the Jam clock is also stopped, the Lineup clock is stopped and reset.");

		addClock(Clock.ID_PERIOD);
	}

	public void clockRunningChange(Clock clock, boolean running) {
		ClockModel lc = getScoreBoardModel().getClockModel(Clock.ID_LINEUP);
		boolean jcRunning = getScoreBoardModel().getClockModel(Clock.ID_JAM).isRunning();
		boolean atEnd = (clock.getTime() == (clock.isCountDirectionDown() ? clock.getMinimumTime() : clock.getMaximumTime()));
		if (!running && !jcRunning && atEnd) {
			lc.stop();
			lc.resetTime();
		}
	}
}
