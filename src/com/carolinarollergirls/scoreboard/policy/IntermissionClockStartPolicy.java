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
    super(ID, DESCRIPTION);

    addParameterModel(new IntermissionTimeParameterModel(this, SET_INTERMISSION_TIME_TO, "String", DEFAULT_INTERMISSION_TIME));
    addParameterModel(new DefaultPolicyModel.DefaultParameterModel(this, SET_INTERMISSION_NUMBER, "Boolean", String.valueOf(true)));
  }

  public void setScoreBoardModel(ScoreBoardModel sbm) {
    super.setScoreBoardModel(sbm);
    addClock(Clock.ID_JAM);
    addClock(Clock.ID_PERIOD);
    addClock(Clock.ID_TIMEOUT);
  }

  protected void startIntermissionClock() {
    ClockModel ic = getScoreBoardModel().getClockModel(Clock.ID_INTERMISSION);
    if (!ic.isRunning()) {
      if (Boolean.parseBoolean(getParameter(SET_INTERMISSION_NUMBER).getValue()))
        ic.setNumber(getScoreBoardModel().getClockModel(Clock.ID_PERIOD).getNumber());
      //FIXME - might be better to have some validity checking/enforcement in property setting instead of at usage time here
      try {
        ic.setTime(ClockConversion.fromHumanReadable(getParameter(SET_INTERMISSION_TIME_TO).getValue()));
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
    ClockModel tc = getScoreBoardModel().getClockModel(Clock.ID_TIMEOUT);
    boolean atPeriodEnd = (pc.getTime() == (pc.isCountDirectionDown() ? pc.getMinimumTime() : pc.getMaximumTime()));
    if (!jc.isRunning() && !pc.isRunning() && !tc.isRunning() && atPeriodEnd)
      startIntermissionClock();
  }

  public static final String SET_INTERMISSION_TIME_TO = "Set Intermission Time To";
  public static final String SET_INTERMISSION_NUMBER = "Set Intermission Number";

  public static final String DEFAULT_INTERMISSION_TIME = "15:00";

  public static final String ID = "Intermission Clock Start";
  public static final String DESCRIPTION = "When the Period is over, this sets and starts the Intermission clock time and optionally sets the number to the Period number.";

  public class IntermissionTimeParameterModel extends DefaultPolicyModel.DefaultParameterModel {

	public IntermissionTimeParameterModel(PolicyModel pM, String n, String t, String v) {
		super(pM, n, t, v);
	}

	public void reset() {
		String d = "";
		try {
			d = ClockConversion.toHumanReadable(Long.parseLong(getScoreBoardModel().getSettings().get("Clock." + Clock.ID_INTERMISSION + ".Time")));
		} catch ( Exception e ) {
		}
		try {
			if (d != "") {
				setValue(d);
			} else {
				setValue(defaultValue);
			}
		} catch ( IllegalArgumentException iaE ) {
		}
	}
  }
}
