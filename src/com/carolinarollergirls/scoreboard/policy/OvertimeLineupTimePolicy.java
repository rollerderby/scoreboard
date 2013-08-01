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

/**
 * This class is a flag to the DefaultScoreBoardModel implementation.
 *
 * That implementation checks this policy to see if it should adjust the Lineup clock when starting (and ending) Overtime.  If this Policy is not present in the ScoreBoard, no adjustment is made to the Lineup clock when starting Overtime.
 */
public class OvertimeLineupTimePolicy extends DefaultPolicyModel
{
  public OvertimeLineupTimePolicy() {
    super(ID, DESCRIPTION);

    addParameterModel(new DefaultPolicyModel.DefaultParameterModel(this, OVERTIME_LINEUP_TIME, "Long", String.valueOf(60)));
    addParameterModel(new DefaultPolicyModel.DefaultParameterModel(this, SAVED_LINEUP_TIME, "Long", String.valueOf(0)));
    addParameterModel(new DefaultPolicyModel.DefaultParameterModel(this, RESTORE_LINEUP_TIME, "Boolean", String.valueOf(false)));
  }

  public void startOvertime() {
    if (!isEnabled())
      return;

    ClockModel lc = getScoreBoardModel().getClockModel(Clock.ID_LINEUP);
    long otLineupTime = (Long.parseLong(getParameter(OVERTIME_LINEUP_TIME).getValue()) * 1000);
    if (lc.getMaximumTime() < otLineupTime) {
      getParameterModel(SAVED_LINEUP_TIME).setValue(String.valueOf(lc.getMaximumTime()));
      getParameterModel(RESTORE_LINEUP_TIME).setValue(String.valueOf(true));
      lc.setMaximumTime(otLineupTime);
    }
  }

  public void stopOvertime() {
    if (!isEnabled())
      return;

    ClockModel lc = getScoreBoardModel().getClockModel(Clock.ID_LINEUP);
    if (Boolean.parseBoolean(getParameter(RESTORE_LINEUP_TIME).getValue())) {
      lc.setMaximumTime(Long.parseLong(getParameter(SAVED_LINEUP_TIME).getValue()));
      getParameterModel(SAVED_LINEUP_TIME).setValue(String.valueOf(0));
      getParameterModel(RESTORE_LINEUP_TIME).setValue(String.valueOf(false));
    }
  }

  public static final String OVERTIME_LINEUP_TIME = "Overtime Lineup Time";
  public static final String SAVED_LINEUP_TIME = "Saved Lineup Time";
  public static final String RESTORE_LINEUP_TIME = "Restore Lineup Time";

  public static final String ID = "Overtime Lineup Time";
  public static final String DESCRIPTION = "When entering Overtime and this policy is enabled, if the Lineup clock maximum time is less than the 'Overtime Lineup Time' parameter (specified in seconds), the 'Saved Lineup Time' parameter (specified in milliseconds) will be set to the Lineup clock's current maximum time and the 'Restore Lineup Time' parameter will be enabled, and then the Lineup time will be set to the 'Overtime Lineup Time' parameter value.  Then when ending Overtime and this policy is enabled and the 'Restore Lineup Time' parameter is enabled, the Lineup time will be set to the 'Saved Lineup Time' parameter, and the 'Saved Lineup Time' parameter will be set to 0 and 'Restore Lineup Time' parameter disabled.  The intention of this Policy is, if the Lineup clock is set to count down from 30 seconds, the current WFTDA ruleset specifies a 1 minute Lineup time before the Overtime Jam, so when Overtime is started the Lineup clock needs to be adjusted to count the full 1 minute Lineup.";
}
