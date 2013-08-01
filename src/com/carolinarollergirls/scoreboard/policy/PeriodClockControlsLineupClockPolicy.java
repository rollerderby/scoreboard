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

public class PeriodClockControlsLineupClockPolicy extends AbstractClockRunningChangePolicy
{
  public PeriodClockControlsLineupClockPolicy() { super(ID, DESCRIPTION); }

  public void setScoreBoardModel(ScoreBoardModel sbm) {
    super.setScoreBoardModel(sbm);
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

  public static final String ID = "Period Clock Controls Lineup Clock";
  public static final String DESCRIPTION = "This controls the Lineup clock based on the Period clock.  When the Period clock stops and its time is equal to its minimum (if counting down, or its maximum if counting up), and the Jam clock is also stopped, the Lineup clock is stopped and reset.";
}
