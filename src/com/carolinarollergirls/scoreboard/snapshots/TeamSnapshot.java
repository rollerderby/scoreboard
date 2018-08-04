package com.carolinarollergirls.scoreboard.snapshots;

import java.util.HashMap;

import com.carolinarollergirls.scoreboard.model.SkaterModel;
import com.carolinarollergirls.scoreboard.model.TeamModel;

public class TeamSnapshot {
	public TeamSnapshot(TeamModel team) {
		id = team.getId();
		score = team.getScore();
		lastscore = team.getLastScore();
		timeouts = team.getTimeouts();
		officialReviews = team.getOfficialReviews();
		leadJammer = team.getLeadJammer();
		starPass = team.isStarPass();
		in_timeout = team.inTimeout();
		in_official_review = team.inOfficialReview();
		skaterSnapshots = new HashMap<String, SkaterSnapshot>();
		for (SkaterModel skater : team.getSkaterModels()) {
			skaterSnapshots.put(skater.getId(), new SkaterSnapshot(skater));
		}
	}

	public String getId() { return id; }
	public int getScore() { return score;}
	public int getLastScore() { return lastscore; }
	public int getTimeouts() { return timeouts; }
	public int getOfficialReviews() { return officialReviews; }
	public String getLeadJammer() { return leadJammer; }
	public boolean getStarPass() { return starPass; }
	public boolean inTimeout() { return in_timeout; }
	public boolean inOfficialReview() { return in_official_review; }
	public HashMap<String, SkaterSnapshot> getSkaterSnapshots() { return skaterSnapshots; }
	public SkaterSnapshot getSkaterSnapshot(String skater) { return skaterSnapshots.get(skater); }
	
	protected String id;
	protected int score;
	protected int lastscore;
	protected int timeouts;
	protected int officialReviews;
	protected String leadJammer;
	protected boolean starPass;
	protected boolean in_timeout;
	protected boolean in_official_review;
	protected HashMap<String, SkaterSnapshot> skaterSnapshots;
}
