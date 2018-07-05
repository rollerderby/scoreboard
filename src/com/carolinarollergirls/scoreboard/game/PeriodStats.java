package com.carolinarollergirls.scoreboard.game;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.carolinarollergirls.scoreboard.Game;

public class PeriodStats extends Updater {
	public PeriodStats(Game g, int p) {
		super(g);
		period = p;
		jams = new ArrayList<JamStats>();
	}

	public String getUpdaterBase() {
		return game.getUpdaterBase() + ".Period(" + period + ")";
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("period", period);
		JSONArray j = new JSONArray();
		for (JamStats js : jams) {
			j.put(js.toJSON());
		}
		json.put("jams", j);

		return json;
	}

	protected void queueUpdates() {
	}

	public JamStats getJamStats(int jam, boolean truncateAfter) {
		while (jams.size() < jam) {
			jams.add(new JamStats(game, this, jams.size() + 1));
		}

		while (truncateAfter && jams.size() > jam) {
			jams.remove(jams.get(jams.size() - 1));
		}
		return jams.get(jam - 1);
	}

	public int getPeriod()               { return period; }
	public ArrayList<JamStats> getJams() { return jams; }

	private int period;
	private ArrayList<JamStats> jams;
}
