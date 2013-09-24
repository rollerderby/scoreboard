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

import java.awt.image.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.policy.*;

public class DefaultTeamModel extends DefaultScoreBoardEventProvider implements TeamModel
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
		reset();
	}

	public String getProviderName() { return "Team"; }
	public Class getProviderClass() { return Team.class; }
	public String getProviderId() { return getId(); }

	public ScoreBoard getScoreBoard() { return scoreBoardModel.getScoreBoard(); }
	public ScoreBoardModel getScoreBoardModel() { return scoreBoardModel; }

	public void reset() {
		setName(DEFAULT_NAME_PREFIX+getId());
		setLogo(DEFAULT_LOGO);
		setScore(DEFAULT_SCORE);
		setTimeouts(DEFAULT_TIMEOUTS);
		setOfficialReviews(DEFAULT_OFFICIAL_REVIEWS);
		setLeadJammer(DEFAULT_LEADJAMMER);
		setPass(DEFAULT_PASS);
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

	public List<AlternateName> getAlternateNames() {
		return Collections.unmodifiableList(new ArrayList<AlternateName>(alternateNames.values()));
	}
	public List<AlternateNameModel> getAlternateNameModels() {
		return Collections.unmodifiableList(new ArrayList<AlternateNameModel>(alternateNames.values()));
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
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_SCORE, new Integer(score), last));
		}
	}
	public void changeScore(int c) {
		synchronized (scoreLock) {
			setScore(getScore() + c);
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

	public List<SkaterModel> getSkaterModels() {
		return Collections.unmodifiableList(new ArrayList<SkaterModel>(skaters.values()));
	}
	public List<Skater> getSkaters() {
		return Collections.unmodifiableList(new ArrayList<Skater>(skaters.values()));
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
	public SkaterModel addSkaterModel(String id) { return addSkaterModel(id, "", ""); }
	public SkaterModel addSkaterModel(String id, String n, String num) {
		SkaterModel sM = new DefaultSkaterModel(this, id, n, num);
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

	public boolean isLeadJammer() { return leadJammer; }
	public void setLeadJammer(boolean lead) {
		synchronized (skaterLock) {
			boolean leadIndependent = false;
			try { leadIndependent = getScoreBoard().getPolicy(TeamLeadJammerIndependentPolicy.ID).isEnabled(); }
			catch ( Exception e ) { }
			if (leadIndependent) {
				_setLeadJammer(lead);
			} else {
				try { getPositionModel(Position.ID_JAMMER).getSkaterModel().setLeadJammer(lead); }
				catch ( NullPointerException npE ) { /* No Jammer set */ }
			}
		}
	}
	public void _setLeadJammer(boolean lead) {
		Boolean last = new Boolean(leadJammer);
		leadJammer = lead;
		scoreBoardChange(new ScoreBoardEvent(this, EVENT_LEAD_JAMMER, new Boolean(leadJammer), last));
	}


	public int getPass() { return pass; }
	public void setPass(int pass) {
		synchronized (skaterLock) {
			try { getPositionModel(Position.ID_JAMMER).getSkaterModel().setPass(pass); }
			catch ( NullPointerException npE ) { }
		}
	}
	public void changePass(int change) {
		synchronized (skaterLock) {
			try { getPositionModel(Position.ID_JAMMER).getSkaterModel().changePass(change); }
			catch ( NullPointerException npE ) { }
		}
	}
	public void _setPass(int p) {
		Integer last = new Integer(pass);
		pass = p;
		scoreBoardChange(new ScoreBoardEvent(this, EVENT_PASS, new Integer(pass), last));
	}


	protected ScoreBoardModel scoreBoardModel;

	protected String id;
	protected String name;
	protected Object nameLock = new Object();
	protected String logo;
	protected Object logoLock = new Object();
	protected int score;
	protected Object scoreLock = new Object();
	protected int timeouts;
	protected Object timeoutsLock = new Object();
	protected int officialReviews;
	protected Object officialReviewsLock = new Object();
	protected boolean leadJammer = false;
	protected int pass = 0;

	protected Map<String,AlternateNameModel> alternateNames = new ConcurrentHashMap<String,AlternateNameModel>();
	protected Object alternateNameLock = new Object();

	protected Map<String,SkaterModel> skaters = new ConcurrentHashMap<String,SkaterModel>();
	protected Map<String,PositionModel> positions = new ConcurrentHashMap<String,PositionModel>();
	protected Object skaterLock = new Object();

	public static final String DEFAULT_NAME_PREFIX = "Team ";
	public static final String DEFAULT_LOGO = "";
	public static final int DEFAULT_SCORE = 0;
	public static final int DEFAULT_TIMEOUTS = 3;
	public static final int DEFAULT_OFFICIAL_REVIEWS = 1;
	public static final boolean DEFAULT_LEADJAMMER = false;
	public static final int DEFAULT_PASS = 0;

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
}
