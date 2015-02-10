package com.carolinarollergirls.scoreboard.game;

import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

import com.carolinarollergirls.scoreboard.Game;
import com.carolinarollergirls.scoreboard.Skater;
import com.carolinarollergirls.scoreboard.Team;

public class TeamInfo {
	public TeamInfo(Game g, String t) {
		game = g;
		team = t;
		snapshot();
	}

	public void snapshot() {
		Team t = game.getTeam(team);
		name = t.getName();
		color = "";

		Team.AlternateName an = t.getAlternateName(Team.AlternateName.ID_OPERATOR);
		if (an != null)
			color = an.getName();

		skaters = new ArrayList<SkaterInfo>();
		for (Skater s : t.getSkaters()) {
			skaters.add(new SkaterInfo(game, s));
		}
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("team", team);
		json.put("name", name);
		json.put("color", color);
		JSONArray s = new JSONArray();
		for (SkaterInfo si : skaters)
			s.put(si.toJSON());
		json.put("skaters", s);

		return json;
	}

	private Game game;
	private String team;
	private String name;
	private String color;
	private ArrayList<SkaterInfo> skaters;
}
