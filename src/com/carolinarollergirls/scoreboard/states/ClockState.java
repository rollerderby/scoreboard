package com.carolinarollergirls.scoreboard.states;

public class ClockState {
	public ClockState(String id, int number, long time, boolean isRunning, long lastTime) {
		this.id = id;
		this.number = number;
		this.time = time;
		this.isRunning = isRunning;
		this.lastTime = lastTime;
	}
	
	public String getId() { return id; }
	public int getNumber() { return number; }
	public long getTime() { return time; }
	public boolean isRunning() { return isRunning; }
	public long getLastTime() { return lastTime; }
	
	protected String id;
	protected int number;
	protected long time;
	protected boolean isRunning;
	protected long lastTime;
}
