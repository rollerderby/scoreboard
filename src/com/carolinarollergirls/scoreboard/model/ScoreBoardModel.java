package com.carolinarollergirls.scoreboard.model;

import java.util.*;

import java.awt.image.*;

import com.carolinarollergirls.scoreboard.*;

public interface ScoreBoardModel extends ScoreBoard
{
	public ScoreBoard getScoreBoard();

	/** Reset the entire ScoreBoard. */
	public void reset();

	public void setTimeoutOwner(String owner);

	public void startJam();
	public void stopJam();

	public void timeout();
	public void timeout(TeamModel team);

	public void unStartJam();
	public void unStopJam();
	public void unTimeout();

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

