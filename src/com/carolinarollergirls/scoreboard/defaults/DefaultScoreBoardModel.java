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

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.xml.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.policy.OvertimeLineupTimePolicy;

public class DefaultScoreBoardModel extends DefaultScoreBoardEventProvider implements ScoreBoardModel
{
	public DefaultScoreBoardModel() {
		settings = new DefaultSettingsModel(this, this);
		Ruleset.registerRule(settings, "ScoreBoard." + Clock.ID_INTERMISSION + ".PreGame");
		Ruleset.registerRule(settings, "ScoreBoard." + Clock.ID_INTERMISSION + ".Intermission");
		Ruleset.registerRule(settings, "ScoreBoard." + Clock.ID_INTERMISSION + ".Unofficial");
		Ruleset.registerRule(settings, "ScoreBoard." + Clock.ID_INTERMISSION + ".Official");
		Ruleset.registerRule(settings, "Clock." + Clock.ID_INTERMISSION + ".Time");

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

		reset();
		loadPolicies();
		addInPeriodListeners();
		xmlScoreBoard = new XmlScoreBoard(this);
	}

	public String getProviderName() { return "ScoreBoard"; }
	public Class<ScoreBoard> getProviderClass() { return ScoreBoard.class; }
	public String getProviderId() { return ""; }

	public XmlScoreBoard getXmlScoreBoard() { return xmlScoreBoard; }

	protected void loadPolicies() {
		Enumeration<?> keys = ScoreBoardManager.getProperties().propertyNames();

		while (keys.hasMoreElements()) {
			String key = keys.nextElement().toString();
			if (!key.startsWith(POLICY_KEY+"."))
				continue;

			String name = ScoreBoardManager.getProperty(key);

			try {
				PolicyModel policyModel = (PolicyModel)Class.forName(name).newInstance();
				addPolicyModel(policyModel);
				ScoreBoardManager.printMessage("Loaded Policy : "+name);
			} catch ( Exception e ) {
				ScoreBoardManager.printMessage("Could not load ScoreBoard policy "+name+" : " + e.getMessage());
			}
		}
	}

	public ScoreBoard getScoreBoard() { return this; }

	public void reset() {
		_getRuleset().apply(true);

		Iterator<ClockModel> c = getClockModels().iterator();
		while (c.hasNext())
			c.next().reset();
		Iterator<TeamModel> t = getTeamModels().iterator();
		while (t.hasNext())
			t.next().reset();
		Iterator<PolicyModel> p = getPolicyModels().iterator();
		while (p.hasNext())
			p.next().reset();

		periodClockWasRunning = false;
		jamClockWasRunning = false;
		lineupClockWasRunning = false;
		timeoutClockWasRunning = false;
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
	}

