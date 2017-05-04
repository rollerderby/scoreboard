package com.carolinarollergirls.scoreboard.game;

import org.json.JSONException;
import org.json.JSONObject;

import com.carolinarollergirls.scoreboard.Game;
import com.carolinarollergirls.scoreboard.Skater;

public class SkaterStats extends Updater {
	public SkaterStats(Game g, TeamStats ts, Skater s) {
		super(g);
		team_stats = ts;
		id = s.getId();
		position = s.getPosition();
		penaltyBox = s.isPenaltyBox();
		queueUpdates();
	}

	public String getUpdaterBase() {
		return team_stats.getUpdaterBase() + ".Skater(" + position + ")";
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("id", id);
		json.put("position", position);
		json.put("penaltyBox", penaltyBox);

		return json;
	}

	protected void queueUpdates() {
		update("Id", id);
		update("Position", position);
		update("PenaltyBox", penaltyBox);
	}

	public TeamStats getTeamStats()  { return team_stats; }
	public String    getId()         { return id; }
	public String    getPosition()   { return position; }
	public boolean   getPenaltyBox() { return penaltyBox; }

	private TeamStats team_stats;
	private String id;
	private String position;
	private boolean penaltyBox;
}
