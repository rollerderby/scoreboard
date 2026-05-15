package com.carolinarollergirls.scoreboard.core.game;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.jr.ob.JSON;

import com.carolinarollergirls.scoreboard.core.interfaces.BoxTrip;
import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame;
import com.carolinarollergirls.scoreboard.core.interfaces.Expulsion;
import com.carolinarollergirls.scoreboard.core.interfaces.FloorPosition;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Jam;
import com.carolinarollergirls.scoreboard.core.interfaces.Media.MediaFile;
import com.carolinarollergirls.scoreboard.core.interfaces.Media.MediaType;
import com.carolinarollergirls.scoreboard.core.interfaces.Official;
import com.carolinarollergirls.scoreboard.core.interfaces.OfficialPosition;
import com.carolinarollergirls.scoreboard.core.interfaces.OfficialsCrew;
import com.carolinarollergirls.scoreboard.core.interfaces.Penalty;
import com.carolinarollergirls.scoreboard.core.interfaces.Period;
import com.carolinarollergirls.scoreboard.core.interfaces.Period.PeriodSnapshot;
import com.carolinarollergirls.scoreboard.core.interfaces.Position;
import com.carolinarollergirls.scoreboard.core.interfaces.PreparedTeam;
import com.carolinarollergirls.scoreboard.core.interfaces.Rulesets;
import com.carolinarollergirls.scoreboard.core.interfaces.Rulesets.Ruleset;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.core.interfaces.Timeout;
import com.carolinarollergirls.scoreboard.core.interfaces.TimeoutOwner;
import com.carolinarollergirls.scoreboard.core.prepared.OfficialsCrewImpl;
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

public final class GameImpl extends ScoreBoardEventProviderImpl<Game> implements Game {
    public GameImpl(ScoreBoard sb, PreparedTeam team1, PreparedTeam team2, Ruleset rs) {
        super(sb, UUID.randomUUID().toString(), ScoreBoard.GAME);
        initReferences(rs, null);
        getTeam(Team.ID_1).loadPreparedTeam(team1);
        getTeam(Team.ID_2).loadPreparedTeam(team2);
        jsonSnapshotter = new JSONStateSnapshotter(sb.getJsm(), this, sb.useMetrics());
    }
    public GameImpl(ScoreBoard parent, String id) {
        super(parent, id, ScoreBoard.GAME);
        initReferences(scoreBoard.getRulesets().get(Rulesets.DEFAULT), null);
        jsonSnapshotter = new JSONStateSnapshotter(getScoreBoard().getJsm(), this, parent.useMetrics());
    }
    public GameImpl(Game source) {
        super(source.getScoreBoard(), UUID.randomUUID().toString(), ScoreBoard.GAME);
        initReferences(source.getRuleset(), source);
        set(NAME_FORMAT, source.get(NAME_FORMAT));
        for (ValWithId ei : source.getAll(EVENT_INFO)) { add(EVENT_INFO, ei); }
        for (Official nso : source.getAll(NSO)) { add(NSO, new OfficialImpl(this, nso)); }
        for (Official so : source.getAll(REF)) { add(REF, new OfficialImpl(this, so)); }
        Official hnso = source.get(HEAD_NSO);
        if (hnso != null) {
            String hnsoName = hnso.get(Official.NAME);
            for (Official nso : getAll(NSO)) {
                if (nso.get(Official.NAME) == hnsoName) { set(HEAD_NSO, nso); }
            }
        }
        jsonSnapshotter = new JSONStateSnapshotter(getScoreBoard().getJsm(), this, getScoreBoard().useMetrics());
    }