	public boolean isInOvertime() { return inOvertime; }
	public void setInOvertime(boolean o) {
		synchronized (inOvertimeLock) {
			Boolean last = new Boolean(inOvertime);
			inOvertime = o;
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_IN_OVERTIME, new Boolean(inOvertime), last));
		}
		if (!o) {
			try {
				OvertimeLineupTimePolicy p = (OvertimeLineupTimePolicy)getPolicy(OvertimeLineupTimePolicy.ID);
				if (null != p)
					p.stopOvertime();
			} catch ( ClassCastException ccE ) {
				ScoreBoardManager.printMessage("Internal Error: invalid OvertimeLineupTimePolicy : "+ccE.getMessage());
			}
		}
	}
	public void startOvertime() {
		synchronized (runLock) {
			requestBatchStart();
			ClockModel pc = getClockModel(Clock.ID_PERIOD);
			ClockModel jc = getClockModel(Clock.ID_JAM);
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
			try {
				OvertimeLineupTimePolicy p = (OvertimeLineupTimePolicy)getPolicy(OvertimeLineupTimePolicy.ID);
				if (null != p)
					p.startOvertime();
			} catch ( ClassCastException ccE ) {
				ScoreBoardManager.printMessage("Internal Error: invalid OvertimeLineupTimePolicy : "+ccE.getMessage());
			}
			getClockModel(Clock.ID_LINEUP).resetTime();
			getClockModel(Clock.ID_LINEUP).start();
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
				ClockModel pc = getClockModel(Clock.ID_PERIOD);
				ClockModel jc = getClockModel(Clock.ID_JAM);
				ClockModel tc = getClockModel(Clock.ID_TIMEOUT);
				ClockModel ic = getClockModel(Clock.ID_INTERMISSION);
				lineupClockWasRunning = getClockModel(Clock.ID_LINEUP).isRunning();

				// If intermission clock has almost run down, end intermission
				if (ic.isRunning() && Math.abs(ic.getTime() - (ic.isCountDirectionDown() ? ic.getMinimumTime() : ic.getMaximumTime())) < 60000) {
					ic.setTime(ic.isCountDirectionDown() ? ic.getMinimumTime() : ic.getMaximumTime());
				}
				// If Period Clock is at end, start a new period
				if (pc.isTimeAtEnd()) {
					pc.changeNumber(1);
					pc.resetTime();
					jc.setNumber(jc.getMinimumNumber());
					jc.resetTime();
					getTeamModel("1").setOfficialReviews(1);
					getTeamModel("2").setOfficialReviews(1);
					getTeamModel("1").setRetainedOfficialReview(false);
					getTeamModel("2").setRetainedOfficialReview(false);
				}
				periodClockWasRunning = pc.isRunning();
				pc.start();

				// If Jam Clock is not at start (2:00), increment number and reset time
				if (!jc.isTimeAtStart())
					jc.changeNumber(1);
				jc.resetTime();
				jc.start();

				timeoutClockWasRunning = tc.isRunning();
				tc.stop();

				getTeamModel("1").startJam();
				getTeamModel("2").startJam();
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
			ScoreBoardManager.gameSnapshot(true);

			ClockModel pc = getClockModel(Clock.ID_PERIOD);
			ClockModel jc = getClockModel(Clock.ID_JAM);
			ClockModel lc = getClockModel(Clock.ID_LINEUP);
			timeoutClockWasRunning = false;
			jamClockWasRunning = true;

			requestBatchStart();
			jc.stop();
			getTeamModel("1").stopJam();
			getTeamModel("2").stopJam();

			if (pc.isRunning()) {
				lc.resetTime();
				lc.start();
			}
			requestBatchEnd();
		}
	}
	private void _stopTimeout() {
		synchronized (runLock) {
			ClockModel tc = getClockModel(Clock.ID_TIMEOUT);
			ClockModel lc = getClockModel(Clock.ID_LINEUP);
			lastTimeoutOwner = getTimeoutOwner();
			wasOfficialReview = isOfficialReview();
			timeoutClockWasRunning = true;
			jamClockWasRunning = false;
			
			requestBatchStart();
			lc.resetTime();
			lc.start();
			tc.stop();
			requestBatchEnd();
		}
	}
	private void _startLineup() {
		synchronized (runLock) {
			ClockModel lc = getClockModel(Clock.ID_LINEUP);
			ClockModel ic = getClockModel(Clock.ID_INTERMISSION);
			// If intermission clock has almost run down, set it to end, so end of intermission policies are run
			if (ic.isRunning() && Math.abs(ic.getTime() - (ic.isCountDirectionDown() ? ic.getMinimumTime() : ic.getMaximumTime())) < 60000) {
				ic.setTime(ic.isCountDirectionDown() ? ic.getMinimumTime() : ic.getMaximumTime());
			}

			timeoutClockWasRunning = false;
			jamClockWasRunning = false;

			requestBatchStart();
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

			if (null==team && tc.isRunning() && getTimeoutOwner() == "") {
				setTimeoutOwner("O");
			} else {
				setTimeoutOwner(null==team?"":team.getId());
			}
			setOfficialReview(review);

			TeamModel t1 = getTeamModel("1");
			TeamModel t2 = getTeamModel("2");
			if (null==team) {
				t1.setInTimeout(false);
				t1.setInOfficialReview(false);
				t2.setInTimeout(false);
				t2.setInOfficialReview(false);
			} else if (t1.getId().equals(team.getId())) {
				t1.setInTimeout(!review);
				t1.setInOfficialReview(review);
				t2.setInTimeout(false);
				t2.setInOfficialReview(false);
			} else {
				t1.setInTimeout(false);
				t1.setInOfficialReview(false);
				t2.setInTimeout(!review);
				t2.setInOfficialReview(review);
			}

			if (!tc.isRunning()) {
				// Make sure period clock, jam clock, and lineup clock are stopped
				jamClockWasRunning = jc.isRunning();
				lineupClockWasRunning = lc.isRunning();

				pc.stop();
				jc.stop();
				lc.stop();

				tc.resetTime();
				tc.start();

				if (jamClockWasRunning) {
					getTeamModel("1").stopJam();
					getTeamModel("2").stopJam();
				}
			}
			requestBatchEnd();

			ScoreBoardManager.gameSnapshot();
		}
	}

	public void unStartJam() {
		synchronized (runLock) {
			if (!getClock(Clock.ID_JAM).isRunning())
				return;

			requestBatchStart();
			if (lineupClockWasRunning)
				getClockModel(Clock.ID_LINEUP).unstop();
			if (timeoutClockWasRunning) {
				setTimeoutOwner(lastTimeoutOwner);
				setOfficialReview(wasOfficialReview);
				getClockModel(Clock.ID_TIMEOUT).unstop();
			}
			if (!periodClockWasRunning)
				getClockModel(Clock.ID_PERIOD).unstart();
			getClockModel(Clock.ID_JAM).unstart();
			getTeamModel("1").unStartJam();
			getTeamModel("2").unStartJam();
			requestBatchEnd();

			ScoreBoardManager.gameSnapshot();
		}
	}
	public void unStopJam() {
		synchronized (runLock) {
			if (!(getClock(Clock.ID_LINEUP).isRunning() || getClockModel(Clock.ID_INTERMISSION).isRunning()))
				return;

			requestBatchStart();
			if (getClock(Clock.ID_LINEUP).isRunning()) {
				getClockModel(Clock.ID_LINEUP).stop();
			}
			if (getClock(Clock.ID_INTERMISSION).isRunning()) {
				getClockModel(Clock.ID_INTERMISSION).stop();
			}
			if (timeoutClockWasRunning) {
				setTimeoutOwner(lastTimeoutOwner);
				setOfficialReview(wasOfficialReview);
				getClockModel(Clock.ID_TIMEOUT).unstop();
			} 
			if (jamClockWasRunning) {
				getTeamModel("1").unStopJam();
				getTeamModel("2").unStopJam();
				getClockModel(Clock.ID_JAM).unstop();
			}
			requestBatchEnd();

			ScoreBoardManager.gameSnapshot();
		}
	}
	public void unTimeout() {
		synchronized (runLock) {
			if (!getClock(Clock.ID_TIMEOUT).isRunning())
				return;

			requestBatchStart();
			if (lineupClockWasRunning)
				getClockModel(Clock.ID_LINEUP).unstop();
			if (jamClockWasRunning) {
				getClockModel(Clock.ID_JAM).unstop();
				getTeamModel("1").unStopJam();
				getTeamModel("2").unStopJam();
			}
			getClockModel(Clock.ID_PERIOD).unstop();
			getClockModel(Clock.ID_TIMEOUT).unstart();
			requestBatchEnd();

			ScoreBoardManager.gameSnapshot();
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

	public Settings getSettings() { return (Settings)settings; }
	public SettingsModel getSettingsModel() { return settings; }

	public List<ClockModel> getClockModels() { return new ArrayList<ClockModel>(clocks.values()); }
	public List<TeamModel> getTeamModels() { return new ArrayList<TeamModel>(teams.values()); }
	public List<PolicyModel> getPolicyModels() { return new ArrayList<PolicyModel>(policies.values()); }

	public List<Clock> getClocks() { return new ArrayList<Clock>(getClockModels()); }
	public List<Team> getTeams() { return new ArrayList<Team>(getTeamModels()); }
	public List<Policy> getPolicies() { return new ArrayList<Policy>(getPolicyModels()); }

	public Clock getClock(String id) { return getClockModel(id).getClock(); }
	public Team getTeam(String id) { return getTeamModel(id).getTeam(); }
	public Policy getPolicy(String id) { try { return getPolicyModel(id).getPolicy(); } catch ( NullPointerException npE ) { return null; } }

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

	public PolicyModel getPolicyModel(String id) {
		synchronized (policies) {
			return policies.get(id);
		}
	}
	public void addPolicyModel(PolicyModel model) throws IllegalArgumentException {
		if ((model.getId() == null) || (model.getId().equals("")))
			throw new IllegalArgumentException("PolicyModel has null or empty Id");

		try {
			model.setScoreBoardModel(this);
		} catch ( Exception e ) {
			e.printStackTrace();
			throw new IllegalArgumentException("Exception while setting ScoreBoardModel on PolicyModel : "+e.getMessage());
		}

		synchronized (policies) {
			policies.put(model.getId(), model);
			model.addScoreBoardListener(this);
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_ADD_POLICY, model, null));
		}
	}
	public void removePolicyModel(PolicyModel model) {
		synchronized (policies) {
			policies.remove(model.getId());
			model.removeScoreBoardListener(this);
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_REMOVE_POLICY, model, null));
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
	protected HashMap<String,PolicyModel> policies = new HashMap<String,PolicyModel>();

	protected Object runLock = new Object();

	protected String timeoutOwner;
	protected Object timeoutOwnerLock = new Object();
	protected boolean officialReview;
	protected Object officialReviewLock = new Object();

	protected boolean inPeriod = false;
	protected Object inPeriodLock = new Object();

	protected boolean inOvertime = false;
	protected Object inOvertimeLock = new Object();

	protected boolean officialScore = false;
	protected Object officialScoreLock = new Object();

	protected Ruleset ruleset = null;
	protected Object rulesetLock = new Object();
	protected DefaultSettingsModel settings = null;

	protected boolean periodClockWasRunning = false;
	protected boolean jamClockWasRunning = false;
	protected boolean lineupClockWasRunning = false;
	protected boolean timeoutClockWasRunning = false;
	protected String lastTimeoutOwner = "";
	protected boolean wasOfficialReview = false;

	protected XmlScoreBoard xmlScoreBoard;

	protected ScoreBoardListener periodStartListener = new ScoreBoardListener() {
			public void scoreBoardChange(ScoreBoardEvent event) {
				if (!isInPeriod())
					setInPeriod(true);
			}
		};
	protected ScoreBoardListener periodEndListener = new ScoreBoardListener() {
			public void scoreBoardChange(ScoreBoardEvent event) {
				Clock p = getClock(Clock.ID_PERIOD);
				Clock j = getClock(Clock.ID_JAM);
				Clock t = getClock(Clock.ID_TIMEOUT);

				if (isInPeriod() && !p.isRunning() && p.isTimeAtEnd() && !j.isRunning() && !t.isRunning())
					setInPeriod(false);
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
				Clock j = getClock(Clock.ID_JAM);
				if (event.getProvider() == j && !j.isRunning() && j.isTimeAtEnd()) {
					_stopJam();
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

	public static final String DEFAULT_TIMEOUT_OWNER = "";

	public static final String POLICY_KEY = DefaultScoreBoardModel.class.getName() + ".policy";
}

