package com.carolinarollergirls.scoreboard.core.impl;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.HashMap;
import java.util.Map;

import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.OrderedScoreBoardEventProvider.IValue;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.penalties.PenaltyCodesManager;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ClockConversion;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;
import com.carolinarollergirls.scoreboard.xml.XmlScoreBoard;
import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Media;
import com.carolinarollergirls.scoreboard.core.Period;
import com.carolinarollergirls.scoreboard.core.Period.PeriodSnapshot;
import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Settings;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.Timeout;
import com.carolinarollergirls.scoreboard.core.TimeoutOwner;

public class ScoreBoardImpl extends ScoreBoardEventProviderImpl implements ScoreBoard {
    public ScoreBoardImpl() {
        super(null, null, "", null, ScoreBoard.class, Value.class, Child.class, NChild.class, Period.NChild.class, Command.class);
        setupScoreBoard();
    }

    protected void setupScoreBoard() {
        setCopy(Value.CURRENT_PERIOD_NUMBER, this, Value.CURRENT_PERIOD, IValue.NUMBER, true);
        setCopy(Value.IN_PERIOD, this, Value.CURRENT_PERIOD, Period.Value.RUNNING, false);
        setCopy(Value.UPCOMING_JAM_NUMBER, this, Value.UPCOMING_JAM, IValue.NUMBER, true);
        setCopy(Value.TIMEOUT_OWNER, this, Value.CURRENT_TIMEOUT, Timeout.Value.OWNER, false);
        setCopy(Value.OFFICIAL_REVIEW, this, Value.CURRENT_TIMEOUT, Timeout.Value.REVIEW, false);
        for (Button b : Button.values()) {
            b.setScoreBoard(this);
        }
        add(Child.SETTINGS, new SettingsImpl(this));
        addWriteProtection(Child.SETTINGS);
        add(Child.RULESETS, new RulesetsImpl(this));
        addWriteProtection(Child.RULESETS);
        add(Child.PENALTY_CODES, new PenaltyCodesManager(this));
        addWriteProtection(Child.PENALTY_CODES);
        add(Child.MEDIA, new MediaImpl(this, ScoreBoardManager.getDefaultPath()));
        addWriteProtection(Child.MEDIA);
        getTeam(Team.ID_1);
        getTeam(Team.ID_2);
        addWriteProtection(Child.TEAM);
        getClock(Clock.ID_PERIOD);
        getClock(Clock.ID_JAM);
        getClock(Clock.ID_LINEUP);
        getClock(Clock.ID_TIMEOUT);
        getClock(Clock.ID_INTERMISSION);
        addWriteProtection(Child.CLOCK);
        reset();
        addInPeriodListeners();
        xmlScoreBoard = new XmlScoreBoard(this);
    }

    @Override
    public XmlScoreBoard getXmlScoreBoard() { return xmlScoreBoard; }

