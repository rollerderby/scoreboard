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

public class TimeoutClockIncrementsTimeoutNumberPolicy extends AbstractClockRunningChangePolicy
{
  public TimeoutClockIncrementsTimeoutNumberPolicy() {
    super();

    addClock(Clock.ID_TIMEOUT);
  }

  public void reset() {
    super.reset();
    setDescription("This controls the Timeout clock number.  When the Timeout clock stops, the Timeout number is incremented..");
  }

  public void clockRunningChange(Clock clock, boolean running) {
    if (!running)
      scoreBoardModel.getClockModel(clock.getId()).changeNumber(1);
  }
}
