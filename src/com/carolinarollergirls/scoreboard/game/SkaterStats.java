package com.carolinarollergirls.scoreboard.game;

import org.json.JSONException;
import org.json.JSONObject;

import com.carolinarollergirls.scoreboard.Game;
import com.carolinarollergirls.scoreboard.Skater;

public class SkaterStats {
	public SkaterStats(Game g, Skater s) {
		game = g;
		id = s.getId();
		position = s.getPosition();
		penaltyBox = s.isPenaltyBox();
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("id", id);
		json.put("position", position);
		json.put("penaltyBox", penaltyBox);

		return json;
	}

	public Game    getGame()       { return game; }
	public String  getId()         { return id; }
	public String  getPosition()   { return position; }
	public boolean getPenaltyBox() { return penaltyBox; }

	private Game game;
	private String id;
	private String position;
	private boolean penaltyBox;
}
