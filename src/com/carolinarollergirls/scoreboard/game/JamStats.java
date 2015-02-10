package com.carolinarollergirls.scoreboard.game;

import org.json.JSONArray;
import org.json.JSONObject;

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

	public void snapshot() {
		Clock periodClock = game.getClock(Clock.ID_PERIOD);
		Clock jamClock = game.getClock(Clock.ID_JAM);
		boolean getSkaters = jamClock.isRunning();

		this.periodClock = periodClock.getTime();
		this.jamClock = jamClock.getTime();
		for (TeamStats ts : teams)
			ts.snapshot(getSkaters);
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("jam", jam);
		json.put("periodClock", periodClock);
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
	private long periodClock;
	private long jamClock;
}
