package com.carolinarollergirls.scoreboard.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
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

		alternate_names.clear();
		colors.clear();
		skaters.clear();

		for (Team.AlternateName an : t.getAlternateNames())
			alternate_names.put(an.getId(), an.getName());
		for (Team.Color c : t.getColors())
			colors.put(c.getId(), c.getColor());
		for (Skater s : t.getSkaters())
			skaters.add(new SkaterInfo(game, s));
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("team", team);
		json.put("name", name);
		json.put("logo", logo);
		json.put("color", alternate_names.get("operator"));

		JSONObject an = new JSONObject();
		for (String key : alternate_names.keySet())
			an.put(key, alternate_names.get(key));
		json.put("alternate_names", an);

		JSONObject c = new JSONObject();
		for (String key : colors.keySet())
			c.put(key, colors.get(key));
		json.put("colors", c);

		JSONArray s = new JSONArray();
		for (SkaterInfo si : skaters)
			s.put(si.toJSON());
		json.put("skaters", s);

		return json;
	}

	private Game game;
	private String team;
	private String name;
	private String logo;

	private Map<String, String> alternate_names = new Hashtable<String, String>();
	private Map<String, String> colors = new Hashtable<String, String>();
	private List<SkaterInfo> skaters = new ArrayList<SkaterInfo>();
}
