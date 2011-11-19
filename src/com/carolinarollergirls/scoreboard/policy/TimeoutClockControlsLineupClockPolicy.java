package com.carolinarollergirls.scoreboard.policy;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public class TimeoutClockControlsLineupClockPolicy extends AbstractClockRunningChangePolicy
{
  public TimeoutClockControlsLineupClockPolicy() {
    super();
    setDescription("This controls the Lineup clock based on the Timeout clock.  When the Timeout clock starts, the Lineup clock is stopped then reset.");

    addClock(Clock.ID_TIMEOUT);
  }

  public void clockRunningChange(Clock clock, boolean running) {
    ClockModel lc = getScoreBoardModel().getClockModel(Clock.ID_LINEUP);
    if (running) {
      lc.stop();
      lc.resetTime();
    }
  }
}
