package com.carolinarollergirls.scoreboard.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.carolinarollergirls.scoreboard.core.implementation.ScoreBoardImpl;

public class ScoreBoardClock extends TimerTask {
    private ScoreBoardClock() {
	offset = System.currentTimeMillis();
	timer.scheduleAtFixedRate(this, CLOCK_UPDATE_INTERVAL / 4, CLOCK_UPDATE_INTERVAL / 4);
    }

    public static ScoreBoardClock getInstance() {
	return instance;
    }

    public long getCurrentTime() {
	synchronized (coreLock) {
	    updateTime();
	    return currentTime;
	}
    }

    public void rewindTo(long time) {
	synchronized (coreLock) {
	    lastRewind = currentTime - time;
	    // changing offset instead of currentTime has two reasons:
	    // 1. The change only becomes visible to clients after the clock has restarted.
	    // 2. Repeating values for currentTime would cause UpdateClockTimerTask to just
	    //    idle until it has caught up, instead of advancing the clocks by the desired amount.
	    offset -= lastRewind;
	}
    }

    public long getLastRewind() {
	synchronized (coreLock) {
	    return lastRewind;
	}
    }

    public void advance(long ms) {
	synchronized(coreLock) {
	    currentTime += ms;
	    updateClients();
	}
    }

    public void stop() {
	synchronized (coreLock) {
	    updateTime();
	    stopCounter++;
	}
    }

    public void start(boolean doCatchUp) {
	synchronized (coreLock) {
	    if (!doCatchUp) {
		offset = System.currentTimeMillis() - currentTime;
	    }
	    stopCounter--;
	}
    }

    public void registerClient(ScoreBoardClockClient client) {
	synchronized (coreLock) {
	    clients.add(client);
	}
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

    public void run() {
	synchronized (coreLock) {
	    if (stopCounter == 0) {
		updateTime();
		updateClients();
	    }
	}
    }

    private long offset;
    private long currentTime;
    private int stopCounter = 0;
    private long lastRewind = 0;

    private Timer timer = new Timer();

    private static final ScoreBoardClock instance = new ScoreBoardClock();

    private Object coreLock = ScoreBoardImpl.getCoreLock();

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