    private void initReferences(Ruleset rs, Game source) {
        addProperties(props);

        setCopy(CURRENT_PERIOD_NUMBER, this, CURRENT_PERIOD, Period.NUMBER, true);
        setCopy(IN_PERIOD, this, CURRENT_PERIOD, Period.RUNNING, false);
        setCopy(IN_SUDDEN_SCORING, this, CURRENT_PERIOD, Period.SUDDEN_SCORING, false);
        setCopy(UPCOMING_JAM_NUMBER, this, UPCOMING_JAM, Jam.NUMBER, true);
        setCopy(INJURY_CONTINUATION_UPCOMING, this, UPCOMING_JAM, Jam.INJURY_CONTINUATION, false);
        setCopy(TIMEOUT_OWNER, this, CURRENT_TIMEOUT, Timeout.OWNER, false);
        setCopy(OFFICIAL_REVIEW, this, CURRENT_TIMEOUT, Timeout.REVIEW, false);
        setCopy(RULESET_NAME, this, RULESET, Ruleset.NAME, true);
        setRuleset(rs);
        if (rs == null) {
            for (Rule r : Rule.values()) { set(r, source.get(r)); }
        }
        add(TEAM, source != null ? new TeamImpl(this, source.getTeam(Team.ID_1)) : new TeamImpl(this, Team.ID_1));
        add(TEAM, source != null ? new TeamImpl(this, source.getTeam(Team.ID_2)) : new TeamImpl(this, Team.ID_2));
        addWriteProtection(TEAM);
        add(CLOCK, new ClockImpl(this, Clock.ID_PERIOD));
        add(CLOCK, new ClockImpl(this, Clock.ID_JAM));
        add(CLOCK, new ClockImpl(this, Clock.ID_LINEUP));
        add(CLOCK, new ClockImpl(this, Clock.ID_TIMEOUT));
        add(CLOCK, new ClockImpl(this, Clock.ID_INTERMISSION));
        getOfficialPosition("IPRF");
        getOfficialPosition("IPRR");
        getOfficialPosition("JR1");
        getOfficialPosition("JR2");
        getOfficialPosition("OPRF");
        getOfficialPosition("OPRM");
        getOfficialPosition("OPRR");
        getOfficialPosition("PBM");
        getOfficialPosition("JT");
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
        setRecalculated(EXPORT_BLOCKED_BY)
            .addSource(get(TEAM, Team.ID_1), Team.SCORE_ADJUSTMENT)
            .addSource(get(TEAM, Team.ID_2), Team.SCORE_ADJUSTMENT);
        setRecalculated(INHIBIT_FINAL_SCORE)
            .addSource(this, IN_PERIOD)
            .addIndirectSource(this, CURRENT_TIMEOUT, Timeout.RUNNING)
            .addSource(getClock(Clock.ID_INTERMISSION), Clock.TIME);
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
        setRecalculated(UPCOMING_JAM).addIndirectSource(this, CURRENT_PERIOD, Period.CURRENT_JAM);
        updateTeamJams();

        setInPeriod(false);
        setInOvertime(false);
        setOfficialScore(false);
        snapshot = null;
        replacePending = null;

        setLabels();

        // handle period clock running down between jams
        addScoreBoardListener(new ConditionalScoreBoardListener<>(
            Clock.class, getClock(Clock.ID_PERIOD).getId(), Clock.RUNNING, Boolean.FALSE, new ScoreBoardListener() {
                @Override
                public void scoreBoardChange(ScoreBoardEvent<?> event) {
                    if (getBoolean(Rule.PERIOD_END_BETWEEN_JAMS)) {
                        _possiblyEndPeriod();
                        setLabels();
                    }
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

        // handle intermission restart
        addScoreBoardListener(new ConditionalScoreBoardListener<>(
            Clock.class, getClock(Clock.ID_INTERMISSION).getId(), Clock.RUNNING, Boolean.TRUE,
            new ScoreBoardListener() {
                @Override
                public void scoreBoardChange(ScoreBoardEvent<?> event) {
                    if (getClock(Clock.ID_PERIOD).isTimeAtStart() && getCurrentPeriod().numberOf(Period.JAM) == 0 &&
                        getCurrentPeriodNumber() > 0) {
                        // intermission clock has been restarted before period start - undo period advancement
                        set(CURRENT_PERIOD, getCurrentPeriod().getPrevious());
                    }
                }
            }));

        // handle auto-start (if enabled)
        addScoreBoardListener(new ConditionalScoreBoardListener<>(
            Clock.class, getClock(Clock.ID_LINEUP).getId(), Clock.TIME, new ScoreBoardListener() {
                @Override
                public void scoreBoardChange(ScoreBoardEvent<?> event) {
                    _possiblyAutostart();
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

        // handle auto-end TTO (if enabled) or stopping period clock after certain
        // amount of timeout
        addScoreBoardListener(new ConditionalScoreBoardListener<>(
            Clock.class, getClock(Clock.ID_TIMEOUT).getId(), Clock.TIME, new ScoreBoardListener() {
                @Override
                public void scoreBoardChange(ScoreBoardEvent<?> event) {
                    if (getBooleanSetting(ScoreBoard.SETTING_AUTO_END_TTO) && (getTimeoutOwner() instanceof Team) &&
                        (!getCurrentTimeout().isReview() || get(OR_IS_TO)) &&
                        (Long) event.getValue() == getLong(Rule.TTO_DURATION) &&
                        (ScoreBoardClock.getInstance().isRunning() || quickClockThreshold == 0L)) {
                        stopJamTO();
                    }
                    if ((Long) event.getValue() == getLong(Rule.STOP_PC_AFTER_TO_DURATION) &&
                        getClock(Clock.ID_PERIOD).isRunning()) {
                        getClock(Clock.ID_PERIOD).stop();
                    }
                }
            }));

        // handle score changes after end of game (label update, time for ORs)
        addScoreBoardListener(new ConditionalScoreBoardListener<>(Team.class, Team.SCORE, new ScoreBoardListener() {
            @Override
            public void scoreBoardChange(ScoreBoardEvent<?> event) {
                if (isOvertimeConditions(false)) { setLabels(); }
                if (getTeam(Team.ID_1).get(Team.OFFICIAL_REVIEWS) > 0 ||
                    getTeam(Team.ID_2).get(Team.OFFICIAL_REVIEWS) > 0) {
                    earliestFinalScoreTime =
                        Math.max(earliestFinalScoreTime, ScoreBoardClock.getInstance().getCurrentTime() + (10 * 1000));
                    set(INHIBIT_FINAL_SCORE, true);
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
        if (prop == UPCOMING_JAM && !source.isFile()) {
            Jam j = getCurrentPeriod().getCurrentJam().getNext();
            if (j == null) { j = new JamImpl(this, getCurrentPeriod().getCurrentJam()); }
            j.setParent(this);
            while (j.hasNext()) { j.getNext().delete(); }
            return j;
        } else if (prop == NO_MORE_JAM) {
            if (isInJam() || !isInPeriod()) { return false; }
            if (getInt(Rule.JAMS_PER_PERIOD) > 0) { return false; }
            if (!getBoolean(Rule.PERIOD_END_BETWEEN_JAMS)) { return false; }
            if (getBoolean(Rule.LINEUP_STOPS_PERIOD_CLOCK)) { return false; }
            if (getClock(Clock.ID_PERIOD).isTimeAtStart()) { return false; }
            Jam lastJam = getCurrentPeriod().getCurrentJam();
            long pcRemaining = getClock(Clock.ID_PERIOD).getMaximumTime() - lastJam.getPeriodClockElapsedEnd();
            if (pcRemaining > getLong(Rule.LINEUP_DURATION)) { return false; }
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
        } else if (prop == INHIBIT_FINAL_SCORE) {
            return getBoolean(Rule.ENFORCE_TIME_TO_OR) && this == scoreBoard.getCurrentGame().get(CurrentGame.GAME) &&
                // Only allow a running game to be ended prematurely during intermission or a
                // timeout
                ((getCurrentPeriod().isRunning() && !getCurrentTimeout().isRunning()) ||
                 // only allow final score at the end of a game after teams had a chance to OR the last decisions
                 ScoreBoardClock.getInstance().getCurrentTime() < earliestFinalScoreTime);
        } else if (prop == OFFICIAL_SCORE) {
            if (get(INHIBIT_FINAL_SCORE)) { return false; }
        } else if (prop == EXPORT_BLOCKED_BY) {
            if (!getTeam(Team.ID_1).getAll(Team.SCORE_ADJUSTMENT).isEmpty() ||
                !getTeam(Team.ID_2).getAll(Team.SCORE_ADJUSTMENT).isEmpty()) {
                return "unapplied score adjustments";
            } else {
                return exportFailureText;
            }
        } else if (prop == FIIIVE_SECONDS) {
            if (isInJam()) { return false; }
            if ((Boolean) value && !(Boolean) last) {
                if (!getClock(Clock.ID_LINEUP).isRunning()) { stopJamTO(); }
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
            updateTeamJams();
        } else if (prop == CURRENT_TIMEOUT && value == null) {
            return;
        }
        if (prop == CURRENT_PERIOD) {
            Period p = (Period) value;
            if (p.getCurrentJam().getNext() != getUpcomingJam()) { set(UPCOMING_JAM, p.getCurrentJam().getNext()); }
            while (p.hasNext()) { p.getNext().delete(); }
            updateTeamJams();
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
        if (prop == FILENAME && jsonSnapshotter != null) { jsonSnapshotter.setFilename((String) value); }
        if (prop == OFFICIAL_SCORE && (boolean) value && source == Source.WS) {
            Clock pc = getClock(Clock.ID_PERIOD);
            Clock ic = getClock(Clock.ID_INTERMISSION);
            Clock tc = getClock(Clock.ID_TIMEOUT);
            Clock lc = getClock(Clock.ID_LINEUP);
            if (!ic.isRunning()) {
                // Game ended prematurely. Go to intermission, so "Final Score" is displayed.
                // Don't change period number or time. That info may have to be reported to the
                // governing body.
                if (isInJam()) { _endJam(true); }
                if (tc.isRunning()) {
                    tc.stop();
                    getCurrentTimeout().stop(false);
                }
                if (lc.isRunning()) { lc.stop(); }
                if (pc.isRunning()) { pc.stop(); }
                _startIntermission();
            }
            jsonSnapshotter.writeOnNextUpdate();
        }
        if (prop == OR_IS_TO && getCurrentTimeout().isRunning()) {
            if ((boolean) value && "".equals(getCurrentTimeout().get(Timeout.OR_REQUEST))) {
                getCurrentTimeout().set(Timeout.OR_REQUEST, Timeout.OR_AS_TO_TEXT);
            }
            if (!(boolean) value && Timeout.OR_AS_TO_TEXT.equals(getCurrentTimeout().get(Timeout.OR_REQUEST))) {
                getCurrentTimeout().set(Timeout.OR_REQUEST, "");
            }
        }
    }

    @Override
    protected <T extends ValueWithId> boolean mayAdd(Child<T> prop, T item, Source source) {
        if (prop == RULE && getRuleset() != null && source == Source.WS) {
            getRuleset().add(Ruleset.RULE, (ValWithId) item);
            return false;
        }
        if (prop == Period.JAM && item != getUpcomingJam() && !source.isFile()) { return false; }
        return true;
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
        if (prop == EVENT_INFO && get(STATE) == State.PREPARED && INFO_START_TIME.equals(item.getId()) &&
            scoreBoard.getCurrentGame().get(CurrentGame.GAME) == this) {
            LocalTime time = LocalTime.parse(item.getValue());
            LocalDate date = "".equals(get(EVENT_INFO, INFO_DATE).getValue())
                                 ? LocalDate.now()
                                 : LocalDate.parse(get(EVENT_INFO, INFO_DATE).getValue());
            long timeToStart = ChronoUnit.MILLIS.between(LocalDateTime.now(), LocalDateTime.of(date, time));
            if (timeToStart > 0) {
                Clock ic = getClock(Clock.ID_INTERMISSION);
                ic.stop();
                ic.setMaximumTime(timeToStart);
                ic.resetTime();
                ic.start();
            }
        }
    }

    @Override
    protected void itemRemoved(Child<?> prop, ValueWithId item, Source source) {
        if (prop == EXPULSION) { ((Expulsion) item).delete(); }
        if (prop == PERIOD && item == getCurrentPeriod() && source != Source.RENUMBER) {
            set(CURRENT_PERIOD, getLast(PERIOD));
            if (!getClock(Clock.ID_INTERMISSION).isRunning()) { _preparePeriod(); }
        }
        if (prop == NSO && item == get(HEAD_NSO)) { set(HEAD_NSO, null); }
        if (prop == REF && item == get(HEAD_REF)) { set(HEAD_REF, null); }
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
            if (statsbookExporter == null) {
                set(UPDATE_IN_PROGRESS, true);
                statsbookExporter = new StatsbookExporter(this);
            }
            jsonSnapshotter.writeFile();
        } else if (prop == START_BOX_TRIP) {
            add(Team.BOX_TRIP, new BoxTripImpl(this));
        } else if (prop == START_JAMMER_BOX_TRIP) {
            jammerBoxEntry();
        } else if (prop == COPY) {
            parent.add(ownType, new GameImpl(this));
        } else if (prop == EARLY_5) {
            Clock lc = getClock(Clock.ID_LINEUP);
            long delay = 1000 * Integer.valueOf(getSetting(ScoreBoard.SETTING_EARLY_5_DELAY));
            if (lc.isRunning()) {
                set(AUTO_FIVE, true);
                lineupTimeFor5s = Math.min(lc.getTimeElapsed() + delay, lineupTimeFor5s);
                lineupTimeForStart = lineupTimeFor5s + 5000L;
            }
        } else if (prop == LOAD_OFFICIALS_CREW) {
            OfficialsCrew crew = get(OFFICIALS_CREW);
            if (crew.numberOf(OfficialsCrew.NSO) > 0) { removeAll(NSO); }
            if (crew.numberOf(OfficialsCrew.REF) > 0) { removeAll(REF); }
            for (OfficialsCrew.Member nso : crew.getAll(OfficialsCrew.NSO)) { add(NSO, new OfficialImpl(this, nso)); }
            for (OfficialsCrew.Member ref : crew.getAll(OfficialsCrew.REF)) { add(REF, new OfficialImpl(this, ref)); }
        } else if (prop == STORE_OFFICIALS_CREW) {
            OfficialsCrew crew = new OfficialsCrewImpl(this);
            scoreBoard.add(ScoreBoard.OFFICIALS_CREW, crew);
            set(OFFICIALS_CREW, crew);
        }
    }

    @Override
    public ScoreBoardEventProvider create(Child<? extends ScoreBoardEventProvider> prop, String id, Source source) {
        synchronized (coreLock) {
            if (prop == CLOCK) { return new ClockImpl(this, id); }
            if (prop == TEAM) { return new TeamImpl(this, id); }
            if (prop == Period.JAM) {
                int num = Integer.parseInt(id);
                if (source.isFile()) {
                    getUpcomingJam().set(Jam.NUMBER, num, Source.RENUMBER, Flag.SPECIAL_CASE);
                    return getUpcomingJam();
                } else if (num == getCurrentPeriod().getCurrentJamNumber()) {
                    // could be a race around jam start
                    return getCurrentPeriod().getCurrentJam();
                }
            }
            if (prop == PERIOD) {
                int num = Integer.parseInt(id);
                if (0 <= num && (num <= getInt(Rule.NUMBER_PERIODS) || source.isFile())) {
                    return new PeriodImpl(this, num);
                }
            }
            if (prop == Team.BOX_TRIP) { return new BoxTripImpl(this, id); }
            if (prop == NSO) { return new OfficialImpl(this, id, NSO); }
            if (prop == REF) { return new OfficialImpl(this, id, REF); }
            if (prop == OFFICIAL_POSITION) { return new OfficialPositionImpl(this, id); }
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
            // Undo button may have a label from autosave but undo will not work after restart
            setLabels();
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
            Clock lc = getClock(Clock.ID_LINEUP);

            if (!isOvertimeConditions(false)) { return; }

            createSnapshot(ACTION_OVERTIME);

            if (getCurrentTimeout().isRunning()) { _endTimeout(true, false); }
            setInOvertime(true);
            long otLineupTime = getLong(Rule.OVERTIME_LINEUP_DURATION);
            if (lc.getMaximumTime() < otLineupTime) { lc.setMaximumTime(otLineupTime); }
            _startLineup();
            setLabels();
        }
    }
    public boolean isOvertimeConditions(boolean checkScores) {
        Clock pc = getClock(Clock.ID_PERIOD);

        if (pc.isRunning() || isInJam()) { return false; }
        if (pc.getNumber() < getInt(Rule.NUMBER_PERIODS)) { return false; }
        if (!pc.isTimeAtEnd()) { return false; }
        if (isOfficialScore()) { return false; }

        if (checkScores && getTeam(Team.ID_1).getScore() != getTeam(Team.ID_2).getScore()) { return false; }

        return true;
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
            if (!isInJam() && !restoreRunning && !isOfficialScore() && !quickClockControl(Button.START)) {
                createSnapshot(ACTION_START_JAM);
                _startJam();
                finishReplace();
            }
        }
    }
    @Override
    public void stopJamTO() {
        synchronized (coreLock) {
            Clock lc = getClock(Clock.ID_LINEUP);
            if ((getCurrentTimeout().isRunning() || !lc.isRunning()) && !restoreRunning && !isOfficialScore() &&
                !quickClockControl(Button.STOP)) {
                autostartRan = false;

                if (isInJam()) {
                    createSnapshot(ACTION_STOP_JAM);
                    _endJam(true);
                    finishReplace();
                } else if (getCurrentTimeout().isRunning()) {
                    createSnapshot(ACTION_STOP_TO);
                    _endTimeout(true, false);
                    finishReplace();
                } else if (!lc.isRunning()) {
                    if (isOvertimeConditions(true)) {
                        startOvertime();
                        finishReplace();
                    } else {
                        createSnapshot(ACTION_LINEUP);
                        _startLineup();
                        finishReplace();
                    }
                }
            }
        }
    }
    @Override
    public void timeout() {
        synchronized (coreLock) {
            if (!restoreRunning && !isOfficialScore() && !quickClockControl(Button.TIMEOUT)) {
                if (getCurrentTimeout().isRunning()) {
                    createSnapshot(ACTION_RE_TIMEOUT);
                } else {
                    createSnapshot(ACTION_TIMEOUT);
                }
                _startTimeout();
                finishReplace();
            }
        }
    }
    @Override
    public void setTimeoutType(TimeoutOwner owner, boolean review) {
        synchronized (coreLock) {
            Clock tc = getClock(Clock.ID_TIMEOUT);
            Clock pc = getClock(Clock.ID_PERIOD);

            if (restoreRunning) { return; }
            if (!getCurrentTimeout().isRunning()) { timeout(); }
            getCurrentTimeout().set(Timeout.REVIEW, review);
            getCurrentTimeout().set(Timeout.OWNER, owner);
            if (!getBoolean(Rule.STOP_PC_ON_TO) && getInt(Rule.JAMS_PER_PERIOD) == 0) {
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

        if (pc.isTimeAtEnd() && !pc.isRunning() && !isInJam() && !getCurrentTimeout().isRunning()) {
            setInPeriod(false);
            setOfficialScore(false);
            _endLineup();
            tc.stop();
            set(CURRENT_TIMEOUT, noTimeoutDummy);
            _startIntermission();
        }
    }
    private void _startJam() {
        Clock pc = getClock(Clock.ID_PERIOD);
        Clock jc = getClock(Clock.ID_JAM);

        _endIntermission(false);
        _endLineup();
        if (!(getBoolean(Rule.TO_JAM) && getCurrentTimeout().isRunning())) {
            if (getInt(Rule.JAMS_PER_PERIOD) == 0) { pc.start(); }
            _endTimeout(false, true);
        }
        getCurrentPeriod().startJam();
        set(IN_JAM, true);
        jc.restart();

        getTeam(Team.ID_1).startJam();
        getTeam(Team.ID_2).startJam();
        for (BoxTrip bt : getAll(Team.BOX_TRIP)) { bt.startJam(); }
    }
    private void _endJam(boolean lineupFollows) {
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
        for (BoxTrip bt : getAll(Team.BOX_TRIP)) { bt.stopJam(); }
        setInOvertime(false);

        if (getInt(Rule.JAMS_PER_PERIOD) > 0) {
            pc.elapseTime(getLong(Rule.JAM_DURATION));
            if (pc.isTimeAtEnd()) {
                _possiblyEndPeriod();
            } else {
                _startLineup();
            }
        } else if (pc.isRunning()) {
            _startLineup();
        } else if (lineupFollows) {
            _possiblyEndPeriod();
        }
        if (getTeam(Team.ID_1).get(Team.OFFICIAL_REVIEWS) > 0 || getTeam(Team.ID_2).get(Team.OFFICIAL_REVIEWS) > 0) {
            earliestFinalScoreTime = ScoreBoardClock.getInstance().getCurrentTime() + getLong(Rule.LINEUP_DURATION);
        }
        jsonSnapshotter.writeOnNextUpdate();
    }
    private void _startLineup() {
        Clock lc = getClock(Clock.ID_LINEUP);
        Clock pc = getClock(Clock.ID_PERIOD);

        if (lc.isRunning()) { return; }

        _endIntermission(false);
        setInPeriod(true);
        lineupTimeForStart = isInOvertime() ? getLong(Rule.OVERTIME_LINEUP_DURATION) : getLong(Rule.LINEUP_DURATION);
        lineupTimeFor5s = lineupTimeForStart - 5000L;
        set(AUTO_FIVE, getBooleanSetting(ScoreBoard.SETTING_AUTO_5));
        lc.changeNumber(1);
        lc.restart();
        if (getBoolean(Rule.LINEUP_STOPS_PERIOD_CLOCK)) { pc.stop(); }
    }
    private void _endLineup() {
        Clock lc = getClock(Clock.ID_LINEUP);

        lc.stop();
        lc.set(Clock.NAME, "Lineup");
        set(FIIIVE_SECONDS, false);
        set(AUTO_FIVE, false);
    }
    private void _startTimeout() {
        Clock pc = getClock(Clock.ID_PERIOD);
        Clock tc = getClock(Clock.ID_TIMEOUT);

        if (getCurrentTimeout().isRunning()) {
            // end the previous timeout before starting a new one
            _endTimeout(false, false);
        }

        if (getBoolean(Rule.STOP_PC_ON_TO)) { pc.stop(); }
        _endLineup();
        _endJam(false);
        _endIntermission(false);
        setInPeriod(true);
        set(CURRENT_TIMEOUT, new TimeoutImpl(getCurrentPeriod().getCurrentJam()));
        getCurrentTimeout().getParent().add(Period.TIMEOUT, getCurrentTimeout());
        tc.changeNumber(1);
        tc.restart();
        set(OR_IS_TO, false);
    }
    private void _endTimeout(boolean lineupFollows, boolean jamFollows) {
        Clock tc = getClock(Clock.ID_TIMEOUT);
        Clock pc = getClock(Clock.ID_PERIOD);
        Clock lc = getClock(Clock.ID_LINEUP);

        if (getCurrentTimeout().isRunning()) { getCurrentTimeout().stop(jamFollows); }
        if (!getBoolean(Rule.NO_TO_CLOCK_STOP) || jamFollows) {
            tc.stop();
            set(CURRENT_TIMEOUT, noTimeoutDummy);
        }
        if (lineupFollows) {
            if (pc.isTimeAtEnd()) {
                _possiblyEndPeriod();
            } else {
                if (get(NO_MORE_JAM)) { pc.start(); }
                _startLineup();
                lc.set(Clock.NAME, "Post Timeout");
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
        Clock lc = getClock(Clock.ID_LINEUP);

        String setting =
            getSetting(get(FIIIVE_SECONDS) ? ScoreBoard.SETTING_AUTO_START_5 : ScoreBoard.SETTING_AUTO_START);

        if (!restoreRunning && lc.getTimeElapsed() >= lineupTimeForStart && !autostartRan) {
            autostartRan = true;
            if (Clock.ID_JAM.equals(setting)) {
                startJam();
            } else if (Clock.ID_TIMEOUT.equals(setting)) {
                timeout();
            }
        } else if (lc.getTimeElapsed() >= lineupTimeFor5s && get(AUTO_FIVE)) {
            set(FIIIVE_SECONDS, true);
        }
    }

    protected void createSnapshot(String type) {
        if (!restoreRunning) { snapshot = new GameSnapshot(this, type); }
    }
    protected void restoreSnapshot() {
        restoreRunning = true;
        ScoreBoardClock.getInstance().rewindTo(snapshot.getSnapshotTime());
        for (Clock clock : getAll(CLOCK)) { clock.restoreSnapshot(snapshot.getClockSnapshot(clock.getProviderId())); }
        set(IN_JAM, snapshot.inJam());
        set(IN_PERIOD, snapshot.inPeriod());
        set(CURRENT_PERIOD, snapshot.getCurrentPeriod());
        getCurrentPeriod().restoreSnapshot(snapshot.getPeriodSnapshot());
        if (getCurrentTimeout() != snapshot.getCurrentTimeout() && getCurrentTimeout() != noTimeoutDummy) {
            getCurrentTimeout().delete();
        }
        set(CURRENT_TIMEOUT, snapshot.getCurrentTimeout());
        getCurrentTimeout().set(Timeout.RUNNING, snapshot.inTimeout());
        set(OR_IS_TO, snapshot.orIsTo());
        set(FIIIVE_SECONDS, snapshot.fiveSeconds());
        set(IN_OVERTIME, snapshot.inOvertime());
        for (Team team : getAll(TEAM)) { team.restoreSnapshot(snapshot.getTeamSnapshot(team.getProviderId())); }
        for (BoxTrip bt : getAll(Team.BOX_TRIP)) { bt.restoreSnapshot(snapshot.getBoxTripSnapshot(bt.getId())); };
        for (Team team : getAll(TEAM)) {
            for (BoxTrip bt : team.getAll(Team.BOX_TRIP)) {
                bt.restoreSnapshot(snapshot.getBoxTripSnapshot(bt.getId()));
            }
        }
        restoreRunning = false;
    }
    protected void finishReplace() {
        if (replacePending != null) {
            ScoreBoardClock.getInstance().start(true);
            replacePending = null;
        }
        setLabels();
    }
    @Override
    public void clockUndo(boolean replace) {
        synchronized (coreLock) {
            if (replacePending != null) {
                createSnapshot(ACTION_NO_REPLACE);
                finishReplace();
                lastButton = Button.UNDO;
            } else if (snapshot != null) {
                ScoreBoardClock.getInstance().stop();
                restoreSnapshot();
                if (replace) {
                    replacePending = snapshot.getType();
                } else {
                    ScoreBoardClock.getInstance().start(true);
                    lastButton = Button.UNDO;
                    lastButtonTime = 0;
                }
                snapshot = null;
                setLabels();
            }
        }
    }

    protected boolean quickClockControl(Button button) {
        if (quickClockAlwaysAllowed) { return false; }
        long currentTime = ScoreBoardClock.getInstance().getCurrentTime();
        long lastTime = lastButtonTime;
        boolean differentButton = button != lastButton;
        boolean canReplace = snapshot != null && !(button == Button.START && snapshot.inJam()) &&
                             !(button == Button.STOP && snapshot.getClockSnapshot(Clock.ID_LINEUP).isRunning() &&
                               !snapshot.getCurrentTimeout().isRunning());
        lastButton = button;
        lastButtonTime = currentTime;
        if (replacePending != null || currentTime - lastTime >= quickClockThreshold) { return false; }
        if (differentButton && canReplace) {
            // assume this is a quick correction
            clockUndo(true);
            return false;
        }
        return true;
    }
    public void allowQuickClockControls(boolean allow) { quickClockAlwaysAllowed = allow; }

    private void jammerBoxEntry() {
        // start a clock
        BoxTrip newBt = new BoxTripImpl(this);
        newBt.set(BoxTrip.JAMMER, true);
        add(Team.BOX_TRIP, newBt);
        for (Team team : getAll(TEAM)) {
            if (team.getPosition(team.isStarPass() ? FloorPosition.PIVOT : FloorPosition.JAMMER).isPenaltyBox()) {
                Team newTeam = team.getOtherTeam();
                Position newPosition =
                    newTeam.getPosition(newTeam.isStarPass() ? FloorPosition.PIVOT : FloorPosition.JAMMER);
                if (newPosition.isPenaltyBox()) {
                    // both jammers already marked in box - let the operator resolve
                    return;
                }
                newBt.add(BoxTrip.FIELDING,
                          newPosition.getCurrentFielding()); // this will trigger jammer swap logic
                return;
            }
        }
        for (BoxTrip bt : getAll(Team.BOX_TRIP)) {
            if (bt != newBt && bt.get(BoxTrip.JAMMER)) {
                // this is a swap, but we don't know who is who - assume basic swap
                long shorteningAmount = bt.getClock().getTimeRemaining();
                bt.set(BoxTrip.SHORTENED, 1);
                newBt.set(BoxTrip.SHORTENED, 1);
                bt.getClock().changeMaximumTime(-shorteningAmount);
                newBt.getClock().changeMaximumTime(-shorteningAmount);
                return;
            }
        }
    }

    private String getSetting(String key) { return scoreBoard.getSettings().get(key); }
    private boolean getBooleanSetting(String key) { return Boolean.parseBoolean(scoreBoard.getSettings().get(key)); }

    public String getLabel(Button button) { return get(LABEL, button.toString()).getValue(); }
    public void setLabel(Button button, String label) { add(LABEL, new ValWithId(button.toString(), label)); }
    protected void setLabels() {
        setLabel(Button.START, isInJam() || (getCurrentTimeout().isRunning() && !getBoolean(Rule.TO_JAM))
                                   ? ACTION_NONE
                                   : ACTION_START_JAM);
        setLabel(Button.STOP, isInJam()                               ? ACTION_STOP_JAM
                              : getCurrentTimeout().isRunning()       ? ACTION_STOP_TO
                              : getClock(Clock.ID_LINEUP).isRunning() ? ACTION_NONE
                              : isOvertimeConditions(true)            ? ACTION_OVERTIME
                                                                      : ACTION_LINEUP);
        setLabel(Button.TIMEOUT, getCurrentTimeout().isRunning() ? ACTION_RE_TIMEOUT : ACTION_TIMEOUT);
        setLabel(Button.UNDO, replacePending != null ? ACTION_NO_REPLACE
                              : snapshot == null     ? ACTION_NONE
                                                     : UNDO_PREFIX + snapshot.getType());
        setLabel(Button.REPLACED, replacePending == null ? ACTION_NONE : replacePending);
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
    public void exportDone(boolean success, String failureText) {
        exportFailureText = failureText;
        set(EXPORT_BLOCKED_BY, failureText);
        if (success) {
            set(LAST_FILE_UPDATE,
                LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
        }
        set(UPDATE_IN_PROGRESS, false);
        statsbookExporter = null;
    }
    @Override
    public void clearStatsbookError() {
        exportFailureText = "";
        set(EXPORT_BLOCKED_BY, "");
    }

    @Override
    public OfficialPosition getOfficialPosition(String id) {
        return getOrCreate(OFFICIAL_POSITION, id);
    }

    public static void setQuickClockThreshold(long threshold) { quickClockThreshold = threshold; } // for unit tests

    protected GameSnapshot snapshot = null;
    protected String replacePending = null;
    protected boolean autostartRan = false;
    protected boolean restoreRunning = false;

    private long lineupTimeFor5s = 0L;
    private long lineupTimeForStart = 0L;

    protected static File jsonDirectory = new File(BasePath.get(), "html/game-data/json");

    protected Timeout noTimeoutDummy;

    protected Button lastButton = Button.UNDO;
    protected long lastButtonTime = ScoreBoardClock.getInstance().getCurrentTime();
    protected static long quickClockThreshold = 1000; // ms
    protected boolean quickClockAlwaysAllowed = false;
    protected long earliestFinalScoreTime = ScoreBoardClock.getInstance().getCurrentTime();

    protected StatsbookExporter statsbookExporter;
    protected JSONStateSnapshotter jsonSnapshotter;
    protected String exportFailureText = "";

    public static class GameSnapshot {
        private GameSnapshot(GameImpl g, String type) {
            snapshotTime = ScoreBoardClock.getInstance().getCurrentTime();
            this.type = type;
            currentTimeout = g.getCurrentTimeout();
            inOvertime = g.isInOvertime();
            inSuddenScoring = g.isInSuddenScoring();
            inJam = g.isInJam();
            inPeriod = g.isInPeriod();
            inTimeout = g.getCurrentTimeout().isRunning();
            orIsTo = g.get(OR_IS_TO);
            fiveSeconds = g.get(FIIIVE_SECONDS);
            currentPeriod = g.getCurrentPeriod();
            periodSnapshot = g.getCurrentPeriod().snapshot();
            clockSnapshots = new HashMap<>();
            for (Clock clock : g.getAll(CLOCK)) { clockSnapshots.put(clock.getProviderId(), clock.snapshot()); }
            teamSnapshots = new HashMap<>();
            for (Team team : g.getAll(TEAM)) { teamSnapshots.put(team.getProviderId(), team.snapshot()); }
            boxTripSnapshots = new HashMap<>();
            for (BoxTrip bt : g.getAll(Team.BOX_TRIP)) { boxTripSnapshots.put(bt.getId(), bt.snapshot()); }
            for (Team team : g.getAll(TEAM)) {
                for (BoxTrip bt : team.getAll(Team.BOX_TRIP)) { boxTripSnapshots.put(bt.getId(), bt.snapshot()); }
            }
        }

        public String getType() { return type; }
        public long getSnapshotTime() { return snapshotTime; }
        public Timeout getCurrentTimeout() { return currentTimeout; }
        public boolean inOvertime() { return inOvertime; }
        public boolean inSuddenScoring() { return inSuddenScoring; }
        public boolean inJam() { return inJam; }
        public boolean inTimeout() { return inTimeout; }
        public boolean orIsTo() { return orIsTo; }
        public boolean fiveSeconds() { return fiveSeconds; }
        public boolean inPeriod() { return inPeriod; }
        public Period getCurrentPeriod() { return currentPeriod; }
        public PeriodSnapshot getPeriodSnapshot() { return periodSnapshot; }
        public Map<String, Clock.ClockSnapshot> getClockSnapshots() { return clockSnapshots; }
        public Map<String, Team.TeamSnapshot> getTeamSnapshots() { return teamSnapshots; }
        public Clock.ClockSnapshot getClockSnapshot(String clock) { return clockSnapshots.get(clock); }
        public Team.TeamSnapshot getTeamSnapshot(String team) { return teamSnapshots.get(team); }
        public Clock.ClockSnapshot getBoxTripSnapshot(String bt) { return boxTripSnapshots.get(bt); }

        protected String type;
        protected long snapshotTime;
        protected Timeout currentTimeout;
        protected boolean inOvertime;
        protected boolean inSuddenScoring;
        protected boolean inJam;
        protected boolean inTimeout;
        protected boolean orIsTo;
        protected boolean fiveSeconds;
        protected boolean inPeriod;
        protected Period currentPeriod;
        protected PeriodSnapshot periodSnapshot;
        protected Map<String, Clock.ClockSnapshot> clockSnapshots;
        protected Map<String, Team.TeamSnapshot> teamSnapshots;
        protected Map<String, Clock.ClockSnapshot> boxTripSnapshots;
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
