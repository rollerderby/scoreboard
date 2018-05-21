package com.carolinarollergirls.scoreboard.states;

import java.util.HashMap;

public class StateSet {
	public StateSet(String timeoutOwner, boolean isOfficialReview, boolean inPeriod, boolean restartPc,
			HashMap<String, ClockState> clockStates, HashMap<String, TeamState> teamStates) {
		this.timeoutOwner = timeoutOwner;
		this.isOfficialReview = isOfficialReview;
		this.inPeriod = inPeriod;
		this.restartPc = restartPc;
		this.clockStates = clockStates;
		this.teamStates = teamStates;
	}
	
	public String getTimeoutOwner() { return timeoutOwner; }
	public boolean isOfficialReview() { return isOfficialReview; }
	public boolean inPeriod() { return inPeriod; }
	public boolean restartPc() { return restartPc; }
	public HashMap<String, ClockState> getClockStates() { return clockStates; }
	public HashMap<String, TeamState> getTeamStates() { return teamStates; }
	public ClockState getClockState(String clock) { return clockStates.get(clock); }
	public TeamState getTeamState(String team) { return teamStates.get(team); }
	
	protected String timeoutOwner;
	protected boolean isOfficialReview;
	protected boolean inPeriod;
	protected boolean restartPc;
	protected HashMap<String, ClockState> clockStates;
	protected HashMap<String, TeamState> teamStates;
}
