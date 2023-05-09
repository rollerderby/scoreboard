package com.carolinarollergirls.scoreboard.core.game;

import com.carolinarollergirls.scoreboard.core.interfaces.Fielding;
import com.carolinarollergirls.scoreboard.core.interfaces.FloorPosition;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Jam;
import com.carolinarollergirls.scoreboard.core.interfaces.Position;
import com.carolinarollergirls.scoreboard.core.interfaces.Role;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoringTrip;
import com.carolinarollergirls.scoreboard.core.interfaces.Skater;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.core.interfaces.TeamJam;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.ParentOrderedScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.RecalculateScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.event.ValueWithId;
import com.carolinarollergirls.scoreboard.rules.Rule;

public class TeamJamImpl extends ParentOrderedScoreBoardEventProviderImpl<TeamJam> implements TeamJam {
    public TeamJamImpl(Jam j, String teamId) {
        super(j, teamId, Jam.TEAM_JAM);
        addProperties(props);
        game = j.getPeriod().getGame();
        team = game.getTeam(teamId);
        setRecalculated(CURRENT_TRIP).addSource(this, SCORING_TRIP);
        setCopy(CURRENT_TRIP_NUMBER, this, CURRENT_TRIP, ScoringTrip.NUMBER, true);
        setRecalculated(TOTAL_SCORE).addSource(this, LAST_SCORE).addSource(this, JAM_SCORE).addSource(this, OS_OFFSET);
        setCopy(LAST_SCORE, this, PREVIOUS, TOTAL_SCORE, true);
        jamScoreListener = setRecalculated(JAM_SCORE).addSource(this, SCORING_TRIP);
        afterSPScoreListener = setRecalculated(AFTER_S_P_SCORE).addSource(this, SCORING_TRIP);
        setRecalculated(NO_INITIAL).addSource(this, CURRENT_TRIP);
        setRecalculated(DISPLAY_LEAD).addSource(this, LEAD).addSource(this, LOST);
        setRecalculated(STAR_PASS).addSource(this, STAR_PASS_TRIP);
        for (Position p : team.getAll(Team.POSITION)) { add(FIELDING, new FieldingImpl(this, p)); }
        addWriteProtection(FIELDING);
        getOrCreate(SCORING_TRIP, 1);
    }

