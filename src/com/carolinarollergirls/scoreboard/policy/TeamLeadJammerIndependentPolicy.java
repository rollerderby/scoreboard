package com.carolinarollergirls.scoreboard.policy;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

/**
 * This class is a flag to the DefaultTeamModel implementation.
 *
 * That implementation checks this policy to see if it should
 * track lead jammer independent of its Jammer Position.
 * If this Policy is not present in the ScoreBoard, the
 * team defaults to tracking lead jammer by its Jammer Position.
 */
public class TeamLeadJammerIndependentPolicy extends DefaultPolicyModel
{
	public TeamLeadJammerIndependentPolicy() {
		super(ID);
		setDescription("When enabled, teams will use their Position(Jammer) to track LeadJammer.  When disabled, teams will track LeadJammer internally.  This should be enabled if tracking lineups, and disabled if not tracking lineups.");
	}

	public static final String ID = "TeamLeadJammerIndependentPolicy";
}
