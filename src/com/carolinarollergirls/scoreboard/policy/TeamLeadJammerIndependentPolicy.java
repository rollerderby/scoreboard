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
 * This class is a flag to the DefaultTeamModel implementation.
 *
 * That implementation checks this policy to see if it should
 * track lead jammer independent of its Jammer Position.
 * If this Policy is not present in the ScoreBoard, the
 * team defaults to tracking lead jammer by its Jammer Position.
 */
public class TeamLeadJammerIndependentPolicy extends DefaultPolicyModel
{
  public TeamLeadJammerIndependentPolicy() { super(ID, DESCRIPTION); }

  public static final String ID = "Team Lead Jammer Independent";
  public static final String DESCRIPTION = "When enabled, teams will use their Position(Jammer) to track LeadJammer.  When disabled, teams will track LeadJammer internally.  This should be enabled if tracking lineups, and disabled if not tracking lineups.";
}
