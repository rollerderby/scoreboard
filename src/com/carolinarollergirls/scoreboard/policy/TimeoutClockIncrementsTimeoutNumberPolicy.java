package com.carolinarollergirls.scoreboard.policy;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public class TimeoutClockIncrementsTimeoutNumberPolicy extends AbstractClockRunningChangePolicy
{
  public TimeoutClockIncrementsTimeoutNumberPolicy() {
    super();
    setDescription("This controls the Timeout clock number.  When the Timeout clock stops, the Timeout number is incremented..");

    addClock(Clock.ID_TIMEOUT);
  }

  public void clockRunningChange(Clock clock, boolean running) {
    if (!running)
      scoreBoardModel.getClockModel(clock.getId()).changeNumber(1);
  }
}
