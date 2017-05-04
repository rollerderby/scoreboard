package com.carolinarollergirls.scoreboard.game;

import java.util.ArrayList;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.carolinarollergirls.scoreboard.Game;
import com.carolinarollergirls.scoreboard.Position;
import com.carolinarollergirls.scoreboard.Skater;
import com.carolinarollergirls.scoreboard.Team;

public class TeamStats extends Updater {
	public TeamStats(Game g, JamStats js, String t) {
		super(g);
		jam_stats = js;
		team = t;
	}

	public String getUpdaterBase() {
		return jam_stats.getUpdaterBase() + ".Team(" + team + ")";
	}

	public void snapshot(boolean jamInProgress, boolean getSkaters) {
		Team t = game.getTeam(team);
		totalScore = t.getScore();
		jamScore = t.getScore() - t.getLastScore();
		timeouts = t.getTimeouts();
		officialReviews = t.getOfficialReviews();
		if (jamInProgress) {
			leadJammer = t.getLeadJammer();
			starPass = t.isStarPass();
		}

		if (getSkaters) {
			skaters = new ArrayList<SkaterStats>();
			for (String p : Position.FLOOR_POSITIONS) {
				update("Skater(" + p + ")", null);
			}

			for (Position p : t.getPositions()) {
				String pos = p.getId();
				if (Position.FLOOR_POSITIONS.contains(pos)) {
					Skater s = p.getSkater();
					if (s != null)
						skaters.add(new SkaterStats(game, this, p.getSkater()));
				}
			}
		}
		queueUpdates();
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("team", team);
		json.put("jamScore", jamScore);
		json.put("totalScore", totalScore);
		json.put("timeouts", timeouts);
		json.put("officialReviews", officialReviews);
		json.put("leadJammer", leadJammer);
		json.put("starPass", starPass);
		JSONArray s = new JSONArray();
		if (skaters != null)
			for (SkaterStats ss : skaters)
				s.put(ss.toJSON());
		json.put("skaters", s);

		return json;
	}

	protected void queueUpdates() {
		update("Team", team);
		update("JamScore", jamScore);
		update("TotalScore", totalScore);
		update("Timeouts", timeouts);
		update("OfficialReviews", officialReviews);
		update("LeadJammer", leadJammer);
		update("StarPass", starPass);
	}

	public JamStats               getJamStats()        { return jam_stats; }
	public String                 getTeam()            { return team; }
	public long                   getTotalScore()      { return totalScore; }
	public long                   getJamScore()        { return jamScore; }
	public long                   getTimeouts()        { return timeouts; }
	public long                   getOfficialReviews() { return officialReviews; }
	public String                 getLeadJammer()      { return leadJammer; }
	public boolean                getStarPass()        { return starPass; }
	public ArrayList<SkaterStats> getSkaters()         { return skaters; }

	private JamStats jam_stats;
	private String team;
	private long totalScore;
	private long jamScore;
	private long timeouts;
	private long officialReviews;
	private String leadJammer;
	private boolean starPass;
	private ArrayList<SkaterStats> skaters;
}
