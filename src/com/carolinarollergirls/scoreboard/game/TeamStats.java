package com.carolinarollergirls.scoreboard.game;

import java.util.ArrayList;
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

	public void snapshot(boolean getSkaters) {
		Team t = game.getTeam(team);
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
		json.put("isStarPass", isStarPass);
		JSONArray s = new JSONArray();
		if (skaters != null)
			for (SkaterStats ss : skaters)
				s.put(ss.toJSON());
		json.put("skaters", s);

		return json;
	}

	private Game game;
	private String team;
	private long totalScore;
	private long jamScore;
	private long timeouts;
	private long officialReviews;
	private String leadJammer;
	private boolean isStarPass;
	private ArrayList<SkaterStats> skaters;
}
