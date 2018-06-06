package com.carolinarollergirls.scoreboard.states;

import java.util.HashMap;

public class StateSet {
	public StateSet(String type, String startType, String stopType, String timeoutType,
			String timeoutOwner, boolean isOfficialReview, boolean inOvertime, boolean inPeriod, boolean restartPc,
			HashMap<String, ClockState> clockStates, HashMap<String, TeamState> teamStates) {

		this.timestamp = System.currentTimeMillis();
		this.type = type;
		this.startType = startType;
		this.stopType = stopType;
		this.timeoutType = timeoutType;
		this.timeoutOwner = timeoutOwner;
		this.isOfficialReview = isOfficialReview;
		this.inOvertime = inOvertime;
		this.inPeriod = inPeriod;
		this.restartPc = restartPc;
		this.clockStates = clockStates;
		this.teamStates = teamStates;
	}

	public long getTimestamp() { return timestamp; }
	public String getType() { return type; }
	public String getStartType() { return startType; }
	public String getStopType() { return stopType; }
	public String getTimeoutType() { return timeoutType; }
	public String getTimeoutOwner() { return timeoutOwner; }
	public boolean isOfficialReview() { return isOfficialReview; }
	public boolean inOvertime() { return inOvertime; }
	public boolean inPeriod() { return inPeriod; }
	public boolean restartPc() { return restartPc; }
	public HashMap<String, ClockState> getClockStates() { return clockStates; }
	public HashMap<String, TeamState> getTeamStates() { return teamStates; }
	public ClockState getClockState(String clock) { return clockStates.get(clock); }
	public TeamState getTeamState(String team) { return teamStates.get(team); }
	
	protected long timestamp;
	protected String type;
	protected String startType;
	protected String stopType;
	protected String timeoutType;
	protected String timeoutOwner;
	protected boolean isOfficialReview;
	protected boolean inOvertime;
	protected boolean inPeriod;
	protected boolean restartPc;
	protected HashMap<String, ClockState> clockStates;
	protected HashMap<String, TeamState> teamStates;
}
