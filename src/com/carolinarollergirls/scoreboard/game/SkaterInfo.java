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

		queueUpdates();
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
		if (fo_exp != null)
			json.put("fo_exp", fo_exp.toJSON());

		return json;
	}

	private void queueUpdates() {
		update("Id", id);
		update("Name", name);
		update("Number", number);

		for (int i = 0; i < 9; i++) {
			String base = "Penalty(" + (i + 1) + ")";
			if (i < penalties.size())
				penalties.get(i).queueUpdates(getUpdaterBase() + "." + base);
			else
				update(base, null);
		}
		if (fo_exp != null)
			fo_exp.queueUpdates(getUpdaterBase() + ".Penalty(FO_EXP)");
		else
			update("Penalty(FO_EXP)", null);
	}

	public void Penalty(String penaltyId, boolean fe, int p, int j, String c) {
		if (penaltyId == null || (fe && c != null)) {
			// New Penalty (Or FO/Exp, just overwrite it)
			newPenalty(fe, p, j, c);
		} else if (fe) {
			// Must be a delete request
			fo_exp = null;
		} else {
			// Updating/Deleting existing Penalty.  Find it and process
			for (Penalty p2 : penalties) {
				if (p2.getId().equals(penaltyId)) {
					if (c != null) {
						p2.period = p;
						p2.jam = j;
						p2.code = c;
					} else {
						penalties.remove(p2);
					}
					break;
				}
			}
		}
		queueUpdates();
		updateState();
	}

	private void newPenalty(boolean fe, int p, int j, String c) {
		if (fe) {
			fo_exp = new Penalty(game, p, j, c);
		} else {
			// Non FO/Exp, make sure skater has 9 or less regular penalties before adding another
			if (penalties.size() < 9) {
				penalties.add(new Penalty(game, p, j, c));
			}
		}
	}

	public TeamInfo      getTeamInfo()  { return team_info; }
	public String        getId()        { return id; }
	public String        getName()      { return name; }
	public String        getNumber()    { return number; }
	public List<Penalty> getPenalties() { return penalties; }
	public Penalty       getFOExp()     { return fo_exp; }

	private TeamInfo team_info;
	private String id;
	private String name;
	private String number;
	private List<Penalty> penalties;
	private Penalty fo_exp;

	public class Penalty extends Updater {
		public Penalty(Game g, int p, int j, String c) {
			super(g);
			id = UUID.randomUUID().toString();
			period = p;
			jam = j;
			code = c;
		}

		public void queueUpdates(String base) {
			this.base = base;
			update("Id", id);
			update("Period", period);
			update("Jam", jam);
			update("Code", code);
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
