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

import com.carolinarollergirls.scoreboard.Ruleset;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.model.FrontendSettingsModel;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.model.ClockModel;
import com.carolinarollergirls.scoreboard.model.ScoreBoardModel;
import com.carolinarollergirls.scoreboard.model.SettingsModel;
import com.carolinarollergirls.scoreboard.model.StatsModel;
import com.carolinarollergirls.scoreboard.model.TeamModel;
import com.carolinarollergirls.scoreboard.penalties.PenaltyCodesManager;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;
import com.carolinarollergirls.scoreboard.view.Clock;
import com.carolinarollergirls.scoreboard.view.FrontendSettings;
import com.carolinarollergirls.scoreboard.view.ScoreBoard;
import com.carolinarollergirls.scoreboard.view.Settings;
import com.carolinarollergirls.scoreboard.view.Skater.Penalty;
import com.carolinarollergirls.scoreboard.view.Stats;
import com.carolinarollergirls.scoreboard.view.Team;
import com.carolinarollergirls.scoreboard.xml.XmlScoreBoard;

public class DefaultScoreBoardModel extends DefaultScoreBoardEventProvider implements ScoreBoardModel
{
	public DefaultScoreBoardModel() {
			setupScoreBoard();
	}

	protected void setupScoreBoard(){
		settings = new DefaultSettingsModel(this);
		settings.addScoreBoardListener(this);
		settings.addRuleMapping(SETTING_NUMBER_PERIODS, new String[] {"Clock." + Clock.ID_PERIOD + ".MaximumNumber", "Clock." + Clock.ID_INTERMISSION + ".MaximumNumber"});
		settings.addRuleMapping(SETTING_PERIOD_DURATION, new String[] {"Clock." + Clock.ID_PERIOD + ".MaximumTime"});
		settings.addRuleMapping(SETTING_JAM_DURATION, new String[] {"Clock." + Clock.ID_JAM + ".MaximumTime"});
		
		Ruleset.registerRule(settings, SETTING_NUMBER_PERIODS);
		Ruleset.registerRule(settings, SETTING_PERIOD_DURATION);
		Ruleset.registerRule(settings, SETTING_PERIOD_DIRECTION);
		Ruleset.registerRule(settings, SETTING_PERIOD_END_BETWEEN_JAMS);
		Ruleset.registerRule(settings, SETTING_JAM_NUMBER_PER_PERIOD);
		Ruleset.registerRule(settings, SETTING_JAM_DURATION);
		Ruleset.registerRule(settings, SETTING_JAM_DIRECTION);
		Ruleset.registerRule(settings, SETTING_LINEUP_DURATION);
		Ruleset.registerRule(settings, SETTING_OVERTIME_LINEUP_DURATION);
		Ruleset.registerRule(settings, SETTING_LINEUP_DIRECTION);
		Ruleset.registerRule(settings, SETTING_TTO_DURATION);
		Ruleset.registerRule(settings, SETTING_TIMEOUT_DIRECTION);
		Ruleset.registerRule(settings, SETTING_STOP_PC_ON_TO);
		Ruleset.registerRule(settings, SETTING_STOP_PC_ON_OTO);
		Ruleset.registerRule(settings, SETTING_STOP_PC_ON_TTO);
		Ruleset.registerRule(settings, SETTING_STOP_PC_ON_OR);
		Ruleset.registerRule(settings, SETTING_STOP_PC_AFTER);
		Ruleset.registerRule(settings, SETTING_INTERMISSION_DURATION + "1");
		Ruleset.registerRule(settings, SETTING_INTERMISSION_DURATION + "2");
		Ruleset.registerRule(settings, SETTING_INTERMISSION_DURATION + "3");
		Ruleset.registerRule(settings, SETTING_INTERMISSION_DURATION + "4");
		Ruleset.registerRule(settings, SETTING_INTERMISSION_DURATION + "5");
		Ruleset.registerRule(settings, SETTING_INTERMISSION_TYPE_SEQUENCE);
		Ruleset.registerRule(settings, SETTING_INTERMISSION_DIRECTION);
		Ruleset.registerRule(settings, SETTING_AUTO_START);
		Ruleset.registerRule(settings, SETTING_AUTO_START_BUFFER);
		Ruleset.registerRule(settings, SETTING_AUTO_START_JAM);
		Ruleset.registerRule(settings, SETTING_AUTO_END_JAM);
		Ruleset.registerRule(settings, SETTING_AUTO_END_TTO);
		Ruleset.registerRule(settings, Team.SETTING_NUMBER_TIMEOUTS);
		Ruleset.registerRule(settings, Team.SETTING_TIMEOUTS_PER_PERIOD);
		Ruleset.registerRule(settings, Team.SETTING_NUMBER_REVIEWS);
		Ruleset.registerRule(settings, Team.SETTING_REVIEWS_PER_PERIOD);
		Ruleset.registerRule(settings, PenaltyCodesManager.SETTING_PENALTIES_FILE);
		Ruleset.registerRule(settings, Penalty.SETTING_FO_LIMIT);

		stats = new DefaultStatsModel(this);
		stats.addScoreBoardListener(this);
		frontendSettings = new DefaultFrontendSettingsModel(this);
		frontendSettings.addScoreBoardListener(this);
		reset();
		addInPeriodListeners();
		xmlScoreBoard = new XmlScoreBoard(this);
		//Button may have a label from autosave but undo will not work after restart
		setLabel(BUTTON_UNDO, ACTION_NONE);
	}

