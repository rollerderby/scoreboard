package com.carolinarollergirls.scoreboard.game;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.carolinarollergirls.scoreboard.Clock;
import com.carolinarollergirls.scoreboard.ClockConversion;
import com.carolinarollergirls.scoreboard.Game;
import com.carolinarollergirls.scoreboard.Team;

public class JamStats extends Updater {
	public JamStats(Game g, PeriodStats ps, long j) {
		super(g);
		period_stats = ps;
		jam = j;
		teams = new TeamStats[2];
		teams[0] = new TeamStats(g, this, Team.ID_1);
		teams[1] = new TeamStats(g, this, Team.ID_2);
	}

	public String getUpdaterBase() {
		return period_stats.getUpdaterBase() + ".Jam(" + jam + ")";
	}

	public void snapshot(boolean jamEnd) {
		Clock periodClock = game.getClock(Clock.ID_PERIOD);
		Clock jamClock = game.getClock(Clock.ID_JAM);
		boolean jamClockRunning = jamClock.isRunning();

		if (jamClockRunning) {
			if (periodWallClockStart == null) {
				periodClockStart = periodClock.getTime();
				periodWallClockStart = new Date();
			}
			periodWallClockEnd = null;
		}
		if (jamEnd) {
			periodClockEnd = periodClock.getTime();
			periodWallClockEnd = new Date();
		}
		this.jamClock = jamClock.getTime();
		this.jamLength = jamClock.isCountDirectionDown() ? (jamClock.getMaximumTime() - jamClock.getTime()) : jamClock.getTime();

		for (TeamStats ts : teams)
			ts.snapshot(jamClockRunning, jamClockRunning || jamEnd);

		queueUpdates();
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("jam", jam);
		if (periodWallClockStart != null) {
			json.put("periodClockStart", ClockConversion.toHumanReadable(periodClockStart));
			json.put("periodWallClockStart", periodWallClockStart);
		}
		if (periodWallClockEnd != null) {
			json.put("periodClockEnd", ClockConversion.toHumanReadable(periodClockEnd));
			json.put("periodWallClockEnd", periodWallClockEnd);
		}
		json.put("jamClock", ClockConversion.toHumanReadable(jamClock));
		json.put("jamLength", ClockConversion.toHumanReadable(jamLength));
		JSONArray t = new JSONArray();
		for (TeamStats ts : teams)
			t.put(ts.toJSON());
		json.put("teams", t);

		return json;
	}

	protected void queueUpdates() {
		update("Jam", jam);
		if (periodWallClockStart != null) {
			update("PeriodClockStart", ClockConversion.toHumanReadable(periodClockStart));
			update("PeriodWallClockStart", periodWallClockStart);
		}
		if (periodWallClockEnd != null) {
			update("PeriodClockEnd", ClockConversion.toHumanReadable(periodClockEnd));
			update("PeriodWallClockEnd", periodWallClockEnd);
		}
		update("JamClock", ClockConversion.toHumanReadable(jamClock));
		update("JamLength", ClockConversion.toHumanReadable(jamLength));
	}

	public PeriodStats getPeriodStats()   { return period_stats; }
	public long getJam()                  { return jam; }
	public TeamStats[] getTeams()         { return teams; }
	public long getPeriodClockStart()     { return periodClockStart; }
	public long getPeriodClockEnd()       { return periodClockEnd; }
	public Date getPeriodWallClockStart() { return periodWallClockStart; }
	public Date getPeriodWallClockEnd()   { return periodWallClockEnd; }
	public long getJamClock()             { return jamClock; }
	public long getJamLength()            { return jamLength; }

	private PeriodStats period_stats;
	private long jam;
	private TeamStats[] teams;
	private long periodClockStart;
	private long periodClockEnd;
	private Date periodWallClockStart = null;
	private Date periodWallClockEnd = null;
	private long jamClock;
	private long jamLength;
}
