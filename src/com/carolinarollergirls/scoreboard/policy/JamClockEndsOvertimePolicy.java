package com.carolinarollergirls.scoreboard.policy;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public class JamClockEndsOvertimePolicy extends AbstractClockRunningChangePolicy
{
  public JamClockEndsOvertimePolicy() {
    super();

    addClock(Clock.ID_JAM);
  }

  public void reset() {
    super.reset();
    setDescription("This ends Overtime (if the bout is in Overtime).  When the Jam clock stops, Overtime is set to false.");
  }

  protected void clockRunningChange(Clock clock, boolean running) {
    if (!running && getScoreBoardModel().getOvertime())
      getScoreBoardModel().setOvertime(false);
  }
}
