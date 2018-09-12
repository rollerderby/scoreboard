package com.carolinarollergirls.scoreboard.defaults;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.carolinarollergirls.scoreboard.Clock;
import com.carolinarollergirls.scoreboard.FrontendSettings;
import com.carolinarollergirls.scoreboard.Ruleset;
import com.carolinarollergirls.scoreboard.ScoreBoard;
import com.carolinarollergirls.scoreboard.Settings;
import com.carolinarollergirls.scoreboard.Stats;
import com.carolinarollergirls.scoreboard.Team;
import com.carolinarollergirls.scoreboard.event.AsyncScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.model.FrontendSettingsModel;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.model.ClockModel;
import com.carolinarollergirls.scoreboard.model.ScoreBoardModel;
import com.carolinarollergirls.scoreboard.model.SettingsModel;
import com.carolinarollergirls.scoreboard.model.StatsModel;
import com.carolinarollergirls.scoreboard.model.TeamModel;
import com.carolinarollergirls.scoreboard.penalties.PenaltyCodesManager;
import com.carolinarollergirls.scoreboard.xml.XmlScoreBoard;

public class DefaultScoreBoardModel extends SimpleScoreBoardEventProvider implements ScoreBoardModel
{
	public DefaultScoreBoardModel() {
		setupScoreBoard();
	}

	protected void setupScoreBoard(){
		settings = new DefaultSettingsModel(this);
		settings.addScoreBoardListener(this);
		Ruleset.registerRule(settings, "ScoreBoard." + Clock.ID_INTERMISSION + ".PreGame");
		Ruleset.registerRule(settings, "ScoreBoard." + Clock.ID_INTERMISSION + ".Intermission");
		Ruleset.registerRule(settings, "ScoreBoard." + Clock.ID_INTERMISSION + ".Unofficial");
		Ruleset.registerRule(settings, "ScoreBoard." + Clock.ID_INTERMISSION + ".Official");
		Ruleset.registerRule(settings, "ScoreBoard.Clock.Sync");
		Ruleset.registerRule(settings, "ScoreBoard." + Clock.ID_JAM + ".ResetNumberEachPeriod");
		Ruleset.registerRule(settings, "ScoreBoard." + Clock.ID_LINEUP + ".AutoStart");
		Ruleset.registerRule(settings, "ScoreBoard." + Clock.ID_LINEUP + ".AutoStartBuffer");
		Ruleset.registerRule(settings, "ScoreBoard." + Clock.ID_LINEUP + ".AutoStartType");
		Ruleset.registerRule(settings, "Clock." + Clock.ID_INTERMISSION + ".Time");
		Ruleset.registerRule(settings, "Clock." + Clock.ID_LINEUP + ".Time");
		Ruleset.registerRule(settings, "Clock." + Clock.ID_LINEUP + ".OvertimeTime");

		settings.addRuleMapping("ScoreBoard.BackgroundStyle", new String[] { "ScoreBoard.Preview_BackgroundStyle", "ScoreBoard.View_BackgroundStyle" });
		settings.addRuleMapping("ScoreBoard.BoxStyle",        new String[] { "ScoreBoard.Preview_BoxStyle",        "ScoreBoard.View_BoxStyle" });
		settings.addRuleMapping("ScoreBoard.CurrentView",     new String[] { "ScoreBoard.Preview_CurrentView",     "ScoreBoard.View_CurrentView" });
		settings.addRuleMapping("ScoreBoard.CustomHtml",      new String[] { "ScoreBoard.Preview_CustomHtml",      "ScoreBoard.View_CustomHtml" });
		settings.addRuleMapping("ScoreBoard.HideJamTotals",   new String[] { "ScoreBoard.Preview_HideJamTotals",   "ScoreBoard.View_HideJamTotals" });
		settings.addRuleMapping("ScoreBoard.Image",           new String[] { "ScoreBoard.Preview_Image",           "ScoreBoard.View_Image" });
		settings.addRuleMapping("ScoreBoard.SidePadding",     new String[] { "ScoreBoard.Preview_SidePadding",     "ScoreBoard.View_SidePadding" });
		settings.addRuleMapping("ScoreBoard.SwapTeams",       new String[] { "ScoreBoard.Preview_SwapTeams",       "ScoreBoard.View_SwapTeams" });
		settings.addRuleMapping("ScoreBoard.Video",           new String[] { "ScoreBoard.Preview_Video",           "ScoreBoard.View_Video" });

		Ruleset.registerRule(settings, "ScoreBoard.BackgroundStyle");
		Ruleset.registerRule(settings, "ScoreBoard.BoxStyle");
		Ruleset.registerRule(settings, "ScoreBoard.CurrentView");
		Ruleset.registerRule(settings, "ScoreBoard.CustomHtml");
		Ruleset.registerRule(settings, "ScoreBoard.HideJamTotals");
		Ruleset.registerRule(settings, "ScoreBoard.Image");
		Ruleset.registerRule(settings, "ScoreBoard.SidePadding");
		Ruleset.registerRule(settings, "ScoreBoard.SwapTeams");
		Ruleset.registerRule(settings, "ScoreBoard.Video");
		Ruleset.registerRule(settings, PenaltyCodesManager.PenaltiesFileSetting);

		stats = new DefaultStatsModel(this);
		stats.addScoreBoardListener(this);
		frontendSettings = new DefaultFrontendSettingsModel(this);
		frontendSettings.addScoreBoardListener(this);
		reset();
		addInPeriodListeners();
		xmlScoreBoard = new XmlScoreBoard(this);
	}

