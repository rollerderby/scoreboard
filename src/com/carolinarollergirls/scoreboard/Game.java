package com.carolinarollergirls.scoreboard;

import io.prometheus.client.Histogram;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.carolinarollergirls.scoreboard.game.JamStats;
import com.carolinarollergirls.scoreboard.game.PeriodStats;
import com.carolinarollergirls.scoreboard.game.TeamInfo;
import com.carolinarollergirls.scoreboard.jetty.WS;

public class Game {
	public Game(ScoreBoard sb) {
		this.logging = false;
		this.sb = sb;
	}
	
	public void start(String i) {
		updateState();
		update("Game", null);
		updateState();

		identifier = i;
		teams = new TeamInfo[2];
		teams[0] = new TeamInfo(this, Team.ID_1);
		teams[1] = new TeamInfo(this, Team.ID_2);
		periods = new ArrayList<PeriodStats>();
		logging = true;

		Thread t = new Thread(new SaveThread());
		t.start();

		updateState();
	}

	public void stop() {
		synchronized (saveLock) {
			identifier = "";
			teams = null;
			periods = null;
			logging = false;
			saveLock.notifyAll();
		}
	}

	private JamStats findJamStats(int period, int jam, boolean truncateAfter) {
		while (periods.size() < period) {
			periods.add(new PeriodStats(this, periods.size() + 1));
		}
		PeriodStats ps = periods.get(period - 1);

		while (truncateAfter && periods.size() > period) {
			periods.remove(periods.get(periods.size() - 1));
		}

		return ps.getJamStats(jam, truncateAfter);
	}

	public void snapshot(boolean jamEnd) {
		if (!logging || sb == null)
			return;

		synchronized (saveLock) {
			try {
				teams[0].snapshot();
				teams[1].snapshot();

				int period = sb.getClock(Clock.ID_PERIOD).getNumber();
				int jam = sb.getClock(Clock.ID_JAM).getNumber();

				JamStats js = findJamStats(period, jam, true);
				js.snapshot(jamEnd);

				if (jamEnd) { // only write the data to file once per jam to combat lag from writing it multiple times over at the end of each jam
					saveLock.notifyAll();
				}
			} catch (Exception e) {
				ScoreBoardManager.printMessage("Error catching snapshot: " + e.getMessage());
				e.printStackTrace();
			}
		}

		updateState();
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("identifier", identifier);

		JSONArray t = new JSONArray();
		t.put(teams[0].toJSON());
		t.put(teams[1].toJSON());
		json.put("teams", t);

		JSONArray p = new JSONArray();
		for (PeriodStats ps : periods) {
			p.put(ps.toJSON());
		}
		json.put("periods", p);

		return json;
	}

	public Team getTeam(String id) {
		if (sb == null)
			return null;
		return sb.getTeam(id);
	}

	public Clock getClock(String id) {
		if (sb == null)
			return null;
		return sb.getClock(id);
	}

	public String getUpdaterBase() { return "Game"; }

	private class SaveThread implements Runnable {
		public void run() {
			save();
			while (true) {
				synchronized (saveLock) {
					try { saveLock.wait(); }
					catch ( Exception e ) { }

					if (!logging)
						return;

					save();
				}
			}
		}

		public void save() {
			synchronized (saveLock) {
				File file = new File(new File(ScoreBoardManager.getDefaultPath(), "GameData"), identifier.replaceAll("[^a-zA-Z0-9\\.\\-]", "_") + ".json");
				file.getParentFile().mkdirs();
				FileWriter out = null;
				Histogram.Timer timer = saveDuration.startTimer();
				try {
					out = new FileWriter(file);
					out.write(toJSON().toString(2));
				} catch (Exception e) {
					ScoreBoardManager.printMessage("Error saving game data: " + e.getMessage());
					e.printStackTrace();
				} finally {
					if (out != null) {
						try { out.close(); } catch (Exception e) { }
					}
					timer.observeDuration();
				}
			}
		}
	}

	public void update(String key, Object value) {
		synchronized (updateMap) {
			updateMap.put(key, value);
		}
	}

	public void updateState() {
		synchronized (updateMap) {
			if (updateMap.size() == 0)
				return;
			WS.updateState(updateMap);
			updateMap.clear();
		}
	}

	private LinkedHashMap<String, Object> updateMap = new LinkedHashMap<String, Object>();
	protected ScoreBoard sb = null;
	private TeamInfo[] teams = null;
	private ArrayList<PeriodStats> periods = null;
	private boolean logging = false;
	private String identifier = "";
	private Object saveLock = new Object();
	private static final Histogram saveDuration = Histogram.build()
	  .name("crg_game_save_duration_seconds").help("Time spent saving JSON to disk").register();
}
