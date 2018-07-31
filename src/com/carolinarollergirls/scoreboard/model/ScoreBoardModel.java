package com.carolinarollergirls.scoreboard.model;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.HashMap;
import java.util.List;

import com.carolinarollergirls.scoreboard.ScoreBoard;
import com.carolinarollergirls.scoreboard.model.ClockModel.ClockSnapshot;
import com.carolinarollergirls.scoreboard.model.TeamModel.TeamSnapshot;

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

	public void clockUndo();
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

	public class ScoreBoardSnapshot {
		public ScoreBoardSnapshot(ScoreBoardModel sbm, String type) {
			snapshotTime = System.currentTimeMillis();
			this.type = type; 
			timeoutOwner = sbm.getTimeoutOwner();
			isOfficialReview = sbm.isOfficialReview();
			inOvertime = sbm.isInOvertime();
			inPeriod = sbm.isInPeriod();
			clockSnapshots = new HashMap<String, ClockSnapshot>();
			for (ClockModel clock : sbm.getClockModels()) {
				clockSnapshots.put(clock.getId(), new ClockSnapshot(clock));
			}
			teamSnapshots = new HashMap<String, TeamSnapshot>();
			for (TeamModel team : sbm.getTeamModels()) {
				teamSnapshots.put(team.getId(), new TeamSnapshot(team));
			}
		}

		public String getType() { return type; }
		public long getSnapshotTime() { return snapshotTime; }
		public String getTimeoutOwner() { return timeoutOwner; }
		public boolean isOfficialReview() { return isOfficialReview; }
		public boolean inOvertime() { return inOvertime; }
		public boolean inPeriod() { return inPeriod; }
		public HashMap<String, ClockSnapshot> getClockSnapshots() { return clockSnapshots; }
		public HashMap<String, TeamSnapshot> getTeamSnapshots() { return teamSnapshots; }
		public ClockSnapshot getClockSnapshot(String clock) { return clockSnapshots.get(clock); }
		public TeamSnapshot getTeamSnapshot(String team) { return teamSnapshots.get(team); }
		
		protected String type;
		protected long snapshotTime;
		protected String timeoutOwner;
		protected boolean isOfficialReview;
		protected boolean inOvertime;
		protected boolean inPeriod;
		protected HashMap<String, ClockSnapshot> clockSnapshots;
		protected HashMap<String, TeamSnapshot> teamSnapshots;
	}
}

