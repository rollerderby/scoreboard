package com.carolinarollergirls.scoreboard.policy;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public class CountdownClockControlsStartJamPolicy extends AbstractClockRunningChangePolicy
{
	public CountdownClockControlsStartJamPolicy() {
		super();
		setDescription("This starts the Jam by calling the ScoreBoardModel.startJam() method when the Countdown clock reaches its minimum value (by default 0).");

		addClock(Clock.ID_COUNTDOWN);
	}

	public void clockRunningChange(Clock clock, boolean running) {
		long endTime = clock.isCountDirectionDown() ? clock.getMinimumTime() : clock.getMaximumTime();
		if (!running && clock.getTime() == endTime) {
			getScoreBoardModel().startJam();
		}
	}
}
