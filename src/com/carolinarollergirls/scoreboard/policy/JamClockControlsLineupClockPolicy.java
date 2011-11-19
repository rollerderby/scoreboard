package com.carolinarollergirls.scoreboard.policy;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public class JamClockControlsLineupClockPolicy extends AbstractClockRunningChangePolicy
{
  public JamClockControlsLineupClockPolicy() {
    super();

    addClock(Clock.ID_JAM);
  }

  public void reset() {
    super.reset();
    setDescription("This controls the Lineup clock based on the Jam clock.  When the Jam clock starts, the Lineup clock is stopped.  When the Jam clock stops, and the Period clock is running, and the Lineup clock is not running, the Lineup clock is reset then started.");
  }

  public void clockRunningChange(Clock clock, boolean running) {
    ClockModel pc = getScoreBoardModel().getClockModel(Clock.ID_PERIOD);
    ClockModel lc = getScoreBoardModel().getClockModel(Clock.ID_LINEUP);
    if (running) {
      lc.stop();
    } else if (pc.isRunning() && !lc.isRunning()) {
      lc.resetTime();
      lc.start();
    }
  }
}
