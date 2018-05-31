package com.carolinarollergirls.scoreboard.defaults;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.carolinarollergirls.scoreboard.Clock;
import com.carolinarollergirls.scoreboard.Ruleset;
import com.carolinarollergirls.scoreboard.ScoreBoard;
import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.Settings;
import com.carolinarollergirls.scoreboard.Team;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.model.ClockModel;
import com.carolinarollergirls.scoreboard.model.ScoreBoardModel;
import com.carolinarollergirls.scoreboard.model.SettingsModel;
import com.carolinarollergirls.scoreboard.model.TeamModel;
import com.carolinarollergirls.scoreboard.states.ClockState;
import com.carolinarollergirls.scoreboard.states.StateSet;
import com.carolinarollergirls.scoreboard.states.TeamState;
import com.carolinarollergirls.scoreboard.xml.XmlScoreBoard;

public class DefaultScoreBoardModel extends DefaultScoreBoardEventProvider implements ScoreBoardModel
{
	public DefaultScoreBoardModel() {
		settings = new DefaultSettingsModel(this, this);
		Ruleset.registerRule(settings, "ScoreBoard." + Clock.ID_INTERMISSION + ".PreGame");
		Ruleset.registerRule(settings, "ScoreBoard." + Clock.ID_INTERMISSION + ".Intermission");
		Ruleset.registerRule(settings, "ScoreBoard." + Clock.ID_INTERMISSION + ".Unofficial");
		Ruleset.registerRule(settings, "ScoreBoard." + Clock.ID_INTERMISSION + ".Official");
		Ruleset.registerRule(settings, SETTING_INTERMISSION_TIME);
		Ruleset.registerRule(settings, SETTING_RESET_JAM_NUMBER);
		Ruleset.registerRule(settings, SETTING_LU_TIME);
		Ruleset.registerRule(settings, SETTING_OT_LU_TIME);
		Ruleset.registerRule(settings, SETTING_AUTOSTART);
		Ruleset.registerRule(settings, SETTING_AUTOSTART_TYPE);
		Ruleset.registerRule(settings, SETTING_AUTOSTART_BUFFER_TIME);
		Ruleset.registerRule(settings, SETTING_LINEUP_AFTER_TO);
		Ruleset.registerRule(settings, SETTING_STOP_CLOCK_TO);
		Ruleset.registerRule(settings, SETTING_STOP_CLOCK_OTO);
		Ruleset.registerRule(settings, SETTING_STOP_CLOCK_TTO);
		Ruleset.registerRule(settings, SETTING_STOP_CLOCK_OR);
		Ruleset.registerRule(settings, SETTING_STOP_CLOCK_DURATION);
		Ruleset.registerRule(settings, SETTING_UNDO_LIMIT);
		Ruleset.registerRule(settings, SETTING_UNDO_STACK_SIZE);

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
		Ruleset.registerRule(settings, "ScoreBoard.Overlay.TeamLogos");
		Ruleset.registerRule(settings, "ScoreBoard.Overlay.LogoBackground");

		reset();
		addInPeriodListeners();
		xmlScoreBoard = new XmlScoreBoard(this);
	}

	public String getProviderName() { return "ScoreBoard"; }
	public Class<?> getProviderClass() { return ScoreBoard.class; }
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

		undoStack = new ArrayDeque<StateSet>(20);
		setTimeoutOwner(DEFAULT_TIMEOUT_OWNER);
		setOfficialReview(false);
		setInPeriod(false);
		setInOvertime(false);

