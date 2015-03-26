package com.carolinarollergirls.scoreboard;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.carolinarollergirls.scoreboard.game.*;
import com.carolinarollergirls.scoreboard.jetty.WS;

public class Game {
	public Game(ScoreBoard sb) {
		this.logging = false;
		this.sb = sb;
	}
	
	public void start(String i) {
		identifier = i;
		teams = new TeamInfo[2];
		teams[0] = new TeamInfo(this, Team.ID_1);
		teams[1] = new TeamInfo(this, Team.ID_2);
		periods = new ArrayList<PeriodStats>();
		logging = true;

		Thread t = new Thread(new SaveThread());
		t.start();
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
				LinkedHashMap<String, Object> stateUpdates = new LinkedHashMap<String, Object>();
				teams[0].snapshot(stateUpdates, "Game");
				teams[1].snapshot(stateUpdates, "Game");

				int period = sb.getClock(Clock.ID_PERIOD).getNumber();
				int jam = sb.getClock(Clock.ID_JAM).getNumber();

				JamStats js = findJamStats(period, jam, true);
				js.snapshot(stateUpdates, "Game.Period(" + period + ")", jamEnd);

				saveLock.notifyAll();
				for (String k : stateUpdates.keySet()) {
					Object v = stateUpdates.get(k);
					String c = v == null ? "NULL" : v.getClass().getName();
					ScoreBoardManager.printMessage("k: " + k + "  v: " + v + "  " + c);
				}
				WS.updateState(stateUpdates);
			} catch (Exception e) {
				ScoreBoardManager.printMessage("Error catching snapshot: " + e.getMessage());
				e.printStackTrace();
			}
		}
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

	public SkaterInfo getSkater(String id) {
		for (TeamInfo t : teams) {
			for (SkaterInfo s : t.getSkaters()) {
				if (s.getId().equals(id)) {
					return s;
				}
			}
		}
		return null;
	}

	private void saveFile() {
		synchronized (saveLock) {
			saveLock.notifyAll();
		}
	}

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
				File file = new File(new File(ScoreBoardManager.getDefaultPath(), "GameData"), identifier + ".json");
				file.getParentFile().mkdirs();
				FileWriter out = null;
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
				}
			}
		}
	}

	protected ScoreBoard sb = null;
	private TeamInfo[] teams = null;
	private ArrayList<PeriodStats> periods = null;
	private boolean logging = false;
	private String identifier = "";
	private Object saveLock = new Object();
}
