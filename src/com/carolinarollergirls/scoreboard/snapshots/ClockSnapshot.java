package com.carolinarollergirls.scoreboard.snapshots;

import com.carolinarollergirls.scoreboard.model.ClockModel;

public class ClockSnapshot {
	public ClockSnapshot(ClockModel clock) {
		id = clock.getId();
		number = clock.getNumber();
		time = clock.getTime();
		isRunning = clock.isRunning();
	}
	
	public String getId() { return id; }
	public int getNumber() { return number; }
	public long getTime() { return time; }
	public boolean isRunning() { return isRunning; }
	
	protected String id;
	protected int number;
	protected long time;
	protected boolean isRunning;
}
