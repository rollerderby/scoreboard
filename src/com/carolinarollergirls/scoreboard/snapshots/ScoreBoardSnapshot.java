package com.carolinarollergirls.scoreboard.snapshots;

import java.util.HashMap;

import com.carolinarollergirls.scoreboard.model.ClockModel;
import com.carolinarollergirls.scoreboard.model.ScoreBoardModel;
import com.carolinarollergirls.scoreboard.model.TeamModel;

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