	public String getProviderName() { return "ScoreBoard"; }
	public Class<ScoreBoard> getProviderClass() { return ScoreBoard.class; }
	public String getProviderId() { return ""; }

	public XmlScoreBoard getXmlScoreBoard() { return xmlScoreBoard; }

	public ScoreBoard getScoreBoard() { return this; }

	public void reset() {
		_getRuleset().apply(true);

		Iterator<ClockModel> c = getClockModels().iterator();
		while (c.hasNext())
			c.next().reset();
		Iterator<TeamModel> t = getTeamModels().iterator();
		while (t.hasNext())
			t.next().reset();

		setTimeoutOwner(DEFAULT_TIMEOUT_OWNER);
		setOfficialReview(false);
		setInPeriod(false);
		setInOvertime(false);
		setOfficialScore(false);
		restartPcAfterTimeout = false;
		
		settings.reset();
		stats.reset();
		// Custom settings are not reset, as broadcast overlays settings etc.
		// shouldn't be lost just because the next game is starting.
	}

	public boolean isInPeriod() { return inPeriod; }
	public void setInPeriod(boolean p) {
		synchronized (inPeriodLock) {
			if (p == inPeriod) { return; }
			Boolean last = new Boolean(inPeriod);
			inPeriod = p;
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_IN_PERIOD, new Boolean(inPeriod), last));
		}
	}
	protected void addInPeriodListeners() {
		addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_PERIOD, "Running", Boolean.FALSE, periodEndListener));
		addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_JAM, "Running", Boolean.FALSE, jamEndListener));
		addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_INTERMISSION, "Running", Boolean.FALSE, intermissionEndListener));
		addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_LINEUP, "Time", lineupClockListener));
	}

	public boolean isInOvertime() { return inOvertime; }
	public void setInOvertime(boolean o) {
		if (o == inOvertime) { return; }
		synchronized (inOvertimeLock) {
			Boolean last = new Boolean(inOvertime);
			inOvertime = o;
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_IN_OVERTIME, new Boolean(inOvertime), last));
		}
		ClockModel lc = getClockModel(Clock.ID_LINEUP);
		if (!o && lc.isCountDirectionDown()) {
			lc.setMaximumTime(settings.getLong("Clock." + Clock.ID_LINEUP + ".Time"));
		}
	}
	public void startOvertime() {
		synchronized (runLock) {
			ClockModel pc = getClockModel(Clock.ID_PERIOD);
			ClockModel jc = getClockModel(Clock.ID_JAM);
			ClockModel lc = getClockModel(Clock.ID_LINEUP);

			if (pc.isRunning() || jc.isRunning())
				return;
			if (pc.getNumber() < pc.getMaximumNumber())
				return;
			if (!pc.isTimeAtEnd())
				return;
			createSnapshot(ACTION_OVERTIME);
			
			requestBatchStart();
			setInOvertime(true);
			_endTimeout();
			long otLineupTime = settings.getLong("Clock." + Clock.ID_LINEUP + ".OvertimeTime");
			if (lc.getMaximumTime() < otLineupTime) {
				lc.setMaximumTime(otLineupTime);
			}
			_startLineup();
			requestBatchEnd();
		}
	}

	public boolean isOfficialScore() { return officialScore; }
	public void setOfficialScore(boolean o) {
		synchronized (officialScoreLock) {
			Boolean last = new Boolean(officialScore);
			officialScore = o;
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_OFFICIAL_SCORE, new Boolean(officialScore), last));
		}
	}

	public void startJam() {
		synchronized (runLock) {
			if (!getClock(Clock.ID_JAM).isRunning()) {
				createSnapshot(ACTION_START_JAM);
				_startJam();
			}
		}
	}
	public void stopJamTO() {
		synchronized (runLock) {
			ClockModel jc = getClockModel(Clock.ID_JAM);
			ClockModel lc = getClockModel(Clock.ID_LINEUP);
			ClockModel tc = getClockModel(Clock.ID_TIMEOUT);

			if (jc.isRunning()) {
				createSnapshot(ACTION_STOP_JAM);
				_endJam(false);
			} else if (tc.isRunning()) {
				createSnapshot(ACTION_STOP_TO);
				_endTimeout();
			} else if (!lc.isRunning()) {
				createSnapshot(ACTION_LINEUP);
				_startLineup();
			}
		}
	}
	public void timeout() { 
		synchronized (runLock) {
			createSnapshot(ACTION_TIMEOUT);
			_startTimeout();
		}
	}
	public void startTimeoutType(String owner, boolean review) {
		ClockModel tc = getClockModel(Clock.ID_TIMEOUT);

		requestBatchStart();
		if (!tc.isRunning()) {
			timeout();
		}
		setTimeoutOwner(owner);
		setOfficialReview(review);
		if (owner != TIMEOUT_OWNER_NONE && owner != TIMEOUT_OWNER_OTO) {
			restartPcAfterTimeout = false;
		}
		requestBatchEnd();
	}
	private void _preparePeriod() {
		ClockModel pc = getClockModel(Clock.ID_PERIOD);
		ClockModel jc = getClockModel(Clock.ID_JAM);
		ClockModel ic = getClockModel(Clock.ID_INTERMISSION);

		requestBatchStart();
		pc.setNumber(ic.getNumber()+1);
		pc.resetTime();
		restartPcAfterTimeout = false;
		if (settings.getBoolean("ScoreBoard." + Clock.ID_JAM + ".ResetNumberEachPeriod")) {
			jc.setNumber(jc.getMinimumNumber());
		}
		for (TeamModel t : getTeamModels()) {
			t.resetTimeouts(false);
		}		
		requestBatchEnd();
	}
	private void _possiblyEndPeriod() {
		ClockModel pc = getClockModel(Clock.ID_PERIOD);
		ClockModel jc = getClockModel(Clock.ID_JAM);
		ClockModel tc = getClockModel(Clock.ID_TIMEOUT);

		if (pc.isTimeAtEnd() && !pc.isRunning() && !jc.isRunning() && !tc.isRunning()) {
			requestBatchStart();
			setInPeriod(false);
			setOfficialScore(false);
			_endLineup();
			_startIntermission();
			requestBatchEnd();
		}
	}
	private void _startJam() {
		ClockModel pc = getClockModel(Clock.ID_PERIOD);
		ClockModel jc = getClockModel(Clock.ID_JAM);

		requestBatchStart();
		_endIntermission(false);
		_endTimeout();
		_endLineup();
		setInPeriod(true);
		pc.start();
		jc.startNext();

		getTeamModel(Team.ID_1).startJam();
		getTeamModel(Team.ID_2).startJam();
		requestBatchEnd();
	}
	private void _endJam(boolean force) {
		ClockModel pc = getClockModel(Clock.ID_PERIOD);
		ClockModel jc = getClockModel(Clock.ID_JAM);

		if (!jc.isRunning() && !force) { return; }
		
		requestBatchStart();
		jc.stop();
		getTeamModel(Team.ID_1).stopJam();
		getTeamModel(Team.ID_2).stopJam();
		setInOvertime(false);

		//TODO: Make this value configurable in the ruleset.
		if (pc.getTimeRemaining() < 30000) {
			restartPcAfterTimeout = true;
		}
		if (pc.isRunning()) {
			_startLineup();
		} else {
			_possiblyEndPeriod();
		}
		requestBatchEnd();
	}
	private void _startLineup() {
		ClockModel lc = getClockModel(Clock.ID_LINEUP);

		requestBatchStart();
		_endIntermission(false);
		setInPeriod(true);
		lc.startNext();
		requestBatchEnd();
	}
	private void _endLineup() {
		ClockModel lc = getClockModel(Clock.ID_LINEUP);

		requestBatchStart();
		lc.stop();
		requestBatchEnd();
	}
	private void _startTimeout() {
		ClockModel pc = getClockModel(Clock.ID_PERIOD);
		ClockModel tc = getClockModel(Clock.ID_TIMEOUT);

		requestBatchStart();
		if (tc.isRunning()) {
			//TODO: Make Official Timeout its own button that calls startTimeoutType()
			if (getTimeoutOwner()==TIMEOUT_OWNER_NONE) {
				setTimeoutOwner(TIMEOUT_OWNER_OTO);
			} else {
				setTimeoutOwner(TIMEOUT_OWNER_NONE);
			}
			requestBatchEnd();
			return; 
		}
		
		pc.stop();
		_endLineup();
		_endJam(false);
		_endIntermission(false);
		setInPeriod(true);
		tc.startNext();
		requestBatchEnd();
	}
	private void _endTimeout() {
		ClockModel tc = getClockModel(Clock.ID_TIMEOUT);
		ClockModel pc = getClockModel(Clock.ID_PERIOD);

		if (!tc.isRunning()) { return; }
		
		requestBatchStart();
		tc.stop();
		setTimeoutOwner(TIMEOUT_OWNER_NONE);
		setOfficialReview(false);
		if (pc.isTimeAtEnd()) {
			_possiblyEndPeriod();
		} else {
			if (restartPcAfterTimeout) {
				pc.start();
			}
			_startLineup();
		}
		requestBatchEnd();
	}
	private void _startIntermission() {
		ClockModel pc = getClockModel(Clock.ID_PERIOD);
		ClockModel ic = getClockModel(Clock.ID_INTERMISSION);

		requestBatchStart();
		ic.setNumber(pc.getNumber());
		ic.setMaximumTime(settings.getLong("Clock." + Clock.ID_INTERMISSION + ".Time"));
		ic.resetTime();
		ic.start();		
		requestBatchEnd();
	}
	private void _endIntermission(boolean force) {
		ClockModel ic = getClockModel(Clock.ID_INTERMISSION);
		ClockModel pc = getClockModel(Clock.ID_PERIOD);

		if (!ic.isRunning() && !force) { return; }
		
		requestBatchStart();
		ic.stop();
		if (ic.getTimeRemaining() < 60000 && pc.getNumber() < pc.getMaximumNumber()) {
			//If less than one minute of intermission is left and there is another period, 
			// go to the next period. Otherwise extend the previous period.
			_preparePeriod();
		}
		requestBatchEnd();
	}
	private void _possiblyAutostart() {
		ClockModel pc = getClockModel(Clock.ID_PERIOD);
		ClockModel jc = getClockModel(Clock.ID_JAM);
		ClockModel lc = getClockModel(Clock.ID_LINEUP);
		ClockModel tc = getClockModel(Clock.ID_TIMEOUT);
		
		long bufferTime = settings.getLong("ScoreBoard." + Clock.ID_LINEUP + ".AutoStartBuffer"); 
		long triggerTime = bufferTime + (isInOvertime() ? 
					settings.getLong("Clock." + Clock.ID_LINEUP + ".OvertimeTime") :
					settings.getLong("Clock." + Clock.ID_LINEUP + ".Time"));

		requestBatchStart();
		if (lc.getTimeElapsed() >= triggerTime) {
			if (Boolean.parseBoolean(settings.get("ScoreBoard." + Clock.ID_LINEUP + ".AutoStartType"))) {
				startJam();
				jc.elapseTime(bufferTime);
			} else {
				timeout();
				pc.elapseTime(-bufferTime);
				tc.elapseTime(bufferTime);
			}
		}
		requestBatchEnd();
	}


	protected void createSnapshot(String type) {
		snapshot = new ScoreBoardSnapshot(this, DefaultClockModel.updateClockTimerTask.getCurrentTime(), type);
	}
	protected long restoreSnapshot() {
		long relapseTime = DefaultClockModel.updateClockTimerTask.getCurrentTime() - snapshot.getSnapshotTime();
		for (ClockModel clock : getClockModels()) {
			clock.restoreSnapshot(snapshot.getClockSnapshot(clock.getId()));
		}
		for (TeamModel team : getTeamModels()) {
			team.restoreSnapshot(snapshot.getTeamSnapshot(team.getId()));
		}
		setTimeoutOwner(snapshot.getTimeoutOwner());
		setOfficialReview(snapshot.isOfficialReview());
		setInOvertime(snapshot.inOvertime());
		setInPeriod(snapshot.inPeriod());
		restartPcAfterTimeout = snapshot.restartPcAfterTo();
		snapshot = null;
		return relapseTime;
	}
	protected void relapseTime(long time) {
		for (ClockModel clock : getClockModels()) {
			if (clock.isRunning()) {
				clock.elapseTime(time);
			}
		}
	}
	public void clockUndo() {
		if (snapshot == null) { return; }
		synchronized (runLock) {
			requestBatchStart();
			long time = restoreSnapshot();
			relapseTime(time);
			requestBatchEnd();
		}
	}
	public void unStartJam() {
		if (snapshot != null && 
				snapshot.getType() == ACTION_START_JAM) {
			clockUndo();
		}
	}
	public void unStopJam() {
		if (snapshot != null && 
				(snapshot.getType() == ACTION_STOP_JAM ||
				 snapshot.getType() == ACTION_STOP_TO ||
				 snapshot.getType() == ACTION_LINEUP)) {
			clockUndo();
		}
	}
	public void unTimeout() {
		if (snapshot != null && 
				snapshot.getType() == ACTION_TIMEOUT) {
			clockUndo();
		}
	}

	public Ruleset _getRuleset() {
		synchronized (rulesetLock) {
			if (ruleset == null) {
				ruleset = Ruleset.findRuleset(null, true);
			}
			return ruleset;
		}
	}
	public String getRuleset() { return _getRuleset().getId().toString(); }
	public void setRuleset(String id) {
		synchronized (rulesetLock) {
			String last = getRuleset();
			ruleset = Ruleset.findRuleset(id, true);
			ruleset.apply(false);
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_RULESET, ruleset.getId().toString(), last));
		}
	}

	public void penalty(String teamId, String skaterId, String penaltyId, boolean fo_exp, int period, int jam, String code){
		getTeamModel(teamId).penalty(skaterId, penaltyId, fo_exp, period, jam, code);
	}

	public Settings getSettings() { return settings; }
	public SettingsModel getSettingsModel() { return settings; }

	public FrontendSettings getFrontendSettings() { return frontendSettings; }
	public FrontendSettingsModel getFrontendSettingsModel() { return frontendSettings; }

	public Stats getStats() { return (Stats)stats; }
	public StatsModel getStatsModel() { return stats; }

	public List<ClockModel> getClockModels() { return new ArrayList<ClockModel>(clocks.values()); }
	public List<TeamModel> getTeamModels() { return new ArrayList<TeamModel>(teams.values()); }

	public List<Clock> getClocks() { return new ArrayList<Clock>(getClockModels()); }
	public List<Team> getTeams() { return new ArrayList<Team>(getTeamModels()); }

	public Clock getClock(String id) { return getClockModel(id).getClock(); }
	public Team getTeam(String id) { return getTeamModel(id).getTeam(); }

	public ClockModel getClockModel(String id) {
		synchronized (clocks) {
// FIXME - don't auto-create!	 return null instead - or throw exception.	Need to update all callers to handle first.
			if (!clocks.containsKey(id))
				createClockModel(id);

			return clocks.get(id);
		}
	}

	public TeamModel getTeamModel(String id) {
		synchronized (teams) {
// FIXME - don't auto-create!	 return null instead - or throw exception.	Need to update all callers to handle first.
			if (!teams.containsKey(id))
				createTeamModel(id);

			return teams.get(id);
		}
	}

	public String getTimeoutOwner() { return timeoutOwner; }
	public void setTimeoutOwner(String owner) {
		synchronized (timeoutOwnerLock) {
			String last = timeoutOwner;
			timeoutOwner = owner;
			for (TeamModel tm : getTeamModels()) {
				tm.setInTimeout(tm.getId() == owner);
			}
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_TIMEOUT_OWNER, timeoutOwner, last));
		}
	}
	public boolean isOfficialReview() { return officialReview; }
	public void setOfficialReview(boolean official) {
		synchronized (officialReviewLock) {
			boolean last = officialReview;
			officialReview = official;
			for (TeamModel tm : getTeamModels()) {
				tm.setInOfficialReview(tm.getId() == getTimeoutOwner() && official);
			}
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_OFFICIAL_REVIEW, new Boolean(officialReview), last));
		}
	}

	protected void createClockModel(String id) {
		if ((id == null) || (id.equals("")))
			return;

		ClockModel model = new DefaultClockModel(this, id);
		model.addScoreBoardListener(this);
		clocks.put(id, model);
		scoreBoardChange(new ScoreBoardEvent(this, EVENT_ADD_CLOCK, model, null));
	}

	protected void createTeamModel(String id) {
		if ((id == null) || (id.equals("")))
			return;

		TeamModel model = new DefaultTeamModel(this, id);
		model.addScoreBoardListener(this);
		teams.put(id, model);
		scoreBoardChange(new ScoreBoardEvent(this, EVENT_ADD_TEAM, model, null));
	}

	// Have all events delivered by the ScoreBoard asynchronously.
	@Override
	public void scoreBoardChange(ScoreBoardEvent event) {
		asbl.scoreBoardChange(event);
	}

	protected AsyncScoreBoardListener asbl = new AsyncScoreBoardListener(
			new ScoreBoardListener(){
				public void scoreBoardChange(ScoreBoardEvent event) {
					DefaultScoreBoardModel.this.dispatch(event);
				}
			}
			);


	protected HashMap<String,ClockModel> clocks = new HashMap<String,ClockModel>();
	protected HashMap<String,TeamModel> teams = new HashMap<String,TeamModel>();

	protected Object runLock = new Object();
	protected ScoreBoardSnapshot snapshot = null;

	protected String timeoutOwner;
	protected Object timeoutOwnerLock = new Object();
	protected boolean officialReview;
	protected Object officialReviewLock = new Object();
	protected boolean restartPcAfterTimeout;

	protected boolean inPeriod = false;
	protected Object inPeriodLock = new Object();

	protected boolean inOvertime = false;
	protected Object inOvertimeLock = new Object();

	protected boolean officialScore = false;
	protected Object officialScoreLock = new Object();

	protected Ruleset ruleset = null;
	protected Object rulesetLock = new Object();
	protected DefaultSettingsModel settings = null;
	protected DefaultFrontendSettingsModel frontendSettings = null;
	protected DefaultStatsModel stats = null;

	protected XmlScoreBoard xmlScoreBoard;

	protected ScoreBoardListener periodEndListener = new ScoreBoardListener() {
		public void scoreBoardChange(ScoreBoardEvent event) {
			_possiblyEndPeriod();
		}
	};
	protected ScoreBoardListener jamEndListener = new ScoreBoardListener() {
		public void scoreBoardChange(ScoreBoardEvent event) {
			ClockModel jc = getClockModel(Clock.ID_JAM);
			if (jc.isTimeAtEnd()) {
				//clock has run down naturally
				_endJam(true);
			}
		}
	};
	protected ScoreBoardListener intermissionEndListener = new ScoreBoardListener() {
		public void scoreBoardChange(ScoreBoardEvent event) {
			if (getClock(Clock.ID_INTERMISSION).isTimeAtEnd()) {
				//clock has run down naturally
				_endIntermission(true);
			}
		}
	};
	protected ScoreBoardListener lineupClockListener = new ScoreBoardListener() {
		public void scoreBoardChange(ScoreBoardEvent event) {
			if (settings.getBoolean("ScoreBoard." + Clock.ID_LINEUP + ".AutoStart")) {
				_possiblyAutostart();
			}
		}
	};

	public static class ScoreBoardSnapshot {
		private ScoreBoardSnapshot(DefaultScoreBoardModel sbm, long time, String type) {
			snapshotTime = time;
			this.type = type; 
			timeoutOwner = sbm.getTimeoutOwner();
			isOfficialReview = sbm.isOfficialReview();
			inOvertime = sbm.isInOvertime();
			inPeriod = sbm.isInPeriod();
			restartPcAfterTo = sbm.restartPcAfterTimeout;
			clockSnapshots = new HashMap<String, DefaultClockModel.ClockSnapshotModel>();
			for (ClockModel clock : sbm.getClockModels()) {
				clockSnapshots.put(clock.getId(), clock.snapshot());
			}
			teamSnapshots = new HashMap<String, TeamModel.TeamSnapshotModel>();
			for (TeamModel team : sbm.getTeamModels()) {
				teamSnapshots.put(team.getId(), team.snapshot());
			}
		}

		public String getType() { return type; }
		public long getSnapshotTime() { return snapshotTime; }
		public String getTimeoutOwner() { return timeoutOwner; }
		public boolean isOfficialReview() { return isOfficialReview; }
		public boolean inOvertime() { return inOvertime; }
		public boolean inPeriod() { return inPeriod; }
		public boolean restartPcAfterTo() { return restartPcAfterTo; }
		public Map<String, ClockModel.ClockSnapshotModel> getClockSnapshots() { return clockSnapshots; }
		public Map<String, TeamModel.TeamSnapshotModel> getTeamSnapshots() { return teamSnapshots; }
		public DefaultClockModel.ClockSnapshotModel getClockSnapshot(String clock) { return clockSnapshots.get(clock); }
		public TeamModel.TeamSnapshotModel getTeamSnapshot(String team) { return teamSnapshots.get(team); }
		
		protected String type;
		protected long snapshotTime;
		protected String timeoutOwner;
		protected boolean isOfficialReview;
		protected boolean inOvertime;
		protected boolean inPeriod;
		protected boolean restartPcAfterTo;
		protected Map<String, ClockModel.ClockSnapshotModel> clockSnapshots;
		protected Map<String, TeamModel.TeamSnapshotModel> teamSnapshots;
	}

	public static final String TIMEOUT_OWNER_OTO = "O";
	public static final String TIMEOUT_OWNER_NONE = "";
	public static final String DEFAULT_TIMEOUT_OWNER = TIMEOUT_OWNER_NONE;

	public static final String POLICY_KEY = DefaultScoreBoardModel.class.getName() + ".policy";
	
	public static final String ACTION_START_JAM = "Start Jam";
	public static final String ACTION_STOP_JAM = "Stop Jam";
	public static final String ACTION_STOP_TO = "End Timeout";
	public static final String ACTION_LINEUP = "Lineup";
	public static final String ACTION_TIMEOUT = "Timeout";
	public static final String ACTION_OVERTIME = "Overtime";
}