		settings.reset();
		setLabel(BUTTON_START, LABEL_START);
		setLabel(BUTTON_STOP, LABEL_LINEUP);
		setLabel(BUTTON_TIMEOUT, LABEL_TIMEOUT);
		setLabel(BUTTON_UNDO, LABEL_EMPTY);
	}

	public boolean isInPeriod() { return inPeriod; }
	public void setInPeriod(boolean p) {
		synchronized (inPeriodLock) {
			Boolean last = new Boolean(inPeriod);
			inPeriod = p;
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_IN_PERIOD, new Boolean(inPeriod), last));
		}
	}
	protected void addInPeriodListeners() {
		addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_PERIOD, "Running", Boolean.TRUE, periodStartListener));
		addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_PERIOD, "Running", Boolean.FALSE, periodEndListener));
		addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_JAM, "Running", Boolean.FALSE, periodEndListener));
		addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_JAM, "Running", Boolean.TRUE, jamStartListener));
		addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_TIMEOUT, "Running", Boolean.FALSE, periodEndListener));
		addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_TIMEOUT, "Running", Boolean.TRUE, timeoutStartListener));
		addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_TIMEOUT, "Running", Boolean.FALSE, timeoutEndListener));
		addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_INTERMISSION, "Running", Boolean.FALSE, intermissionEndListener));
		addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_LINEUP, "Time", lineupClockListener));
		addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_TIMEOUT, "Time", timeoutClockListener));
	}

	public boolean isInOvertime() { return inOvertime; }
	public void setInOvertime(boolean o) {
		synchronized (inOvertimeLock) {
			Boolean last = new Boolean(inOvertime);
			inOvertime = o;
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_IN_OVERTIME, new Boolean(inOvertime), last));
		}
	}
	public void startOvertime() {
		synchronized (runLock) {
			ClockModel pc = getClockModel(Clock.ID_PERIOD);
			ClockModel jc = getClockModel(Clock.ID_JAM);
			ClockModel lc = getClockModel(Clock.ID_LINEUP);
			ClockModel ic = getClockModel(Clock.ID_INTERMISSION);
			ClockModel tc = getClockModel(Clock.ID_TIMEOUT);
			
			//only start overtime, if we are at the after the last period
			if (pc.isRunning() || jc.isRunning())
				return;
			if (pc.getNumber() < pc.getMaximumNumber())
				return;
			if (pc.getTimeElapsed() < pc.getMaximumTime())
				return;

			requestBatchStart();
			saveClockState(LABEL_UN_OVERTIME);
			setLabel(BUTTON_START, LABEL_START);
			setLabel(BUTTON_STOP, LABEL_EMPTY);
			setLabel(BUTTON_TIMEOUT, LABEL_TIMEOUT);
			setInPeriod(true);
			setInOvertime(true);
			boolean fromTimeout = tc.isRunning();
			ic.stop();
			tc.stop();
			long otLineupTime = settings.getLong(SETTING_OT_LU_TIME);
			if (lc.getMaximumTime() < otLineupTime) {
				lc.setMaximumTime(otLineupTime);
			}
			lc.resetTime();
			if (!fromTimeout) {
				lc.elapseTime(ic.getTimeElapsed());
			}
			lc.start();
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
		requestBatchStart();
		saveClockState(LABEL_UN_START);
		
		if (getClockModel(Clock.ID_TIMEOUT).isRunning()) {
			_stopTimeout();
		}
		
		_startJam();
		requestBatchEnd();
	}
	private void _startJam() {
		synchronized (runLock) {
			if (!getClock(Clock.ID_JAM).isRunning()) {
				requestBatchStart();
				ClockModel pc = getClockModel(Clock.ID_PERIOD);
				ClockModel jc = getClockModel(Clock.ID_JAM);
				ClockModel ic = getClockModel(Clock.ID_INTERMISSION);
				
				setLabel(BUTTON_START, LABEL_EMPTY);
				setLabel(BUTTON_STOP, LABEL_STOP_JAM);
				setLabel(BUTTON_TIMEOUT, LABEL_TIMEOUT);

				ic.stop();
				pc.start();
				jc.startNew();

				for (TeamModel tm : teams.values()) {
					tm.startJam();
				}
				requestBatchEnd();

				ScoreBoardManager.gameSnapshot();
			}
		}
	}
	public void stopJam() {
		synchronized (runLock) {
			requestBatchStart();
			if (getClockModel(Clock.ID_JAM).isRunning()) {
				saveClockState(LABEL_UN_STOP_JAM);
				_stopJam();
			} else if (getClockModel(Clock.ID_TIMEOUT).isRunning()) {
				saveClockState(LABEL_UN_STOP_TO);
				_stopTimeout();
			} else if (!getClockModel(Clock.ID_LINEUP).isRunning()) {
				saveClockState(LABEL_UN_LINEUP);
				_startLineup();
			}
			requestBatchEnd();
		}
	}
	private void _stopJam() {
		synchronized (runLock) {
			ScoreBoardManager.gameSnapshot(true);

			requestBatchStart();
			setLabel(BUTTON_START, LABEL_START);
			setLabel(BUTTON_STOP, LABEL_EMPTY);

			ClockModel pc = getClockModel(Clock.ID_PERIOD);
			ClockModel jc = getClockModel(Clock.ID_JAM);
			ClockModel lc = getClockModel(Clock.ID_LINEUP);

			jc.stop();
			for (TeamModel tm : teams.values()) {
				tm.stopJam();
			}

			if (pc.isRunning()) {
				lc.startNew();
			}
			if (isInOvertime()) {
				setInOvertime(false);
			}
			requestBatchEnd();
		}
	}
	private void _stopTimeout() {
		synchronized (runLock) {
			requestBatchStart();
			setLabel(BUTTON_STOP, LABEL_EMPTY);
			setLabel(BUTTON_TIMEOUT, LABEL_TIMEOUT);

			ClockModel tc = getClockModel(Clock.ID_TIMEOUT);
			ClockModel lc = getClockModel(Clock.ID_LINEUP);
			ClockModel pc = getClockModel(Clock.ID_PERIOD);
			
			if (!pc.isTimeAtEnd() && settings.getBoolean(SETTING_LINEUP_AFTER_TO)) {
				lc.startNew();
			}
			if (restartPcAtTimeoutEnd) {
				restartPcAtTimeoutEnd = false;
				pc.start();
			}
			if (pc.isTimeAtEnd() || settings.getBoolean(SETTING_LINEUP_AFTER_TO)) {
				tc.stop();
			}
			requestBatchEnd();
		}
	}
	private void _startLineup() {
		synchronized (runLock) {
			requestBatchStart();
			setLabel(BUTTON_STOP, LABEL_EMPTY);

			ClockModel lc = getClockModel(Clock.ID_LINEUP);
			ClockModel ic = getClockModel(Clock.ID_INTERMISSION);

			ic.stop();
			lc.startNew();
			requestBatchEnd();
		}
	}

	public void timeout() { timeout(null); }
	public void timeout(TeamModel team) { timeout(team, false); }
	public void timeout(TeamModel team, boolean review) {
		requestBatchStart();
		if (!getClockModel(Clock.ID_TIMEOUT).isRunning()) {
			saveClockState(LABEL_UN_TO);

			if (getClockModel(Clock.ID_JAM).isRunning()) {
				_stopJam();
			}
			
			_startTimeout();
		}

		_setTimeoutType(team, review);
		requestBatchEnd();
	}
	private void _startTimeout() {
		synchronized (runLock) {
			requestBatchStart();
			ClockModel tc = getClockModel(Clock.ID_TIMEOUT);
			ClockModel pc = getClockModel(Clock.ID_PERIOD);
			ClockModel lc = getClockModel(Clock.ID_LINEUP);
	
			setLabel(BUTTON_START, LABEL_START);
			setLabel(BUTTON_STOP, LABEL_STOP_TO);
			setLabel(BUTTON_TIMEOUT, LABEL_OTO);
			setTimeoutOwner(DEFAULT_TIMEOUT_OWNER);
	
			if (settings.getBoolean(SETTING_STOP_CLOCK_TO)) {
				pc.stop();
			}
			lc.stop();
			tc.startNew();
			
			if (pc.getTimeRemaining() + lc.getTimeElapsed() < 30000) {
				//last Jam did end with less than 30s on the period clock. If this is just an OTO, restart pc afterwards
				restartPcAtTimeoutEnd = true;
			}
			requestBatchEnd();
		}
	}
	private void _setTimeoutType(TeamModel team, boolean review) {
		synchronized (runLock) {
			requestBatchStart();
			ClockModel tc = getClockModel(Clock.ID_TIMEOUT);
			ClockModel pc = getClockModel(Clock.ID_PERIOD);

			String newOwner = (null==team ? (tc.isRunning() ? OFFICIAL_TIMEOUT_OWNER : DEFAULT_TIMEOUT_OWNER) : team.getId());
			
			if (team != null) {
				setLabel(BUTTON_TIMEOUT, LABEL_OTO);
				restartPcAtTimeoutEnd = false;
			} else if (newOwner.equals(OFFICIAL_TIMEOUT_OWNER)) {
				setLabel(BUTTON_TIMEOUT, LABEL_EMPTY);
			} else {
				setLabel(BUTTON_TIMEOUT, LABEL_OTO);
			}
			
			if (pc.isRunning() && (
					(team != null && !review && settings.getBoolean(SETTING_STOP_CLOCK_TTO)) ||
					 (team != null && review && settings.getBoolean(SETTING_STOP_CLOCK_OR)) ||
					 (newOwner == OFFICIAL_TIMEOUT_OWNER && settings.getBoolean(SETTING_STOP_CLOCK_OTO)))) {
				//we have a rule set that stops the period clock only for some types of timeouts and this is one of them
				pc.stop();
				if (tc.getTimeElapsed() < 15000) {
					//if type was changed within the first 15 seconds of timeout assume that it was this type
					//of timeout from the start and adjust the period clock accordingly
					pc.elapseTime(-tc.getTimeElapsed());
				}
			}
			
			setTimeoutOwner(newOwner);
			setOfficialReview(review);

			for (TeamModel tm : teams.values()) {
				tm.setInTimeout(tm == team  && !review);
				tm.setInOfficialReview(tm == team && review);
			}
			
			requestBatchEnd();

			ScoreBoardManager.gameSnapshot();
		}
	}
	
	protected void saveClockState(String type) {
		if (undoLabelTimer != null) {
			//If the last change can still be cancelled, stop the task that resets the label.
			undoLabelTimer.cancel();
		}
		while (undoStack.size() >= settings.getLong(SETTING_UNDO_STACK_SIZE)) {
			//keep the size of the stack limited
			undoStack.removeLast();
		}
		HashMap<String, ClockState> clockStates = new HashMap<String, ClockState>();
		for (ClockModel clock : getClockModels()) {
			clockStates.put(clock.getId(), clock.getState());
		}
		HashMap<String, TeamState> teamStates = new HashMap<String, TeamState>();
		for (TeamModel team : getTeamModels()) {
			teamStates.put(team.getId(), team.getState());
		}
		undoStack.push(new StateSet(type, getLabel(BUTTON_START),	getLabel(BUTTON_STOP),
				getLabel(BUTTON_TIMEOUT), timeoutOwner, officialReview, inOvertime,
				restartPcAtTimeoutEnd, inPeriod, clockStates, teamStates));
		setLabel(BUTTON_UNDO, type);

		resetUndoLabelIn(settings.getLong(SETTING_UNDO_LIMIT));
	}
	
	public void undoClockChange() {
		synchronized (runLock) {
			if (undoStack.isEmpty()) return;

			if (undoLabelTimer != null) {
				undoLabelTimer.cancel();
			}
			requestBatchStart();
			StateSet savedState = undoStack.pop();
			if (System.currentTimeMillis() - savedState.getTimestamp() < settings.getLong(SETTING_UNDO_LIMIT)) {
				setLabel(BUTTON_START, savedState.getStartType());
				setLabel(BUTTON_STOP, savedState.getStopType());
				setLabel(BUTTON_TIMEOUT, savedState.getTimeoutType());
				setTimeoutOwner(savedState.getTimeoutOwner());
				setOfficialReview(savedState.isOfficialReview());
				setInOvertime(savedState.inOvertime());
				restartPcAtTimeoutEnd = savedState.restartPc();
				setInPeriod(savedState.inPeriod());
				for (TeamState ts : savedState.getTeamStates().values()) {
					getTeamModel(ts.getId()).undo(ts);
				}
				Collection<ClockState> clocks = savedState.getClockStates().values();
				for (ClockState cs : clocks) {
					//unstart clocks before unstop, as unstop may cause secondary effects
					// from time having ticked away
					if (!cs.isRunning())
						getClockModel(cs.getId()).undo(cs);
				}
				for (ClockState cs : clocks) {
					//unstop period clock last in order to avoid ending the period 
					// even though we are in a Jam
					if (cs.isRunning() && !cs.getId().equals(Clock.ID_PERIOD))
						getClockModel(cs.getId()).undo(cs);
				}
				ClockState pc = savedState.getClockState(Clock.ID_PERIOD);
				if (pc.isRunning()) {
					getClockModel(Clock.ID_PERIOD).undo(pc);
				}
			}
			requestBatchEnd();
			if (!undoStack.isEmpty()) {
				StateSet previous = undoStack.peek();
				long availableFor = settings.getLong(SETTING_UNDO_LIMIT) - (System.currentTimeMillis() - previous.getTimestamp());
				if (availableFor > 0) {
					setLabel(BUTTON_UNDO, previous.getType());
					resetUndoLabelIn(availableFor);
				} else {
					setLabel(BUTTON_UNDO, LABEL_EMPTY);
				} 
			} else {
				setLabel(BUTTON_UNDO, LABEL_EMPTY);
			}
		}
	}
	
	protected void resetUndoLabelIn(long delay) {
		undoLabelTimer = new Timer();
		undoLabelTimer.schedule(new TimerTask(){
			@Override
			public void run() {
				setLabel(BUTTON_UNDO, LABEL_EMPTY);
			}
		}, delay);		
	}
	
	protected String getLabel(String id) {
		return settings.get(id);
	}
	protected void setLabel(String id, String value) {
		settings.set(id, value);
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

	public Settings getSettings() { return (Settings)settings; }
	public SettingsModel getSettingsModel() { return settings; }

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
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_TIMEOUT_OWNER, timeoutOwner, last));
		}
	}
	public boolean isOfficialReview() { return officialReview; }
	public void setOfficialReview(boolean official) {
		synchronized (officialReviewLock) {
			boolean last = officialReview;
			officialReview = official;
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

	protected Object runLock = new Object();
	protected Deque<StateSet> undoStack = new ArrayDeque<StateSet>();
	protected Timer undoLabelTimer = null;

	protected String timeoutOwner;
	protected Object timeoutOwnerLock = new Object();
	protected boolean officialReview;
	protected Object officialReviewLock = new Object();
	protected boolean restartPcAtTimeoutEnd = false;

	protected boolean inPeriod = false;
	protected Object inPeriodLock = new Object();

	protected boolean inOvertime = false;
	protected Object inOvertimeLock = new Object();

	protected boolean officialScore = false;
	protected Object officialScoreLock = new Object();

	protected Ruleset ruleset = null;
	protected Object rulesetLock = new Object();
	protected DefaultSettingsModel settings = null;

	protected XmlScoreBoard xmlScoreBoard;

	protected ScoreBoardListener periodStartListener = new ScoreBoardListener() {
		public void scoreBoardChange(ScoreBoardEvent event) {
			if (!isInPeriod())
				setInPeriod(true);
		}
	};
	protected ScoreBoardListener periodEndListener = new ScoreBoardListener() {
		public void scoreBoardChange(ScoreBoardEvent event) {
			ClockModel pc = getClockModel(Clock.ID_PERIOD);
			ClockModel jc = getClockModel(Clock.ID_JAM);
			ClockModel tc = getClockModel(Clock.ID_TIMEOUT);
			ClockModel ic = getClockModel(Clock.ID_INTERMISSION);
			ClockModel lc = getClockModel(Clock.ID_LINEUP);

			requestBatchStart();
			if (event.getProvider() == jc && !jc.isRunning() && jc.isTimeAtEnd()) {
				//This listener also listens to jam ends. If jam clock has run down, run end of jam routine.
				_stopJam();
			}

			if (isInPeriod() && !pc.isRunning() && pc.isTimeAtEnd() && !jc.isRunning() && !tc.isRunning()) {
				setLabel(BUTTON_STOP, LABEL_LINEUP);
				setInPeriod(false);
				setOfficialScore(false);
				lc.stop();
				if (!ic.isRunning()) {
					ic.setNumber(pc.getNumber());
					ic.setMaximumTime(settings.getLong(SETTING_INTERMISSION_TIME));
					ic.resetTime();
					ic.start();
				}
			}
			requestBatchEnd();
		}
	};
	protected ScoreBoardListener jamStartListener = new ScoreBoardListener() {
		public void scoreBoardChange(ScoreBoardEvent event) {
			ClockModel lc = getClockModel(Clock.ID_LINEUP);
			if (lc.isRunning())
				lc.stop();
		}
	};
	protected ScoreBoardListener timeoutStartListener = new ScoreBoardListener() {
		public void scoreBoardChange(ScoreBoardEvent event) {
			ClockModel ic = getClockModel(Clock.ID_INTERMISSION);
			ClockModel tc = getClockModel(Clock.ID_TIMEOUT);

			if (!isInPeriod() && ic.isRunning() && tc.isRunning()) {
				ic.stop();
				setInPeriod(true);
			}
		}
	};
	protected ScoreBoardListener timeoutEndListener = new ScoreBoardListener() {
		public void scoreBoardChange(ScoreBoardEvent event) {
			requestBatchStart();
			setTimeoutOwner(DEFAULT_TIMEOUT_OWNER);
			setOfficialReview(false);
			for (TeamModel tm : teams.values()) {
				tm.setInTimeout(false);
				tm.setInOfficialReview(false);
			}
			requestBatchEnd();
		}
	};
	protected ScoreBoardListener intermissionEndListener = new ScoreBoardListener() {
		public void scoreBoardChange(ScoreBoardEvent event) {
			ClockModel ic = getClockModel(Clock.ID_INTERMISSION);
			ClockModel pc = getClockModel(Clock.ID_PERIOD);
			ClockModel jc = getClockModel(Clock.ID_JAM);
			//Only consider it intermission end, if more than half of the intermission has run
			// and there is another period.
			//Otherwise assume previous period is extended
			if (ic.getTimeRemaining() < settings.getLong(SETTING_INTERMISSION_TIME)/2
					&& pc.getNumber() != pc.getMaximumNumber()) {
				requestBatchStart();
				pc.changeNumber(1);
				pc.resetTime();
				if (settings.getBoolean(SETTING_RESET_JAM_NUMBER)) {
					jc.setNumber(jc.getMinimumNumber());
				}
				for (TeamModel tm : teams.values()) {
					tm.resetTimeouts(false);
				}
				requestBatchEnd();
			}
		}
	};
	protected ScoreBoardListener lineupClockListener = new ScoreBoardListener() {
		public void scoreBoardChange(ScoreBoardEvent event) {
			if (settings.getBoolean(SETTING_AUTOSTART)) { 
				//auto start jam or timeout after lineup + buffer time has elapsed
				ClockModel lc = getClockModel(Clock.ID_LINEUP);
				long bufferTime = settings.getLong(SETTING_AUTOSTART_BUFFER_TIME);
				long triggerTime = bufferTime + 
						(isInOvertime() ? settings.getLong(SETTING_OT_LU_TIME) : settings.getLong(SETTING_LU_TIME));
				if (lc.getTimeElapsed() >= triggerTime) {
					if (settings.getBoolean(SETTING_AUTOSTART_TYPE)) {
						//auto start jam
						requestBatchStart();
						ClockModel jc = getClockModel(Clock.ID_JAM);
						startJam();
						jc.elapseTime(bufferTime);
						requestBatchEnd();
					} else {
						//auto start timeout
						requestBatchStart();
						ClockModel pc = getClockModel(Clock.ID_PERIOD);
						ClockModel tc = getClockModel(Clock.ID_TIMEOUT);
						timeout();
						pc.elapseTime(-bufferTime);
						tc.elapseTime(bufferTime);
						requestBatchEnd();
					}
				}
			}
		}
	};
	protected ScoreBoardListener timeoutClockListener = new ScoreBoardListener() {
		public void scoreBoardChange(ScoreBoardEvent event) {
			ClockModel pc = getClockModel(Clock.ID_PERIOD);
			ClockModel tc = getClockModel(Clock.ID_TIMEOUT);
			if (pc.isRunning()) { 
				//We are in a timeout that keeps the period clock running. Check if it should stop due to timeout duration.
				if (tc.getTimeElapsed() >= settings.getLong(SETTING_STOP_CLOCK_DURATION)) {
					pc.stop();
				}
			}
		}
	};

	public static final String DEFAULT_TIMEOUT_OWNER = "";
	public static final String OFFICIAL_TIMEOUT_OWNER = "O";
	
	public static final String SETTING_LINEUP_AFTER_TO = "ScoreBoard." + Clock.ID_TIMEOUT + ".ClockAfter";
	public static final String SETTING_STOP_CLOCK_TO = "ScoreBoard." + Clock.ID_TIMEOUT + ".StopOnUnspecifiedTO";
	public static final String SETTING_STOP_CLOCK_OTO = "ScoreBoard." + Clock.ID_TIMEOUT + ".StopOnOfficialTO";
	public static final String SETTING_STOP_CLOCK_TTO = "ScoreBoard." + Clock.ID_TIMEOUT + ".StopOnTeamTO";
	public static final String SETTING_STOP_CLOCK_OR = "ScoreBoard." + Clock.ID_TIMEOUT + ".StopOnOR";
	public static final String SETTING_STOP_CLOCK_DURATION = "ScoreBoard." + Clock.ID_TIMEOUT + ".StopAfter";
	public static final String SETTING_INTERMISSION_TIME = "ScoreBoard." + Clock.ID_INTERMISSION + ".Time";
	public static final String SETTING_RESET_JAM_NUMBER = "ScoreBoard." + Clock.ID_JAM + ".Number";
	public static final String SETTING_LU_TIME = "ScoreBoard." + Clock.ID_LINEUP + ".Time";
	public static final String SETTING_OT_LU_TIME = "ScoreBoard." + Clock.ID_LINEUP + ".OvertimeTime";
	public static final String SETTING_AUTOSTART = "ScoreBoard." + Clock.ID_LINEUP + ".AutoStart";
	public static final String SETTING_AUTOSTART_TYPE = "ScoreBoard." + Clock.ID_LINEUP + ".AutoStartType";
	public static final String SETTING_AUTOSTART_BUFFER_TIME = "ScoreBoard." + Clock.ID_LINEUP + ".AutoStartBufferTime";
	public static final String SETTING_UNDO_LIMIT = "Clock.UndoTimeLimit";
	public static final String SETTING_UNDO_STACK_SIZE = "Clock.UndoStackSize";
	
	public static final String BUTTON_START = "ScoreBoard.Button.StartLabel";
	public static final String BUTTON_STOP = "ScoreBoard.Button.StopLabel";
	public static final String BUTTON_TIMEOUT = "ScoreBoard.Button.TimeoutLabel";
	public static final String BUTTON_UNDO = "ScoreBoard.Button.UndoLabel";
	
	public static final String LABEL_EMPTY = "";
	public static final String LABEL_START = "Start Jam";
	public static final String LABEL_STOP_JAM = "Stop Jam";
	public static final String LABEL_STOP_TO = "Stop TO";
	public static final String LABEL_LINEUP = "Lineup";
	public static final String LABEL_TIMEOUT = "Timeout";
	public static final String LABEL_OTO = "Official TO";
	public static final String LABEL_UN_START = "Un-Start Jam";
	public static final String LABEL_UN_STOP_JAM = "Un-Stop Jam";
	public static final String LABEL_UN_LINEUP = "Un-Lineup";
	public static final String LABEL_UN_TO = "Un-Timeout";
	public static final String LABEL_UN_STOP_TO = "Un-Stop TO";
	public static final String LABEL_UN_OVERTIME = "Un-Overtime";
}

