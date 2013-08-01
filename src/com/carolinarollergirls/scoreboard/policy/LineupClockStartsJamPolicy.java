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
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public class LineupClockStartsJamPolicy extends AbstractClockTimeChangePolicy
{
  public LineupClockStartsJamPolicy() {
    super(ID, DESCRIPTION);

    addParameterModel(new DefaultPolicyModel.DefaultParameterModel(this, JAM_TRIGGER_TIME, "Double", String.valueOf(DEFAULT_JAM_TRIGGER_TIME)));
    addParameterModel(new DefaultPolicyModel.DefaultParameterModel(this, OVERTIME_JAM_TRIGGER_TIME, "Double", String.valueOf(DEFAULT_OVERTIME_JAM_TRIGGER_TIME)));
  }

  public void setScoreBoardModel(ScoreBoardModel sbm) {
    super.setScoreBoardModel(sbm);
    addClock(Clock.ID_LINEUP);
  }

  public void reset() {
    super.reset();

    setEnabled(false);
  }

  public void clockTimeChange(Clock clock, long time) {
    boolean trigger = false, avoidInfiniteLoop = false;
    long triggerTime = getLongTime(JAM_TRIGGER_TIME);
    if (getScoreBoard().isInOvertime())
      triggerTime = getLongTime(OVERTIME_JAM_TRIGGER_TIME);
    if (clock.isCountDirectionDown()) {
      avoidInfiniteLoop = (clock.getMaximumTime() <= triggerTime);
      trigger = (time <= triggerTime);
    } else {
      avoidInfiniteLoop = (clock.getMinimumTime() >= triggerTime);
      trigger = (time >= triggerTime);
    }
    if (trigger && !avoidInfiniteLoop) {
      ClockModel lc = getScoreBoardModel().getClockModel(clock.getId());
      lc.stop();
      lc.reset();
      getScoreBoardModel().startJam();
    }
  }

  protected long getLongTime(String param) {
    double d = Double.valueOf(getParameter(param).getValue()).doubleValue();
    return Double.valueOf(d * 1000).longValue();
  }

  public static final String JAM_TRIGGER_TIME = "Jam trigger time";
  public static final String OVERTIME_JAM_TRIGGER_TIME = "Overtime Jam trigger time";

  public static final Double DEFAULT_JAM_TRIGGER_TIME = new Double(30);
  public static final Double DEFAULT_OVERTIME_JAM_TRIGGER_TIME = new Double(60);

  public static final String ID = "Lineup Clock Starts Jam";
  public static final String DESCRIPTION = "This starts the jam based on the Lineup clock.  When the Lineup clock reaches or exceeds the trigger value (by default 30 seconds), the jam is started (via StartJam), and the Lineup clock is stopped and reset (to avoid possible accidental triggers of this policy).  By default, this policy is disabled.  Also note that if the Lineup clock's minimum time is greater than or equal to the trigger time (if the Lineup clock is counting up; if counting down, the maximum time is less than or equal to the trigger time), this policy will not operate, to avoid an infinite loop.";
}
