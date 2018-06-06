package com.carolinarollergirls.scoreboard.defaults;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;
import java.util.concurrent.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.policy.*;

public class DefaultTeamModel extends DefaultScoreBoardEventProvider implements TeamModel, Ruleset.RulesetReceiver
{
	public DefaultTeamModel(ScoreBoardModel sbm, String i) {
		Iterator<String> posIds = Position.FLOOR_POSITIONS.iterator();
		while (posIds.hasNext()) {
			String id = posIds.next();
			PositionModel pM = new DefaultPositionModel(this, id);
			positions.put(id, pM);
			pM.addScoreBoardListener(this);
		}

		scoreBoardModel = sbm;
		id = i;

		// Register for default values from the rulesets
		Ruleset.registerRule(this, "Team." + id + ".Name");
		Ruleset.registerRule(this, "Team.Timeouts");
		Ruleset.registerRule(this, "Team.OfficialReviews");

		reset();
	}

	public void applyRule(String rule, Object value) {
		if (rule.equals("Team.Timeouts"))
			setTimeouts((Integer)value);
		else if (rule.equals("Team.OfficialReviews"))
			setOfficialReviews((Integer)value);
		else if (rule.equals("Team." + id + ".Name"))
			setName((String)value);
	}

	public String getProviderName() { return "Team"; }
	public Class getProviderClass() { return Team.class; }
	public String getProviderId() { return getId(); }

	public ScoreBoard getScoreBoard() { return scoreBoardModel.getScoreBoard(); }
	public ScoreBoardModel getScoreBoardModel() { return scoreBoardModel; }

	public void reset() {
		// Get default values from active ruleset
		getScoreBoard()._getRuleset().apply(true, this);

		setLogo(DEFAULT_LOGO);
		setScore(DEFAULT_SCORE);
		setLastScore(DEFAULT_SCORE);
		_setLeadJammer(DEFAULT_LEADJAMMER);
		_setStarPass(DEFAULT_STARPASS);

		setInTimeout(false);
		setInOfficialReview(false);
		setRetainedOfficialReview(false);

		removeAlternateNameModels();
		removeColorModels();
		Iterator<PositionModel> p = getPositionModels().iterator();
		Iterator<SkaterModel> s = getSkaterModels().iterator();
		while (s.hasNext()) {
			try { removeSkaterModel(s.next().getId()); }
			catch ( SkaterNotFoundException snfE ) { }
		}
		while (p.hasNext())
			p.next().reset();
	}

	public String getId() { return id; }

	public Team getTeam() { return this; }

