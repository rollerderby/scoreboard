package com.carolinarollergirls.scoreboard.core.impl;
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
import java.util.List;
import java.util.Map;

import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.penalties.PenaltyCodesManager;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ClockConversion;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;
import com.carolinarollergirls.scoreboard.xml.XmlScoreBoard;
import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.Media;
import com.carolinarollergirls.scoreboard.core.Period;
import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Settings;
import com.carolinarollergirls.scoreboard.core.Stats;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.TimeoutOwner;

public class ScoreBoardImpl extends ScoreBoardEventProviderImpl implements ScoreBoard {
    public ScoreBoardImpl() {
        setupScoreBoard();
    }

    protected void setupScoreBoard() {
	for (Button b : Button.values()) {
	    b.setScoreBoard(this);
	}
	children.put(Child.CLOCK, new HashMap<String, ValueWithId>());
	children.put(Child.TEAM, new HashMap<String, ValueWithId>());
        children.put(NChild.PERIOD, new HashMap<String, ValueWithId>());
	children.put(Child.STATS, new HashMap<String, ValueWithId>());
        add(Child.STATS, new StatsImpl(this));
	children.put(Child.SETTINGS, new HashMap<String, ValueWithId>());
        add(Child.SETTINGS, new SettingsImpl(this));
	children.put(Child.RULESETS, new HashMap<String, ValueWithId>());
	add(Child.RULESETS, new RulesetsImpl(this));
	children.put(Child.PENALTY_CODES, new HashMap<String, ValueWithId>());
        add(Child.PENALTY_CODES, new PenaltyCodesManager(this));
	children.put(Child.MEDIA, new HashMap<String, ValueWithId>());
        add(Child.MEDIA, new MediaImpl(this, ScoreBoardManager.getDefaultPath()));
        reset();
        getTeam(Team.ID_1);
        getTeam(Team.ID_2);
        getClock(Clock.ID_PERIOD);
        getClock(Clock.ID_JAM);
        getClock(Clock.ID_LINEUP);
        getClock(Clock.ID_TIMEOUT);
        getClock(Clock.ID_INTERMISSION);
        addInPeriodListeners();
        xmlScoreBoard = new XmlScoreBoard(this);
        //Button may have a label from autosave but undo will not work after restart
        Button.UNDO.setLabel(ACTION_NONE);
    }

    public String getProviderName() { return "ScoreBoard"; }
    public Class<ScoreBoard> getProviderClass() { return ScoreBoard.class; }
    public String getId() { return ""; }
    public ScoreBoardEventProvider getParent() { return null; }
    public List<Class<? extends Property>> getProperties() { return properties; }

    public XmlScoreBoard getXmlScoreBoard() { return xmlScoreBoard; }

    public static Object getCoreLock() { return coreLock; }

    public Object valueFromString(PermanentProperty prop, String sValue) {
	if (prop == Value.TIMEOUT_OWNER) { return getTimeoutOwner(sValue); }
	return Boolean.parseBoolean(sValue);
    }
    public boolean set(PermanentProperty prop, Object value, Flag flag) {
	synchronized (coreLock) {
	    if (prop == Value.IN_PERIOD) { return false; }
	    boolean result = super.set(prop, value, flag);
	    if (result && prop == Value.IN_OVERTIME && !(Boolean)value) {
		Clock lc = getClock(Clock.ID_LINEUP);
		if (lc.isCountDirectionDown()) {
		    lc.setMaximumTime(getRulesets().getLong(Rule.LINEUP_DURATION));
		}
	    }
	    return result;
	}
    }
    public Object get(PermanentProperty prop) {
	synchronized (coreLock) {
	    if (prop == Value.IN_PERIOD) { return currentPeriod.get(Period.Value.RUNNING); }
	    return super.get(prop);
	}
    }
    
    public void execute(CommandProperty prop) {
	switch ((Command)prop) {
	case RESET:
	    reset();
	    break;
	case START_JAM:
	    startJam();
	    break;
	case STOP_JAM:
	    stopJamTO();
	    break;
	case TIMEOUT:
	    timeout();
	    break;
	case CLOCK_UNDO:
	    clockUndo(false);
	    break;
	case CLOCK_REPLACE:
	    clockUndo(true);
	    break;
	case START_OVERTIME:
	    startOvertime();
	    break;
	case OFFICIAL_TIMEOUT:
	    setTimeoutType(TimeoutOwners.OTO, false);
	    break;
	}
    }
    
