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

public class TeamInfo extends Updater {
	public TeamInfo(Game g, String t) {
		super(g);
		team = t;
		snapshot();
	}

	public String getUpdaterBase() {
		return game.getUpdaterBase() + ".Team(" + team + ")";
	}

	public void snapshot() {
		Team t = game.getTeam(team);
		name = t.getName();

		// Add/Update/Remove Alternate Names
		List<Team.AlternateName> t_an = t.getAlternateNames();
		for (Team.AlternateName an : t_an) {
			alternateNames.put(an.getId(), an.getName());
		}
		for (String id : alternateNames.keySet()) {
			boolean found = false;
			for (Team.AlternateName an : t_an) {
				if (an.getId().equals(id)) {
					found = true;
					break;
				}
			}
			if (!found) {
				alternateNames.remove(id);
				update("AlternateName(" + id + ")", null);
			}
		}

		// Add/Update/Remove Colors
		List<Team.Color> t_c = t.getColors();
		for (Team.Color c : t_c) {
			colors.put(c.getId(), c.getColor());
		}
		for (String id : colors.keySet()) {
			boolean found = false;
			for (Team.Color c : t_c) {
				if (c.getId().equals(id)) {
					found = true;
					break;
				}
			}
			if (!found) {
				colors.remove(id);
				update("Color(" + id + ")", null);
			}
		}

		// Add/Update/Remove Skaters
		List<Skater> t_s = t.getSkaters();
		for (Skater s : t_s) {
			boolean found = false;
			for (SkaterInfo si : skaters) {
				if (si.getId().equals(s.getId())) {
					si.snapshot(s);
					found = true;
					break;
				}
			}
			if (!found)
				skaters.add(new SkaterInfo(game, this, s));
		}
		SkaterInfo[] sa = new SkaterInfo[skaters.size()];
		skaters.toArray(sa);
		for (SkaterInfo si : sa) {
			boolean found = false;
			for (Skater s : t_s) {
				if (si.getId().equals(s.getId())) {
					found = true;
					break;
				}
			}
			if (!found) {
				skaters.remove(si);
				update("Skater(" + si.getId() + ")", null);
			}
		}

		queueUpdates();
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

	protected void queueUpdates() {
		update("Team", team);
		update("Name", name);
		update("Logo", logo);
		update("Color", alternateNames.get("operator"));

		for (String key : alternateNames.keySet())
			update("AlternateName(" + key + ")", alternateNames.get(key));

		for (String key : colors.keySet())
			update("Color(" + key + ")", colors.get(key));
	}

	public String              getTeam()           { return team; }
	public String              getName()           { return name; }
	public String              getLogo()           { return logo; }
	public Map<String, String> getAlternateNames() { return alternateNames; }
	public Map<String, String> getColors()         { return colors; }
	public List<SkaterInfo>    getSkaters()        { return skaters; }

	private String team;
	private String name;
	private String logo;
	private Map<String, String> alternateNames = new Hashtable<String, String>();
	private Map<String, String> colors = new Hashtable<String, String>();
	private List<SkaterInfo> skaters = new ArrayList<SkaterInfo>();
}
