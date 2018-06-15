package com.carolinarollergirls.scoreboard.model;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;

import com.carolinarollergirls.scoreboard.*;

public interface ScoreBoardModel extends ScoreBoard
{
	public ScoreBoard getScoreBoard();

	/** Reset the entire ScoreBoard. */
	public void reset();

	public void setTimeoutOwner(String owner);
	public void setOfficialReview(boolean official);

	public void setInOvertime(boolean inOvertime);
	public void startOvertime();

	public void setInPeriod(boolean inPeriod);

	public void setOfficialScore(boolean official);

	public void startJam();
	public void stopJam();

	public void timeout();
	public void timeout(TeamModel team);
	public void timeout(TeamModel team, boolean review);

	public void unStartJam();
	public void unStopJam();
	public void unTimeout();

  public void penalty(String teamId, String skaterId, String penaltyId, boolean fo_exp, int period, int jam, String code);

	public void setRuleset(String id);
	public SettingsModel getSettingsModel();

// FIXME - need methods to add/remove clocks and teams! */
	public List<ClockModel> getClockModels();
	public ClockModel getClockModel(String id);

	public List<TeamModel> getTeamModels();
	public TeamModel getTeamModel(String id);

	public List<PolicyModel> getPolicyModels();
	public PolicyModel getPolicyModel(String id);
	public void addPolicyModel(PolicyModel model) throws IllegalArgumentException; /* If the model's Id is null/empty */
	public void removePolicyModel(PolicyModel model);
}

