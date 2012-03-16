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
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public class TimeoutClockControlsLineupClockPolicy extends AbstractClockRunningChangePolicy
{
  public TimeoutClockControlsLineupClockPolicy() {
    super();
  }

  public void setScoreBoardModel(ScoreBoardModel sbm) {
    super.setScoreBoardModel(sbm);
    addClock(Clock.ID_TIMEOUT);
  }

  public void reset() {
    super.reset();
    setDescription("This controls the Lineup clock based on the Timeout clock.  When the Timeout clock starts, the Lineup clock is stopped then reset.");
  }

  public void clockRunningChange(Clock clock, boolean running) {
    ClockModel lc = getScoreBoardModel().getClockModel(Clock.ID_LINEUP);
    if (running) {
      lc.stop();
      lc.resetTime();
    }
  }
}