    @Override
    protected Object computeValue(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == Value.UPCOMING_JAM && !(value instanceof Jam)) {
            value = new JamImpl(this, getCurrentPeriod().getCurrentJam());
        }
        return value;
    }

    @Override
    protected void valueChanged(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == Value.IN_OVERTIME && !(Boolean)value) {
            Clock lc = getClock(Clock.ID_LINEUP);
            if (lc.isCountDirectionDown()) {
                lc.setMaximumTime(getRulesets().getLong(Rule.LINEUP_DURATION));
            }
        } else if (prop == Value.UPCOMING_JAM) {
            removeAll(Period.NChild.JAM);
            add(Period.NChild.JAM, (Jam)value);
        } else if (prop == Value.CURRENT_TIMEOUT && value == null) {
            return;
        } if ( prop == Value.CURRENT_PERIOD ) {
            for (ValueWithId t : getAll(Child.TEAM)) {
                ((Team)t).recountTimeouts();
            }
        }
    }

    @Override
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
            setTimeoutType(Timeout.Owners.OTO, false);
            break;
        }
    }

    @Override
    public ValueWithId create(AddRemoveProperty prop, String id) { 
        synchronized (coreLock) {
            if (prop == Child.CLOCK) { return new ClockImpl(this, id); }
            if (prop == Child.TEAM) { return new TeamImpl(this, id); }
            if (prop == Period.NChild.JAM) { return new JamImpl(this, Integer.parseInt(id)); }
            if (prop == NChild.PERIOD) {
                int num = Integer.parseInt(id);
                if (0 <= num && num <= getRulesets().getInt(Rule.NUMBER_PERIODS)) {
                    return new PeriodImpl(this, num);
                }
            }
            return null;
        }
    }

    @Override
    public void reset() {
        synchronized (coreLock) {
            for (ValueWithId t : getAll(Child.TEAM)) {
                ((Team)t).reset();
            }
            set(Value.IN_JAM, false);
            removeAll(Period.NChild.JAM);
            removeAll(NChild.PERIOD);
            set(Value.CURRENT_PERIOD, getOrCreate(NChild.PERIOD, "0"));
            noTimeoutDummy = new TimeoutImpl(getCurrentPeriod(), "noTimeout");
            getCurrentPeriod().add(Period.Child.TIMEOUT, noTimeoutDummy);
            set(Value.CURRENT_TIMEOUT, noTimeoutDummy);
            set(Value.UPCOMING_JAM, new JamImpl(this, getCurrentPeriod().getCurrentJam()));
            for (ValueWithId c : getAll(Child.CLOCK)) {
                ((Clock)c).reset();
            }
            updateTeamJams();

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
    @Override
    public void postAutosaveUpdate() {
        synchronized(coreLock) {
            requestBatchStart();
            //Button may have a label from autosave but undo will not work after restart
            Button.UNDO.setLabel(ACTION_NONE);
            requestBatchEnd();
        }
    }

    @Override
    public boolean isInPeriod() { return (Boolean)get(Value.IN_PERIOD); }
    @Override
    public void setInPeriod(boolean p) { set(Value.IN_PERIOD, p); }
    @Override
    public Period getOrCreatePeriod(int p) { return (Period)getOrCreate(NChild.PERIOD, p); }
    @Override
    public Period getCurrentPeriod() { 	return (Period)get(Value.CURRENT_PERIOD); }
    @Override
    public int getCurrentPeriodNumber() { return (Integer)get(Value.CURRENT_PERIOD_NUMBER); }
    protected void addInPeriodListeners() {
        addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_PERIOD, Clock.Value.RUNNING, Boolean.FALSE, periodEndListener));
        addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_JAM, Clock.Value.RUNNING, Boolean.FALSE, jamEndListener));
        addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_INTERMISSION, Clock.Value.RUNNING, Boolean.FALSE, intermissionEndListener));
        addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_LINEUP, Clock.Value.TIME, lineupClockListener));
        addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_TIMEOUT, Clock.Value.TIME, timeoutClockListener));
    }

    @Override
    public boolean isInJam() { return (Boolean)get(Value.IN_JAM); }
    @Override
    public Jam getUpcomingJam() { return (Jam)get(Value.UPCOMING_JAM); }
    protected void advanceUpcomingJam() {
        Jam upcoming = getUpcomingJam();
        Jam next = new JamImpl(this, upcoming);
        set(Value.UPCOMING_JAM, next, Flag.INTERNAL);
        upcoming.setParent(getCurrentPeriod());
        getCurrentPeriod().add(Period.NChild.JAM, upcoming);
    }

    @Override
    public void updateTeamJams() {
        for (ValueWithId t : getAll(Child.TEAM)) {
            ((Team)t).updateTeamJams();
        }
    }
    
    @Override
    public boolean isInOvertime() { return (Boolean)get(Value.IN_OVERTIME); }
    @Override
    public void setInOvertime(boolean o) { set(Value.IN_OVERTIME, o); }
    @Override
    public void startOvertime() {
        synchronized (coreLock) {
            Clock pc = getClock(Clock.ID_PERIOD);
            Clock lc = getClock(Clock.ID_LINEUP);

            if (pc.isRunning() || isInJam()) {
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

    @Override
    public boolean isOfficialScore() { return (Boolean)get(Value.OFFICIAL_SCORE); }
    @Override
    public void setOfficialScore(boolean o) { set(Value.OFFICIAL_SCORE, o); }

    @Override
    public void startJam() {
        synchronized (coreLock) {
            if (!isInJam()) {
                createSnapshot(ACTION_START_JAM);
                setLabels(ACTION_NONE, ACTION_STOP_JAM, ACTION_TIMEOUT);
                _startJam();
                finishReplace();
            }
        }
    }
    @Override
    public void stopJamTO() {
        synchronized (coreLock) {
            Clock jc = getClock(Clock.ID_JAM);
            Clock lc = getClock(Clock.ID_LINEUP);
            Clock tc = getClock(Clock.ID_TIMEOUT);
            Clock ic = getClock(Clock.ID_INTERMISSION);

            if (jc.isRunning()) {
                createSnapshot(ACTION_STOP_JAM);
                setLabels(ACTION_START_JAM, ACTION_NONE, ACTION_TIMEOUT);
                _endJam();
                finishReplace();
            } else if (tc.isRunning()) {
                createSnapshot(ACTION_STOP_TO);
                setLabels(ACTION_START_JAM, ACTION_NONE, ACTION_TIMEOUT);
                _endTimeout(false);
                finishReplace();
            } else if (!lc.isRunning() && (!ic.isRunning() || ic.getTimeElapsed() > 1000L)) {
                // just after starting intermission this is almost surely the operator trying to end
                // a last jam that was just auto ended or accidentally hitting the button twice
                // - ignore, as this confuses operators and makes undoing the Jam end impossible
                createSnapshot(ACTION_LINEUP);
                setLabels(ACTION_START_JAM, ACTION_NONE, ACTION_TIMEOUT);
                _startLineup();
                finishReplace();
            }
        }
    }
    @Override
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
    @Override
    public void setTimeoutType(TimeoutOwner owner, boolean review) {
        synchronized (coreLock) {
            Clock tc = getClock(Clock.ID_TIMEOUT);
            Clock pc = getClock(Clock.ID_PERIOD);

            requestBatchStart();
            if (!getCurrentTimeout().isRunning()) {
                timeout();
            }
            getCurrentTimeout().set(Timeout.Value.REVIEW, review);
            getCurrentTimeout().set(Timeout.Value.OWNER, owner);
            if (!getRulesets().getBoolean(Rule.STOP_PC_ON_TO)) {
                boolean stopPc = false;
                if (owner instanceof Team) {
                    if (review && getRulesets().getBoolean(Rule.STOP_PC_ON_OR)) {
                        stopPc = true;
                    }
                    if (!review && getRulesets().getBoolean(Rule.STOP_PC_ON_TTO)) {
                        stopPc = true;
                    }
                } else if (owner == Timeout.Owners.OTO && getRulesets().getBoolean(Rule.STOP_PC_ON_OTO)) {
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

        requestBatchStart();
        set(Value.CURRENT_PERIOD, getOrCreatePeriod(getCurrentPeriodNumber()+1));
        if (getRulesets().getBoolean(Rule.JAM_NUMBER_PER_PERIOD)) {
            getUpcomingJam().set(IValue.NUMBER, 1, Flag.INTERNAL);
            // show Jam 0 on the display for the upcoming period
            scoreBoardChange(new ScoreBoardEvent(jc,Clock.Value.NUMBER, 0, jc.getNumber()));
        }
        pc.resetTime();
        jc.resetTime();
        restartPcAfterTimeout = false;
        requestBatchEnd();
    }
    private void _possiblyEndPeriod() {
        Clock pc = getClock(Clock.ID_PERIOD);
        Clock tc = getClock(Clock.ID_TIMEOUT);

        if (pc.isTimeAtEnd() && !pc.isRunning() && !isInJam() && !tc.isRunning()) {
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
        pc.start();
        advanceUpcomingJam();
        getCurrentPeriod().startJam();
        set(Value.IN_JAM, true);
        jc.restart();

        getTeam(Team.ID_1).startJam();
        getTeam(Team.ID_2).startJam();
        requestBatchEnd();
    }
    private void _endJam() {
        Clock pc = getClock(Clock.ID_PERIOD);
        Clock jc = getClock(Clock.ID_JAM);

        if (!isInJam()) { return; }

        requestBatchStart();
        jc.stop();
        set(Value.IN_JAM, false);
        getCurrentPeriod().stopJam();
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
        lc.changeNumber(1);
        lc.restart();
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
        if (getCurrentTimeout().isRunning()) {
            if (tc.getTimeElapsed() < 1000L) {
                // This is almost surely an accidental double press
                // ignore as it messes up stats and makes undo impossible
                requestBatchEnd();
                return;
            }
            //end the previous timeout before starting a new one
            _endTimeout(true);
        }

        if (getRulesets().getBoolean(Rule.STOP_PC_ON_TO)) {
            pc.stop();
        }
        _endLineup();
        _endJam();
        _endIntermission(false);
        setInPeriod(true);
        set(Value.CURRENT_TIMEOUT, new TimeoutImpl(getCurrentPeriod().getCurrentJam()));
        getCurrentTimeout().getParent().add(Period.Child.TIMEOUT, getCurrentTimeout());
        tc.changeNumber(1);
        tc.restart();
        requestBatchEnd();
    }
    private void _endTimeout(boolean timeoutFollows) {
        Clock tc = getClock(Clock.ID_TIMEOUT);
        Clock pc = getClock(Clock.ID_PERIOD);

        if (!getCurrentTimeout().isRunning()) { return; }

        requestBatchStart();
        if (!getSettings().get(SETTING_CLOCK_AFTER_TIMEOUT).equals(Clock.ID_TIMEOUT)) {
            tc.stop();
        }
        if (getTimeoutOwner() instanceof Team) {
            restartPcAfterTimeout = false;
        }
        getCurrentTimeout().stop();
        if (!timeoutFollows) {
            set(Value.CURRENT_TIMEOUT, noTimeoutDummy);
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
        Clock ic = getClock(Clock.ID_INTERMISSION);

        requestBatchStart();
        long duration = 0;
        String[] sequence = getRulesets().get(Rule.INTERMISSION_DURATIONS).split(",");
        int number = Math.min(getCurrentPeriodNumber(), sequence.length);
        if (number > 0) {
            duration = ClockConversion.fromHumanReadable(sequence[number-1]);
        }
        ic.setMaximumTime(duration);
        ic.restart();
        requestBatchEnd();
    }
    private void _endIntermission(boolean force) {
        Clock ic = getClock(Clock.ID_INTERMISSION);
        Clock pc = getClock(Clock.ID_PERIOD);

        if (!ic.isRunning() && !force && getCurrentPeriodNumber() > 0) { return; }

        requestBatchStart();
        ic.stop();
        if (getCurrentPeriodNumber() == 0 || 
                (ic.getTimeRemaining() < 60000 
                        && pc.getNumber() < pc.getMaximumNumber())) {
            //If less than one minute of intermission is left and there is another period,
            // go to the next period. Otherwise extend the previous period.
            //Always start period 1 as there is no previous period to extend.
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
        if (getCurrentPeriod() != snapshot.getCurrentPeriod() &&
                getCurrentPeriod().getAll(Period.NChild.JAM).size() > 0) {
            Jam movedJam = (Jam) getCurrentPeriod().getFirst(Period.NChild.JAM);
            getCurrentPeriod().remove(Period.NChild.JAM, movedJam);
            movedJam.setParent(this);
            set(Value.UPCOMING_JAM, movedJam);
            getCurrentPeriod().unlink();
        }
        set(Value.CURRENT_PERIOD, snapshot.getCurrentPeriod());
        getCurrentPeriod().restoreSnapshot(snapshot.getPeriodSnapshot());
        for (ValueWithId clock : getAll(Child.CLOCK)) {
            ((Clock)clock).restoreSnapshot(snapshot.getClockSnapshot(clock.getId()));
        }
        for (ValueWithId team : getAll(Child.TEAM)) {
            ((Team)team).restoreSnapshot(snapshot.getTeamSnapshot(team.getId()));
        }
        if (getCurrentTimeout() != snapshot.getCurrentTimeout() && getCurrentTimeout() != noTimeoutDummy) {
            getCurrentTimeout().unlink();
        }
        if (getCurrentTimeout() != snapshot.getCurrentTimeout() && snapshot.getCurrentTimeout() != noTimeoutDummy) {
            snapshot.getCurrentTimeout().set(Timeout.Value.RUNNING, true);
        }
        set(Value.CURRENT_TIMEOUT, snapshot.getCurrentTimeout());
        setInOvertime(snapshot.inOvertime());
        set(Value.IN_JAM, snapshot.inJam());
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
    @Override
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

    @Override
    public Settings getSettings() { return (Settings)get(Child.SETTINGS, ""); }

    @Override
    public Rulesets getRulesets() { return (Rulesets)get(Child.RULESETS, ""); }

    @Override
    public PenaltyCodesManager getPenaltyCodesManager() { return (PenaltyCodesManager)get(Child.PENALTY_CODES, ""); }

    @Override
    public Media getMedia() { return (Media)get(Child.MEDIA, ""); }

    @Override
    public Clock getClock(String id) { return (Clock)getOrCreate(Child.CLOCK, id); }

    @Override
    public Team getTeam(String id) { return (Team)getOrCreate(Child.TEAM, id); }

    @Override
    public TimeoutOwner getTimeoutOwner(String id) {
        if (id == null) { id = ""; }
        for (Timeout.Owners o : Timeout.Owners.values()) {
            if (o.getId().equals(id)) { return o; }
        }
        if (getTeam(id) != null) {
            return getTeam(id);
        }
        return Timeout.Owners.NONE;
    }

    @Override
    public Timeout getCurrentTimeout() { return (Timeout)get(Value.CURRENT_TIMEOUT); }
    @Override
    public TimeoutOwner getTimeoutOwner() { return (TimeoutOwner)get(Value.TIMEOUT_OWNER); }
    @Override
    public void setTimeoutOwner(TimeoutOwner owner) { set(Value.TIMEOUT_OWNER, owner); }
    @Override
    public boolean isOfficialReview() { return (Boolean)get(Value.OFFICIAL_REVIEW); }
    @Override
    public void setOfficialReview(boolean official) { set(Value.OFFICIAL_REVIEW, official); }

    protected ScoreBoardSnapshot snapshot = null;
    protected boolean replacePending = false;

    protected Timeout noTimeoutDummy;
    protected boolean restartPcAfterTimeout;

    protected XmlScoreBoard xmlScoreBoard;

    protected ScoreBoardListener periodEndListener = new ScoreBoardListener() {
        @Override
        public void scoreBoardChange(ScoreBoardEvent event) {
            if (getRulesets().getBoolean(Rule.PERIOD_END_BETWEEN_JAMS)) {
                _possiblyEndPeriod();
            }
        }
    };
    protected ScoreBoardListener jamEndListener = new ScoreBoardListener() {
        @Override
        public void scoreBoardChange(ScoreBoardEvent event) {
            Clock jc = getClock(Clock.ID_JAM);
            if (jc.isTimeAtEnd() && getRulesets().getBoolean(Rule.AUTO_END_JAM)) {
                //clock has run down naturally
                requestBatchStart();
                setLabels(ACTION_START_JAM, ACTION_NONE, ACTION_TIMEOUT);
                _endJam();
                requestBatchEnd();
            }
        }
    };
    protected ScoreBoardListener intermissionEndListener = new ScoreBoardListener() {
        @Override
        public void scoreBoardChange(ScoreBoardEvent event) {
            if (getClock(Clock.ID_INTERMISSION).isTimeAtEnd()) {
                //clock has run down naturally
                _endIntermission(true);
            }
        }
    };
    protected ScoreBoardListener lineupClockListener = new ScoreBoardListener() {
        @Override
        public void scoreBoardChange(ScoreBoardEvent event) {
            if (getRulesets().getBoolean(Rule.AUTO_START)) {
                _possiblyAutostart();
            }
        }
    };
    protected ScoreBoardListener timeoutClockListener = new ScoreBoardListener() {
        @Override
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
        private ScoreBoardSnapshot(ScoreBoardImpl sb, String type) {
            snapshotTime = ScoreBoardClock.getInstance().getCurrentTime();
            this.type = type;
            currentTimeout = sb.getCurrentTimeout();
            inOvertime = sb.isInOvertime();
            inJam = sb.isInJam();
            inPeriod = sb.isInPeriod();
            restartPcAfterTo = sb.restartPcAfterTimeout;
            currentPeriod = sb.getCurrentPeriod();
            periodSnapshot = sb.getCurrentPeriod().snapshot();
            labels = new HashMap<>();
            for (Button button : Button.values()) {
                labels.put(button, button.getLabel());
            }
            clockSnapshots = new HashMap<>();
            for (ValueWithId clock : sb.getAll(Child.CLOCK)) {
                clockSnapshots.put(clock.getId(), ((Clock)clock).snapshot());
            }
            teamSnapshots = new HashMap<>();
            for (ValueWithId team : sb.getAll(Child.TEAM)) {
                teamSnapshots.put(team.getId(), ((Team)team).snapshot());
            }
        }

        public String getType() { return type; }
        public long getSnapshotTime() { return snapshotTime; }
        public Timeout getCurrentTimeout() { return currentTimeout; }
        public boolean inOvertime() { return inOvertime; }
        public boolean inJam() { return inJam; }
        public boolean inPeriod() { return inPeriod; }
        public boolean restartPcAfterTo() { return restartPcAfterTo; }
        public Period getCurrentPeriod() { return currentPeriod; }
        public PeriodSnapshot getPeriodSnapshot() { return periodSnapshot; }
        public Map<Button, String> getLabels() { return labels; }
        public Map<String, Clock.ClockSnapshot> getClockSnapshots() { return clockSnapshots; }
        public Map<String, Team.TeamSnapshot> getTeamSnapshots() { return teamSnapshots; }
        public ClockImpl.ClockSnapshot getClockSnapshot(String clock) { return clockSnapshots.get(clock); }
        public Team.TeamSnapshot getTeamSnapshot(String team) { return teamSnapshots.get(team); }

        protected String type;
        protected long snapshotTime;
        protected Timeout currentTimeout;
        protected boolean inOvertime;
        protected boolean inJam;
        protected boolean inPeriod;
        protected boolean restartPcAfterTo;
        protected Period currentPeriod;
        protected PeriodSnapshot periodSnapshot;
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
}

