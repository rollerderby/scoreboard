package com.carolinarollergirls.scoreboard.states;

public class TeamState {
	public TeamState(String id, int score, int lastscore, int timeouts,
			int officialReviews, String leadJammer, boolean starPass,
			boolean in_jam,	boolean in_timeout, boolean in_official_review) {
		this.id = id;
		this.score = score;
		this.lastscore = lastscore;
		this.timeouts = timeouts;
		this.officialReviews = officialReviews;
		this.leadJammer = leadJammer;
		this.starPass = starPass;
		this.in_jam = in_jam;
		this.in_timeout = in_timeout;
		this.in_official_review = in_official_review;
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
