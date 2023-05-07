package com.carolinarollergirls.scoreboard.core.game;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.jr.ob.JSON;

import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.Expulsion;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Jam;
import com.carolinarollergirls.scoreboard.core.interfaces.Media.MediaFile;
import com.carolinarollergirls.scoreboard.core.interfaces.Media.MediaType;
import com.carolinarollergirls.scoreboard.core.interfaces.Penalty;
import com.carolinarollergirls.scoreboard.core.interfaces.Period;
import com.carolinarollergirls.scoreboard.core.interfaces.Period.PeriodSnapshot;
import com.carolinarollergirls.scoreboard.core.interfaces.PreparedTeam;
import com.carolinarollergirls.scoreboard.core.interfaces.Rulesets;
import com.carolinarollergirls.scoreboard.core.interfaces.Rulesets.Ruleset;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.core.interfaces.Timeout;
import com.carolinarollergirls.scoreboard.core.interfaces.TimeoutOwner;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.event.ValueWithId;
import com.carolinarollergirls.scoreboard.json.JSONStateSnapshotter;
import com.carolinarollergirls.scoreboard.penalties.PenaltyCode;
import com.carolinarollergirls.scoreboard.penalties.PenaltyCodesDefinition;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.rules.RuleDefinition;
import com.carolinarollergirls.scoreboard.utils.BasePath;
import com.carolinarollergirls.scoreboard.utils.ClockConversion;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;
import com.carolinarollergirls.scoreboard.utils.StatsbookExporter;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public class GameImpl extends ScoreBoardEventProviderImpl<Game> implements Game {
    public GameImpl(ScoreBoard sb, PreparedTeam team1, PreparedTeam team2, Ruleset rs) {
        super(sb, UUID.randomUUID().toString(), ScoreBoard.GAME);
        initReferences(rs);
        getTeam(Team.ID_1).loadPreparedTeam(team1);
        getTeam(Team.ID_2).loadPreparedTeam(team2);
        jsonSnapshotter = new JSONStateSnapshotter(sb.getJsm(), this);
    }
    public GameImpl(ScoreBoard parent, String id) {
        super(parent, id, ScoreBoard.GAME);
        initReferences(scoreBoard.getRulesets().getRuleset(Rulesets.ROOT_ID));
        jsonSnapshotter = new JSONStateSnapshotter(getScoreBoard().getJsm(), this);
    }

    private void initReferences(Ruleset rs) {
        addProperties(props);
        addProperties(Period.JAM);

        setCopy(CURRENT_PERIOD_NUMBER, this, CURRENT_PERIOD, Period.NUMBER, true);
        setCopy(IN_PERIOD, this, CURRENT_PERIOD, Period.RUNNING, false);
        setCopy(IN_SUDDEN_SCORING, this, CURRENT_PERIOD, Period.SUDDEN_SCORING, false);
        setCopy(UPCOMING_JAM_NUMBER, this, UPCOMING_JAM, Jam.NUMBER, true);
        setCopy(INJURY_CONTINUATION_UPCOMING, this, UPCOMING_JAM, Jam.INJURY_CONTINUATION, false);
        setCopy(TIMEOUT_OWNER, this, CURRENT_TIMEOUT, Timeout.OWNER, false);
        setCopy(OFFICIAL_REVIEW, this, CURRENT_TIMEOUT, Timeout.REVIEW, false);
        setCopy(RULESET_NAME, this, RULESET, Ruleset.NAME, true);
        setRuleset(rs);
        add(TEAM, new TeamImpl(this, Team.ID_1));
        add(TEAM, new TeamImpl(this, Team.ID_2));
        addWriteProtection(TEAM);
        add(CLOCK, new ClockImpl(this, Clock.ID_PERIOD));
        add(CLOCK, new ClockImpl(this, Clock.ID_JAM));
        add(CLOCK, new ClockImpl(this, Clock.ID_LINEUP));
        add(CLOCK, new ClockImpl(this, Clock.ID_TIMEOUT));
        add(CLOCK, new ClockImpl(this, Clock.ID_INTERMISSION));
        addWriteProtection(CLOCK);
        addWriteProtectionOverride(EXPULSION, Source.NON_WS);
        addWriteProtectionOverride(IN_JAM, Source.NON_WS);
        addWriteProtectionOverride(IN_OVERTIME, Source.NON_WS);
        addWriteProtectionOverride(CURRENT_TIMEOUT, Source.NON_WS);
        setRecalculated(NO_MORE_JAM)
            .addSource(this, IN_JAM)
            .addSource(this, IN_PERIOD)
            .addSource(this, RULE)
            .addIndirectSource(this, CURRENT_PERIOD, Period.TIMEOUT);
        setRecalculated(NAME)
            .addSource(this, NAME_FORMAT)
            .addSource(this, STATE)
            .addSource(this, EVENT_INFO)
            .addSource(get(TEAM, Team.ID_1), Team.DISPLAY_NAME)
            .addSource(get(TEAM, Team.ID_2), Team.DISPLAY_NAME)
            .addSource(get(TEAM, Team.ID_1), Team.SCORE)
            .addSource(get(TEAM, Team.ID_2), Team.SCORE);
        setRecalculated(FILENAME)
            .addSource(this, EVENT_INFO)
            .addSource(get(TEAM, Team.ID_1), Team.FILE_NAME)
            .addSource(get(TEAM, Team.ID_2), Team.FILE_NAME);
        setRecalculated(STATE).addSource(this, CURRENT_PERIOD_NUMBER).addSource(this, OFFICIAL_SCORE);
        set(IN_JAM, false);
        set(NAME_FORMAT, "");
        removeAll(Period.JAM);
        removeAll(PERIOD);
        set(CURRENT_PERIOD, getOrCreate(PERIOD, "0"));
        addWriteProtectionOverride(PERIOD, Source.NON_WS);
        addWriteProtectionOverride(Period.JAM, Source.NON_WS);
        noTimeoutDummy = new TimeoutImpl(getCurrentPeriod(), "noTimeout");
        getCurrentPeriod().add(Period.TIMEOUT, noTimeoutDummy);
        set(CURRENT_TIMEOUT, noTimeoutDummy);
        set(UPCOMING_JAM, new JamImpl(this, getCurrentPeriod().getCurrentJam()));
        updateTeamJams();

        setInPeriod(false);
        setInOvertime(false);
        setOfficialScore(false);
        snapshot = null;
        replacePending = false;

        setLabel(Button.START, ACTION_START_JAM);
        setLabel(Button.STOP, ACTION_LINEUP);
        setLabel(Button.TIMEOUT, ACTION_TIMEOUT);
        setLabel(Button.UNDO, ACTION_NONE);
        setLabel(Button.REPLACED, ACTION_NONE);

        // handle period clock running down between jams
        addScoreBoardListener(new ConditionalScoreBoardListener<>(
            Clock.class, getClock(Clock.ID_PERIOD).getId(), Clock.RUNNING, Boolean.FALSE, new ScoreBoardListener() {
                @Override
                public void scoreBoardChange(ScoreBoardEvent<?> event) {
                    if (getBoolean(Rule.PERIOD_END_BETWEEN_JAMS)) { _possiblyEndPeriod(); }
                }
            }));

        // handle intermission end
        addScoreBoardListener(
            new ConditionalScoreBoardListener<>(Clock.class, getClock(Clock.ID_INTERMISSION).getId(), Clock.RUNNING,
                                                Boolean.FALSE, new ScoreBoardListener() {
                                                    @Override
                                                    public void scoreBoardChange(ScoreBoardEvent<?> event) {
                                                        if (getClock(Clock.ID_INTERMISSION).isTimeAtEnd()) {
                                                            // clock has run down naturally
                                                            _endIntermission(true);
                                                        }
                                                    }
                                                }));

        // handle auto-start (if enabled)
        addScoreBoardListener(new ConditionalScoreBoardListener<>(
            Clock.class, getClock(Clock.ID_LINEUP).getId(), Clock.TIME, new ScoreBoardListener() {
                @Override
                public void scoreBoardChange(ScoreBoardEvent<?> event) {
                    if (!"".equals(getSetting(ScoreBoard.SETTING_AUTO_START))) { _possiblyAutostart(); }
                }
            }));

        // handle auto-end jam (if enabled)
        addScoreBoardListener(new ConditionalScoreBoardListener<>(
            Clock.class, getClock(Clock.ID_JAM).getId(), Clock.RUNNING, Boolean.FALSE, new ScoreBoardListener() {
                @Override
                public void scoreBoardChange(ScoreBoardEvent<?> event) {
                    Clock jc = getClock(Clock.ID_JAM);
                    if (jc.isTimeAtEnd() && getBooleanSetting(ScoreBoard.SETTING_AUTO_END_JAM)) {
                        // clock has run down naturally
                        stopJamTO();
                    }
                }
            }));

        // handle auto-end TTO (if enabled) or stopping period clock after certain amount of timeout
        addScoreBoardListener(new ConditionalScoreBoardListener<>(
            Clock.class, getClock(Clock.ID_TIMEOUT).getId(), Clock.TIME, new ScoreBoardListener() {
                @Override
                public void scoreBoardChange(ScoreBoardEvent<?> event) {
                    if (getBooleanSetting(ScoreBoard.SETTING_AUTO_END_TTO) && (getTimeoutOwner() instanceof Team) &&
                        !getCurrentTimeout().isReview() && (Long) event.getValue() == getLong(Rule.TTO_DURATION)) {
                        stopJamTO();
                    }
                    if ((Long) event.getValue() == getLong(Rule.STOP_PC_AFTER_TO_DURATION) &&
                        getClock(Clock.ID_PERIOD).isRunning()) {
                        getClock(Clock.ID_PERIOD).stop();
                    }
                }
            }));

        // handle changes to the ruleset (if following a preset ruleset)
        scoreBoard.getRulesets().addScoreBoardListener(
            new ConditionalScoreBoardListener<>(Ruleset.class, Ruleset.RULE, new ScoreBoardListener() {
                @Override
                public void scoreBoardChange(ScoreBoardEvent<?> event) {
                    refreshRuleset((Ruleset) event.getProvider());
                }
            }));

        // handle file updates
        scoreBoard.getMedia()
            .getFormat("game-data")
            .getType("json")
            .addScoreBoardListener(
                new ConditionalScoreBoardListener<>(MediaType.class, MediaType.FILE, new ScoreBoardListener() {
                    @Override
                    public void scoreBoardChange(ScoreBoardEvent<?> event) {
                        if (((MediaFile) event.getValue()).getId().equals(getFilename() + ".json")) {
                            set(JSON_EXISTS, true);
                        }
                    }
                }));
        scoreBoard.getMedia()
            .getFormat("game-data")
            .getType("xlsx")
            .addScoreBoardListener(
                new ConditionalScoreBoardListener<>(MediaType.class, MediaType.FILE, new ScoreBoardListener() {
                    @Override
                    public void scoreBoardChange(ScoreBoardEvent<?> event) {
                        if (((MediaFile) event.getValue()).getId().equals(getFilename() + ".xlsx")) {
                            set(STATSBOOK_EXISTS, true);
                        }
                    }
                }));
    }

    @Override
    protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == UPCOMING_JAM && !(value instanceof Jam)) {
            if (getCurrentPeriod() != null) { value = new JamImpl(this, getCurrentPeriod().getCurrentJam()); }
        } else if (prop == NO_MORE_JAM) {
            if (isInJam() || !isInPeriod()) { return false; }
            if (!getBoolean(Rule.PERIOD_END_BETWEEN_JAMS)) { return false; }
            if (getClock(Clock.ID_PERIOD).isTimeAtStart()) { return false; }
            Jam lastJam = getCurrentPeriod().getCurrentJam();
            long pcRemaining = getClock(Clock.ID_PERIOD).getMaximumTime() - lastJam.getPeriodClockElapsedEnd();
            if (pcRemaining >= getLong(Rule.LINEUP_DURATION)) { return false; }
            boolean ttoForcesJam = getBoolean(Rule.STOP_PC_ON_TO) || getBoolean(Rule.STOP_PC_ON_TTO);
            boolean orForcesJam = getBoolean(Rule.STOP_PC_ON_TO) || getBoolean(Rule.STOP_PC_ON_OR);
            boolean otoForcesJam = getBoolean(Rule.EXTRA_JAM_AFTER_OTO) &&
                                   (getBoolean(Rule.STOP_PC_ON_TO) || getBoolean(Rule.STOP_PC_ON_TTO));
            for (Timeout t : lastJam.getAll(Jam.TIMEOUTS_AFTER)) {
                if (t.getOwner() instanceof Team) {
                    if (t.isReview() && orForcesJam) { return false; }
                    if (!t.isReview() && ttoForcesJam) { return false; }
                } else if (otoForcesJam) {
                    return false;
                }
            }
            return true;
        } else if (prop == NAME_FORMAT && "".equals(value)) {
            return getSetting(SETTING_DEFAULT_NAME_FORMAT);
        } else if (prop == NAME) {
            String game = get(EVENT_INFO, INFO_GAME_NUMBER) == null ? "" : get(EVENT_INFO, INFO_GAME_NUMBER).getValue();
            String date = get(EVENT_INFO, INFO_DATE) == null ? "" : get(EVENT_INFO, INFO_DATE).getValue();
            String time = get(EVENT_INFO, INFO_START_TIME) == null ? "" : get(EVENT_INFO, INFO_START_TIME).getValue();
            return get(NAME_FORMAT)
                .replace("%d", date)
                .replace("%t", time)
                .replace("%g", game)
                .replace("%G", game.equals("") ? "" : ("Game " + game + ":"))
                .replace("%1", getTeam(Team.ID_1).get(Team.DISPLAY_NAME))
                .replace("%2", getTeam(Team.ID_2).get(Team.DISPLAY_NAME))
                .replace("%s", get(STATE).toString())
                .replace("%S", getTeam(Team.ID_1).get(Team.SCORE) + " - " + getTeam(Team.ID_2).get(Team.SCORE))
                .trim();
        } else if (prop == FILENAME) {
            if (get(STATE) != State.PREPARED && flag != Flag.SPECIAL_CASE) { // we have already written a file
                return source.isFile() ? value : last;
            }
            String date = get(EVENT_INFO, INFO_DATE) == null ? "0000-00-00" : get(EVENT_INFO, INFO_DATE).getValue();
            String team1 = getTeam(Team.ID_1).get(Team.FILE_NAME).replaceAll("\\W+", "");
            String team2 = getTeam(Team.ID_2).get(Team.FILE_NAME).replaceAll("\\W+", "");
            String newName = "STATS-" + date + "_" + team1 + "_vs_" + team2;
            if (newName.equals(last)) {
                return newName;
            } else {
                return checkNewFilename(newName);
            }
        } else if (prop == STATSBOOK_EXISTS) {
            return BasePath.get().toPath().resolve("html/game-data/xlsx/" + getFilename() + ".xlsx").toFile().canRead();
        } else if (prop == JSON_EXISTS) {
            return BasePath.get().toPath().resolve("html/game-data/json/" + getFilename() + ".json").toFile().canRead();
        } else if (prop == RULESET && value != null && !source.isFile()) {
            if (get(STATE) != State.PREPARED && source == Source.WS) {
                return null; // no change after game start
            } else {
                setCurrentRulesetRecurse(((Ruleset) value));
            }
        } else if (prop == STATE) {
            if (getCurrentPeriodNumber() == 0) {
                return State.PREPARED;
            } else if (!isOfficialScore()) {
                return State.RUNNING;
            } else {
                return State.FINISHED;
            }
        } else if (prop == OFFICIAL_SCORE) {
            if (this == scoreBoard.getCurrentGame() && getCurrentPeriod().isRunning() &&
                !getCurrentTimeout().isRunning()) {
                // Only allow a running game to be ended prematurely during intermission or a timeout
                return false;
            }
        }
        return value;
    }

    @Override
    protected void valueChanged(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == IN_OVERTIME) {
            if (isInJam()) {
                getCurrentPeriod().getCurrentJam().set(Jam.OVERTIME, (Boolean) value);
            } else {
                getUpcomingJam().set(Jam.OVERTIME, (Boolean) value);
            }
            if (!(Boolean) value) {
                Clock lc = getClock(Clock.ID_LINEUP);
                if (lc.isCountDirectionDown()) { lc.setMaximumTime(getLong(Rule.LINEUP_DURATION)); }
            }
        } else if (prop == UPCOMING_JAM) {
            removeAll(Period.JAM);
            add(Period.JAM, (Jam) value);
        } else if (prop == CURRENT_TIMEOUT && value == null) {
            return;
        }
        if (prop == CURRENT_PERIOD) {
            for (Team t : getAll(TEAM)) { t.recountTimeouts(); }
        }
        if (prop == STATE && (State) value == State.RUNNING && (State) last == State.PREPARED) {
            set(RULESET, null);
            getTeam(Team.ID_1).set(Team.PREPARED_TEAM_CONNECTED, false);
            getTeam(Team.ID_2).set(Team.PREPARED_TEAM_CONNECTED, false);
            if (get(EVENT_INFO, INFO_DATE) == null || "".equals(get(EVENT_INFO, INFO_DATE).getValue())) {
                add(EVENT_INFO, new ValWithId(INFO_DATE, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)));
                set(FILENAME, "", Flag.SPECIAL_CASE);
            }
            if (get(EVENT_INFO, INFO_START_TIME) == null || "".equals(get(EVENT_INFO, INFO_START_TIME).getValue())) {
                add(EVENT_INFO,
                    new ValWithId(
                        INFO_START_TIME,
                        LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).format(DateTimeFormatter.ISO_LOCAL_TIME)));
            }
            if (!"Never".equals(get(LAST_FILE_UPDATE))) { set(LAST_FILE_UPDATE, "Pre Game"); }
        }
        if (prop == OFFICIAL_SCORE && (boolean) value && source == Source.WS) {
            Clock pc = getClock(Clock.ID_PERIOD);
            Clock ic = getClock(Clock.ID_INTERMISSION);
            Clock tc = getClock(Clock.ID_TIMEOUT);
            Clock lc = getClock(Clock.ID_LINEUP);
            if (!ic.isRunning()) {
                // Game ended prematurely. Go to intermission, so "Final Score" is displayed.
                // Don't change period number or time. That info may have to be reported to the governing body.
                if (isInJam()) { _endJam(); }
                if (tc.isRunning()) {
                    tc.stop();
                    getCurrentTimeout().stop();
                }
                if (lc.isRunning()) { lc.stop(); }
                if (pc.isRunning()) { pc.stop(); }
                _startIntermission();
            }
            jsonSnapshotter.writeOnNextUpdate();
        }
    }

    @Override
    protected void itemAdded(Child<?> prop, ValueWithId item, Source source) {
        if (prop == RULE) {
            if (Rule.PENALTIES_FILE.toString().equals(item.getId())) {
                setRuleDefinitionsFromJSON(item.getValue());
            } else if (Rule.NUMBER_TIMEOUTS.toString().equals(item.getId()) ||
                       Rule.TIMEOUTS_PER_PERIOD.toString().equals(item.getId()) ||
                       Rule.NUMBER_REVIEWS.toString().equals(item.getId()) ||
                       Rule.REVIEWS_PER_PERIOD.toString().equals(item.getId()) ||
                       Rule.NUMBER_RETAINS.toString().equals(item.getId()) ||
                       Rule.RDCL_PER_HALF_RULES.toString().equals(item.getId())) {
                for (Team t : getAll(TEAM)) { t.recountTimeouts(); }
            }
        }
    }

    @Override
    protected void itemRemoved(Child<?> prop, ValueWithId item, Source source) {
        if (prop == EXPULSION) { ((Expulsion) item).delete(); }
    }

    @Override
    public void execute(Command prop, Source source) {
        if (prop == START_JAM) {
            startJam();
        } else if (prop == STOP_JAM) {
            stopJamTO();
        } else if (prop == TIMEOUT) {
            timeout();
        } else if (prop == CLOCK_UNDO) {
            clockUndo(false);
        } else if (prop == CLOCK_REPLACE) {
            clockUndo(true);
        } else if (prop == START_OVERTIME) {
            startOvertime();
        } else if (prop == OFFICIAL_TIMEOUT) {
            setTimeoutType(Timeout.Owners.OTO, false);
        } else if (prop == EXPORT) {
            jsonSnapshotter.writeFile();
            if (statsbookExporter == null) {
                set(UPDATE_IN_PROGRESS, true);
                statsbookExporter = new StatsbookExporter(this);
            }
        }
    }

    @Override
    public ScoreBoardEventProvider create(Child<? extends ScoreBoardEventProvider> prop, String id, Source source) {
        synchronized (coreLock) {
            if (prop == CLOCK) { return new ClockImpl(this, id); }
            if (prop == TEAM) { return new TeamImpl(this, id); }
            if (prop == Period.JAM) { return new JamImpl(this, Integer.parseInt(id)); }
            if (prop == PERIOD) {
                int num = Integer.parseInt(id);
                if (0 <= num && num <= getInt(Rule.NUMBER_PERIODS)) { return new PeriodImpl(this, num); }
            }
            if (prop == NSO) { return new OfficialImpl(this, id, NSO); }
            if (prop == REF) { return new OfficialImpl(this, id, REF); }
            if (prop == EXPULSION && source.isFile()) {
                if (elements.get(Penalty.class) == null) { return null; }
                Penalty p = (Penalty) elements.get(Penalty.class).get(id);
                if (p != null) {
                    Expulsion e = get(EXPULSION, p.getId());
                    return e == null ? new ExpulsionImpl(this, p) : e;
                }
            }
            return null;
        }
    }

    @Override
    public void postAutosaveUpdate() {
        synchronized (coreLock) {
            // Button may have a label from autosave but undo will not work after restart
            setLabel(Button.UNDO, ACTION_NONE);
        }
    }

    @Override
    public boolean isInPeriod() {
        return get(IN_PERIOD);
    }
    @Override
    public void setInPeriod(boolean p) {
        set(IN_PERIOD, p);
    }
    @Override
    public Period getOrCreatePeriod(int p) {
        return getOrCreate(PERIOD, p);
    }
    @Override
    public Period getCurrentPeriod() {
        return get(CURRENT_PERIOD);
    }
    @Override
    public int getCurrentPeriodNumber() {
        return get(CURRENT_PERIOD_NUMBER);
    }

    @Override
    public boolean isInJam() {
        return get(IN_JAM);
    }
    @Override
    public Jam getUpcomingJam() {
        return get(UPCOMING_JAM);
    }
    protected void advanceUpcomingJam() {
        Jam upcoming = getUpcomingJam();
        Jam next = new JamImpl(this, upcoming);
        set(UPCOMING_JAM, next);
        upcoming.setParent(getCurrentPeriod());
        getCurrentPeriod().add(Period.JAM, upcoming);
    }

    @Override
    public void updateTeamJams() {
        for (Team t : getAll(TEAM)) { t.updateTeamJams(); }
    }

    @Override
    public boolean isInOvertime() {
        return get(IN_OVERTIME);
    }
    @Override
    public void setInOvertime(boolean o) {
        set(IN_OVERTIME, o);
    }
    @Override
    public void startOvertime() {
        synchronized (coreLock) {
            Clock pc = getClock(Clock.ID_PERIOD);
            Clock lc = getClock(Clock.ID_LINEUP);

            if (pc.isRunning() || isInJam()) { return; }
            if (pc.getNumber() < getInt(Rule.NUMBER_PERIODS)) { return; }
            if (!pc.isTimeAtEnd()) { return; }
            if (isOfficialScore()) { return; }

            createSnapshot(ACTION_OVERTIME);

            _endTimeout(false);
            setInOvertime(true);
            setLabels(ACTION_START_JAM, ACTION_NONE, ACTION_TIMEOUT);
            long otLineupTime = getLong(Rule.OVERTIME_LINEUP_DURATION);
            if (lc.getMaximumTime() < otLineupTime) { lc.setMaximumTime(otLineupTime); }
            _startLineup();
        }
    }

    @Override
    public boolean isInSuddenScoring() {
        return get(IN_SUDDEN_SCORING);
    }

    @Override
    public boolean isLastTwoMinutes() {
        return (getClock(Clock.ID_PERIOD).getTimeRemaining() < 120000 &&
                getCurrentPeriodNumber() == getInt(Rule.NUMBER_PERIODS));
    }

    @Override
    public boolean isOfficialScore() {
        return get(OFFICIAL_SCORE);
    }
    @Override
    public void setOfficialScore(boolean o) {
        set(OFFICIAL_SCORE, o);
    }

    @Override
    public void startJam() {
        synchronized (coreLock) {
            if (!isInJam() && !isOfficialScore()) {
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
            autostartRan = false;
            Clock lc = getClock(Clock.ID_LINEUP);
            Clock tc = getClock(Clock.ID_TIMEOUT);
            Clock ic = getClock(Clock.ID_INTERMISSION);

            if (isInJam()) {
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
                // just after starting intermission this is almost surely the operator trying to
                // end a last jam that was just auto ended or accidentally hitting the button twice
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
            Clock tc = getClock(Clock.ID_TIMEOUT);
            if (tc.isRunning()) {
                if (tc.getTimeElapsed() < 1000L) {
                    // This is almost surely an accidental double press
                    // ignore as it messes up stats and makes undo impossible
                    return;
                }
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

            if (!getCurrentTimeout().isRunning()) { timeout(); }
            getCurrentTimeout().set(Timeout.REVIEW, review);
            getCurrentTimeout().set(Timeout.OWNER, owner);
            if (!getBoolean(Rule.STOP_PC_ON_TO)) {
                boolean stopPc = false;
                if (owner instanceof Team) {
                    if (review && getBoolean(Rule.STOP_PC_ON_OR)) { stopPc = true; }
                    if (!review && getBoolean(Rule.STOP_PC_ON_TTO)) { stopPc = true; }
                } else if (owner == Timeout.Owners.OTO && getBoolean(Rule.STOP_PC_ON_OTO)) {
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
        }
    }
    private void _preparePeriod() {
        Clock pc = getClock(Clock.ID_PERIOD);
        Clock jc = getClock(Clock.ID_JAM);

        set(CURRENT_PERIOD, getOrCreatePeriod(getCurrentPeriodNumber() + 1));
        if (getBoolean(Rule.JAM_NUMBER_PER_PERIOD)) {
            getUpcomingJam().set(Jam.NUMBER, 1, Source.RENUMBER, Flag.SPECIAL_CASE);
        }

        if (getBoolean(Rule.SUDDEN_SCORING)) {
            int pointsDiff = Math.abs(getTeam(Team.ID_1).getScore() - getTeam(Team.ID_2).getScore());
            int trailingScore = Math.min(getTeam(Team.ID_1).getScore(), getTeam(Team.ID_2).getScore());
            if (pointsDiff >= getInt(Rule.SUDDEN_SCORING_MIN_POINTS_DIFFERENCE) &&
                trailingScore <= getInt(Rule.SUDDEN_SCORING_MAX_TRAILING_POINTS)) {
                set(IN_SUDDEN_SCORING, true);
            }
        }
        pc.resetTime();
        jc.resetTime();
    }
    private void _possiblyEndPeriod() {
        Clock pc = getClock(Clock.ID_PERIOD);
        Clock tc = getClock(Clock.ID_TIMEOUT);

        if (pc.isTimeAtEnd() && !pc.isRunning() && !isInJam() && !tc.isRunning()) {
            setLabels(ACTION_START_JAM, ACTION_LINEUP, ACTION_TIMEOUT);
            setInPeriod(false);
            setOfficialScore(false);
            _endLineup();
            _startIntermission();
        }
    }
    private void _startJam() {
        Clock pc = getClock(Clock.ID_PERIOD);
        Clock jc = getClock(Clock.ID_JAM);

        _endIntermission(false);
        _endTimeout(false);
        _endLineup();
        pc.start();
        advanceUpcomingJam();
        getCurrentPeriod().startJam();
        set(IN_JAM, true);
        jc.restart();

        getTeam(Team.ID_1).startJam();
        getTeam(Team.ID_2).startJam();
    }
    private void _endJam() {
        Clock pc = getClock(Clock.ID_PERIOD);
        Clock jc = getClock(Clock.ID_JAM);

        if (!isInJam()) { return; }

        jc.stop();
        getCurrentPeriod().stopJam();
        // Order is crucial here.
        // Moving this above Period.stopJam() will break NoMoreJam detection
        // Moving it below Team.stopJam() will break setting positions/fieldings
        set(IN_JAM, false);
        getTeam(Team.ID_1).stopJam();
        getTeam(Team.ID_2).stopJam();
        setInOvertime(false);

        if (pc.isRunning()) {
            _startLineup();
        } else {
            _possiblyEndPeriod();
        }
        jsonSnapshotter.writeOnNextUpdate();
    }
    private void _startLineup() {
        Clock lc = getClock(Clock.ID_LINEUP);

        _endIntermission(false);
        setInPeriod(true);
        lc.changeNumber(1);
        lc.restart();
    }
    private void _endLineup() {
        Clock lc = getClock(Clock.ID_LINEUP);

        lc.stop();
    }
    private void _startTimeout() {
        Clock pc = getClock(Clock.ID_PERIOD);
        Clock tc = getClock(Clock.ID_TIMEOUT);

        if (getCurrentTimeout().isRunning()) {
            // end the previous timeout before starting a new one
            _endTimeout(true);
        }

        if (getBoolean(Rule.STOP_PC_ON_TO)) { pc.stop(); }
        _endLineup();
        _endJam();
        _endIntermission(false);
        setInPeriod(true);
        set(CURRENT_TIMEOUT, new TimeoutImpl(getCurrentPeriod().getCurrentJam()));
        getCurrentTimeout().getParent().add(Period.TIMEOUT, getCurrentTimeout());
        tc.changeNumber(1);
        tc.restart();
    }
    private void _endTimeout(boolean timeoutFollows) {
        Clock tc = getClock(Clock.ID_TIMEOUT);
        Clock pc = getClock(Clock.ID_PERIOD);

        if (!getCurrentTimeout().isRunning()) { return; }

        if (!getSetting(ScoreBoard.SETTING_CLOCK_AFTER_TIMEOUT).equals(Clock.ID_TIMEOUT)) { tc.stop(); }
        getCurrentTimeout().stop();
        if (!timeoutFollows) {
            set(CURRENT_TIMEOUT, noTimeoutDummy);
            if (pc.isTimeAtEnd()) {
                _possiblyEndPeriod();
            } else {
                if (get(NO_MORE_JAM)) { pc.start(); }
                if (getSetting(ScoreBoard.SETTING_CLOCK_AFTER_TIMEOUT).equals(Clock.ID_LINEUP)) { _startLineup(); }
            }
        }
        jsonSnapshotter.writeOnNextUpdate();
    }
    private void _startIntermission() {
        Clock ic = getClock(Clock.ID_INTERMISSION);

        ic.setMaximumTime(ic.getCurrentIntermissionTime());
        ic.restart();
    }
    private void _endIntermission(boolean force) {
        Clock ic = getClock(Clock.ID_INTERMISSION);
        Clock pc = getClock(Clock.ID_PERIOD);

        if (!ic.isRunning() && !force && getCurrentPeriodNumber() > 0) { return; }

        ic.stop();
        if (!isOfficialScore() && (getCurrentPeriodNumber() == 0 || (ic.getTimeRemaining() < ic.getTimeElapsed() &&
                                                                     pc.getNumber() < getInt(Rule.NUMBER_PERIODS)))) {
            // If less than half of intermission is left and there is another period,
            // go to the next period. Otherwise extend the previous period.
            // Always start period 1 as there is no previous period to extend.
            // Skip all of this if the score is official (premature game end)
            _preparePeriod();
        }
    }
    private void _possiblyAutostart() {
        Clock pc = getClock(Clock.ID_PERIOD);
        Clock jc = getClock(Clock.ID_JAM);
        Clock lc = getClock(Clock.ID_LINEUP);
        Clock tc = getClock(Clock.ID_TIMEOUT);

        long bufferTime = ClockConversion.fromHumanReadable(getSetting(ScoreBoard.SETTING_AUTO_START_BUFFER));
        long triggerTime =
            bufferTime + (isInOvertime() ? getLong(Rule.OVERTIME_LINEUP_DURATION) : getLong(Rule.LINEUP_DURATION));

        if (lc.getTimeElapsed() >= triggerTime && !autostartRan) {
            autostartRan = true;
            if (Clock.ID_JAM.equals(getSetting(ScoreBoard.SETTING_AUTO_START))) {
                startJam();
                jc.elapseTime(bufferTime);
            } else if (Clock.ID_TIMEOUT.equals(getSetting(ScoreBoard.SETTING_AUTO_START))) {
                timeout();
                pc.elapseTime(-bufferTime);
                tc.elapseTime(bufferTime);
            }
        }
    }

    protected void createSnapshot(String type) {
        snapshot = new GameSnapshot(this, type);
        setLabel(Button.UNDO, UNDO_PREFIX + type);
    }
    protected void restoreSnapshot() {
        ScoreBoardClock.getInstance().rewindTo(snapshot.getSnapshotTime());
        if (getCurrentPeriod() != snapshot.getCurrentPeriod()) {
            if (getCurrentPeriod().numberOf(Period.JAM) > 0) {
                // We're undoing a period advancement. Move the upcoming Jam
                // (and any associated Fieldings) back out before deleting the period.
                Jam movedJam = getCurrentPeriod().getFirst(Period.JAM);
                getCurrentPeriod().remove(Period.JAM, movedJam);
                movedJam.setParent(this);
                set(UPCOMING_JAM, movedJam);
            }
            getCurrentPeriod().delete();
            set(CURRENT_PERIOD, snapshot.getCurrentPeriod());
        }
        getCurrentPeriod().restoreSnapshot(snapshot.getPeriodSnapshot());
        if (getCurrentTimeout() != snapshot.getCurrentTimeout() && getCurrentTimeout() != noTimeoutDummy) {
            getCurrentTimeout().delete();
        }
        if (getCurrentTimeout() != snapshot.getCurrentTimeout() && snapshot.getCurrentTimeout() != noTimeoutDummy) {
            snapshot.getCurrentTimeout().set(Timeout.RUNNING, true);
        }
        set(CURRENT_TIMEOUT, snapshot.getCurrentTimeout());
        setInOvertime(snapshot.inOvertime());
        set(IN_JAM, snapshot.inJam());
        setInPeriod(snapshot.inPeriod());
        for (Clock clock : getAll(CLOCK)) { clock.restoreSnapshot(snapshot.getClockSnapshot(clock.getId())); }
        for (Team team : getAll(TEAM)) { team.restoreSnapshot(snapshot.getTeamSnapshot(team.getId())); }
        for (Button button : Button.values()) { setLabel(button, snapshot.getLabels().get(button)); }
        setLabel(Button.UNDO, ACTION_NONE);
        setLabel(Button.REPLACED, snapshot.getType());
        snapshot = null;
    }
    protected void finishReplace() {
        if (!replacePending) { return; }
        ScoreBoardClock.getInstance().start(true);
        replacePending = false;
    }
    @Override
    public void clockUndo(boolean replace) {
        synchronized (coreLock) {
            if (replacePending) {
                createSnapshot(ACTION_NO_REPLACE);
                finishReplace();
            } else if (snapshot != null) {
                ScoreBoardClock.getInstance().stop();
                restoreSnapshot();
                if (replace) {
                    replacePending = true;
                    setLabel(Button.UNDO, ACTION_NO_REPLACE);
                } else {
                    ScoreBoardClock.getInstance().start(true);
                }
            }
        }
    }

    private String getSetting(String key) { return scoreBoard.getSettings().get(key); }
    private boolean getBooleanSetting(String key) { return Boolean.parseBoolean(scoreBoard.getSettings().get(key)); }

    public String getLabel(Button button) { return get(LABEL, button.toString()).getValue(); }
    public void setLabel(Button button, String label) { add(LABEL, new ValWithId(button.toString(), label)); }
    protected void setLabels(String startLabel, String stopLabel, String timeoutLabel) {
        setLabel(Button.START, startLabel);
        setLabel(Button.STOP, stopLabel);
        setLabel(Button.TIMEOUT, timeoutLabel);
    }

    @Override
    public Clock getClock(String id) {
        return get(CLOCK, id);
    }

    @Override
    public Team getTeam(String id) {
        return get(TEAM, id);
    }

    @Override
    public Timeout getCurrentTimeout() {
        return get(CURRENT_TIMEOUT);
    }
    @Override
    public TimeoutOwner getTimeoutOwner() {
        return get(TIMEOUT_OWNER);
    }
    @Override
    public void setTimeoutOwner(TimeoutOwner owner) {
        set(TIMEOUT_OWNER, owner);
    }
    @Override
    public boolean isOfficialReview() {
        return get(OFFICIAL_REVIEW);
    }
    @Override
    public void setOfficialReview(boolean official) {
        set(OFFICIAL_REVIEW, official);
    }

    @Override
    public Ruleset getRuleset() {
        return get(RULESET);
    }
    @Override
    public String getRulesetName() {
        return get(RULESET_NAME);
    }
    @Override
    public void setRuleset(Ruleset rs) {
        set(RULESET, rs);
    }
    @Override
    public void refreshRuleset(Ruleset rs) {
        if (getRuleset() == null) { return; }
        for (Ruleset tRs = getRuleset(); !Rulesets.ROOT_ID.equals(tRs.getId()); tRs = tRs.getParentRuleset()) {
            if (tRs.equals(rs)) {
                setCurrentRulesetRecurse(getRuleset());
                scoreBoardChange(new ScoreBoardEvent<>(this, RULESET, getRuleset(), getRuleset()));
                break;
            }
        }
    }

    private void setCurrentRulesetRecurse(Ruleset rs) {
        if (rs == null) { return; }
        if (!rs.getId().equals(Rulesets.ROOT_ID)) { setCurrentRulesetRecurse(rs.getParentRuleset()); }
        for (ValWithId r : rs.getAll(Ruleset.RULE)) {
            if (scoreBoard.getRulesets().getRuleDefinition(r.getId()).isValueValid(r.getValue())) { add(RULE, r); }
        }
    }

    @Override
    public String get(Rule k) {
        return get(RULE, k.toString()).getValue();
    }
    @Override
    public boolean getBoolean(Rule k) {
        return Boolean.parseBoolean(get(k));
    }
    @Override
    public int getInt(Rule k) {
        return Integer.parseInt(get(k));
    }
    @Override
    public long getLong(Rule k) {
        synchronized (coreLock) {
            switch (k.getRuleDefinition().getType()) {
            case TIME: return ClockConversion.fromHumanReadable(get(k));
            default: return Long.parseLong(get(k));
            }
        }
    }
    @Override
    public void set(Rule k, String v) {
        synchronized (coreLock) {
            RuleDefinition r = k.getRuleDefinition();
            if (r == null || !r.isValueValid(v)) { return; }
            add(RULE, new ValWithId(k.toString(), v));
        }
    }

    public void setRuleDefinitionsFromJSON(String file) {
        File penaltyFile = new File(BasePath.get(), file);
        try (Reader reader = new FileReader(penaltyFile)) {
            PenaltyCodesDefinition def = JSON.std.beanFrom(PenaltyCodesDefinition.class, reader);
            removeAll(PENALTY_CODE);
            def.add(new PenaltyCode("?", "Unknown"));
            for (PenaltyCode p : def.getPenalties()) { add(PENALTY_CODE, p); }
        } catch (Exception e) { throw new RuntimeException("Failed to load Penalty Data from file", e); }
    }

    @Override
    public String getFilename() {
        return get(FILENAME);
    }

    public String checkNewFilename(String baseName) {
        String fullName = baseName;
        int suffix = 0;
        while (filenameIsUsed(fullName)) {
            suffix++;
            fullName = baseName + "_" + String.valueOf(suffix);
        }
        return fullName;
    }

    public boolean filenameIsUsed(String filename) {
        for (Game g : scoreBoard.getAll(ScoreBoard.GAME)) {
            if (filename.equals(g.getFilename())) { return true; }
        }

        return new File(jsonDirectory, filename + ".json").exists();
    }

    @Override
    public void exportDone(boolean success) {
        if (success) {
            set(LAST_FILE_UPDATE,
                LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
        }
        set(UPDATE_IN_PROGRESS, false);
        statsbookExporter = null;
    }

    protected GameSnapshot snapshot = null;
    protected boolean replacePending = false;
    protected boolean autostartRan = false;

    protected static File jsonDirectory = new File(BasePath.get(), "html/game-data/json");

    protected Timeout noTimeoutDummy;

    protected StatsbookExporter statsbookExporter;
    protected JSONStateSnapshotter jsonSnapshotter;

    public static class GameSnapshot {
        private GameSnapshot(GameImpl g, String type) {
            snapshotTime = ScoreBoardClock.getInstance().getCurrentTime();
            this.type = type;
            currentTimeout = g.getCurrentTimeout();
            inOvertime = g.isInOvertime();
            inSuddenScoring = g.isInSuddenScoring();
            inJam = g.isInJam();
            inPeriod = g.isInPeriod();
            currentPeriod = g.getCurrentPeriod();
            periodSnapshot = g.getCurrentPeriod().snapshot();
            labels = new HashMap<>();
            for (Button button : Button.values()) { labels.put(button, g.getLabel(button)); }
            clockSnapshots = new HashMap<>();
            for (Clock clock : g.getAll(CLOCK)) { clockSnapshots.put(clock.getId(), clock.snapshot()); }
            teamSnapshots = new HashMap<>();
            for (Team team : g.getAll(TEAM)) { teamSnapshots.put(team.getId(), team.snapshot()); }
        }

        public String getType() { return type; }
        public long getSnapshotTime() { return snapshotTime; }
        public Timeout getCurrentTimeout() { return currentTimeout; }
        public boolean inOvertime() { return inOvertime; }
        public boolean inSuddenScoring() { return inSuddenScoring; }
        public boolean inJam() { return inJam; }
        public boolean inPeriod() { return inPeriod; }
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
        protected boolean inSuddenScoring;
        protected boolean inJam;
        protected boolean inPeriod;
        protected Period currentPeriod;
        protected PeriodSnapshot periodSnapshot;
        protected Map<Button, String> labels;
        protected Map<String, Clock.ClockSnapshot> clockSnapshots;
        protected Map<String, Team.TeamSnapshot> teamSnapshots;
    }

    static public enum Button {
        START("Start"),
        STOP("Stop"),
        @SuppressWarnings("hiding")
        TIMEOUT("Timeout"),
        UNDO("Undo"),
        REPLACED("Replaced");

        private Button(String i) { id = i; }

        @Override
        public String toString() {
            return id;
        }

        private String id;
    }
}
