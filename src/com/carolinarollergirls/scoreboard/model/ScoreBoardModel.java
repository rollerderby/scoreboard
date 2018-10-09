package com.carolinarollergirls.scoreboard.model;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.List;

import com.carolinarollergirls.scoreboard.view.ScoreBoard;

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
	public void stopJamTO();

	public void timeout();
	public void setTimeoutType(String team, boolean review);

	public void clockUndo(boolean replace);

	public void penalty(String teamId, String skaterId, String penaltyId, boolean fo_exp, int period, int jam, String code);

	public void setRuleset(String id);
	public SettingsModel getSettingsModel();
	public FrontendSettingsModel getFrontendSettingsModel();
	public StatsModel getStatsModel();

// FIXME - need methods to add/remove clocks and teams! */
	public List<ClockModel> getClockModels();
	public ClockModel getClockModel(String id);

	public List<TeamModel> getTeamModels();
	public TeamModel getTeamModel(String id);
}

