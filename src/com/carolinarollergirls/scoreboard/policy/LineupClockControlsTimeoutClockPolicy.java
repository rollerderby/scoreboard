package com.carolinarollergirls.scoreboard.policy;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public class LineupClockControlsTimeoutClockPolicy extends AbstractClockTimeChangePolicy
{
  public LineupClockControlsTimeoutClockPolicy() {
    super();

    addParameterModel(new DefaultPolicyModel.DefaultParameterModel(this, TIMEOUT_TRIGGER_TIME, "Double", String.valueOf(DEFAULT_TIMEOUT_TRIGGER_TIME)));
    addParameterModel(new DefaultPolicyModel.DefaultParameterModel(this, PERIOD_ROLLBACK_TIME, "Double", String.valueOf(DEFAULT_PERIOD_ROLLBACK_TIME)));

    addClock(Clock.ID_LINEUP);
  }

  public void reset() {
    super.reset();
    setDescription("This starts a Timeout based on the Lineup clock.  When the Lineup clock reaches or exceeds the trigger value (by default 30 seconds), a Timeout is initiated.  If the period rollback value is not zero, the Period clock time will be rolled back by that value.  The period rollback time is intended to be used as a 'buffer' so the jam can start a couple seconds after the 30-second lineup to allow for slight differences between the scoreboard operator's lineup clock time and the NSO scorekeeper's lineup clock time, but for lineups that go 'too far' past the 30-second time a timeout will be called and the period clock 'rolled back' to exactly the time it was at when the lineup clock hit 30 seconds.  So for example, if the trigger time was 35 seconds, the rollback value should be 5 seconds.  By default, this policy is disabled.");

    setEnabled(false);
  }

  public void clockTimeChange(Clock clock, long time) {
    boolean trigger = false;
    long triggerTime = getLongTime(TIMEOUT_TRIGGER_TIME);
    if (clock.isCountDirectionDown())
      trigger = (time <= triggerTime);
    else
      trigger = (time >= triggerTime);
    if (trigger) {
      long rollbackTime = getLongTime(PERIOD_ROLLBACK_TIME);
      ClockModel pC = getScoreBoardModel().getClockModel(Clock.ID_PERIOD);
      if (!pC.isCountDirectionDown())
        rollbackTime *= -1;
      if (0 != rollbackTime)
        pC.changeTime(rollbackTime);
      getScoreBoardModel().timeout();
    }
  }

  protected long getLongTime(String param) {
    double d = Double.valueOf(getParameter(param).getValue()).doubleValue();
    return Double.valueOf(d * 1000).longValue();
  }

  public static final String TIMEOUT_TRIGGER_TIME = "Timeout trigger time";
  public static final String PERIOD_ROLLBACK_TIME = "Period rollback time";

  public static final Double DEFAULT_TIMEOUT_TRIGGER_TIME = new Double(30);
  public static final Double DEFAULT_PERIOD_ROLLBACK_TIME = new Double(0);
}