	public String getName() { return name; }
	public void setName(String n) {
		synchronized (nameLock) {
			String last = name;
			name = n;
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_NAME, name, last));
		}
	}

	public void startJam() {
		synchronized (scoreLock) {
			saved_lastscore = getLastScore();
			setLastScore(getScore());
		}
	}
	public void unStartJam() {
		setLastScore(saved_lastscore);
	}

	public void stopJam() {
		requestBatchStart();

		benchSkaters();
		saved_leadJammer = leadJammer;
		saved_starPass = starPass;
		_setLeadJammer(Team.LEAD_NO_LEAD);
		_setStarPass(false);

		requestBatchEnd();
	}
	public void unStopJam() {
		requestBatchStart();
		unBenchSkaters();
		_setLeadJammer(saved_leadJammer);
		_setStarPass(saved_starPass);
		requestBatchEnd();
	}

	public void benchSkaters() {
		for (SkaterModel sM : skaters.values())
			sM.bench();
	}
	public void unBenchSkaters() {
		for (SkaterModel sM : skaters.values())
			sM.unBench();
	}

	public List<AlternateName> getAlternateNames() {
		synchronized (alternateNameLock) {
			return Collections.unmodifiableList(new ArrayList<AlternateName>(alternateNames.values()));
		}
	}
	public List<AlternateNameModel> getAlternateNameModels() {
		synchronized (alternateNameLock) {
			return Collections.unmodifiableList(new ArrayList<AlternateNameModel>(alternateNames.values()));
		}
	}
	public AlternateName getAlternateName(String i) { return getAlternateNameModel(i); }
	public AlternateNameModel getAlternateNameModel(String i) { return alternateNames.get(i); }
	public void setAlternateNameModel(String i, String n) {
		synchronized (alternateNameLock) {
			if (alternateNames.containsKey(i)) {
				alternateNames.get(i).setName(n);
			} else {
				AlternateNameModel anm = new DefaultAlternateNameModel(this, i, n);
				alternateNames.put(i, anm);
				anm.addScoreBoardListener(this);
				scoreBoardChange(new ScoreBoardEvent(this, EVENT_ADD_ALTERNATE_NAME, anm, null));
			}
		}
	}
	public void removeAlternateNameModel(String i) {
		synchronized (alternateNameLock) {
			AlternateNameModel anm = alternateNames.remove(i);
			anm.removeScoreBoardListener(this);
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_REMOVE_ALTERNATE_NAME, anm, null));
		}
	}
	public void removeAlternateNameModels() {
		synchronized (alternateNameLock) {
			Iterator<AlternateNameModel> i = getAlternateNameModels().iterator();
			while (i.hasNext())
				removeAlternateNameModel(i.next().getId());
		}
	}

	public List<Color> getColors() {
		synchronized (colorLock) {
			return Collections.unmodifiableList(new ArrayList<Color>(colors.values()));
		}
	}
	public List<ColorModel> getColorModels() {
		synchronized (colorLock) {
			return Collections.unmodifiableList(new ArrayList<ColorModel>(colors.values()));
		}
	}
	public Color getColor(String i) { return getColorModel(i); }
	public ColorModel getColorModel(String i) { return colors.get(i); }
	public void setColorModel(String i, String c) {
		synchronized (colorLock) {
			if (colors.containsKey(i)) {
				ColorModel cm = colors.get(i);
				cm.setColor(c);
			} else {
				ColorModel cm = new DefaultColorModel(this, i, c);
				colors.put(i, cm);
				cm.addScoreBoardListener(this);
				scoreBoardChange(new ScoreBoardEvent(this, EVENT_ADD_COLOR, cm, null));
			}
		}
	}
	public void removeColorModel(String i) {
		synchronized (colorLock) {
			ColorModel cm = colors.remove(i);
			cm.removeScoreBoardListener(this);
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_REMOVE_COLOR, cm, null));
		}
	}
	public void removeColorModels() {
		synchronized (colorLock) {
			Iterator<ColorModel> i = getColorModels().iterator();
			while (i.hasNext())
				removeColorModel(i.next().getId());
		}
	}

	public String getLogo() { return logo; }
	public void setLogo(String l) {
		synchronized (logoLock) {
			String last = logo;
			logo = l;
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_LOGO, logo, last));
		}
	}

	public void timeout() {
		if (getTimeouts() > 0) {
			changeTimeouts(-1);
			getScoreBoardModel().timeout(this);
		}
	}
	public void officialReview() {
		if (getOfficialReviews() > 0) {
			changeOfficialReviews(-1);
			getScoreBoardModel().timeout(this, true);
		}
	}

	public int getScore() { return score; }
	public void setScore(int s) {
		synchronized (scoreLock) {
			if (0 > s)
				s = 0;
			Integer last = new Integer(score);
			score = s;
			if (s < getLastScore())
				setLastScore(s);
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_SCORE, new Integer(score), last));
		}
	}
	public void changeScore(int c) {
		synchronized (scoreLock) {
			setScore(getScore() + c);
		}
	}

	public int getLastScore() { return lastscore; }
	public void setLastScore(int s) {
		synchronized (lastscoreLock) {
			if (0 > s)
				s = 0;
			if (getScore() < s)
				s = getScore();
			Integer last = new Integer(lastscore);
			lastscore = s;
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_LAST_SCORE, new Integer(lastscore), last));
		}
	}
	public void changeLastScore(int c) {
		synchronized (lastscoreLock) {
			setLastScore(getLastScore() + c);
		}
	}

	public boolean inTimeout() {
		synchronized (timeoutsLock) {
			return in_timeout;
		}
	}
	public void setInTimeout(boolean b) {
		synchronized (timeoutsLock) {
			if (b==in_timeout)
				return;
			Boolean last = new Boolean(in_timeout);
			in_timeout = b;
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_IN_TIMEOUT, new Boolean(b), last));
		}
	}

	public boolean inOfficialReview() {
		synchronized (timeoutsLock) {
			return in_official_review;
		}
	}
	public void setInOfficialReview(boolean b) {
		synchronized (timeoutsLock) {
			if (b==in_official_review)
				return;
			Boolean last = new Boolean(in_official_review);
			in_official_review = b;
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_IN_OFFICIAL_REVIEW, new Boolean(b), last));
		}
	}

	public boolean retainedOfficialReview() {
		synchronized (timeoutsLock) {
			return retained_official_review;
		}
	}
	public void setRetainedOfficialReview(boolean b) {
		synchronized (timeoutsLock) {
			if (b==retained_official_review)
				return;

			if (b && officialReviews == 0)
				setOfficialReviews(1);

			Boolean last = new Boolean(retained_official_review);
			retained_official_review = b;
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_RETAINED_OFFICIAL_REVIEW, new Boolean(b), last));
		}
	}

	public int getTimeouts() { return timeouts; }
