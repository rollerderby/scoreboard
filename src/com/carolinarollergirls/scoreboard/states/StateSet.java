package com.carolinarollergirls.scoreboard.states;

import java.util.HashMap;

public class StateSet {
	public StateSet(String timeoutOwner, boolean isOfficialReview, boolean inPeriod, 
			HashMap<String, ClockState> clockStates, HashMap<String, TeamState> teamStates) {
		this.timeoutOwner = timeoutOwner;
		this.isOfficialReview = isOfficialReview;
		this.inPeriod = inPeriod;
		this.clockStates = clockStates;
		this.teamStates = teamStates;
	}
	
	public String getTimeoutOwner() { return timeoutOwner; }
	public boolean isOfficialReview() { return isOfficialReview; }
	public boolean inPeriod() { return inPeriod; }
	public HashMap<String, ClockState> getClockStates() { return clockStates; }
	public HashMap<String, TeamState> getTeamStates() { return teamStates; }
	public ClockState getClockState(String clock) { return clockStates.get(clock); }
	public TeamState getTeamState(String team) { return teamStates.get(team); }
	
	protected String timeoutOwner;
	protected boolean isOfficialReview;
	protected boolean inPeriod;
	protected HashMap<String, ClockState> clockStates;
	protected HashMap<String, TeamState> teamStates;
}
