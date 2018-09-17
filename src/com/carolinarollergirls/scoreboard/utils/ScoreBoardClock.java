package com.carolinarollergirls.scoreboard.utils;

import com.carolinarollergirls.scoreboard.defaults.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;

public class ScoreBoardClock extends DefaultScoreBoardEventProvider{
	private ScoreBoardClock() {
		offset = System.currentTimeMillis();
	}
	
	public String getProviderName() { return "ScoreBoardClock"; }
	public Class<ScoreBoardClock> getProviderClass() { return ScoreBoardClock.class; }
	public String getProviderId() { return ""; }

	public static ScoreBoardClock getInstance() {
		if (instance == null) {
			instance = new ScoreBoardClock();
		}
		return instance;
	}
	
	public synchronized long getCurrentTime() {
		updateTime();
		return currentTime;
	}
	public synchronized void rewindTo(long time) {
		long difference = currentTime - time;
		// changing offset instead of currentTime has two reasons:
		// 1. The change only becomes visible to clients after the clock has restarted.
		// 2. Repeating values for currentTime would cause UpdateClockTimerTask to just 
		//    idle until it has caught up, instead of advancing the clocks by the desired amount.
		offset -= difference;
		scoreBoardChange(new ScoreBoardEvent(this, EVENT_REWIND, difference, 0));
	}
	public synchronized void advance(long ms) {
		long last = currentTime;
		currentTime += ms;
		scoreBoardChange(new ScoreBoardEvent(this, EVENT_MANUAL_CHANGE, currentTime, last));
	}
	
	public synchronized void stop() {
		updateTime();
		stopCounter++;
	}
	public synchronized void start(Boolean doCatchUp) {
		if (!doCatchUp) {
			offset = System.currentTimeMillis() - currentTime;
		}
		stopCounter--;
	}
	
	private synchronized void updateTime() {
		if (stopCounter == 0) {
			currentTime = System.currentTimeMillis() - offset;
		}
	}
	
	private long offset;
	private long currentTime;
	private int stopCounter = 0;
	
	private static ScoreBoardClock instance = null;
	
	public final static String EVENT_MANUAL_CHANGE = "ManualChange";
	public final static String EVENT_REWIND = "Rewind";
}
