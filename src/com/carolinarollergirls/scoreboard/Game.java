package com.carolinarollergirls.scoreboard;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.carolinarollergirls.scoreboard.game.*;

public class Game {
	public Game() {
		this.logging = false;
		this.sb = null;

		Thread t = new Thread(new SaveThread());
		t.start();
	}

	public void setScoreBoard(ScoreBoard sb) {
		this.sb = sb;
	}
	
	public void start(String i) {
		identifier = i;
		teams = new TeamInfo[2];
		teams[0] = new TeamInfo(this, Team.ID_1);
		teams[1] = new TeamInfo(this, Team.ID_2);
		periods = new ArrayList<PeriodStats>();
		logging = true;
	}

	public void stop() {
		this.identifier = "";
		this.teams = null;
		this.periods = null;
		this.logging = false;
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

	private void saveFile() {
		synchronized (saveLock) {
			saveLock.notifyAll();
		}
	}

	private class SaveThread implements Runnable {
		public void run() {
			while (true) {
				synchronized (saveLock) {
					try { saveLock.wait(); }
					catch ( Exception e ) { }

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
	}

	protected ScoreBoard sb = null;
	private TeamInfo[] teams = null;
	private ArrayList<PeriodStats> periods = null;
	private boolean logging = false;
	private String identifier = "";
	private Object saveLock = new Object();
}
