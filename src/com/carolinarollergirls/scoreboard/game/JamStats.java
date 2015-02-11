package com.carolinarollergirls.scoreboard.game;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.Clock;
import com.carolinarollergirls.scoreboard.Game;
import com.carolinarollergirls.scoreboard.Team;

public class JamStats {
	public JamStats(Game game, long jam) {
		this.game = game;
		this.jam = jam;
		this.teams = new TeamStats[2];
		this.teams[0] = new TeamStats(game, Team.ID_1);
		this.teams[1] = new TeamStats(game, Team.ID_2);
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
		for (TeamStats ts : teams)
			ts.snapshot(jamClockRunning || jamEnd);
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("jam", jam);
		if (periodWallClockStart != null) {
			json.put("periodClockStart", periodClockStart);
			json.put("periodWallClockStart", periodWallClockStart);
		}
		if (periodWallClockEnd != null) {
			json.put("periodClockEnd", periodClockEnd);
			json.put("periodWallClockEnd", periodWallClockEnd);
		}
		json.put("jamClock", jamClock);
		JSONArray t = new JSONArray();
		for (TeamStats ts : teams)
			t.put(ts.toJSON());
		json.put("teams", t);

		return json;
	}

	public long getJam() { return jam; }

	private Game game;
	private long jam;
	private TeamStats[] teams;
	private long periodClockStart;
	private long periodClockEnd;
	private Date periodWallClockStart = null;
	private Date periodWallClockEnd = null;
	private long jamClock;
}