	public String getProviderName() { return "ScoreBoard"; }
	public Class<ScoreBoard> getProviderClass() { return ScoreBoard.class; }
	public String getProviderId() { return ""; }

	public XmlScoreBoard getXmlScoreBoard() { return xmlScoreBoard; }
	
	public static Object getCoreLock() { return coreLock; }

	public ScoreBoard getScoreBoard() { return this; }

	public void reset() {
		synchronized (coreLock) {
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
			snapshot = null;
			
			settings.reset();
			stats.reset();
			// Custom settings are not reset, as broadcast overlays settings etc.
			// shouldn't be lost just because the next game is starting.
			
			setLabel(BUTTON_START, ACTION_START_JAM);
			setLabel(BUTTON_STOP, ACTION_LINEUP);
			setLabel(BUTTON_TIMEOUT, ACTION_TIMEOUT);
			setLabel(BUTTON_UNDO, ACTION_NONE);
		}
	}

	public boolean isInPeriod() { return inPeriod; }
	public void setInPeriod(boolean p) {
		synchronized (coreLock) {
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
		addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_TIMEOUT, "Time", timeoutClockListener));
	}

	public boolean isInOvertime() { return inOvertime; }
	public void setInOvertime(boolean o) {
		synchronized (coreLock) {
			if (o == inOvertime) { return; }
			Boolean last = new Boolean(inOvertime);
			inOvertime = o;
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_IN_OVERTIME, new Boolean(inOvertime), last));
			ClockModel lc = getClockModel(Clock.ID_LINEUP);
			if (!o && lc.isCountDirectionDown()) {
				lc.setMaximumTime(settings.getLong(SETTING_LINEUP_DURATION));
			}
		}
	}
	public void startOvertime() {
		synchronized (coreLock) {
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
			setLabels(ACTION_START_JAM, ACTION_NONE, ACTION_TIMEOUT);
			
			requestBatchStart();
			setInOvertime(true);
			_endTimeout(false);
			long otLineupTime = settings.getLong(SETTING_OVERTIME_LINEUP_DURATION);
			if (lc.getMaximumTime() < otLineupTime) {
				lc.setMaximumTime(otLineupTime);
			}
			_startLineup();
			requestBatchEnd();
		}
	}

	public boolean isOfficialScore() { return officialScore; }
	public void setOfficialScore(boolean o) {
		synchronized (coreLock) {
			Boolean last = new Boolean(officialScore);
			officialScore = o;
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_OFFICIAL_SCORE, new Boolean(officialScore), last));
		}
	}

	public void startJam() {
		synchronized (coreLock) {
			if (!getClock(Clock.ID_JAM).isRunning()) {
				createSnapshot(ACTION_START_JAM);
				setLabels(ACTION_NONE, ACTION_STOP_JAM, ACTION_TIMEOUT);
				_startJam();
			}
		}
	}
	public void stopJamTO() {
		synchronized (coreLock) {
			ClockModel jc = getClockModel(Clock.ID_JAM);
			ClockModel lc = getClockModel(Clock.ID_LINEUP);
			ClockModel tc = getClockModel(Clock.ID_TIMEOUT);

			if (jc.isRunning()) {
				createSnapshot(ACTION_STOP_JAM);
				setLabels(ACTION_START_JAM, ACTION_NONE, ACTION_TIMEOUT);
				_endJam(false);
			} else if (tc.isRunning()) {
				createSnapshot(ACTION_STOP_TO);
				setLabels(ACTION_START_JAM, ACTION_NONE, ACTION_TIMEOUT);
				_endTimeout(false);
			} else if (!lc.isRunning()) {
				createSnapshot(ACTION_LINEUP);
				setLabels(ACTION_START_JAM, ACTION_NONE, ACTION_TIMEOUT);
				_startLineup();
			}
		}
	}
	public void timeout() { 
		synchronized (coreLock) {
			if (getClock(Clock.ID_TIMEOUT).isRunning()) {
				createSnapshot(ACTION_RE_TIMEOUT);
			} else {
				createSnapshot(ACTION_TIMEOUT);
			}
			setLabels(ACTION_START_JAM, ACTION_STOP_TO, ACTION_RE_TIMEOUT);
			_startTimeout();
		}
	}
	public void setTimeoutType(String owner, boolean review) {
		synchronized (coreLock) {
			ClockModel tc = getClockModel(Clock.ID_TIMEOUT);
			ClockModel pc = getClockModel(Clock.ID_PERIOD);
	
			requestBatchStart();
			if (!tc.isRunning()) {
				timeout();
			}
			//if overridden TO type is Team TO or OR, credit it back
			for (TeamModel tm : getTeamModels()) {
				if (tm.getId().equals(getTimeoutOwner())) {
					if (isOfficialReview()) {
						tm.changeOfficialReviews(1);
					} else {
						tm.changeTimeouts(1);
					}
				}
			}
			setTimeoutOwner(owner);
			setOfficialReview(review);
			if (!settings.getBoolean(SETTING_STOP_PC_ON_TO)) {
				boolean stopPc = false;
				if (!owner.equals(TIMEOUT_OWNER_NONE)) {
					if (owner.equals(TIMEOUT_OWNER_OTO) ) {
						if (settings.getBoolean(SETTING_STOP_PC_ON_OTO)) {
							stopPc = true;
						}
					} else {
						if (review && settings.getBoolean(SETTING_STOP_PC_ON_OR)) {
							stopPc = true;
						}
						if (!review && settings.getBoolean(SETTING_STOP_PC_ON_TTO)) {
							stopPc = true;
						}
					}
				}
				if (stopPc && pc.isRunning()) {
					pc.stop();
					pc.elapseTime(-tc.getTime());
				}
				if (!stopPc && !pc.isRunning()) {
					pc.elapseTime(tc.getTime());
					pc.start();
				}
			}
			requestBatchEnd();
		}
	}
	private void _preparePeriod() {
		ClockModel pc = getClockModel(Clock.ID_PERIOD);
		ClockModel jc = getClockModel(Clock.ID_JAM);
		ClockModel ic = getClockModel(Clock.ID_INTERMISSION);

		requestBatchStart();
		pc.setNumber(ic.getNumber()+1);
		pc.resetTime();
		restartPcAfterTimeout = false;
		if (settings.getBoolean(SETTING_JAM_NUMBER_PER_PERIOD)) {
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
			setLabels(ACTION_START_JAM, ACTION_LINEUP, ACTION_TIMEOUT);
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
		_endTimeout(false);
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
			//end the previous timeout before starting a new one
			_endTimeout(true);
		}
		
		if (settings.getBoolean(SETTING_STOP_PC_ON_TO)) {
			pc.stop();
		}
		_endLineup();
		_endJam(false);
		_endIntermission(false);
		setInPeriod(true);
		tc.startNext();
		requestBatchEnd();
	}
	private void _endTimeout(boolean timeoutFollows) {
		ClockModel tc = getClockModel(Clock.ID_TIMEOUT);
		ClockModel pc = getClockModel(Clock.ID_PERIOD);

		if (!tc.isRunning()) { return; }
		
		requestBatchStart();
		if (!frontendSettings.get(FRONTEND_SETTING_CLOCK_AFTER_TIMEOUT).equals(Clock.ID_TIMEOUT)) {
			tc.stop();
		}
		if (getTimeoutOwner() != TIMEOUT_OWNER_NONE && getTimeoutOwner() != TIMEOUT_OWNER_OTO) {
			restartPcAfterTimeout = false;
		}
		setTimeoutOwner(TIMEOUT_OWNER_NONE);
		setOfficialReview(false);
		if (!timeoutFollows) {
			if (pc.isTimeAtEnd()) {
				_possiblyEndPeriod();
			} else {
				if (restartPcAfterTimeout) {
					pc.start();
				}
				if (frontendSettings.get(FRONTEND_SETTING_CLOCK_AFTER_TIMEOUT).equals(Clock.ID_LINEUP)) {
					_startLineup();
				}
			}
		}
		requestBatchEnd();
	}
	private void _startIntermission() {
		ClockModel pc = getClockModel(Clock.ID_PERIOD);
		ClockModel ic = getClockModel(Clock.ID_INTERMISSION);

		requestBatchStart();
		ic.setNumber(pc.getNumber());
		long duration = settings.getLong(SETTING_INTERMISSION_DURATION + "1");
		if (!settings.get(SETTING_INTERMISSION_TYPE_SEQUENCE).equals("")) {
			String[] sequence = settings.get(SETTING_INTERMISSION_TYPE_SEQUENCE).split(",");
			if (sequence.length >= ic.getNumber()) {
				int index = Integer.parseInt(sequence[ic.getNumber()-1]);
				if (index > 0 && index < 6) {
					duration = settings.getLong(SETTING_INTERMISSION_DURATION + index);
				}
			}
		}
		ic.setMaximumTime(duration);
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
		
		long bufferTime = settings.getLong(SETTING_AUTO_START_BUFFER); 
		long triggerTime = bufferTime + (isInOvertime() ? 
					settings.getLong(SETTING_OVERTIME_LINEUP_DURATION) :
					settings.getLong(SETTING_LINEUP_DURATION));

		requestBatchStart();
		if (lc.getTimeElapsed() >= triggerTime) {
			if (Boolean.parseBoolean(settings.get(SETTING_AUTO_START_JAM))) {
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
		snapshot = new ScoreBoardSnapshot(this, type);
		setLabel(BUTTON_UNDO, UNDO_PREFIX + type);
	}
	protected void restoreSnapshot() {
		ScoreBoardClock.getInstance().rewindTo(snapshot.getSnapshotTime());
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
		setLabels(snapshot.getStartLabel(), snapshot.getStopLabel(), snapshot.getTimeoutLabel());
		setLabel(BUTTON_UNDO, ACTION_NONE);
		snapshot = null;
	}
	public void clockUndo() {
		synchronized (coreLock) {
			if (snapshot == null) { return; }
			requestBatchStart();
			ScoreBoardClock.getInstance().stop();
			restoreSnapshot();
			ScoreBoardClock.getInstance().start(true);
			requestBatchEnd();
		}
	}
	
	protected void setLabel(String id, String value) {
		settings.set(id, value);
	}
	protected void setLabels(String startLabel, String stopLabel, String timeoutLabel) {
		setLabel(BUTTON_START, startLabel);
		setLabel(BUTTON_STOP, stopLabel);
		setLabel(BUTTON_TIMEOUT, timeoutLabel);
	}

	public Ruleset _getRuleset() {
		synchronized (coreLock) {
			if (ruleset == null) {
				ruleset = Ruleset.findRuleset(null, true);
			}
			return ruleset;
		}
	}
	public String getRuleset() { return _getRuleset().getId().toString(); }
	public void setRuleset(String id) {
		synchronized (coreLock) {
			String last = getRuleset();
			ruleset = Ruleset.findRuleset(id, true);
			ruleset.apply(false);
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_RULESET, ruleset.getId().toString(), last));
		}
	}

	public void penalty(String teamId, String skaterId, String penaltyId, boolean fo_exp, int period, int jam, String code){
		synchronized (coreLock) {
			getTeamModel(teamId).penalty(skaterId, penaltyId, fo_exp, period, jam, code);
		}
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
		synchronized (coreLock) {
// FIXME - don't auto-create!	 return null instead - or throw exception.	Need to update all callers to handle first.
			if (!clocks.containsKey(id))
				createClockModel(id);

			return clocks.get(id);
		}
	}

	public TeamModel getTeamModel(String id) {
		synchronized (coreLock) {
// FIXME - don't auto-create!	 return null instead - or throw exception.	Need to update all callers to handle first.
			if (!teams.containsKey(id))
				createTeamModel(id);

			return teams.get(id);
		}
	}

	public String getTimeoutOwner() { return timeoutOwner; }
	public void setTimeoutOwner(String owner) {
		synchronized (coreLock) {
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
		synchronized (coreLock) {
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

	protected HashMap<String,ClockModel> clocks = new HashMap<String,ClockModel>();
	protected HashMap<String,TeamModel> teams = new HashMap<String,TeamModel>();

	protected ScoreBoardSnapshot snapshot = null;
	
	protected static Object coreLock = new Object();

	protected String timeoutOwner;
	protected boolean officialReview;
	protected boolean restartPcAfterTimeout;

	protected boolean inPeriod = false;

	protected boolean inOvertime = false;

	protected boolean officialScore = false;

	protected Ruleset ruleset = null;
	protected DefaultSettingsModel settings = null;
	protected DefaultFrontendSettingsModel frontendSettings = null;
	protected DefaultStatsModel stats = null;

	protected XmlScoreBoard xmlScoreBoard;

	protected ScoreBoardListener periodEndListener = new ScoreBoardListener() {
		public void scoreBoardChange(ScoreBoardEvent event) {
			if (settings.getBoolean(SETTING_PERIOD_END_BETWEEN_JAMS)) {			
				_possiblyEndPeriod();
			}
		}
	};
	protected ScoreBoardListener jamEndListener = new ScoreBoardListener() {
		public void scoreBoardChange(ScoreBoardEvent event) {
			ClockModel jc = getClockModel(Clock.ID_JAM);
			if (jc.isTimeAtEnd() && settings.getBoolean(SETTING_AUTO_END_JAM)) {
				//clock has run down naturally
				requestBatchStart();
				setLabels(ACTION_NONE, ACTION_STOP_JAM, ACTION_TIMEOUT);
				_endJam(true);
				requestBatchEnd();
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
			if (settings.getBoolean(SETTING_AUTO_START)) {
				_possiblyAutostart();
			}
		}
	};
	protected ScoreBoardListener timeoutClockListener = new ScoreBoardListener() {
		public void scoreBoardChange(ScoreBoardEvent event) {
			if (settings.getBoolean(SETTING_AUTO_END_TTO) &&
					!getTimeoutOwner().equals(TIMEOUT_OWNER_NONE) &&
					!getTimeoutOwner().equals(TIMEOUT_OWNER_OTO) &&
					(long)event.getValue() == settings.getLong(SETTING_TTO_DURATION)) {
				stopJamTO();
			}
			if ((long)event.getValue() == settings.getLong(SETTING_STOP_PC_AFTER) &&
					getClock(Clock.ID_PERIOD).isRunning()) {
				getClockModel(Clock.ID_PERIOD).stop();
			}
		}
	};

	public static class ScoreBoardSnapshot {
		private ScoreBoardSnapshot(DefaultScoreBoardModel sbm, String type) {
			snapshotTime = ScoreBoardClock.getInstance().getCurrentTime();
			this.type = type; 
			timeoutOwner = sbm.getTimeoutOwner();
			isOfficialReview = sbm.isOfficialReview();
			inOvertime = sbm.isInOvertime();
			inPeriod = sbm.isInPeriod();
			restartPcAfterTo = sbm.restartPcAfterTimeout;
			startLabel = sbm.getSettings().get(BUTTON_START);
			stopLabel = sbm.getSettings().get(BUTTON_STOP);
			timeoutLabel = sbm.getSettings().get(BUTTON_TIMEOUT);
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
		public String getStartLabel() { return startLabel; }
		public String getStopLabel() { return stopLabel; }
		public String getTimeoutLabel() { return timeoutLabel; }
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
		protected String startLabel;
		protected String stopLabel;
		protected String timeoutLabel;
		protected Map<String, ClockModel.ClockSnapshotModel> clockSnapshots;
		protected Map<String, TeamModel.TeamSnapshotModel> teamSnapshots;
	}

	public static final String DEFAULT_TIMEOUT_OWNER = TIMEOUT_OWNER_NONE;
}

