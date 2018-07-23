package com.carolinarollergirls.scoreboard.game;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
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
	}

	public JSONObject toJSON() throws JSONException {
		Team t = game.getTeam(team);
		JSONObject json = new JSONObject();
		json.put("team", team);
		json.put("name", t.getName());
		json.put("logo", t.getLogo());
		Team.AlternateName operatorName = t.getAlternateName("operator");
		if (operatorName != null) {
			json.put("color", operatorName.getName());
		}

		JSONObject an = new JSONObject();
		for (Team.AlternateName i : t.getAlternateNames()) {
			an.put(i.getId(), i.getName());
    }
		json.put("alternateNames", an);

		JSONObject c = new JSONObject();
		for (Team.Color i : t.getColors()) {
			c.put(i.getId(), i.getColor());
    }
		json.put("colors", c);


		JSONArray s = new JSONArray();
		for (Skater si : t.getSkaters())
			s.put(new SkaterInfo(game, this, si).toJSON());
		json.put("skaters", s);

		return json;
	}

	protected void queueUpdates() {
	}

	private String team;
}