//FIXME - add MinimumTimeouts and MaximumTimeouts instead of hardcoding 0 and 3
	public void setTimeouts(int t) {
		synchronized (timeoutsLock) {
			if (0 > t)
				t = 0;
			if (3 < t)
				t = 3;
			Integer last = new Integer(timeouts);
			timeouts = t;
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_TIMEOUTS, new Integer(timeouts), last));
		}
	}
	public void changeTimeouts(int c) {
		synchronized (timeoutsLock) {
			setTimeouts(getTimeouts() + c);
		}
	}
	public int getOfficialReviews() { return officialReviews; }
//FIXME - add MinimumOfficialReviews and MaximumOfficialReviews instead of hardcoding 0 and 1
	public void setOfficialReviews(int r) {
		synchronized (officialReviewsLock) {
			if (0 > r)
				r = 0;
			if (1 < r)
				r = 1;
			Integer last = new Integer(officialReviews);
			officialReviews = r;
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_OFFICIAL_REVIEWS, new Integer(officialReviews), last));
		}
	}
	public void changeOfficialReviews(int c) {
		synchronized (officialReviewsLock) {
			setOfficialReviews(getOfficialReviews() + c);
		}
	}

	public static Comparator<Skater> SkaterComparator = new Comparator<Skater>() {
		public int compare(Skater s1, Skater s2) {
			if (s2 == null)
				return 1;
			String n1 = s1.getNumber();
			String n2 = s2.getNumber();
			if (n1 == null) return -1;
			if (n2 == null) return 1;

			return n1.compareTo(n2);
		}
	};

	public List<SkaterModel> getSkaterModels() {
		synchronized (skaterLock) {
			ArrayList<SkaterModel> s = new ArrayList<SkaterModel>(skaters.values());
			Collections.sort(s, SkaterComparator);
			return Collections.unmodifiableList(s);
		}
	}
	public List<Skater> getSkaters() {
		synchronized (skaterLock) {
			ArrayList<Skater> s = new ArrayList<Skater>(skaters.values());
			Collections.sort(s, SkaterComparator);
			return Collections.unmodifiableList(s);
		}
	}
	public Skater getSkater(String id) throws SkaterNotFoundException { return getSkaterModel(id); }
	public SkaterModel getSkaterModel(String id) throws SkaterNotFoundException {
		synchronized (skaterLock) {
			if (skaters.containsKey(id))
				return skaters.get(id);
			else
				throw new SkaterNotFoundException(id);
		}
	}
	public SkaterModel addSkaterModel(String id) { return addSkaterModel(id, "", "", ""); }
	public SkaterModel addSkaterModel(String id, String n, String num, String flags) {
		SkaterModel sM = new DefaultSkaterModel(this, id, n, num, flags);
		addSkaterModel(sM);
		return sM;
	}
	public void addSkaterModel(SkaterModel skater) {
		synchronized (skaterLock) {
			if (null == skater.getId() || "".equals(skater.getId()) || skaters.containsKey(skater.getId()))
				return;

			skaters.put(skater.getId(), skater);
			skater.addScoreBoardListener(this);
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_ADD_SKATER, skater, null));
		}
	}
	public void removeSkaterModel(String id) throws SkaterNotFoundException {
		synchronized (skaterLock) {
			SkaterModel sm = getSkaterModel(id);
			try { getPositionModel(sm.getPosition()).clear(); }
			catch ( PositionNotFoundException pnfE ) { /* was on BENCH */ }
			sm.removeScoreBoardListener(this);
			skaters.remove(id);
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_REMOVE_SKATER, sm, null));
		}
	}

	public Position getPosition(String id) throws PositionNotFoundException { return getPositionModel(id); }
	public PositionModel getPositionModel(String id) throws PositionNotFoundException {
		if (positions.containsKey(id))
			return positions.get(id);
		else
			throw new PositionNotFoundException(id);
	}
	public List<Position> getPositions() { return Collections.unmodifiableList(new ArrayList<Position>(positions.values())); }
	public List<PositionModel> getPositionModels() { return Collections.unmodifiableList(new ArrayList<PositionModel>(positions.values())); }

	public String getLeadJammer() { return leadJammer; }
	public void setLeadJammer(String lead) {
		_setLeadJammer(lead);
		ScoreBoardManager.gameSnapshot();
	}
	private void _setLeadJammer(String lead) {
		if ("false".equals(lead.toLowerCase()))
			lead = Team.LEAD_NO_LEAD;
		else if ("true".equals(lead.toLowerCase()))
			lead = Team.LEAD_LEAD;
		synchronized (skaterLock) {
			requestBatchStart();

			String last = leadJammer;
			leadJammer = lead;
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_LEAD_JAMMER, leadJammer, last));

			if (Team.LEAD_LEAD.equals(lead)) {
				String otherId = id.equals(Team.ID_1) ? Team.ID_2 : Team.ID_1;
				TeamModel otherTeam = getScoreBoardModel().getTeamModel(otherId);
				if (Team.LEAD_LEAD.equals(otherTeam.getLeadJammer())) {
					otherTeam.setLeadJammer(Team.LEAD_NO_LEAD);
				}
			}

			requestBatchEnd();
		}
	}

	public boolean isStarPass() { return starPass; }
	public void setStarPass(boolean starPass) {
		_setStarPass(starPass);
		ScoreBoardManager.gameSnapshot();
	}
	private void _setStarPass(boolean starPass) {
		synchronized (skaterLock) {
			requestBatchStart();

			Boolean last = new Boolean(starPass);
			this.starPass = starPass;
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_STAR_PASS, new Boolean(starPass), last));

			if (starPass && Team.LEAD_LEAD.equals(leadJammer))
				_setLeadJammer(Team.LEAD_LOST_LEAD);

			requestBatchEnd();
		}
	}

  public void penalty(String skaterId, String penaltyId, boolean fo_exp, int period, int jam, String code) {
    getSkaterModel(skaterId).AddPenaltyModel(penaltyId, fo_exp, period, jam, code);
  }



	protected ScoreBoardModel scoreBoardModel;

	protected String id;
	protected String name;
	protected String logo;
	protected int score;
	protected int lastscore;
	protected int timeouts;
	protected int officialReviews;
	protected String leadJammer = Team.LEAD_NO_LEAD;
	protected boolean starPass = false;
	protected boolean in_timeout = false;
	protected boolean in_official_review = false;
	protected boolean retained_official_review = false;

	private int saved_lastscore = 0;
	private String saved_leadJammer = Team.LEAD_NO_LEAD;
	private boolean saved_starPass = false;

	protected Object nameLock = new Object();
	protected Object logoLock = new Object();
	protected Object scoreLock = new Object();
	protected Object lastscoreLock = new Object();
	protected Object timeoutsLock = new Object();
	protected Object officialReviewsLock = new Object();

	protected Map<String,AlternateNameModel> alternateNames = new ConcurrentHashMap<String,AlternateNameModel>();
	protected Object alternateNameLock = new Object();

	protected Map<String,ColorModel> colors = new ConcurrentHashMap<String,ColorModel>();
	protected Object colorLock = new Object();

	protected Map<String,SkaterModel> skaters = new ConcurrentHashMap<String,SkaterModel>();
	protected Map<String,PositionModel> positions = new ConcurrentHashMap<String,PositionModel>();
	protected Object skaterLock = new Object();

	public static final String DEFAULT_NAME_PREFIX = "Team ";
	public static final String DEFAULT_LOGO = "";
	public static final int DEFAULT_SCORE = 0;
	public static final int DEFAULT_TIMEOUTS = 3;
	public static final int DEFAULT_OFFICIAL_REVIEWS = 1;
	public static final String DEFAULT_LEADJAMMER = Team.LEAD_NO_LEAD;
	public static final boolean DEFAULT_STARPASS = false;

	public class DefaultAlternateNameModel extends DefaultScoreBoardEventProvider implements AlternateNameModel
	{
		public DefaultAlternateNameModel(TeamModel t, String i, String n) {
			teamModel = t;
			id = i;
			name = n;
		}
		public String getId() { return id; }
		public String getName() { return name; }
		public void setName(String n) {
			synchronized (nameLock) {
				String last = name;
				name = n;
				scoreBoardChange(new ScoreBoardEvent(this, AlternateName.EVENT_NAME, name, last));
			}
		}

		public Team getTeam() { return getTeamModel(); }
		public TeamModel getTeamModel() { return teamModel; }

		public String getProviderName() { return "AlternateName"; }
		public Class getProviderClass() { return AlternateName.class; }
		public String getProviderId() { return getId(); }

		protected TeamModel teamModel;
		protected String id;
		protected String name;
		protected Object nameLock = new Object();
	}

	public class DefaultColorModel extends DefaultScoreBoardEventProvider implements ColorModel
	{
		public DefaultColorModel(TeamModel t, String i, String c) {
			teamModel = t;
			id = i;
			color = c;
		}
		public String getId() { return id; }
		public String getColor() { return color; }
		public void setColor(String c) {
			synchronized (colorLock) {
				String last = color;
				color = c;
				scoreBoardChange(new ScoreBoardEvent(this, Color.EVENT_COLOR, color, last));
			}
		}

		public Team getTeam() { return getTeamModel(); }
		public TeamModel getTeamModel() { return teamModel; }

		public String getProviderName() { return "Color"; }
		public Class getProviderClass() { return Color.class; }
		public String getProviderId() { return getId(); }

		protected TeamModel teamModel;
		protected String id;
		protected String color;
		protected Object colorLock = new Object();
	}
}
