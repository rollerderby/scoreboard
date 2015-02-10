package com.carolinarollergirls.scoreboard.game;

import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

import com.carolinarollergirls.scoreboard.Game;

public class PeriodStats {
	public PeriodStats(Game g, long p) {
		game = g;
		period = p;
		jams = new ArrayList<JamStats>();
	}

	public JamStats AddJam(long jam) {
		JamStats js = new JamStats(game, jam);
		jams.add(js);

		return js;
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("period", period);
		JSONArray j = new JSONArray();
		for (JamStats js : jams) {
			j.put(js.toJSON());
		}
		json.put("jams", j);

		return json;
	}

	public long getPeriod() { return period; }

	public JamStats getJamStats(long jam) {
		for (JamStats js1 : jams) {
			if (js1.getJam() == jam) {
				return js1;
			}
		}
		return AddJam(jam);
	}


	private Game game;
	private long period;
	private ArrayList<JamStats> jams;
}
