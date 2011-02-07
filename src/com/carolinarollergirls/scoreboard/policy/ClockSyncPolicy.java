package com.carolinarollergirls.scoreboard.policy;

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
	public ClockSyncPolicy() {
		super(ID);
		setDescription("When enabled, all clocks will sync to the nearest second.  This allows all clocks to change in sync, however when any clock is started it will either be delayed or accelerated by up to 500 ms.  When disabled, clocks will start immediately, but will run out of sync with each other.  If disabled and then re-enabled, any currently running clock will remain unsynced until it stops.");
	}

	public static final String ID = "ClockSyncPolicy";
}