    public ValueWithId create(AddRemoveProperty prop, String id) { 
	synchronized (coreLock) {
	    if (prop == Child.CLOCK) { return new ClockImpl(this, id); }
	    if (prop == Child.TEAM) { return new TeamImpl(this, id); }
	    if (prop == NChild.PERIOD) {
		if (Integer.parseInt(id) <= getRulesets().getInt(Rule.NUMBER_PERIODS)) {
		    return new PeriodImpl(this, id);
		}
	    }
	    return null;
	}
    }

    
    public void reset() {
        synchronized (coreLock) {
            for (ValueWithId c : getAll(Child.CLOCK)) {
                ((Clock)c).reset();
            }
            for (ValueWithId t : getAll(Child.TEAM)) {
                ((Team)t).reset();
            }
            removeAll(NChild.PERIOD);
            currentPeriod = getPeriod(1);

            setTimeoutOwner(TimeoutOwners.NONE);
            setOfficialReview(false);
            setInPeriod(false);
            setInOvertime(false);
            setOfficialScore(false);
            restartPcAfterTimeout = false;
            snapshot = null;
            replacePending = false;

            getRulesets().reset();
            // Custom settings are not reset, as broadcast overlays settings etc.
            // shouldn't be lost just because the next game is starting.

            Button.START.setLabel(ACTION_START_JAM);
            Button.STOP.setLabel(ACTION_LINEUP);
            Button.TIMEOUT.setLabel(ACTION_TIMEOUT);
            Button.UNDO.setLabel(ACTION_NONE);
        }
    }

