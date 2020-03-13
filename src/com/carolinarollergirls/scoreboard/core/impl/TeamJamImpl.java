package com.carolinarollergirls.scoreboard.core.impl;

import com.carolinarollergirls.scoreboard.core.Fielding;
import com.carolinarollergirls.scoreboard.core.FloorPosition;
import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.ScoringTrip;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.event.ParentOrderedScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.RecalculateScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public class TeamJamImpl extends ParentOrderedScoreBoardEventProviderImpl<TeamJam> implements TeamJam {
    public TeamJamImpl(Jam j, String teamId) {
        super(j, teamId, Jam.Child.TEAM_JAM, TeamJam.class, Value.class, Child.class, NChild.class);
        team = scoreBoard.getTeam(teamId);
        setRecalculated(Value.CURRENT_TRIP).addSource(this, NChild.SCORING_TRIP);
        setRecalculated(Value.TOTAL_SCORE).addSource(this, Value.LAST_SCORE).addSource(this, Value.JAM_SCORE)
                .addSource(this, Value.OS_OFFSET);
        setCopy(Value.LAST_SCORE, this, IValue.PREVIOUS, Value.TOTAL_SCORE, true);
        jamScoreListener = setRecalculated(Value.JAM_SCORE).addSource(this, NChild.SCORING_TRIP);
        afterSPScoreListener = setRecalculated(Value.AFTER_S_P_SCORE).addSource(this, NChild.SCORING_TRIP);
        setRecalculated(Value.NO_INITIAL).addSource(this, Value.CURRENT_TRIP);
        setRecalculated(Value.DISPLAY_LEAD).addSource(this, Value.LEAD).addSource(this, Value.LOST);
        setRecalculated(Value.STAR_PASS).addSource(this, Value.STAR_PASS_TRIP);
        for (ValueWithId p : team.getAll(Team.Child.POSITION)) {
            add(Child.FIELDING, new FieldingImpl(this, (Position) p));
        }
        addWriteProtection(Child.FIELDING);
        getOrCreate(NChild.SCORING_TRIP, 1);
    }

    @Override
    protected Object computeValue(PermanentProperty prop, Object value, Object last, Source source, Flag flag) {
        if (prop == Value.JAM_SCORE) {
            int sum = 0;
            for (ValueWithId trip : getAll(NChild.SCORING_TRIP)) {
                sum += ((ScoringTrip) trip).getScore();
            }
            return sum;
        }
        if (prop == Value.AFTER_S_P_SCORE) {
            int sum = 0;
            for (ValueWithId trip : getAll(NChild.SCORING_TRIP)) {
                if ((Boolean) ((ScoringTrip) trip).get(ScoringTrip.Value.AFTER_S_P)) {
                    sum += ((ScoringTrip) trip).getScore();
                }
            }
            return sum;
        }
        if (prop == Value.TOTAL_SCORE) {
            return getLastScore() + getJamScore() + getOsOffset();
        }
        if (prop == Value.CURRENT_TRIP) {
            return getLast(NChild.SCORING_TRIP);
        }
        if (prop == Value.NO_INITIAL) {
            if (getCurrentScoringTrip() == null) { return true; }
            return getCurrentScoringTrip().getNumber() == 1;
        }
        if (prop == Value.LOST && getJam().isOvertimeJam()) {
            return false;
        }
        if (prop == Value.DISPLAY_LEAD) {
            return isLead() && !isLost();
        }
        if (prop == Value.STAR_PASS) {
            if (source == Source.RECALCULATE || source.isFile()) {
                return getStarPassTrip() != null;
            } else if ((Boolean) get(Value.NO_PIVOT)) {
                set(Value.STAR_PASS_TRIP, null);
                return last;
            } else {
                set(Value.STAR_PASS_TRIP, (Boolean) value ? getCurrentScoringTrip() : null);
                return last;
            }
        }
        if (value instanceof Integer && prop != Value.OS_OFFSET && (Integer) value < 0) { return 0; }
        return value;
    }
    @Override
    protected void valueChanged(PermanentProperty prop, Object value, Object last, Source source, Flag flag) {
        if (prop == Value.STAR_PASS_TRIP) {
            if (last != null) {
                ((ScoringTrip) last).set(ScoringTrip.Value.AFTER_S_P, false, Flag.SPECIAL_CASE);
            }
            if (value != null) {
                ((ScoringTrip) value).set(ScoringTrip.Value.AFTER_S_P, true, Flag.SPECIAL_CASE);
            }
        }
        if (prop == Value.INJURY) {
            getOtherTeam().set(prop, value);
        }
        if (prop == Value.CURRENT_TRIP) {
            if (value != null && value == team.getCurrentTrip() && scoreBoard.isInJam()) {
                ((ScoringTrip) value).set(ScoringTrip.Value.CURRENT, true);
            }
            if (last != null) {
                ((ScoringTrip) last).set(ScoringTrip.Value.CURRENT, false);
            }
        }
        if (prop == Value.NO_PIVOT && getFielding(FloorPosition.PIVOT).getSkater() != null
                && getFielding(FloorPosition.PIVOT).isCurrent()) {
            getFielding(FloorPosition.PIVOT).getSkater().setRole(FloorPosition.PIVOT.getRole(this));
        }
        if (prop == Value.NO_PIVOT && (Boolean) value) {
            set(Value.STAR_PASS_TRIP, null);
        }
    }

    @Override
    protected void itemAdded(AddRemoveProperty prop, ValueWithId item, Source source) {
        if (prop == NChild.SCORING_TRIP) {
            jamScoreListener.addSource((ScoreBoardEventProvider) item, ScoringTrip.Value.SCORE);
            afterSPScoreListener.addSource((ScoreBoardEventProvider) item, ScoringTrip.Value.SCORE);
            afterSPScoreListener.addSource((ScoreBoardEventProvider) item, ScoringTrip.Value.AFTER_S_P);
        }
    }
    @Override
    protected void itemRemoved(AddRemoveProperty prop, ValueWithId item, Source source) {
        if (prop == NChild.SCORING_TRIP && item == get(Value.STAR_PASS_TRIP)) {
            for (ScoringTrip trip = (ScoringTrip) getLast(NChild.SCORING_TRIP); trip != null; trip = trip
                    .getPrevious()) {
                if (!(Boolean) trip.get(ScoringTrip.Value.AFTER_S_P)) {
                    set(Value.STAR_PASS_TRIP, trip.getNext());
                }
            }
        }
    }
    @Override
    public ValueWithId create(AddRemoveProperty prop, String id, Source source) {
        synchronized (coreLock) {
            if (prop == NChild.SCORING_TRIP) {
                return new ScoringTripImpl(this, Integer.parseInt(id));
            }
            return null;
        }
    }

    @Override
    public Jam getJam() { return (Jam) parent; }
    @Override
    public Team getTeam() { return team; }

    @Override
    public TeamJam getOtherTeam() { return getJam().getTeamJam(Team.ID_1.equals(subId) ? Team.ID_2 : Team.ID_1); }

    @Override
    public boolean isRunningOrEnded() { return this == team.getRunningOrEndedTeamJam(); }
    @Override
    public boolean isRunningOrUpcoming() { return this == team.getRunningOrUpcomingTeamJam(); }

    @Override
    public int getLastScore() { return (Integer) get(Value.LAST_SCORE); }
    @Override
    public void setLastScore(int l) { set(Value.LAST_SCORE, l); }

    @Override
    public int getOsOffset() { return (Integer) get(Value.OS_OFFSET); }
    @Override
    public void setOsOffset(int o) { set(Value.OS_OFFSET, o); }
    @Override
    public void changeOsOffset(int c) { set(Value.OS_OFFSET, c, Flag.CHANGE); }

    @Override
    public int getJamScore() { return (Integer) get(Value.JAM_SCORE); }
    @Override
    public int getTotalScore() { return (Integer) get(Value.TOTAL_SCORE); }

    @Override
    public ScoringTrip getCurrentScoringTrip() { return (ScoringTrip) get(Value.CURRENT_TRIP); }
    @Override
    public void addScoringTrip() { getOrCreate(NChild.SCORING_TRIP, getCurrentScoringTrip().getNumber() + 1); }
    @Override
    public void removeScoringTrip() {
        if (getAll(NChild.SCORING_TRIP).size() > 1) { getCurrentScoringTrip().delete(); }
    }

    @Override
    public boolean isLost() { return (Boolean) get(Value.LOST); }
    @Override
    public boolean isLead() { return (Boolean) get(Value.LEAD); }
    @Override
    public boolean isCalloff() { return (Boolean) get(Value.CALLOFF); }
    @Override
    public boolean isInjury() { return (Boolean) get(Value.INJURY); }
    @Override
    public boolean isDisplayLead() { return (Boolean) get(Value.DISPLAY_LEAD); }

    @Override
    public boolean isStarPass() { return (Boolean) get(Value.STAR_PASS); }
    @Override
    public ScoringTrip getStarPassTrip() { return (ScoringTrip) get(Value.STAR_PASS_TRIP); }

    @Override
    public boolean hasNoPivot() { return (Boolean) get(Value.NO_PIVOT); }
    @Override
    public void setNoPivot(boolean np) { set(Value.NO_PIVOT, np); }

    @Override
    public Fielding getFielding(FloorPosition fp) { return (Fielding) get(Child.FIELDING, fp.toString()); }

    private Team team;
    private RecalculateScoreBoardListener jamScoreListener;
    private RecalculateScoreBoardListener afterSPScoreListener;
}
