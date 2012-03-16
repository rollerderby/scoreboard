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

public class IntermissionClockStartPolicy extends AbstractClockRunningChangePolicy
{
  public IntermissionClockStartPolicy() {
    super();

    addParameterModel(new DefaultPolicyModel.DefaultParameterModel(this, SET_INTERMISSION_TIME_TO, "String", DEFAULT_INTERMISSION_TIME));
    addParameterModel(new DefaultPolicyModel.DefaultParameterModel(this, SET_INTERMISSION_NUMBER, "Boolean", String.valueOf(true)));
  }

  public void setScoreBoardModel(ScoreBoardModel sbm) {
    super.setScoreBoardModel(sbm);
    addClock(Clock.ID_JAM);
    addClock(Clock.ID_PERIOD);
  }

  public void reset() {
    super.reset();
    setDescription("When the Period is over, this sets and starts the Intermission clock time and optionally sets the number to the Period number.");
  }

  // FIXME - these should be common utility methods, and probably implemented using Formatter and Scanner.
  protected String msToMinSec(long ms) {
    long min = ms / 60000;
    long sec = (ms / 1000) % 60;
    if (min > 0)
      return Long.toString(min)+":"+(sec < 10?"0":"")+Long.toString(sec);
    return Long.toString(sec);
  }
  protected long minSecToMs(String time) throws NumberFormatException {
    int colon = time.indexOf(":");
    long min = 0;
    long sec = 0;
    if (0 > colon) {
      sec = Long.parseLong(time);
    } else {
      min = Long.parseLong(time.substring(0, colon));
      sec = Long.parseLong(time.substring(colon+1));
    }
    return ((min*60)+sec)*1000;
  }

  protected void startIntermissionClock() {
    ClockModel ic = getScoreBoardModel().getClockModel(Clock.ID_INTERMISSION);
    if (!ic.isRunning()) {
      if (Boolean.parseBoolean(getParameter(SET_INTERMISSION_NUMBER).getValue()))
        ic.setNumber(getScoreBoardModel().getClockModel(Clock.ID_PERIOD).getNumber());
      //FIXME - might be better to have some validity checking/enforcement in property setting instead of at usage time here
      try {
        ic.setTime(minSecToMs(getParameter(SET_INTERMISSION_TIME_TO).getValue()));
      } catch ( NumberFormatException nfE ) {
        // This probably isn't really what is desired, but we should reset the time to something...
        ic.resetTime();
      }
      ic.start();
    }
  }

  public void clockRunningChange(Clock clock, boolean running) {
    ClockModel jc = getScoreBoardModel().getClockModel(Clock.ID_JAM);
    ClockModel pc = getScoreBoardModel().getClockModel(Clock.ID_PERIOD);
    boolean atPeriodEnd = (pc.getTime() == (pc.isCountDirectionDown() ? pc.getMinimumTime() : pc.getMaximumTime()));
    if (!jc.isRunning() && !pc.isRunning() && atPeriodEnd)
      startIntermissionClock();
  }

  public static final String SET_INTERMISSION_TIME_TO = "SetIntermissionTimeTo";
  public static final String SET_INTERMISSION_NUMBER = "SetIntermissionNumber";

  public static final String DEFAULT_INTERMISSION_TIME = "15:00";
}
