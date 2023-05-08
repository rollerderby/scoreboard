package com.carolinarollergirls.scoreboard.core.game;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import com.carolinarollergirls.scoreboard.core.interfaces.BoxTrip;
import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.Fielding;
import com.carolinarollergirls.scoreboard.core.interfaces.FloorPosition;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Period;
import com.carolinarollergirls.scoreboard.core.interfaces.Position;
import com.carolinarollergirls.scoreboard.core.interfaces.PreparedTeam;
import com.carolinarollergirls.scoreboard.core.interfaces.PreparedTeam.PreparedSkater;
import com.carolinarollergirls.scoreboard.core.interfaces.Role;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreAdjustment;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoringTrip;
import com.carolinarollergirls.scoreboard.core.interfaces.Settings;
import com.carolinarollergirls.scoreboard.core.interfaces.Skater;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.core.interfaces.TeamJam;
import com.carolinarollergirls.scoreboard.core.interfaces.Timeout;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.IndirectScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.RecalculateScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.event.ValueWithId;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public class TeamImpl extends ScoreBoardEventProviderImpl<Team> implements Team {
    public TeamImpl(Game g, String i) {
        super(g, g.getId() + "_" + i, Game.TEAM);
        game = g;
        subId = i;
        addProperties(props);
        addProperties(preparedProps);
        for (FloorPosition fp : FloorPosition.values()) { add(POSITION, new PositionImpl(this, fp)); }
        addWriteProtection(POSITION);
        addWriteProtectionOverride(FIELDING_ADVANCE_PENDING, Source.NON_WS);
        setCopy(LEAGUE_NAME, this, PREPARED_TEAM, LEAGUE_NAME, false, PREPARED_TEAM_CONNECTED);
        setCopy(TEAM_NAME, this, PREPARED_TEAM, TEAM_NAME, false, PREPARED_TEAM_CONNECTED);
        setCopy(LOGO, this, PREPARED_TEAM, LOGO, false, PREPARED_TEAM_CONNECTED);
        setCopy(CURRENT_TRIP, this, RUNNING_OR_ENDED_TEAM_JAM, TeamJam.CURRENT_TRIP, true);
        setCopy(JAM_SCORE, this, RUNNING_OR_ENDED_TEAM_JAM, TeamJam.JAM_SCORE, true);
        setCopy(LAST_SCORE, this, RUNNING_OR_ENDED_TEAM_JAM, TeamJam.LAST_SCORE, true);
        setCopy(TRIP_SCORE, this, CURRENT_TRIP, ScoringTrip.SCORE, false);
        setCopy(LOST, this, RUNNING_OR_ENDED_TEAM_JAM, TeamJam.LOST, false);
        setCopy(LEAD, this, RUNNING_OR_ENDED_TEAM_JAM, TeamJam.LEAD, false);
        setCopy(CALLOFF, this, RUNNING_OR_ENDED_TEAM_JAM, TeamJam.CALLOFF, false);
        setCopy(INJURY, this, RUNNING_OR_ENDED_TEAM_JAM, TeamJam.INJURY, false);
        setCopy(NO_INITIAL, this, RUNNING_OR_ENDED_TEAM_JAM, TeamJam.NO_INITIAL, false);
        setCopy(DISPLAY_LEAD, this, RUNNING_OR_ENDED_TEAM_JAM, TeamJam.DISPLAY_LEAD, false);
        setCopy(NO_PIVOT, this, RUNNING_OR_UPCOMING_TEAM_JAM, TeamJam.NO_PIVOT, false);
        setCopy(STAR_PASS, this, RUNNING_OR_ENDED_TEAM_JAM, TeamJam.STAR_PASS, false);
        setCopy(STAR_PASS_TRIP, this, RUNNING_OR_ENDED_TEAM_JAM, TeamJam.STAR_PASS_TRIP, false);
        scoreListener = setRecalculated(SCORE)
                            .addIndirectSource(this, RUNNING_OR_ENDED_TEAM_JAM, TeamJam.TOTAL_SCORE)
                            .addSource(this, SCORE_ADJUSTMENT);
        setRecalculated(IN_TIMEOUT)
            .addIndirectSource(g, Game.CURRENT_TIMEOUT, Timeout.OWNER)
            .addIndirectSource(g, Game.CURRENT_TIMEOUT, Timeout.REVIEW)
            .addIndirectSource(g, Game.CURRENT_TIMEOUT, Timeout.RUNNING);
        setRecalculated(IN_OFFICIAL_REVIEW)
            .addIndirectSource(g, Game.CURRENT_TIMEOUT, Timeout.OWNER)
            .addIndirectSource(g, Game.CURRENT_TIMEOUT, Timeout.REVIEW)
            .addIndirectSource(g, Game.CURRENT_TIMEOUT, Timeout.RUNNING);
        setRecalculated(FULL_NAME).addSource(this, LEAGUE_NAME).addSource(this, TEAM_NAME);
        setRecalculated(DISPLAY_NAME)
            .addSource(this, LEAGUE_NAME)
            .addSource(this, TEAM_NAME)
            .addSource(this, FULL_NAME)
            .addSource(scoreBoard.getSettings(), Settings.SETTING);
        setRecalculated(FILE_NAME)
            .addSource(this, LEAGUE_NAME)
            .addSource(this, TEAM_NAME)
            .addSource(this, FULL_NAME)
            .addSource(scoreBoard.getSettings(), Settings.SETTING);
        set(FULL_NAME, "");
        setRecalculated(Team.INITIALS).addSource(this, Team.DISPLAY_NAME);
        addWriteProtectionOverride(TIMEOUTS, Source.ANY_INTERNAL);
        addWriteProtectionOverride(OFFICIAL_REVIEWS, Source.ANY_INTERNAL);
        addWriteProtectionOverride(LAST_REVIEW, Source.ANY_INTERNAL);
        setCopy(RETAINED_OFFICIAL_REVIEW, this, LAST_REVIEW, Timeout.RETAINED_REVIEW, false);
        setCopy(ALTERNATE_NAME, this, PREPARED_TEAM, ALTERNATE_NAME, false, PREPARED_TEAM_CONNECTED);
        setCopy(COLOR, this, PREPARED_TEAM, COLOR, false, PREPARED_TEAM_CONNECTED);
        setCopy(ACTIVE_SCORE_ADJUSTMENT_AMOUNT, this, ACTIVE_SCORE_ADJUSTMENT, ScoreAdjustment.AMOUNT, false);
        providers.put(skaterListener, null);
    }

    @Override
    public String getProviderId() {
        return subId;
    }

    @Override
    protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == FULL_NAME) {
            String league = get(LEAGUE_NAME);
            String team = get(TEAM_NAME);
            String color = get(UNIFORM_COLOR);
            String in = value == null ? "" : (String) value;
            if (!"".equals(league)) {
                if (!"".equals(team)) {
                    if (league.equals(team)) {
                        return league;
                    } else {
                        return league + " - " + team;
                    }
                } else {
                    return league;
                }
            } else {
                if (!"".equals(team)) {
                    return team;
                } else if (!"".equals(in)) {
                    return in;
                } else if (!"".equals(color)) {
                    return color;
                } else {
                    return "Unnamed Team";
                }
            }
        }
        if (prop == DISPLAY_NAME) {
            String setting = scoreBoard.getSettings().get(SETTING_DISPLAY_NAME);
            if (OPTION_TEAM_NAME.equals(setting) && !"".equals(get(TEAM_NAME))) {
                return get(TEAM_NAME);
            } else if (!OPTION_FULL_NAME.equals(setting) && !"".equals(get(LEAGUE_NAME))) {
                return get(LEAGUE_NAME);
            } else {
                return get(FULL_NAME);
            }
        }
        if (prop == FILE_NAME) {
            String setting = scoreBoard.getSettings().get(SETTING_FILE_NAME);
            if (OPTION_TEAM_NAME.equals(setting) && !"".equals(get(TEAM_NAME))) {
                return get(TEAM_NAME);
            } else if (!OPTION_FULL_NAME.equals(setting) && !"".equals(get(LEAGUE_NAME))) {
                return get(LEAGUE_NAME);
            } else {
                return get(FULL_NAME);
            }
        }
        if (prop == INITIALS) { return get(DISPLAY_NAME).replaceAll("[^\\p{Lu}]", ""); }
        if (prop == IN_TIMEOUT) {
            Timeout t = game.getCurrentTimeout();
            return t.isRunning() && this == t.getOwner() && !t.isReview();
        }
        if (prop == IN_OFFICIAL_REVIEW) {
            Timeout t = game.getCurrentTimeout();
            return t.isRunning() && this == t.getOwner() && t.isReview();
        }
        if (prop == TRIP_SCORE && source != Source.COPY) {
            tripScoreTimerTask.cancel();
            if ((Integer) value > 0 && getCurrentTrip().getNumber() == 1 && !game.isInOvertime() &&
                !game.isInSuddenScoring()) {
                // If points arrive during an initial trip and we are not in overtime, assign
                // the points to the first scoring trip instead.
                getCurrentTrip().set(ScoringTrip.ANNOTATION, "Points were added without Add Trip\n" +
                                                                 getCurrentTrip().get(ScoringTrip.ANNOTATION));
                execute(ADD_TRIP);
            }
            if (game.isInJam() && ((Integer) value > 0 || ((Integer) last == 0 && flag != Flag.CHANGE))) {
                // we are during a jam and either points have been entered or the trip score has
                // been explicitly set to 0 - set a timer to advance the trip
                tripScoreTimer.purge();
                tripScoreJamTime = getCurrentTrip().get(ScoringTrip.JAM_CLOCK_END);
                if (tripScoreJamTime == 0L) { tripScoreJamTime = game.getClock(Clock.ID_JAM).getTimeElapsed(); }
                tripScoreTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        execute(ADD_TRIP);
                        getCurrentTrip().getPrevious().set(ScoringTrip.JAM_CLOCK_END, tripScoreJamTime);
                    }
                };
                tripScoreTimer.schedule(tripScoreTimerTask, 4000);
            }
        }
        if (prop == SCORE) {
            int sum = getRunningOrEndedTeamJam().getTotalScore();
            for (ScoreAdjustment adjustment : getAll(SCORE_ADJUSTMENT)) { sum += adjustment.getAmount(); }
            return sum;
        }
        if (prop == NO_INITIAL && source != Source.COPY) {
            if (!(Boolean) value && (Boolean) last) {
                execute(ADD_TRIP, source);
            } else if ((Boolean) value && !(Boolean) last && getCurrentTrip().getNumber() == 2 && get(JAM_SCORE) == 0) {
                execute(REMOVE_TRIP, Source.OTHER);
            }
        }
        if (prop == INJURY && source != Source.COPY && (Boolean) value) { set(CALLOFF, false); }
        if (prop == CALLOFF && source != Source.COPY && (Boolean) value) { set(INJURY, false); }
        if (prop == PREPARED_TEAM && flag == Flag.CHANGE) {
            PreparedTeam pt =
                (value == null ? scoreBoard.getOrCreate(ScoreBoard.PREPARED_TEAM, UUID.randomUUID().toString())
                               : (PreparedTeam) value);
            mergeInto(pt);
            return last;
        }
        if (prop == PREPARED_TEAM && value != last && source == Source.WS) {
            if (getGame().get(Game.STATE) != Game.State.PREPARED) {
                // no team change after game start
                return last;
            }
            set(PREPARED_TEAM_CONNECTED, value != null, source, Flag.SPECIAL_CASE);
        }
        if (prop == PREPARED_TEAM_CONNECTED && flag != Flag.SPECIAL_CASE && get(PREPARED_TEAM) == null) {
            return false;
        }
        if (prop == ACTIVE_SCORE_ADJUSTMENT && value == null && last != null &&
            (source == Source.WS || flag == Flag.SPECIAL_CASE)) {
            ((ScoreAdjustment) last).set(ScoreAdjustment.OPEN, false);
        }
        if (prop == ACTIVE_SCORE_ADJUSTMENT_AMOUNT && get(ACTIVE_SCORE_ADJUSTMENT) == null) {
            if (source == Source.WS) {
                ScoreAdjustment newAdjustment = new ScoreAdjustmentImpl(this, UUID.randomUUID().toString());
                add(SCORE_ADJUSTMENT, newAdjustment);
                set(ACTIVE_SCORE_ADJUSTMENT, newAdjustment);
            } else if (source == Source.COPY) {
                return value;
            } else {
                return last;
            }
        }
        return value;
    }

    @Override
    protected void valueChanged(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == LEAD && (Boolean) value && game.isInJam()) {
            if (getCurrentTrip().getNumber() == 1) { getRunningOrEndedTeamJam().addScoringTrip(); }
            if (getOtherTeam().isLead()) { getOtherTeam().set(LEAD, false); }
        } else if (prop == STAR_PASS) {
            if (getPosition(FloorPosition.JAMMER).getSkater() != null) {
                getPosition(FloorPosition.JAMMER)
                    .getSkater()
                    .setRole(FloorPosition.JAMMER.getRole(getRunningOrUpcomingTeamJam()));
            }
            if (getPosition(FloorPosition.PIVOT).getSkater() != null) {
                getPosition(FloorPosition.PIVOT)
                    .getSkater()
                    .setRole(FloorPosition.PIVOT.getRole(getRunningOrUpcomingTeamJam()));
            }
            if ((Boolean) value && isLead()) { set(LOST, true); }
        } else if ((prop == CALLOFF || prop == INJURY) && game.isInJam() && (Boolean) value) {
            game.stopJamTO();
        } else if (prop == CAPTAIN && last != null) {
            ((Skater) last).set(Skater.FLAGS, "");
        }
    }

    @Override
    public void execute(Command prop, Source source) {
        if (prop == ADD_TRIP) {
            tripScoreTimerTask.cancel();
            getRunningOrEndedTeamJam().addScoringTrip();
            if (!isLead() && !getOtherTeam().isLead()) { set(LOST, true); }
        } else if (prop == REMOVE_TRIP) {
            if (!tripScoreTimerTask.cancel()) { getRunningOrEndedTeamJam().removeScoringTrip(); }
        } else if (prop == ADVANCE_FIELDINGS) {
            advanceFieldings();
        } else if (prop == OFFICIAL_REVIEW) {
            officialReview();
        } else if (prop == TIMEOUT) {
            timeout();
        }
    }

    @Override
    public ScoreBoardEventProvider create(Child<? extends ScoreBoardEventProvider> prop, String id, Source source) {
        synchronized (coreLock) {
            if (prop == SKATER) {
                if (get(PREPARED_TEAM_CONNECTED)) {
                    nextSkaterId = id;
                    get(PREPARED_TEAM).getOrCreate(PreparedTeam.SKATER, UUID.randomUUID().toString());
                    return get(SKATER, id);
                } else {
                    return new SkaterImpl(this, id);
                }
            } else if (prop == BOX_TRIP) {
                return new BoxTripImpl(this, id);
            } else if (prop == SCORE_ADJUSTMENT) {
                return new ScoreAdjustmentImpl(this, id);
            }
            return null;
        }
    }

    @Override
    protected void itemAdded(Child<?> prop, ValueWithId item, Source source) {
        if (prop == TIME_OUT) { recountTimeouts(); }
        if (prop == SCORE_ADJUSTMENT) { scoreListener.addSource(((ScoreAdjustment) item), ScoreAdjustment.AMOUNT); }
    }

    @Override
    protected <T extends ValueWithId> boolean mayRemove(Child<T> prop, T item, Source source) {
        if (prop == SKATER && item != null && !((Skater) item).getAll(Skater.FIELDING).isEmpty()) {
            // skater has been fielded - avoid data corruption
            return false;
        }
        return true;
    }

    @Override
    protected void itemRemoved(Child<?> prop, ValueWithId item, Source source) {
        if (prop == SKATER) {
            Skater s = ((Skater) item);
            if (get(PREPARED_TEAM_CONNECTED)) {
                get(PREPARED_TEAM).remove(PreparedTeam.SKATER, s.get(Skater.PREPARED_SKATER));
            }
            s.delete();
        }
        if (prop == TIME_OUT) { recountTimeouts(); }
        if (prop == SCORE_ADJUSTMENT && item == get(ACTIVE_SCORE_ADJUSTMENT)) { set(ACTIVE_SCORE_ADJUSTMENT, null); }
    }

    @Override
    public Game getGame() {
        return game;
    }

    @Override
    public String getName() {
        return get(FULL_NAME);
    }

    @Override
    public void setName(String n) {
        set(FULL_NAME, n);
    }

    @Override
    public void startJam() {
        synchronized (coreLock) {
            advanceFieldings(); // if this hasn't been manually triggered between jams, do it now
            getCurrentTrip().set(ScoringTrip.CURRENT, true);
        }
    }

    @Override
    public void stopJam() {
        synchronized (coreLock) {
            if (get(ACTIVE_SCORE_ADJUSTMENT) != null) { set(ACTIVE_SCORE_ADJUSTMENT, null, Flag.SPECIAL_CASE); }
            if (isDisplayLead() && !game.getClock(Clock.ID_JAM).isTimeAtEnd() && !isInjury() &&
                !getOtherTeam().isInjury()) {
                set(CALLOFF, true);
            }
            getCurrentTrip().set(ScoringTrip.CURRENT, false);

            set(FIELDING_ADVANCE_PENDING, true);

            updateTeamJams();

            Map<Skater, Role> toField = new HashMap<>();
            TeamJam upcomingTJ = getRunningOrUpcomingTeamJam();
            TeamJam endedTJ = getRunningOrEndedTeamJam();
            for (FloorPosition fp : FloorPosition.values()) {
                Skater s = endedTJ.getFielding(fp).getSkater();
                if (s != null && (endedTJ.getFielding(fp).isInBox() || s.hasUnservedPenalties())) {
                    if (fp.getRole(endedTJ) != fp.getRole(upcomingTJ)) {
                        toField.put(s, fp.getRole(endedTJ));
                    } else {
                        upcomingTJ.getFielding(fp).setSkater(s);
                        BoxTrip bt = endedTJ.getFielding(fp).getCurrentBoxTrip();
                        if (bt != null && bt.isCurrent()) { bt.add(BoxTrip.FIELDING, upcomingTJ.getFielding(fp)); }
                    }
                }
            }
            nextReplacedBlocker = FloorPosition.PIVOT;
            for (Skater s : toField.keySet()) {
                field(s, toField.get(s), upcomingTJ);
                BoxTrip bt = s.getFielding(endedTJ).getCurrentBoxTrip();
                if (bt != null && bt.isCurrent()) { bt.add(BoxTrip.FIELDING, s.getFielding(upcomingTJ)); }
            }

            for (Skater s : getAll(SKATER)) { s.updateEligibility(); }
        }
    }

    private void advanceFieldings() {
        set(FIELDING_ADVANCE_PENDING, false);
        updateTeamJams();
    }

    @Override
    public TeamSnapshot snapshot() {
        synchronized (coreLock) { return new TeamSnapshotImpl(this); }
    }

    @Override
    public void restoreSnapshot(TeamSnapshot s) {
        synchronized (coreLock) {
            if (s.getId() != getId()) { return; }
            set(FIELDING_ADVANCE_PENDING, s.getFieldingAdvancePending());
            updateTeamJams();
            if (game.isInJam()) {
                set(CALLOFF, false);
                set(INJURY, false);
            }
        }
    }

    @Override
    public String getAlternateName(String i) {
        return get(ALTERNATE_NAME, i).getValue();
    }

    @Override
    public String getAlternateName(AlternateNameId id) {
        return getAlternateName(id.toString());
    }

    @Override
    public void setAlternateName(String i, String n) {
        synchronized (coreLock) { add(ALTERNATE_NAME, new ValWithId(i, n)); }
    }

    @Override
    public void removeAlternateName(String i) {
        remove(ALTERNATE_NAME, i);
    }

    @Override
    public String getColor(String i) {
        return get(COLOR, i).getValue();
    }

    @Override
    public void setColor(String i, String c) {
        synchronized (coreLock) { add(COLOR, new ValWithId(i, c)); }
    }

    @Override
    public void removeColor(String i) {
        remove(COLOR, i);
    }

    @Override
    public String getLogo() {
        return get(LOGO);
    }

    @Override
    public void setLogo(String l) {
        set(LOGO, l);
    }

    @Override
    public void loadPreparedTeam(PreparedTeam pt) {
        synchronized (coreLock) {
            set(PREPARED_TEAM_CONNECTED, pt != null, Flag.SPECIAL_CASE);
            set(PREPARED_TEAM, pt);
        }
    }

    protected void mergeInto(PreparedTeam pt) {
        set(PREPARED_TEAM_CONNECTED, false, Flag.SPECIAL_CASE);

        if ("".equals(pt.get(TEAM_NAME))) { pt.set(TEAM_NAME, get(TEAM_NAME)); }
        if ("".equals(pt.get(LEAGUE_NAME))) { pt.set(LEAGUE_NAME, get(LEAGUE_NAME)); }
        if ("".equals(pt.get(LOGO))) { pt.set(LOGO, get(LOGO)); }
        if ("".equals(get(TEAM_NAME))) { set(TEAM_NAME, pt.get(TEAM_NAME)); }
        if ("".equals(get(LEAGUE_NAME))) { set(LEAGUE_NAME, pt.get(LEAGUE_NAME)); }
        if ("".equals(get(LOGO))) { set(LOGO, pt.get(LOGO)); }
        boolean hasColor = "".equals(get(UNIFORM_COLOR));
        for (ValWithId uc : pt.getAll(PreparedTeam.UNIFORM_COLOR)) {
            if (get(UNIFORM_COLOR).equals(uc.getValue())) {
                hasColor = true;
                break;
            }
        }
        if (!hasColor) {
            pt.add(PreparedTeam.UNIFORM_COLOR, new ValWithId(UUID.randomUUID().toString(), get(UNIFORM_COLOR)));
        }
        for (ValWithId an : getAll(ALTERNATE_NAME)) {
            if (pt.get(ALTERNATE_NAME, an.getId()) == null) {
                pt.add(ALTERNATE_NAME, new ValWithId(an.getId(), an.getValue()));
            }
        }
        for (ValWithId c : getAll(COLOR)) {
            if (pt.get(COLOR, c.getId()) == null) { pt.add(COLOR, new ValWithId(c.getId(), c.getValue())); }
        }
        mergeSkaters(pt);

        set(PREPARED_TEAM, pt);
        set(PREPARED_TEAM_CONNECTED, true, Flag.SPECIAL_CASE);
    }

    private void mergeSkaters(PreparedTeam pt) {
        Collection<PreparedSkater> preparedSkaters = pt.getAll(PreparedTeam.SKATER);
        Collection<Skater> localSkaters = getAll(SKATER);
        Map<String, Map<String, PreparedSkater>> preparedByNumberAndName = new HashMap<>();
        for (PreparedSkater ps : preparedSkaters) {
            String number = ps.get(Skater.ROSTER_NUMBER);
            String name = ps.get(Skater.NAME);
            if (preparedByNumberAndName.get(number) == null) { preparedByNumberAndName.put(number, new HashMap<>()); }
            preparedByNumberAndName.get(number).put(name, ps);
        }
        // map full matches
        for (Skater s : localSkaters) {
            String number = s.get(Skater.ROSTER_NUMBER);
            String name = s.get(Skater.NAME);
            try {
                s.mergeInto(preparedByNumberAndName.get(number).get(name));
                preparedByNumberAndName.get(number).remove(name);
                if (preparedByNumberAndName.get(number).isEmpty()) { preparedByNumberAndName.remove(number); }
            } catch (NullPointerException e) {}
        }
        // map matching names with locally unset number
        for (Skater s : localSkaters) {
            if (s.get(Skater.PREPARED_SKATER) != null) { continue; }
            String name = s.get(Skater.NAME);
            try {
                s.mergeInto(preparedByNumberAndName.get("").get(name));
                preparedByNumberAndName.get("").remove(name);
                if (preparedByNumberAndName.get("").isEmpty()) { preparedByNumberAndName.remove(""); }
            } catch (NullPointerException e) {}
        }
        // map matching names with remotely unset number
        for (Skater s : localSkaters) {
            if (s.get(Skater.PREPARED_SKATER) != null) { continue; }
            String name = s.get(Skater.NAME);
            if ("".equals(name)) { continue; }
            for (String number : preparedByNumberAndName.keySet()) {
                try {
                    s.mergeInto(preparedByNumberAndName.get(number).get(name));
                    preparedByNumberAndName.get(number).remove(name);
                    if (preparedByNumberAndName.get(number).isEmpty()) { preparedByNumberAndName.remove(number); }
                    continue;
                } catch (NullPointerException e) {}
            }
        }
        // map matching numbers with locally unset name
        for (Skater s : localSkaters) {
            if (s.get(Skater.PREPARED_SKATER) != null) { continue; }
            String number = s.get(Skater.ROSTER_NUMBER);
            try {
                s.mergeInto(preparedByNumberAndName.get(number).get(""));
                preparedByNumberAndName.get(number).remove("");
                if (preparedByNumberAndName.get(number).isEmpty()) { preparedByNumberAndName.remove(number); }
                continue;
            } catch (NullPointerException e) {}
        }
        // map matching numbers with remotely unset name
        for (Skater s : localSkaters) {
            if (s.get(Skater.PREPARED_SKATER) != null) { continue; }
            String number = s.get(Skater.ROSTER_NUMBER);
            if ("".equals(number) || !preparedByNumberAndName.containsKey(number)) { continue; }
            for (String name : preparedByNumberAndName.get(number).keySet()) {
                try {
                    s.mergeInto(preparedByNumberAndName.get(number).get(name));
                    preparedByNumberAndName.get(number).remove(name);
                    if (preparedByNumberAndName.get(number).isEmpty()) { preparedByNumberAndName.remove(number); }
                    continue;
                } catch (NullPointerException e) {}
            }
        }
        // create new prepared skater for unmatched local ones
        for (Skater s : localSkaters) {
            if (s.get(Skater.PREPARED_SKATER) != null) { continue; }
            s.mergeInto(pt.getOrCreate(PreparedTeam.SKATER, UUID.randomUUID().toString()));
        }
    }

    @Override
    public void timeout() {
        synchronized (coreLock) {
            if (getTimeouts() > 0) { game.setTimeoutType(this, false); }
        }
    }

    @Override
    public void officialReview() {
        synchronized (coreLock) {
            if (getOfficialReviews() > 0) { game.setTimeoutType(this, true); }
        }
    }

    @Override
    public TeamJam getRunningOrUpcomingTeamJam() {
        return get(RUNNING_OR_UPCOMING_TEAM_JAM);
    }

    @Override
    public TeamJam getRunningOrEndedTeamJam() {
        return get(RUNNING_OR_ENDED_TEAM_JAM);
    }

    @Override
    public void updateTeamJams() {
        synchronized (coreLock) {
            set(RUNNING_OR_ENDED_TEAM_JAM, game.getCurrentPeriod().getCurrentJam().getTeamJam(subId));
            set(RUNNING_OR_UPCOMING_TEAM_JAM,
                game.isInJam() ? getRunningOrEndedTeamJam() : getRunningOrEndedTeamJam().getNext());
            for (Position p : getAll(POSITION)) { p.updateCurrentFielding(); }
            for (Skater v : getAll(SKATER)) {
                v.updateFielding(hasFieldingAdvancePending() ? getRunningOrEndedTeamJam()
                                                             : getRunningOrUpcomingTeamJam());
            }
        }
    }

    @Override
    public int getScore() {
        return get(SCORE);
    }

    @Override
    public void applyScoreAdjustment(ScoreAdjustment adjustment) {
        int remainingAmount = adjustment.getTripAppliedTo().tryApplyScoreAdjustment(adjustment);
        if (remainingAmount != 0) {
            adjustment.getJamRecorded().getTeamJam(subId).possiblyChangeOsOffset(remainingAmount);
        }
        adjustment.delete();
    }

    @Override
    public ScoringTrip getCurrentTrip() {
        return get(CURRENT_TRIP);
    }

    public boolean cancelTripAdvancement() { return tripScoreTimerTask.cancel(); }

    @Override
    public boolean inTimeout() {
        return get(IN_TIMEOUT);
    }

    @Override
    public boolean inOfficialReview() {
        return get(IN_OFFICIAL_REVIEW);
    }

    @Override
    public boolean retainedOfficialReview() {
        return get(RETAINED_OFFICIAL_REVIEW);
    }

    @Override
    public void setRetainedOfficialReview(boolean b) {
        set(RETAINED_OFFICIAL_REVIEW, b);
    }

    @Override
    public int getTimeouts() {
        return get(TIMEOUTS);
    }

    @Override
    public int getOfficialReviews() {
        return get(OFFICIAL_REVIEWS);
    }

    @Override
    public void recountTimeouts() {
        boolean toPerPeriod = game.getBoolean(Rule.TIMEOUTS_PER_PERIOD);
        boolean revPerPeriod = game.getBoolean(Rule.REVIEWS_PER_PERIOD);
        int toCount = game.getInt(Rule.NUMBER_TIMEOUTS);
        int revCount = game.getInt(Rule.NUMBER_REVIEWS);
        int retainsLeft = game.getInt(Rule.NUMBER_RETAINS);
        boolean rdclPerHalfRules = game.getBoolean(Rule.RDCL_PER_HALF_RULES);
        boolean otherHalfToUnused = rdclPerHalfRules;
        Timeout lastReview = null;

        for (Timeout t : getAll(TIME_OUT)) {
            boolean isThisRdclHalf = false;
            if (rdclPerHalfRules) {
                boolean gameIsSecondHalf = game.getCurrentPeriodNumber() > 2;
                boolean tIsSecondHalf = ((Period) t.getParent()).getNumber() > 2;
                isThisRdclHalf = (gameIsSecondHalf == tIsSecondHalf);
            }
            if (t.isReview()) {
                if (!revPerPeriod || t.getParent() == game.getCurrentPeriod() || isThisRdclHalf) {
                    if (retainsLeft > 0 && t.isRetained()) {
                        retainsLeft--;
                    } else if (revCount > 0) {
                        revCount--;
                    }
                    if (lastReview == null || t.compareTo(lastReview) > 0) { lastReview = t; }
                }
            } else {
                if (toCount > 0 && (!toPerPeriod || t.getParent() == game.getCurrentPeriod())) {
                    toCount--;
                    otherHalfToUnused = otherHalfToUnused && !isThisRdclHalf;
                }
            }
        }
        if (otherHalfToUnused) { toCount--; }
        set(TIMEOUTS, toCount);
        set(OFFICIAL_REVIEWS, revCount);
        set(LAST_REVIEW, lastReview);
    }

    @Override
    public Skater getSkater(String id) {
        return get(SKATER, id);
    }

    public Skater addSkater(String id) { return getOrCreate(SKATER, id); }

    @Override
    public void addSkater(Skater skater) {
        add(SKATER, skater);
    }

    @Override
    public void removeSkater(String id) {
        remove(SKATER, id);
    }

    @Override
    public Position getPosition(FloorPosition fp) {
        return fp == null ? null : get(POSITION, fp.toString());
    }

    @Override
    public void field(Skater s, Role r) {
        field(s, r, hasFieldingAdvancePending() ? getRunningOrEndedTeamJam() : getRunningOrUpcomingTeamJam());
    }

    @Override
    public void field(Skater s, Role r, TeamJam tj) {
        synchronized (coreLock) {
            if (s == null) { return; }
            if (s.getFielding(tj) != null && s.getFielding(tj).getPosition() == getPosition(FloorPosition.PIVOT)) {
                tj.setNoPivot(r != Role.PIVOT);
                if ((r == Role.BLOCKER || r == Role.PIVOT) &&
                    ((tj.isRunningOrEnded() && hasFieldingAdvancePending()) ||
                     (tj.isRunningOrUpcoming() && !hasFieldingAdvancePending()))) {
                    s.setRole(r);
                }
            }
            if (s.getFielding(tj) == null || s.getRole(tj) != r) {
                Fielding priorFielding = tj.hasPrevious() ? s.getFielding(tj.getPrevious()) : null;
                FloorPosition priorFp = priorFielding == null ? null : priorFielding.getPosition().getFloorPosition();
                Fielding f = getAvailableFielding(r, tj, priorFp);
                if (r == Role.PIVOT && f != null) {
                    if (f.getSkater() != null && (tj.hasNoPivot() || s.getRole(tj) == Role.BLOCKER)) {
                        // If we are moving a blocker to pivot, move the previous pivot to blocker
                        // If we are replacing a blocker from the pivot spot,
                        // see if we have a blocker spot available for them instead
                        Fielding f2;
                        if (s.getRole(tj) == Role.BLOCKER) {
                            f2 = s.getFielding(tj);
                        } else {
                            f2 = getAvailableFielding(Role.BLOCKER, tj, priorFp);
                        }
                        f2.setSkater(f.getSkater());
                    }
                    f.setSkater(s);
                    tj.setNoPivot(false);
                } else if (f != null) {
                    f.setSkater(s);
                } else {
                    s.remove(Skater.FIELDING, s.getFielding(tj));
                }
            }
        }
    }

    private Fielding getAvailableFielding(Role r, TeamJam tj, FloorPosition preferredFp) {
        switch (r) {
        case JAMMER:
            if (tj.isStarPass()) {
                return tj.getFielding(FloorPosition.PIVOT);
            } else {
                return tj.getFielding(FloorPosition.JAMMER);
            }
        case PIVOT:
            if (tj.isStarPass()) {
                return null;
            } else {
                return tj.getFielding(FloorPosition.PIVOT);
            }
        case BLOCKER:
            if (preferredFp != null && preferredFp.getRole() == r && tj.getFielding(preferredFp).getSkater() == null) {
                return tj.getFielding(preferredFp);
            }
            Fielding[] fs = {tj.getFielding(FloorPosition.BLOCKER1), tj.getFielding(FloorPosition.BLOCKER2),
                             tj.getFielding(FloorPosition.BLOCKER3)};
            for (Fielding f : fs) {
                if (f.getSkater() == null) { return f; }
            }
            Fielding fourth = tj.getFielding(tj.isStarPass() ? FloorPosition.JAMMER : FloorPosition.PIVOT);
            if (fourth.getSkater() == null) { return fourth; }
            int tries = 0;
            do {
                if (++tries > 4) { return null; }
                switch (nextReplacedBlocker) {
                case BLOCKER1: nextReplacedBlocker = FloorPosition.BLOCKER2; break;
                case BLOCKER2: nextReplacedBlocker = FloorPosition.BLOCKER3; break;
                case BLOCKER3:
                    nextReplacedBlocker =
                        (tj.hasNoPivot() && !tj.isStarPass()) ? FloorPosition.PIVOT : FloorPosition.BLOCKER1;
                    break;
                case PIVOT: nextReplacedBlocker = FloorPosition.BLOCKER1; break;
                default: break;
                }
            } while (tj.getFielding(nextReplacedBlocker).isInBox());
            return tj.getFielding(nextReplacedBlocker);
        default: return null;
        }
    }

    @Override
    public boolean hasFieldingAdvancePending() {
        return get(FIELDING_ADVANCE_PENDING);
    }

    @Override
    public boolean isLost() {
        return get(LOST);
    }

    @Override
    public boolean isLead() {
        return get(LEAD);
    }

    @Override
    public boolean isCalloff() {
        return get(CALLOFF);
    }

    @Override
    public boolean isInjury() {
        return get(INJURY);
    }

    @Override
    public boolean isDisplayLead() {
        return get(DISPLAY_LEAD);
    }

    protected boolean isFieldingStarPass() {
        if (hasFieldingAdvancePending()) {
            return getRunningOrEndedTeamJam().isStarPass();
        } else {
            return getRunningOrUpcomingTeamJam().isStarPass();
        }
    }

    @Override
    public boolean isStarPass() {
        return get(STAR_PASS);
    }

    public void setStarPass(boolean sp) { set(STAR_PASS, sp); }

    @Override
    public boolean hasNoPivot() {
        return get(NO_PIVOT);
    }

    @Override
    public Team getOtherTeam() {
        String otherId = subId.equals(Team.ID_1) ? Team.ID_2 : Team.ID_1;
        return game.getTeam(otherId);
    }

    protected ScoreBoardListener skaterListener =
        new IndirectScoreBoardListener<>(this, PREPARED_TEAM, PreparedTeam.SKATER, new ScoreBoardListener() {
            @Override
            public void scoreBoardChange(ScoreBoardEvent<?> event) {
                if (get(PREPARED_TEAM_CONNECTED)) {
                    PreparedSkater ps = (PreparedSkater) event.getValue();
                    for (Skater s : getAll(SKATER)) {
                        if (ps == s.get(Skater.PREPARED_SKATER)) {
                            if (event.isRemove() && getGame().get(Game.STATE) == Game.State.PREPARED) {
                                // removing Skaters after game start might break stats
                                remove(SKATER, s);
                            }
                            return;
                        }
                    }
                    if (!event.isRemove()) {
                        add(SKATER, new SkaterImpl(TeamImpl.this, ps, nextSkaterId));
                        nextSkaterId = null;
                    }
                }
            }
        });

    FloorPosition nextReplacedBlocker = FloorPosition.PIVOT;

    private Timer tripScoreTimer = new Timer();
    private TimerTask tripScoreTimerTask = new TimerTask() {
        @Override
        public void run() {} // dummy, so the variable is not
                             // null at the first score entry
    };
    private long tripScoreJamTime; // store the jam clock when starting the timer so we can set the correct value
                                   // when advancing the trip
    private Game game;
    private String subId;
    private String nextSkaterId;

    private RecalculateScoreBoardListener<?> scoreListener;

    public static final String DEFAULT_NAME_PREFIX = "Team ";
    public static final String DEFAULT_LOGO = "";

    public static class TeamSnapshotImpl implements TeamSnapshot {
        private TeamSnapshotImpl(Team team) {
            id = team.getId();
            fieldingAdvancePending = team.hasFieldingAdvancePending();
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public boolean getFieldingAdvancePending() {
            return fieldingAdvancePending;
        }

        protected String id;
        protected boolean fieldingAdvancePending;
    }
}
