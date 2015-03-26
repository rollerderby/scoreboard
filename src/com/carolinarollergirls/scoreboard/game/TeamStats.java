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

public class TeamStats {
	public TeamStats(Game g, String t) {
		game = g;
		team = t;
	}

	public void snapshot(Map<String, Object> stateUpdates, String statePrefix, boolean getSkaters) {
		Team t = game.getTeam(team);
		totalScore = t.getScore();
		jamScore = t.getScore() - t.getLastScore();
		timeouts = t.getTimeouts();
		officialReviews = t.getOfficialReviews();
		leadJammer = t.getLeadJammer();
		starPass = t.isStarPass();

		stateUpdates.put(statePrefix + ".Team(" + team + ").TotalScore", totalScore);
		stateUpdates.put(statePrefix + ".Team(" + team + ").JamScore", jamScore);
		stateUpdates.put(statePrefix + ".Team(" + team + ").Timeouts", timeouts);
		stateUpdates.put(statePrefix + ".Team(" + team + ").OfficialReviews", officialReviews);
		stateUpdates.put(statePrefix + ".Team(" + team + ").LeadJammer", leadJammer);
		stateUpdates.put(statePrefix + ".Team(" + team + ").StarPass", starPass);

		if (getSkaters) {
			skaters = new ArrayList<SkaterStats>();

			for (Position p : t.getPositions()) {
				String pos = p.getId();
				if (Position.FLOOR_POSITIONS.contains(pos)) {
					Skater s = p.getSkater();
					if (s != null)
						skaters.add(new SkaterStats(game, p.getSkater()));
				}
			}
		}
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

	public Game                   getGame()            { return game; }
	public String                 getTeam()            { return team; }
	public long                   getTotalScore()      { return totalScore; }
	public long                   getJamScore()        { return jamScore; }
	public long                   getTimeouts()        { return timeouts; }
	public long                   getOfficialReviews() { return officialReviews; }
	public String                 getLeadJammer()      { return leadJammer; }
	public boolean                getStarPass()        { return starPass; }
	public ArrayList<SkaterStats> getSkaters()         { return skaters; }

	private Game game;
	private String team;
	private long totalScore;
	private long jamScore;
	private long timeouts;
	private long officialReviews;
	private String leadJammer;
	private boolean starPass;
	private ArrayList<SkaterStats> skaters;
}
