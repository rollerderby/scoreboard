package com.carolinarollergirls.scoreboard.defaults;

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
		teamLogoModel = new DefaultTeamLogoModel(this);
		teamLogoModel.addScoreBoardListener(this);
		reset();
	}

	public String getProviderName() { return "Team"; }
	public Class getProviderClass() { return Team.class; }

	public ScoreBoard getScoreBoard() { return scoreBoardModel.getScoreBoard(); }
	public ScoreBoardModel getScoreBoardModel() { return scoreBoardModel; }

	public void reset() {
		setName(DEFAULT_NAME_PREFIX+getId());
		setScore(DEFAULT_SCORE);
		setTimeouts(DEFAULT_TIMEOUTS);
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
		getTeamLogoModel().reset();
	}

	public String getId() { return id; }

	public Team getTeam() { return this; }

	public String getName() { return name; }
	public void setName(String n) {
		name = n;
		scoreBoardChange(new ScoreBoardEvent(this, "Name", name));
	}

	public TeamLogo getTeamLogo() { return getTeamLogoModel().getTeamLogo(); }
	public TeamLogoModel getTeamLogoModel() { return teamLogoModel; }

	public void timeout() {
		if (getTimeouts() > 0) {
			changeTimeouts(-1);
			getScoreBoardModel().timeout(this);
		}
	}

	public int getScore() { return score; }
	public void setScore(int s) {
		if (0 > s)
			s = 0;
		score = s;
		scoreBoardChange(new ScoreBoardEvent(this, "Score", new Integer(score)));
	}
	public void changeScore(int c) {
		setScore(getScore() + c);
	}

	public int getTimeouts() { return timeouts; }
//FIXME - ad MinimumTimeout and MaximumTimeout instead of hardcoding 0 and 3
	public void setTimeouts(int t) {
		if (0 > t)
			t = 0;
		if (3 < t)
			t = 3;
		timeouts = t;
		scoreBoardChange(new ScoreBoardEvent(this, "Timeouts", new Integer(timeouts)));
	}
	public void changeTimeouts(int c) {
		setTimeouts(getTimeouts() + c);
	}

	public List<SkaterModel> getSkaterModels() { return Collections.unmodifiableList(new ArrayList<SkaterModel>(skaters.values())); }
	public List<Skater> getSkaters() {
		List<Skater> list = new ArrayList<Skater>(skaters.size());
		Iterator<SkaterModel> i = getSkaterModels().iterator();
		while (i.hasNext())
			list.add(i.next().getSkater());
		return Collections.unmodifiableList(list);
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
			scoreBoardChange(new ScoreBoardEvent(this, "AddSkater", skater));
		}
	}
	public void removeSkaterModel(String id) throws SkaterNotFoundException {
		SkaterModel sm;
		synchronized (skaterLock) {
			sm = getSkaterModel(id);
			try { getPositionModel(sm.getPosition()).clear(); }
			catch ( PositionNotFoundException pnfE ) { /* was on BENCH */ }
			sm.removeScoreBoardListener(this);
			skaters.remove(id);
			scoreBoardChange(new ScoreBoardEvent(this, "RemoveSkater", sm));
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
		leadJammer = lead;
		scoreBoardChange(new ScoreBoardEvent(this, "LeadJammer", new Boolean(leadJammer)));
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
		pass = p;
		scoreBoardChange(new ScoreBoardEvent(this, "Pass", new Integer(pass)));		
	}


	protected ScoreBoardModel scoreBoardModel;

	protected String id;
	protected String name;
	protected TeamLogoModel teamLogoModel;
	protected int score;
	protected int timeouts;
	protected boolean leadJammer = false;
	protected int pass = 0;

	protected Map<String,SkaterModel> skaters = new ConcurrentHashMap<String,SkaterModel>();
	protected Map<String,PositionModel> positions = new ConcurrentHashMap<String,PositionModel>();
	protected Object skaterLock = new Object();

	public static final String DEFAULT_NAME_PREFIX = "Team ";
	public static final int DEFAULT_SCORE = 0;
	public static final int DEFAULT_TIMEOUTS = 3;
	public static final boolean DEFAULT_LEADJAMMER = false;
	public static final int DEFAULT_PASS = 0;
}
