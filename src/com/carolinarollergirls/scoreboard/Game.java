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

				saveLock.notifyAll();
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

	public SkaterInfo getSkater(String teamId, String skaterId) {
		for (TeamInfo t : teams) {
			if (t.getTeam().equals(teamId)) {
				for (SkaterInfo s : t.getSkaters()) {
					if (s.getId().equals(skaterId)) {
						return s;
					}
				}
			}
		}
		return null;
	}

	public void Penalty(String teamId, String skaterId, String penaltyId, boolean fo_exp, int period, int jam, String code) {
		synchronized (saveLock) {
			SkaterInfo si = getSkater(teamId, skaterId);
			if (si == null)
				return;

			si.Penalty(penaltyId, fo_exp, period, jam, code);
			saveLock.notifyAll();
		}
	}

	public String getUpdaterBase() { return "Game"; }

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
}
