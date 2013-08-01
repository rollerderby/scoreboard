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
 * This class is a flag to the DefaultClockModel implementation.
 *
 * That implementation checks this policy to see if it should sync its time or not.  If this Policy is not present in the ScoreBoard, the clock defaults to syncing its time.
 */
public class ClockSyncPolicy extends DefaultPolicyModel
{
  public ClockSyncPolicy() { super(ID, DESCRIPTION); }

  public static final String ID = "Clock Sync";
  public static final String DESCRIPTION = "When enabled, all clocks will sync to the nearest second.  This allows all clocks to change in sync, however when any clock is started it will either be delayed or accelerated by up to 500 ms.  When disabled, clocks will start immediately, but will run out of sync with each other.  If disabled and then re-enabled, any currently running clock will remain unsynced until it stops.";
}