    public boolean isInPeriod() { return (Boolean)get(Value.IN_PERIOD); }
    public void setInPeriod(boolean p) { currentPeriod.set(Period.Value.RUNNING, p); }
    public Period getPeriod(int p) { return (Period)get(NChild.PERIOD, String.valueOf(p), true); }
    public Period getCurrentPeriod() { return currentPeriod; }
    protected void addInPeriodListeners() {
        addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_PERIOD, Clock.Value.RUNNING, Boolean.FALSE, periodEndListener));
        addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_PERIOD, Clock.Value.NUMBER, periodNumberListener));
        addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_JAM, Clock.Value.RUNNING, Boolean.FALSE, jamEndListener));
        addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_INTERMISSION, Clock.Value.RUNNING, Boolean.FALSE, intermissionEndListener));
        addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_LINEUP, Clock.Value.TIME, lineupClockListener));
        addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_TIMEOUT, Clock.Value.TIME, timeoutClockListener));
    }

    public boolean isInOvertime() { return (Boolean)get(Value.IN_OVERTIME); }
    public void setInOvertime(boolean o) { set(Value.IN_OVERTIME, o); }
    public void startOvertime() {
        synchronized (coreLock) {
            Clock pc = getClock(Clock.ID_PERIOD);
            Clock jc = getClock(Clock.ID_JAM);
            Clock lc = getClock(Clock.ID_LINEUP);

            if (pc.isRunning() || jc.isRunning()) {
                return;
            }
            if (pc.getNumber() < pc.getMaximumNumber()) {
                return;
            }
            if (!pc.isTimeAtEnd()) {
                return;
            }
            createSnapshot(ACTION_OVERTIME);

            requestBatchStart();
            _endTimeout(false);
            setInOvertime(true);
            setLabels(ACTION_START_JAM, ACTION_NONE, ACTION_TIMEOUT);
            long otLineupTime = getRulesets().getLong(Rule.OVERTIME_LINEUP_DURATION);
            if (lc.getMaximumTime() < otLineupTime) {
                lc.setMaximumTime(otLineupTime);
            }
            _startLineup();
            requestBatchEnd();
        }
    }

    public boolean isOfficialScore() { return (Boolean)get(Value.OFFICIAL_SCORE); }
    public void setOfficialScore(boolean o) { set(Value.OFFICIAL_SCORE, o); }

    public void startJam() {
        synchronized (coreLock) {
            if (!getClock(Clock.ID_JAM).isRunning()) {
                createSnapshot(ACTION_START_JAM);
                setLabels(ACTION_NONE, ACTION_STOP_JAM, ACTION_TIMEOUT);
                _startJam();
                finishReplace();
            }
        }
    }
    public void stopJamTO() {
        synchronized (coreLock) {
            Clock jc = getClock(Clock.ID_JAM);
            Clock lc = getClock(Clock.ID_LINEUP);
            Clock tc = getClock(Clock.ID_TIMEOUT);

            if (jc.isRunning()) {
                createSnapshot(ACTION_STOP_JAM);
                setLabels(ACTION_START_JAM, ACTION_NONE, ACTION_TIMEOUT);
                _endJam(false);
                finishReplace();
            } else if (tc.isRunning()) {
                createSnapshot(ACTION_STOP_TO);
                setLabels(ACTION_START_JAM, ACTION_NONE, ACTION_TIMEOUT);
                _endTimeout(false);
                finishReplace();
            } else if (!lc.isRunning()) {
                createSnapshot(ACTION_LINEUP);
                setLabels(ACTION_START_JAM, ACTION_NONE, ACTION_TIMEOUT);
                _startLineup();
                finishReplace();
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
            finishReplace();
        }
    }
    public void setTimeoutType(TimeoutOwner owner, boolean review) {
        synchronized (coreLock) {
            Clock tc = getClock(Clock.ID_TIMEOUT);
            Clock pc = getClock(Clock.ID_PERIOD);

            requestBatchStart();
            if (!tc.isRunning()) {
                timeout();
            }
            //if overridden TO type is Team TO or OR, credit it back
            if (getTimeoutOwner() instanceof Team) {
        	Team t = (Team)getTimeoutOwner();
        	if (isOfficialReview()) {
        	    t.changeOfficialReviews(1);
        	    t.setInOfficialReview(false);
        	} else {
        	    t.changeTimeouts(1);
        	    t.setInTimeout(false);
        	}
            }
            setTimeoutOwner(owner);
            setOfficialReview(review);
            if (review) {
        	owner.setInOfficialReview(true);
            } else {
        	owner.setInTimeout(true);
            }
            if (!getRulesets().getBoolean(Rule.STOP_PC_ON_TO)) {
                boolean stopPc = false;
                if (owner instanceof Team) {
                    if (review && getRulesets().getBoolean(Rule.STOP_PC_ON_OR)) {
                        stopPc = true;
                    }
                    if (!review && getRulesets().getBoolean(Rule.STOP_PC_ON_TTO)) {
                        stopPc = true;
                    }
                } else if (owner == TimeoutOwners.OTO && getRulesets().getBoolean(Rule.STOP_PC_ON_OTO)) {
                    stopPc = true;
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
        Clock pc = getClock(Clock.ID_PERIOD);
        Clock jc = getClock(Clock.ID_JAM);
        Clock ic = getClock(Clock.ID_INTERMISSION);

        requestBatchStart();
        pc.setNumber(ic.getNumber()+1);
        pc.resetTime();
        restartPcAfterTimeout = false;
        if (getRulesets().getBoolean(Rule.JAM_NUMBER_PER_PERIOD)) {
            jc.setNumber(jc.getMinimumNumber());
        }
        jc.resetTime();
        for (ValueWithId t : getAll(Child.TEAM)) {
            ((Team)t).resetTimeouts(false);
        }
        requestBatchEnd();
    }
    private void _possiblyEndPeriod() {
        Clock pc = getClock(Clock.ID_PERIOD);
        Clock jc = getClock(Clock.ID_JAM);
        Clock tc = getClock(Clock.ID_TIMEOUT);

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
        Clock pc = getClock(Clock.ID_PERIOD);
        Clock jc = getClock(Clock.ID_JAM);

        requestBatchStart();
        _endIntermission(false);
        _endTimeout(false);
        _endLineup();
        setInPeriod(true);
        pc.start();
        jc.startNext();

        getTeam(Team.ID_1).startJam();
        getTeam(Team.ID_2).startJam();
        requestBatchEnd();
    }
    private void _endJam(boolean force) {
        Clock pc = getClock(Clock.ID_PERIOD);
        Clock jc = getClock(Clock.ID_JAM);

        if (!jc.isRunning() && !force) { return; }

        requestBatchStart();
        jc.stop();
        getTeam(Team.ID_1).stopJam();
        getTeam(Team.ID_2).stopJam();
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
        Clock lc = getClock(Clock.ID_LINEUP);

        requestBatchStart();
        _endIntermission(false);
        setInPeriod(true);
        lc.startNext();
        requestBatchEnd();
    }
    private void _endLineup() {
        Clock lc = getClock(Clock.ID_LINEUP);

        requestBatchStart();
        lc.stop();
        requestBatchEnd();
    }
    private void _startTimeout() {
        Clock pc = getClock(Clock.ID_PERIOD);
        Clock tc = getClock(Clock.ID_TIMEOUT);

        requestBatchStart();
        if (tc.isRunning()) {
            //end the previous timeout before starting a new one
            _endTimeout(true);
        }

        if (getRulesets().getBoolean(Rule.STOP_PC_ON_TO)) {
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
        Clock tc = getClock(Clock.ID_TIMEOUT);
        Clock pc = getClock(Clock.ID_PERIOD);

        if (!tc.isRunning()) { return; }

        requestBatchStart();
        if (!getSettings().get(SETTING_CLOCK_AFTER_TIMEOUT).equals(Clock.ID_TIMEOUT)) {
            tc.stop();
        }
        if (getTimeoutOwner() instanceof Team) {
            restartPcAfterTimeout = false;
        }
        getTimeoutOwner().setInTimeout(false);
        getTimeoutOwner().setInOfficialReview(false);
        setTimeoutOwner(TimeoutOwners.NONE);
        setOfficialReview(false);
        if (!timeoutFollows) {
            if (pc.isTimeAtEnd()) {
                _possiblyEndPeriod();
            } else {
                if (restartPcAfterTimeout) {
                    pc.start();
                }
                if (getSettings().get(SETTING_CLOCK_AFTER_TIMEOUT).equals(Clock.ID_LINEUP)) {
                    _startLineup();
                }
            }
        }
        requestBatchEnd();
    }
    private void _startIntermission() {
        Clock pc = getClock(Clock.ID_PERIOD);
        Clock ic = getClock(Clock.ID_INTERMISSION);

        requestBatchStart();
        ic.setNumber(pc.getNumber());
        long duration = 0;
        String[] sequence = getRulesets().get(Rule.INTERMISSION_DURATIONS).split(",");
        int number = Math.min(ic.getNumber(), sequence.length);
        if (number > 0) {
            duration = ClockConversion.fromHumanReadable(sequence[number-1]);
        }
        ic.setMaximumTime(duration);
        ic.resetTime();
        ic.start();
        requestBatchEnd();
    }
    private void _endIntermission(boolean force) {
        Clock ic = getClock(Clock.ID_INTERMISSION);
        Clock pc = getClock(Clock.ID_PERIOD);

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
        Clock pc = getClock(Clock.ID_PERIOD);
        Clock jc = getClock(Clock.ID_JAM);
        Clock lc = getClock(Clock.ID_LINEUP);
        Clock tc = getClock(Clock.ID_TIMEOUT);

        long bufferTime = getRulesets().getLong(Rule.AUTO_START_BUFFER);
        long triggerTime = bufferTime + (isInOvertime() ?
        	getRulesets().getLong(Rule.OVERTIME_LINEUP_DURATION) :
        	    getRulesets().getLong(Rule.LINEUP_DURATION));

        requestBatchStart();
        if (lc.getTimeElapsed() >= triggerTime) {
            if (Boolean.parseBoolean(getRulesets().get(Rule.AUTO_START_JAM))) {
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
        Button.UNDO.setLabel(UNDO_PREFIX + type);
    }
    protected void restoreSnapshot() {
        ScoreBoardClock.getInstance().rewindTo(snapshot.getSnapshotTime());
        for (ValueWithId clock : getAll(Child.CLOCK)) {
            ((Clock)clock).restoreSnapshot(snapshot.getClockSnapshot(clock.getId()));
        }
        for (ValueWithId team : getAll(Child.TEAM)) {
            ((Team)team).restoreSnapshot(snapshot.getTeamSnapshot(team.getId()));
        }
        setTimeoutOwner(snapshot.getTimeoutOwner());
        setOfficialReview(snapshot.isOfficialReview());
        setInOvertime(snapshot.inOvertime());
        setInPeriod(snapshot.inPeriod());
        restartPcAfterTimeout = snapshot.restartPcAfterTo();
        for (Button button : Button.values()) {
            button.setLabel(snapshot.getLabels().get(button));
        }
        Button.UNDO.setLabel(ACTION_NONE);
        Button.REPLACED.setLabel(snapshot.getType());
        snapshot = null;
    }
    protected void finishReplace() {
        if (!replacePending) { return; }
        requestBatchStart();
        ScoreBoardClock.getInstance().start(true);
        replacePending = false;
        requestBatchEnd();
    }
    public void clockUndo(boolean replace) {
        synchronized (coreLock) {
            requestBatchStart();
            if (replacePending) {
                createSnapshot(ACTION_NO_REPLACE);
                finishReplace();
            } else if (snapshot != null) {
                ScoreBoardClock.getInstance().stop();
                restoreSnapshot();
                if (replace) {
                    replacePending = true;
                    Button.UNDO.setLabel(ACTION_NO_REPLACE);
                } else {
                    ScoreBoardClock.getInstance().start(true);
                }
            }
            requestBatchEnd();
        }
    }

    protected void setLabels(String startLabel, String stopLabel, String timeoutLabel) {
        Button.START.setLabel(startLabel);
        Button.STOP.setLabel(stopLabel);
        Button.TIMEOUT.setLabel(timeoutLabel);
    }

    public Settings getSettings() { return (Settings)get(Child.SETTINGS, ""); }

    public Rulesets getRulesets() { return (Rulesets)get(Child.RULESETS, ""); }
    
    public PenaltyCodesManager getPenaltyCodesManager() { return (PenaltyCodesManager)get(Child.PENALTY_CODES, ""); }

    public Stats getStats() { return (Stats)get(Child.STATS, ""); }

    public Media getMedia() { return (Media)get(Child.MEDIA, ""); }

    public Clock getClock(String id) { return (Clock)get(Child.CLOCK, id, true); }

    public Team getTeam(String id) { return (Team)get(Child.TEAM, id, true); }

    public TimeoutOwner getTimeoutOwner(String id) {
	for (TimeoutOwners o : TimeoutOwners.values()) {
	    if (o.getId().equals(id)) { return o; }
	}
	return getTeam(id);
    }

    public TimeoutOwner getTimeoutOwner() { return (TimeoutOwner)get(Value.TIMEOUT_OWNER); }
    public void setTimeoutOwner(TimeoutOwner owner) { set(Value.TIMEOUT_OWNER, owner); }
    public boolean isOfficialReview() { return (Boolean)get(Value.OFFICIAL_REVIEW); }
    public void setOfficialReview(boolean official) { set(Value.OFFICIAL_REVIEW, official); }

    protected Period currentPeriod;
    
    protected ScoreBoardSnapshot snapshot = null;
    protected boolean replacePending = false;

    protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	add(Value.class);
	add(Child.class);
	add(NChild.class);
	add(Command.class);
    }};

    protected boolean restartPcAfterTimeout;

    protected XmlScoreBoard xmlScoreBoard;

    protected ScoreBoardListener periodEndListener = new ScoreBoardListener() {
        public void scoreBoardChange(ScoreBoardEvent event) {
            if (getRulesets().getBoolean(Rule.PERIOD_END_BETWEEN_JAMS)) {
                _possiblyEndPeriod();
            }
        }
    };
    protected ScoreBoardListener periodNumberListener = new ScoreBoardListener() {
        public void scoreBoardChange(ScoreBoardEvent event) {
            currentPeriod = getPeriod((Integer)event.getValue());
        }
    };
    protected ScoreBoardListener jamEndListener = new ScoreBoardListener() {
        public void scoreBoardChange(ScoreBoardEvent event) {
            Clock jc = getClock(Clock.ID_JAM);
            if (jc.isTimeAtEnd() && getRulesets().getBoolean(Rule.AUTO_END_JAM)) {
                //clock has run down naturally
                requestBatchStart();
                setLabels(ACTION_START_JAM, ACTION_NONE, ACTION_TIMEOUT);
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
            if (getRulesets().getBoolean(Rule.AUTO_START)) {
                _possiblyAutostart();
            }
        }
    };
    protected ScoreBoardListener timeoutClockListener = new ScoreBoardListener() {
        public void scoreBoardChange(ScoreBoardEvent event) {
            if (getRulesets().getBoolean(Rule.AUTO_END_TTO) &&
                    (getTimeoutOwner() instanceof Team) &&
                    (long)event.getValue() == getRulesets().getLong(Rule.TTO_DURATION)) {
                stopJamTO();
            }
            if ((long)event.getValue() == getRulesets().getLong(Rule.STOP_PC_AFTER_TO_DURATION) &&
                    getClock(Clock.ID_PERIOD).isRunning()) {
                getClock(Clock.ID_PERIOD).stop();
            }
        }
    };

    public static class ScoreBoardSnapshot {
        private ScoreBoardSnapshot(ScoreBoardImpl sbm, String type) {
            snapshotTime = ScoreBoardClock.getInstance().getCurrentTime();
            this.type = type;
            timeoutOwner = sbm.getTimeoutOwner();
            isOfficialReview = sbm.isOfficialReview();
            inOvertime = sbm.isInOvertime();
            inPeriod = sbm.isInPeriod();
            restartPcAfterTo = sbm.restartPcAfterTimeout;
            labels = new HashMap<Button, String>();
            for (Button button : Button.values()) {
        	labels.put(button, button.getLabel());
            }
            clockSnapshots = new HashMap<String, ClockImpl.ClockSnapshot>();
            for (ValueWithId clock : sbm.getAll(Child.CLOCK)) {
                clockSnapshots.put(clock.getId(), ((Clock)clock).snapshot());
            }
            teamSnapshots = new HashMap<String, Team.TeamSnapshot>();
            for (ValueWithId team : sbm.getAll(Child.TEAM)) {
                teamSnapshots.put(team.getId(), ((Team)team).snapshot());
            }
        }

        public String getType() { return type; }
        public long getSnapshotTime() { return snapshotTime; }
        public TimeoutOwner getTimeoutOwner() { return timeoutOwner; }
        public boolean isOfficialReview() { return isOfficialReview; }
        public boolean inOvertime() { return inOvertime; }
        public boolean inPeriod() { return inPeriod; }
        public boolean restartPcAfterTo() { return restartPcAfterTo; }
        public Map<Button, String> getLabels() { return labels; }
        public Map<String, Clock.ClockSnapshot> getClockSnapshots() { return clockSnapshots; }
        public Map<String, Team.TeamSnapshot> getTeamSnapshots() { return teamSnapshots; }
        public ClockImpl.ClockSnapshot getClockSnapshot(String clock) { return clockSnapshots.get(clock); }
        public Team.TeamSnapshot getTeamSnapshot(String team) { return teamSnapshots.get(team); }

        protected String type;
        protected long snapshotTime;
        protected TimeoutOwner timeoutOwner;
        protected boolean isOfficialReview;
        protected boolean inOvertime;
        protected boolean inPeriod;
        protected boolean restartPcAfterTo;
        protected Map<Button, String> labels;
        protected Map<String, Clock.ClockSnapshot> clockSnapshots;
        protected Map<String, Team.TeamSnapshot> teamSnapshots;
    }

    public enum Button {
	START("ScoreBoard.Button.StartLabel"),
	STOP("ScoreBoard.Button.StopLabel"),
	TIMEOUT("ScoreBoard.Button.TimeoutLabel"),
	UNDO("ScoreBoard.Button.UndoLabel"),
	REPLACED("ScoreBoard.Button.ReplacedLabel");
	
	private Button(String s) {
	    setting = s;
	}
	
	public void setScoreBoard(ScoreBoard sb) { scoreBoard = sb; }

	public String getLabel() { return scoreBoard.getSettings().get(setting); }
	public void setLabel(String label) { scoreBoard.getSettings().set(setting, label); }
	
	private ScoreBoard scoreBoard;
	private String setting;
    }

    public enum TimeoutOwners implements TimeoutOwner {
	NONE(""),
	OTO("O");
	
	TimeoutOwners(String id) {
	    this.id = id;
	}
	
	public String getId() { return id; }
	public String toString() { return id; }

	public void setInTimeout(boolean in_timeout) { /*noop*/ }
	public void setInOfficialReview(boolean in_official_review) { /*noop*/ }

	private String id;
    }
}

