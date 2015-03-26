package com.carolinarollergirls.scoreboard.game;

import java.util.Date;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.Clock;
import com.carolinarollergirls.scoreboard.ClockConversion;
import com.carolinarollergirls.scoreboard.Game;
import com.carolinarollergirls.scoreboard.Team;

public class JamStats {
	public JamStats(Game g, PeriodStats ps, long j) {
		game = g;
		periodStats = ps;
		jam = j;
		teams = new TeamStats[2];
		teams[0] = new TeamStats(game, Team.ID_1);
		teams[1] = new TeamStats(game, Team.ID_2);
	}

	public void snapshot(Map<String, Object> stateUpdates, String statePrefix, boolean jamEnd) {
		statePrefix = statePrefix + ".Jam(" + jam + ")";

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

		stateUpdates.put(statePrefix + ".PeriodClockStart", periodClockStart);
		stateUpdates.put(statePrefix + ".PeriodWallClockStart", periodWallClockStart == null ? null : periodWallClockStart.toString());
		stateUpdates.put(statePrefix + ".PeriodClockEnd", periodClockEnd);
		stateUpdates.put(statePrefix + ".PeriodWallClockEnd", periodWallClockEnd == null ? null : periodWallClockEnd.toString());

		for (TeamStats ts : teams)
			ts.snapshot(stateUpdates, statePrefix, jamClockRunning || jamEnd);
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
		JSONArray t = new JSONArray();
		for (TeamStats ts : teams)
			t.put(ts.toJSON());
		json.put("teams", t);

		return json;
	}

	public Game getGame()                 { return game; }
	public PeriodStats getPeriodStats()   { return periodStats; }
	public long getJam()                  { return jam; }
	public TeamStats[] getTeams()         { return teams; }
	public long getPeriodClockStart()     { return periodClockStart; }
	public long getPeriodClockEnd()       { return periodClockEnd; }
	public Date getPeriodWallClockStart() { return periodWallClockStart; }
	public Date getPeriodWallClockEnd()   { return periodWallClockEnd; }
	public long getJamClock()             { return jamClock; }

	private Game game;
	private PeriodStats periodStats;
	private long jam;
	private TeamStats[] teams;
	private long periodClockStart;
	private long periodClockEnd;
	private Date periodWallClockStart = null;
	private Date periodWallClockEnd = null;
	private long jamClock;
}
