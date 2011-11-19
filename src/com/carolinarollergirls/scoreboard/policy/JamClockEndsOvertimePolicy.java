package com.carolinarollergirls.scoreboard.policy;

import java.util.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public class JamClockEndsOvertimePolicy extends AbstractClockRunningChangePolicy
{
  public JamClockEndsOvertimePolicy() {
    super();
    setDescription("This ends Overtime (if the bout is in Overtime).  When the Jam clock stops, Overtime is set to false.");

    addClock(Clock.ID_JAM);
  }

  protected void clockRunningChange(Clock clock, boolean running) {
    if (!running && getScoreBoardModel().getOvertime())
      getScoreBoardModel().setOvertime(false);
  }
}
