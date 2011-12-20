package com.carolinarollergirls.scoreboard.policy;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public class LineupClockStartsJamPolicy extends AbstractClockTimeChangePolicy
{
  public LineupClockStartsJamPolicy() {
    super();

    addParameterModel(new DefaultPolicyModel.DefaultParameterModel(this, JAM_TRIGGER_TIME, "Double", String.valueOf(DEFAULT_JAM_TRIGGER_TIME)));

    addClock(Clock.ID_LINEUP);
  }

  public void reset() {
    super.reset();
    setDescription("This starts the jam based on the Lineup clock.  When the Lineup clock reaches or exceeds the trigger value (by default 30 seconds), the jam is started (via StartJam).  By default, this policy is disabled.");

    setEnabled(false);
  }

  public void clockTimeChange(Clock clock, long time) {
    boolean trigger = false;
    long triggerTime = getLongTime(JAM_TRIGGER_TIME);
    if (clock.isCountDirectionDown())
      trigger = (time <= triggerTime);
    else
      trigger = (time >= triggerTime);
    if (trigger) {
      getScoreBoardModel().startJam();
    }
  }

  protected long getLongTime(String param) {
    double d = Double.valueOf(getParameter(param).getValue()).doubleValue();
    return Double.valueOf(d * 1000).longValue();
  }

  public static final String JAM_TRIGGER_TIME = "Jam trigger time";

  public static final Double DEFAULT_JAM_TRIGGER_TIME = new Double(30);
}
