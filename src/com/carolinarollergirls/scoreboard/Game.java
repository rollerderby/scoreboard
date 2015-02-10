package com.carolinarollergirls.scoreboard;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

import com.carolinarollergirls.scoreboard.model.*;

public class Game {
	protected class PeriodStats {
		public PeriodStats(long period) {
			this.period = period;
			this.jams = new ArrayList<JamStats>();
		}

		public JamStats AddJam(long jam) {
			JamStats js = new JamStats(jam);
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

		protected long period;
		protected ArrayList<JamStats> jams;
	}

	protected class JamStats {
		public JamStats(long jam) {
			this.jam = jam;
			this.teams = new TeamStats[2];
			this.teams[0] = new TeamStats(Team.ID_1);
			this.teams[1] = new TeamStats(Team.ID_2);
		}

		public void snapshot() {
			Clock periodClock = sb.getClock(Clock.ID_PERIOD);
			Clock jamClock = sb.getClock(Clock.ID_JAM);
			boolean getSkaters = jamClock.isRunning();

			this.periodClock = periodClock.getTime();
			this.jamClock = jamClock.getTime();
			for (TeamStats ts : teams)
				ts.snapshot(getSkaters);
		}

		public JSONObject toJSON() {
			JSONObject json = new JSONObject();
			json.put("jam", jam);
			json.put("periodClock", periodClock);
			json.put("jamClock", jamClock);
			JSONArray t = new JSONArray();
			for (TeamStats ts : teams)
				t.put(ts.toJSON());
			json.put("teams", t);

			return json;
		}

		protected long jam;
		protected TeamStats[] teams;
		protected long periodClock;
		protected long jamClock;
	}

	protected class TeamStats {
		public TeamStats(String team) {
			this.team = team;
		}

		public void snapshot(boolean getSkaters) {
			Team t = sb.getTeam(team);
			totalScore = t.getScore();
			jamScore = t.getScore() - t.getLastScore();
			timeouts = t.getTimeouts();
			officialReviews = t.getOfficialReviews();
			leadJammer = t.getLeadJammer();
			isStarPass = t.isStarPass();

			if (getSkaters) {
				skaters = new ArrayList<SkaterStats>();

				for (Position p : t.getPositions()) {
					String pos = p.getId();
					if (Position.FLOOR_POSITIONS.contains(pos)) {
						Skater s = p.getSkater();
						if (s != null)
							skaters.add(new SkaterStats(p.getSkater()));
					}
				}
			}
		}

		public JSONObject toJSON() {
			JSONObject json = new JSONObject();
			json.put("team", team);
			json.put("jamScore", jamScore);
			json.put("totalScore", totalScore);
			json.put("timeouts", timeouts);
			json.put("officialReviews", officialReviews);
			json.put("leadJammer", leadJammer);
			json.put("isStarPass", isStarPass);
			JSONArray s = new JSONArray();
			for (SkaterStats ss : skaters)
				s.put(ss.toJSON());
			json.put("skaters", s);

			return json;
		}

		protected String team;
		protected long totalScore;
		protected long jamScore;
		protected long timeouts;
		protected long officialReviews;
		protected String leadJammer;
		protected boolean isStarPass;
		protected ArrayList<SkaterStats> skaters;
	}

	protected class SkaterStats {
		public SkaterStats(Skater s) {
			name = s.getName();
			number = s.getNumber();
			position = s.getPosition();
			isPenaltyBox = s.isPenaltyBox();
		}

		public JSONObject toJSON() {
			JSONObject json = new JSONObject();
			json.put("name", name);
			json.put("number", number);
			json.put("position", position);
			json.put("isPenaltyBox", isPenaltyBox);

			return json;
		}

		protected String name;
		protected String number;
		protected String position;
		protected boolean isPenaltyBox;
	}

	public Game() {
		this.logging = false;
		this.sb = null;
	}

	public void setScoreBoard(ScoreBoard sb) {
		this.sb = sb;
	}
	
	public void start(String identifier) {
		this.identifier = identifier;
		this.periods = new ArrayList<PeriodStats>();
		this.logging = true;
	}

	public void stop() {
		this.identifier = "";
		this.periods = null;
		this.logging = false;
	}

	private JamStats findJamStats(long period, long jam) {
		PeriodStats ps = null;
		for (PeriodStats ps1 : periods) {
			if (ps1.period == period) {
				ps = ps1;
				break;
			}
		}
		if (ps == null) {
			ps = new PeriodStats(period);
			periods.add(ps);
		}

		for (JamStats js1 : ps.jams) {
			if (js1.jam == jam) {
				return js1;
			}
		}
		return ps.AddJam(jam);
	}

	public void snapshot() {
		if (!logging || sb == null)
			return;

		try {
			long period = sb.getClock(Clock.ID_PERIOD).getNumber();
			long jam = sb.getClock(Clock.ID_JAM).getNumber();

			ScoreBoardManager.printMessage("Looking for period: " + period + "  jam: " + jam);
			JamStats js = findJamStats(period, jam);
			ScoreBoardManager.printMessage("  found js: " + js);

			js.snapshot();

			saveFile();
		} catch (Exception e) {
			ScoreBoardManager.printMessage("Error catching snapshot: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("identifier", identifier);
		JSONArray p = new JSONArray();
		for (PeriodStats ps : periods) {
			p.put(ps.toJSON());
		}
		json.put("periods", p);

		return json;
	}

	private void saveFile() {
		File file = new File(new File(ScoreBoardManager.getDefaultPath(), "GameData"), identifier + ".json");
		file.getParentFile().mkdirs();
		FileWriter out = null;
		try {
			out = new FileWriter(file);
			out.write(this.toJSON().toString(2));
		} catch (Exception e) {
		} finally {
			if (out != null) {
				try { out.close(); } catch (Exception e) { }
			}
		}
	}

	protected ScoreBoard sb = null;
	protected ArrayList<PeriodStats> periods;
	protected boolean logging = false;
	protected String identifier = "";
}
