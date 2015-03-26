package com.carolinarollergirls.scoreboard.game;

import java.util.LinkedList;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.carolinarollergirls.scoreboard.Game;
import com.carolinarollergirls.scoreboard.Skater;

public class SkaterInfo {
	public SkaterInfo(Game g, TeamInfo ti, Skater s) {
		game = g;
		teamInfo = ti;
		id = s.getId();
		name = s.getName();
		number = s.getNumber();
		penalties = new LinkedList<Penalty>();
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("id", id);
		json.put("name", name);
		json.put("number", number);

		JSONArray pa = new JSONArray();
		for (Penalty p : penalties) {
			pa.put(p.toJSON());
		}
		json.put("penalties", pa);

		return json;
	}

	public boolean newPenalty(int p, int j, String c) {
		if (penalties.size() > 9)
			return false;

		penalties.add(new Penalty(p, j, c));
		return true;
	}

	public Game     getGame()     { return game; }
	public TeamInfo getTeamInfo() { return teamInfo; }
	public String   getId()       { return id; }
	public String   getName()     { return name; }
	public String   getNumber()   { return number; }

	private Game game;
	private TeamInfo teamInfo;
	private String id;
	private String name;
	private String number;
	private LinkedList<Penalty> penalties;

	public class Penalty {
		public Penalty(int p, int j, String c) {
			id = UUID.randomUUID().toString();
			period = p;
			jam = j;
			code = c;
		}

		public JSONObject toJSON() throws JSONException {
			JSONObject json = new JSONObject();
			json.put("id", id);
			json.put("period", period);
			json.put("jam", jam);
			json.put("code", code);

			return json;
		}

		public void update(int p, int j, String c) {
			period = p;
			jam = j;
			code = c;
		}

		public String getId() { return id; }
		public int getPeriod() { return period; }
		public int getJam() { return jam; }
		public String getCode() { return code; }

		private String id;
		private int period;
		private int jam;
		private String code;
	}
}
