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

import org.jdom.Element;

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
import com.carolinarollergirls.scoreboard.xml.XmlDocumentEditor;
import com.carolinarollergirls.scoreboard.xml.XmlScoreBoard;

public class DefaultScoreBoardModel extends DefaultScoreBoardEventProvider implements ScoreBoardModel
{
	public DefaultScoreBoardModel() {
		settings = new DefaultSettingsModel(this, this);
		Ruleset.registerRule(settings, "ScoreBoard." + Clock.ID_INTERMISSION + ".PreGame");
		Ruleset.registerRule(settings, "ScoreBoard." + Clock.ID_INTERMISSION + ".Intermission");
		Ruleset.registerRule(settings, "ScoreBoard." + Clock.ID_INTERMISSION + ".Unofficial");
		Ruleset.registerRule(settings, "ScoreBoard." + Clock.ID_INTERMISSION + ".Official");
		Ruleset.registerRule(settings, "Clock." + Clock.ID_INTERMISSION + ".Time");
		Ruleset.registerRule(settings, "Clock." + Clock.ID_JAM + ".Number");
		Ruleset.registerRule(settings, "Clock." + Clock.ID_LINEUP + ".Time");
		Ruleset.registerRule(settings, "Clock." + Clock.ID_LINEUP + ".OvertimeTime");
		Ruleset.registerRule(settings, "Clock." + Clock.ID_LINEUP + ".AutoStart");
		Ruleset.registerRule(settings, "Clock." + Clock.ID_LINEUP + ".AutoStartType");
		Ruleset.registerRule(settings, "Clock." + Clock.ID_LINEUP + ".BufferTime");

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

		setTimeoutOwner(DEFAULT_TIMEOUT_OWNER);
		setOfficialReview(false);
		setInPeriod(false);
		setInOvertime(false);

		settings.reset();
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
		addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_JAM, "Running", Boolean.FALSE, jamEndListener));
		addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_JAM, "Running", Boolean.FALSE, periodEndListener));
		addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_JAM, "Running", Boolean.TRUE, jamStartListener));
		addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_TIMEOUT, "Running", Boolean.FALSE, periodEndListener));
		addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_TIMEOUT, "Running", Boolean.TRUE, timeoutStartListener));
		addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_TIMEOUT, "Running", Boolean.FALSE, timeoutEndListener));
		addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_INTERMISSION, "Running", Boolean.FALSE, intermissionEndListener));
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
			saveClockState();
			requestBatchStart();
			ClockModel pc = getClockModel(Clock.ID_PERIOD);
			ClockModel jc = getClockModel(Clock.ID_JAM);
			ClockModel lc = getClockModel(Clock.ID_LINEUP);
			if (pc.isRunning() || jc.isRunning())
				return;
			if (pc.getNumber() < pc.getMaximumNumber())
				return;
			if (pc.getTime() > pc.getMinimumTime())
				return;
			pc.setTime(1000);
			setInPeriod(true);
			setInOvertime(true);
			getClockModel(Clock.ID_INTERMISSION).stop();
			long otLineupTime = Long.parseLong(settings.get("Clock." + Clock.ID_LINEUP + ".OvertimeTime"));
			if (lc.getMaximumTime() < otLineupTime) {
				lc.setMaximumTime(otLineupTime);
			}
			lc.resetTime();
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
		synchronized (runLock) {
			if (!getClock(Clock.ID_JAM).isRunning()) {
				requestBatchStart();
				saveClockState();
				ClockModel pc = getClockModel(Clock.ID_PERIOD);
				ClockModel jc = getClockModel(Clock.ID_JAM);
				ClockModel tc = getClockModel(Clock.ID_TIMEOUT);
				ClockModel ic = getClockModel(Clock.ID_INTERMISSION);

				if (ic.isRunning()) {
					ic.stop();
				}
				pc.start();

				// If Jam Clock is not at start (2:00), increment number and reset time
				if (!jc.isTimeAtStart())
					jc.changeNumber(1);
				jc.resetTime();
				jc.start();

				tc.stop();

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
			if (getClockModel(Clock.ID_JAM).isRunning()) {
				_stopJam();
			} else if (getClockModel(Clock.ID_TIMEOUT).isRunning()) {
				_stopTimeout();
			} else if (!getClockModel(Clock.ID_LINEUP).isRunning()) {
				_startLineup();
			}
		}
	}
	private void _stopJam() {
		synchronized (runLock) {
			saveClockState();
			ScoreBoardManager.gameSnapshot(true);

			ClockModel pc = getClockModel(Clock.ID_PERIOD);
			ClockModel jc = getClockModel(Clock.ID_JAM);
			ClockModel lc = getClockModel(Clock.ID_LINEUP);

			requestBatchStart();
			jc.stop();
			for (TeamModel tm : teams.values()) {
				tm.stopJam();
			}

			if (pc.isRunning()) {
				lc.resetTime();
				lc.start();
			}
			requestBatchEnd();
		}
	}
	private void _stopTimeout() {
		synchronized (runLock) {
			saveClockState();
			ClockModel tc = getClockModel(Clock.ID_TIMEOUT);
			ClockModel lc = getClockModel(Clock.ID_LINEUP);
			ClockModel pc = getClockModel(Clock.ID_PERIOD);
			
			requestBatchStart();
			if (!pc.isTimeAtEnd()) {
				lc.resetTime();
				lc.start();
			}
			if (restartPcAtTimeoutEnd) {
				restartPcAtTimeoutEnd = false;
				pc.start();
			}
			tc.stop();
			requestBatchEnd();
		}
	}
	private void _startLineup() {
		synchronized (runLock) {
			saveClockState();
			ClockModel lc = getClockModel(Clock.ID_LINEUP);
			ClockModel ic = getClockModel(Clock.ID_INTERMISSION);

			requestBatchStart();
			ic.stop();
			lc.resetTime();
			lc.start();
			requestBatchEnd();
		}
	}

	public void timeout() { timeout(null); }
	public void timeout(TeamModel team) { timeout(team, false); }
	public void timeout(TeamModel team, boolean review) {
		synchronized (runLock) {
			requestBatchStart();
			ClockModel tc = getClockModel(Clock.ID_TIMEOUT);
			ClockModel pc = getClockModel(Clock.ID_PERIOD);
			ClockModel jc = getClockModel(Clock.ID_JAM);
			ClockModel lc = getClockModel(Clock.ID_LINEUP);

			String newOwner = (null==team?(tc.isRunning()?"O":""):team.getId());
			
			if (jc.isRunning()) {
				_stopJam();
			}
			
			if (!tc.isRunning()) {
				saveClockState();
				setTimeoutOwner("");

				pc.stop();
				lc.stop();

				tc.resetTime();
				tc.start();
				
				if (pc.getTimeRemaining() + lc.getTimeElapsed() < 30000) {
					//last Jam did end with less than 30s on the period clock. If this is just an OTO, restart pc afterwards
					restartPcAtTimeoutEnd = true;
				}
			}
			
			if (!(getTimeoutOwner().equals(newOwner) && isOfficialReview() == review)) {
				saveClockState(); 

				if (team != null) {
					restartPcAtTimeoutEnd = false;
				}
				
				setTimeoutOwner(newOwner);
				setOfficialReview(review);

				for (TeamModel tm : teams.values()) {
					tm.setInTimeout(tm == team  && !review);
					tm.setInOfficialReview(tm == team && review);
				}
			}
			
			requestBatchEnd();

			ScoreBoardManager.gameSnapshot();
		}
	}
	
	private void saveClockState() {
		while (undoStack.size() >= 20) {
			//keep the size of the stack limited
			undoStack.remove();
		}
		HashMap<String, ClockState> clockStates = new HashMap<String, ClockState>();
		for (ClockModel clock : getClockModels()) {
			clockStates.put(clock.getId(), clock.getState());
		}
		HashMap<String, TeamState> teamStates = new HashMap<String, TeamState>();
		for (TeamModel team : getTeamModels()) {
			teamStates.put(team.getId(), team.getState());
		}
		undoStack.push(new StateSet(timeoutOwner, inOvertime, restartPcAtTimeoutEnd, inPeriod, clockStates, teamStates));
	}
	
	public void undoClockChange() {
		synchronized (runLock) {
			StateSet savedState = undoStack.pop();
			requestBatchStart();
			setTimeoutOwner(savedState.getTimeoutOwner());
			setOfficialReview(savedState.isOfficialReview());
			setInPeriod(savedState.inPeriod());
			for (TeamState ts : savedState.getTeamStates().values()) {
				getTeamModel(ts.getId()).undo(ts);
			}
			Collection<ClockState> clocks = savedState.getClockStates().values();
			for (ClockState cs : clocks) {
				//unstart clocks before unstop, as unstop may cause secondary effects
				if (!cs.isRunning())
					getClockModel(cs.getId()).undo(cs);
			}
			for (ClockState cs : clocks) {
				//unstop period clock last as it can affect other running clocks
				if (cs.isRunning() && !cs.getId().equals(Clock.ID_PERIOD))
					getClockModel(cs.getId()).undo(cs);
			}
			ClockState pc = savedState.getClockState(Clock.ID_PERIOD);
			if (pc.isRunning()) {
				getClockModel(Clock.ID_PERIOD).undo(pc);
			}
			requestBatchEnd();
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
	protected Deque<StateSet> undoStack = new ArrayDeque<StateSet>(20);

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
				_stopJam();
			}
			if (isInPeriod() && !pc.isRunning() && pc.isTimeAtEnd() && !jc.isRunning() && !tc.isRunning()) {
				setInPeriod(false);
				setOfficialScore(false);
				lc.stop();
				if (!ic.isRunning()) {
					ic.setNumber(pc.getNumber());
					ic.setTime(Long.parseLong(settings.get("Clock." + Clock.ID_INTERMISSION + ".Time")));
					ic.start();
					try { //FIXME: It looks like the element changed here does not exist in the current version of scoreboard.html
						XmlDocumentEditor editor = new XmlDocumentEditor();
						Element pages = getXmlScoreBoard().getDocument().getRootElement().getChild("Pages");
						Element pageE = editor.getElement(pages, "Page", "scoreboard.html", false);
						String intermissionN = String.valueOf(ic.getNumber());
						Element intermissionE = editor.getElement(pageE, "Intermission", intermissionN, false);
						Element confirmedE = editor.setText(intermissionE.getChild("Confirmed"), "false");
						getXmlScoreBoard().mergeElement(confirmedE);
					} catch ( Exception e ) {
						/* Ignore?	probably no existing element for current Intermission... */
					}
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
	protected ScoreBoardListener jamEndListener = new ScoreBoardListener() {
		public void scoreBoardChange(ScoreBoardEvent event) {
			if (isInOvertime()) {
				setInOvertime(false);
			}
		}
	};
	protected ScoreBoardListener timeoutStartListener = new ScoreBoardListener() {
		public void scoreBoardChange(ScoreBoardEvent event) {
			ClockModel ic = getClockModel(Clock.ID_INTERMISSION);
			Clock tc = getClock(Clock.ID_TIMEOUT);

			if (!isInPeriod() && ic.isRunning() && tc.isRunning()) {
				ic.stop();
				setInPeriod(true);
			}
		}
	};
	protected ScoreBoardListener timeoutEndListener = new ScoreBoardListener() {
		public void scoreBoardChange(ScoreBoardEvent event) {
			requestBatchStart();
			setTimeoutOwner("");
			setOfficialReview(false);
			getClockModel(Clock.ID_TIMEOUT).changeNumber(1);
			requestBatchEnd();
		}
	};
	protected ScoreBoardListener intermissionEndListener = new ScoreBoardListener() {
		public void scoreBoardChange(ScoreBoardEvent event) {
			ClockModel ic = getClockModel(Clock.ID_INTERMISSION);
			ClockModel pc = getClockModel(Clock.ID_PERIOD);
			ClockModel jc = getClockModel(Clock.ID_JAM);
			//Only consider it intermission end, if more than half of the intermission has run. 
			//Otherwise assume previous period is extended
			if (Math.abs(ic.getTimeRemaining()) < Long.parseLong(settings.get("Clock." + Clock.ID_INTERMISSION + ".Time"))/2) {
				requestBatchStart();
				pc.changeNumber(pc.getMinimumNumber());
				pc.resetTime();
				if (Boolean.parseBoolean(settings.get("Clock." + Clock.ID_JAM + ".Number"))) {
					jc.setNumber(jc.getMinimumNumber());
				} else {
					jc.changeNumber(1);
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
			if (Boolean.parseBoolean(settings.get("Clock." + Clock.ID_LINEUP + ".AutoStart"))) {
				ClockModel lc = getClockModel(Clock.ID_LINEUP);
				long bufferTime = Long.parseLong(settings.get("Clock." + Clock.ID_LINEUP + ".BufferTime")); 
				long triggerTime = bufferTime + (isInOvertime() ? 
						Long.parseLong(settings.get("Clock." + Clock.ID_LINEUP + ".OvertimeTime")) :
							Long.parseLong(settings.get("Clock." + Clock.ID_LINEUP + ".Time")));
				if (lc.getTimeElapsed() >= triggerTime) {
					if (Boolean.parseBoolean(settings.get("Clock." + Clock.ID_LINEUP + ".AutoStartType"))) {
						requestBatchStart();
						ClockModel jc = getClockModel(Clock.ID_JAM);
						startJam();
						jc.changeTime(jc.isCountDirectionDown()?-bufferTime:bufferTime);
						requestBatchEnd();
					} else {
						requestBatchStart();
						ClockModel pc = getClockModel(Clock.ID_PERIOD);
						ClockModel tc = getClockModel(Clock.ID_TIMEOUT);
						timeout();
						pc.changeTime(pc.isCountDirectionDown()?bufferTime:-bufferTime);
						tc.changeTime(tc.isCountDirectionDown()?-bufferTime:bufferTime);
						requestBatchEnd();
					}
				}
			}
		}
	};

	public static final String DEFAULT_TIMEOUT_OWNER = "";

	public static final String POLICY_KEY = DefaultScoreBoardModel.class.getName() + ".policy";
}