    @Override
    protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == JAM_SCORE) {
            int sum = 0;
            for (ScoringTrip trip : getAll(SCORING_TRIP)) { sum += trip.getScore(); }
            return sum;
        }
        if (prop == AFTER_S_P_SCORE) {
            int sum = 0;
            for (ScoringTrip trip : getAll(SCORING_TRIP)) {
                if (trip.get(ScoringTrip.AFTER_S_P)) { sum += trip.getScore(); }
            }
            return sum;
        }
        if (prop == TOTAL_SCORE) { return getLastScore() + getJamScore() + getOsOffset(); }
        if (prop == CURRENT_TRIP) { return getLast(SCORING_TRIP); }
        if (prop == NO_INITIAL) {
            if (getCurrentScoringTrip() == null) { return true; }
            return getCurrentScoringTrip().getNumber() == 1;
        }
        if ((prop == LEAD || prop == LOST) && getJam().isImmediateScoring()) { return false; }
        if (prop == DISPLAY_LEAD) { return isLead() && !isLost(); }
        if (prop == STAR_PASS) {
            if (source == Source.RECALCULATE || source.isFile()) {
                return getStarPassTrip() != null;
            } else if (get(NO_PIVOT)) {
                set(STAR_PASS_TRIP, null);
                return last;
            } else {
                set(STAR_PASS_TRIP, (Boolean) value ? getCurrentScoringTrip() : null);
                return last;
            }
        }
        if (value instanceof Integer && prop != OS_OFFSET && (Integer) value < 0) { return 0; }
        return value;
    }
    @Override
    protected void valueChanged(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == STAR_PASS_TRIP) {
            if (last != null) { getLast(SCORING_TRIP).set(ScoringTrip.AFTER_S_P, false, Flag.SPECIAL_CASE); }
            if (value != null) { ((ScoringTrip) value).set(ScoringTrip.AFTER_S_P, true, Flag.SPECIAL_CASE); }
        }
        if (prop == INJURY) { getOtherTeam().set(INJURY, (Boolean) value); }
        if (prop == CURRENT_TRIP) {
            if (value != null && value == team.getCurrentTrip() && game.isInJam()) {
                ((ScoringTrip) value).set(ScoringTrip.CURRENT, true);
            }
            if (last != null) { ((ScoringTrip) last).set(ScoringTrip.CURRENT, false); }
        }
        if (prop == NO_PIVOT && getFielding(FloorPosition.PIVOT).getSkater() != null &&
            getFielding(FloorPosition.PIVOT).isCurrent()) {
            getFielding(FloorPosition.PIVOT).getSkater().setRole(FloorPosition.PIVOT.getRole(this));
        }
        if (prop == NO_PIVOT && (Boolean) value) { set(STAR_PASS_TRIP, null); }
    }

    @Override
    protected void itemAdded(Child<?> prop, ValueWithId item, Source source) {
        if (prop == SCORING_TRIP) {
            jamScoreListener.addSource((ScoreBoardEventProvider) item, ScoringTrip.SCORE);
            afterSPScoreListener.addSource((ScoreBoardEventProvider) item, ScoringTrip.SCORE);
            afterSPScoreListener.addSource((ScoreBoardEventProvider) item, ScoringTrip.AFTER_S_P);
        }
    }
    @Override
    protected void itemRemoved(Child<?> prop, ValueWithId item, Source source) {
        if (prop == SCORING_TRIP) {
            possiblyChangeOsOffset(((ScoringTrip) item).getScore());
            if (item == get(STAR_PASS_TRIP)) {
                for (ScoringTrip trip = getLast(SCORING_TRIP); trip != null; trip = trip.getPrevious()) {
                    if (!trip.get(ScoringTrip.AFTER_S_P)) { set(STAR_PASS_TRIP, trip.getNext()); }
                }
            }
        }
    }
    @Override
    public ScoreBoardEventProvider create(Child<? extends ScoreBoardEventProvider> prop, String id, Source source) {
        synchronized (coreLock) {
            if (prop == SCORING_TRIP) { return new ScoringTripImpl(this, Integer.parseInt(id)); }
            return null;
        }
    }

    @Override
    public void execute(Command prop, Source source) {
        if (prop == COPY_LINEUP_TO_CURRENT) { copyLineupToCurrentJam(); }
    }

    private void copyLineupToCurrentJam() {
        if (isRunningOrUpcoming()) { return; }

        TeamJam current = team.getRunningOrUpcomingTeamJam();

        for (FloorPosition fp : FloorPosition.values()) { copySkaterToIfAvailable(fp, current); }
    }

    private void copySkaterToIfAvailable(FloorPosition fp, TeamJam target) {
        Skater s = getFielding(fp).getSkater();
        if (s == null || s.getFielding(target) != null) { return; }

        boolean placeAvailable = target.getFielding(fp).getSkater() == null;
        if (fp.getRole() == Role.BLOCKER) {
            placeAvailable = target.getFielding(FloorPosition.BLOCKER1).getSkater() == null ||
                             target.getFielding(FloorPosition.BLOCKER2).getSkater() == null ||
                             target.getFielding(FloorPosition.BLOCKER3).getSkater() == null ||
                             target.getFielding(FloorPosition.PIVOT).getSkater() == null;
        }

        if (placeAvailable) { team.field(s, fp.getRole(), target); }
    }

    @Override
    public Jam getJam() {
        return (Jam) parent;
    }
    @Override
    public Team getTeam() {
        return team;
    }

    @Override
    public TeamJam getOtherTeam() {
        return getJam().getTeamJam(Team.ID_1.equals(subId) ? Team.ID_2 : Team.ID_1);
    }

    @Override
    public void setupInjuryContinuation() {
        if (getPrevious().isLead()) { set(LEAD, true); }
        removeScoringTrip();
        int priorTrips = getPrevious().getCurrentScoringTrip().getNumber();
        if (priorTrips > 1 || getPrevious().getJam().isImmediateScoring()) {
            getCurrentScoringTrip().set(NUMBER, priorTrips + 1, Source.RENUMBER);
        }
    }

    @Override
    public boolean isRunningOrEnded() {
        return this == team.getRunningOrEndedTeamJam();
    }
    @Override
    public boolean isRunningOrUpcoming() {
        return this == team.getRunningOrUpcomingTeamJam();
    }

    @Override
    public int getLastScore() {
        return get(LAST_SCORE);
    }
    @Override
    public void setLastScore(int l) {
        set(LAST_SCORE, l);
    }

    @Override
    public int getOsOffset() {
        return get(OS_OFFSET);
    }
    @Override
    public void setOsOffset(int o) {
        set(OS_OFFSET, o);
    }
    @Override
    public void changeOsOffset(int c) {
        set(OS_OFFSET, c, Flag.CHANGE);
    }
    @Override
    public void possiblyChangeOsOffset(int amount) {
        if (game.isOfficialScore()) {
            changeOsOffset(amount);
        } else {
            possiblyChangeOsOffset(amount, game.getCurrentPeriod().getCurrentJam(), game.isInJam(),
                                   game.isLastTwoMinutes());
        }
    }
    @Override
    public boolean possiblyChangeOsOffset(int amount, Jam jamRecorded, boolean recordedInJam,
                                          boolean recordedInLastTwoMins) {
        if (game.getBoolean(Rule.WFTDA_LATE_SCORE_RULE)) {
            boolean nextJamNotStarted = getJam() == jamRecorded;
            boolean nextJamNotEnded = nextJamNotStarted || (jamRecorded == getJam().getNext() && recordedInJam);
            boolean lastTwoMinutes = game.isLastTwoMinutes();
            boolean changeOk = (!lastTwoMinutes && nextJamNotEnded) || (lastTwoMinutes && nextJamNotStarted);
            if (!changeOk) {
                changeOsOffset(amount);
                return true;
            }
        }
        return false;
    }

    @Override
    public int getJamScore() {
        return get(JAM_SCORE);
    }
    @Override
    public int getTotalScore() {
        return get(TOTAL_SCORE);
    }

    @Override
    public ScoringTrip getCurrentScoringTrip() {
        return get(CURRENT_TRIP);
    }
    @Override
    public void addScoringTrip() {
        getOrCreate(SCORING_TRIP, getCurrentScoringTrip().getNumber() + 1);
    }
    @Override
    public void removeScoringTrip() {
        getCurrentScoringTrip().execute(ScoringTrip.REMOVE);
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

    @Override
    public boolean isStarPass() {
        return get(STAR_PASS);
    }
    @Override
    public ScoringTrip getStarPassTrip() {
        return get(STAR_PASS_TRIP);
    }

    @Override
    public boolean hasNoPivot() {
        return get(NO_PIVOT);
    }
    @Override
    public void setNoPivot(boolean np) {
        set(NO_PIVOT, np);
    }

    @Override
    public Fielding getFielding(FloorPosition fp) {
        return get(FIELDING, fp.toString());
    }

    private Team team;
    private Game game;
    private RecalculateScoreBoardListener<?> jamScoreListener;
    private RecalculateScoreBoardListener<?> afterSPScoreListener;
}
