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
	}

	public void snapshot(Map<String, Object> stateUpdates, String statePrefix) {
		statePrefix = statePrefix + ".Team(" + team + ")";
		Team t = game.getTeam(team);
		name = t.getName();
		logo = t.getLogo();

		stateUpdates.put(statePrefix + ".Name", name);
		stateUpdates.put(statePrefix + ".Logo", logo);

		// TODO: DO NOT CLEAR!  Just look for changes
		alternateNames.clear();
		colors.clear();
		skaters.clear();

		for (Team.AlternateName an : t.getAlternateNames()) {
			alternateNames.put(an.getId(), an.getName());
			stateUpdates.put(statePrefix + ".AlternateName(" + an.getId() + ")", an.getName());
		}
		for (Team.Color c : t.getColors()) {
			colors.put(c.getId(), c.getColor());
			stateUpdates.put(statePrefix + ".Color(" + c.getId() + ")", c.getColor());
		}
		for (Skater s : t.getSkaters()) {
			skaters.add(new SkaterInfo(game, this, s));
		}
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("team", team);
		json.put("name", name);
		json.put("logo", logo);
		json.put("color", alternateNames.get("operator"));

		JSONObject an = new JSONObject();
		for (String key : alternateNames.keySet())
			an.put(key, alternateNames.get(key));
		json.put("alternateNames", an);

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

	public Game                getGame()           { return game; }
	public String              getTeam()           { return team; }
	public String              getName()           { return name; }
	public String              getLogo()           { return logo; }
	public Map<String, String> getAlternateNames() { return alternateNames; }
	public Map<String, String> getColors()         { return colors; }
	public List<SkaterInfo>    getSkaters()        { return skaters; }

	private Game game;
	private String team;
	private String name;
	private String logo;
	private Map<String, String> alternateNames = new Hashtable<String, String>();
	private Map<String, String> colors = new Hashtable<String, String>();
	private List<SkaterInfo> skaters = new ArrayList<SkaterInfo>();
}
