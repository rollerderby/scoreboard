package com.carolinarollergirls.scoreboard.game;

import org.json.JSONObject;

import com.carolinarollergirls.scoreboard.Game;
import com.carolinarollergirls.scoreboard.Skater;

public class SkaterInfo {
	public SkaterInfo(Game g, Skater s) {
		game = g;
		id = s.getId();
		name = s.getName();
		number = s.getNumber();
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("id", id);
		json.put("name", name);
		json.put("number", number);

		return json;
	}

	private Game game;
	private String id;
	private String name;
	private String number;
}
