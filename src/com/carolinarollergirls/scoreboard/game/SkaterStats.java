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
		isPenaltyBox = s.isPenaltyBox();
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("id", id);
		json.put("position", position);
		json.put("isPenaltyBox", isPenaltyBox);

		return json;
	}

	private Game game;
	private String id;
	private String position;
	private boolean isPenaltyBox;
}
