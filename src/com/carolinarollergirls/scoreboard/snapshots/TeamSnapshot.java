package com.carolinarollergirls.scoreboard.snapshots;

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
		in_jam = team.inJam();
		in_timeout = team.inTimeout();
		in_official_review = team.inOfficialReview();
	}

	public String getId() { return id; }
	public int getScore() { return score;}
	public int getLastScore() { return lastscore; }
	public int getTimeouts() { return timeouts; }
	public int getOfficialReviews() { return officialReviews; }
	public String getLeadJammer() { return leadJammer; }
	public boolean getStarPass() { return starPass; }
	public boolean inJam() { return in_jam; }
	public boolean inTimeout() { return in_timeout; }
	public boolean inOfficialReview() { return in_official_review; }
	
	protected String id;
	protected int score;
	protected int lastscore;
	protected int timeouts;
	protected int officialReviews;
	protected String leadJammer;
	protected boolean starPass;
	protected boolean in_jam;
	protected boolean in_timeout;
	protected boolean in_official_review;
}
