package com.carolinarollergirls.scoreboard.game;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.carolinarollergirls.scoreboard.Game;
import com.carolinarollergirls.scoreboard.Skater;

public class SkaterInfo extends Updater {
	public SkaterInfo(Game g, TeamInfo ti, Skater s) {
		super(g);
		team_info = ti;
		id = s.getId();
		penalties = new LinkedList<Penalty>();
		snapshot(s);
	}

	public String getUpdaterBase() {
		return team_info.getUpdaterBase() + ".Skater(" + id + ")";
	}

	public void snapshot(Skater s) {
		name = s.getName();
		number = s.getNumber();
		flags = s.getFlags();

    penalties.clear();
    for (Skater.Penalty p : s.getPenalties()) {
      penalties.add(new Penalty(game, p.getId(), p.getPeriod(), p.getJam(), p.getCode()));
    }
    fo_exp = null;
    Skater.Penalty fe = s.getFOEXPPenalty();
    if (fe != null) {
      fo_exp = new Penalty(game, fe.getId(), fe.getPeriod(), fe.getJam(), fe.getCode());
    }
  }

	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("id", id);
		json.put("name", name);
		json.put("number", number);
		json.put("flags", flags);

		JSONArray pa = new JSONArray();
		for (Penalty p : penalties) {
			pa.put(p.toJSON());
		}
		json.put("penalties", pa);
		if (fo_exp != null)
			json.put("fo_exp", fo_exp.toJSON());

		return json;
	}

	private void queueUpdates() {
	}

	public TeamInfo      getTeamInfo()  { return team_info; }
	public String        getId()        { return id; }
	public String        getName()      { return name; }
	public String        getNumber()    { return number; }
	public String        getFlags()     { return flags; }
	public List<Penalty> getPenalties() { return penalties; }
	public Penalty       getFOExp()     { return fo_exp; }

	private TeamInfo team_info;
	private String id;
	private String name;
	private String number;
	private String flags;
	private List<Penalty> penalties;
	private Penalty fo_exp;

	public class Penalty extends Updater {
		public Penalty(Game g, String i, int p, int j, String c) {
			super(g);
			id = i;
			period = p;
			jam = j;
			code = c;
		}

		public void queueUpdates(String base) {
		}

		public String getUpdaterBase() {
			return base;
		}

		public JSONObject toJSON() throws JSONException {
			JSONObject json = new JSONObject();
			json.put("id", id);
			json.put("period", period);
			json.put("jam", jam);
			json.put("code", code);

			return json;
		}

		public String getId() { return id; }
		public int getPeriod() { return period; }
		public int getJam() { return jam; }
		public String getCode() { return code; }

		private String id;
		private int period;
		private int jam;
		private String code;
		private String base;
	}
}
