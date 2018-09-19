package com.carolinarollergirls.scoreboard.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ScoreBoardClock extends TimerTask {
	private ScoreBoardClock() {
		offset = System.currentTimeMillis();
		timer.scheduleAtFixedRate(this, CLOCK_UPDATE_INTERVAL / 4, CLOCK_UPDATE_INTERVAL / 4);
	}

	public static ScoreBoardClock getInstance() {
		return instance;
	}
	
	public synchronized long getCurrentTime() {
		updateTime();
		return currentTime;
	}
	public synchronized void rewindTo(long time) {
		lastRewind = currentTime - time;
		// changing offset instead of currentTime has two reasons:
		// 1. The change only becomes visible to clients after the clock has restarted.
		// 2. Repeating values for currentTime would cause UpdateClockTimerTask to just 
		//    idle until it has caught up, instead of advancing the clocks by the desired amount.
		offset -= lastRewind;
	}
	public synchronized long getLastRewind() {
		return lastRewind;
	}
	public synchronized void advance(long ms) {
		currentTime += ms;
		updateClients();
	}
	
	public synchronized void stop() {
		updateTime();
		stopCounter++;
	}
	public synchronized void start(boolean doCatchUp) {
		if (!doCatchUp) {
			offset = System.currentTimeMillis() - currentTime;
		}
		stopCounter--;
	}
	
	public synchronized void registerClient(ScoreBoardClockClient client) {
		clients.add(client);
	}
	
	private void updateTime() {
		if (stopCounter == 0) {
			currentTime = System.currentTimeMillis() - offset;
		}
	}
	
	private void updateClients() {
		for (ScoreBoardClockClient client: clients) {
			client.updateTime(currentTime);
		}
	}
	
	public synchronized void run() {
		if (stopCounter == 0) {
			updateTime();
			updateClients();
		}
	}
	
	private long offset;
	private long currentTime;
	private int stopCounter = 0;
	private long lastRewind = 0;
	
	private Timer timer = new Timer();

	private static final ScoreBoardClock instance = new ScoreBoardClock();
	
	private List<ScoreBoardClockClient> clients = new ArrayList<ScoreBoardClockClient>();
	
	public static final long CLOCK_UPDATE_INTERVAL = 200; /* in ms */

	public interface ScoreBoardClockClient {
		/*
		 * Callback that notifies the client of the current time.
		 * Parameter is the time elapsed since scoreboard startup.
		 */
		void updateTime(long ms);
	}
}
