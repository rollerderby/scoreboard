package com.carolinarollergirls.scoreboard.game;

import org.json.JSONObject;

import com.carolinarollergirls.scoreboard.Game;
import com.carolinarollergirls.scoreboard.Skater;

public class SkaterStats {
	public SkaterStats(Game g, Skater s) {
		game = g;
		skater = s;
		position = s.getPosition();
		isPenaltyBox = s.isPenaltyBox();
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("number", skater.getNumber());
		json.put("position", position);
		json.put("isPenaltyBox", isPenaltyBox);

		return json;
	}

	private Game game;
	private Skater skater;
	private String position;
	private boolean isPenaltyBox;
}
