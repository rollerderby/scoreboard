package com.carolinarollergirls.scoreboard.core.impl;

import java.util.Arrays;

import com.carolinarollergirls.scoreboard.core.Fielding;
import com.carolinarollergirls.scoreboard.core.FloorPosition;
import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.ScoringTrip;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.event.ParentOrderedScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;

public class TeamJamImpl extends ParentOrderedScoreBoardEventProviderImpl<TeamJam> implements TeamJam {
    public TeamJamImpl(Jam j, String teamId) {
        super(j, teamId, Jam.Child.TEAM_JAM, TeamJam.class, Value.class, Child.class, NChild.class);
        team = scoreBoard.getTeam(teamId);
        values.put(Value.LAST_SCORE, 0);
        values.put(Value.OS_OFFSET, 0);
        values.put(Value.JAM_SCORE, 0);
        values.put(Value.TOTAL_SCORE, 0);
        values.put(Value.LOST, false);
        values.put(Value.LEAD, false);
        values.put(Value.CALLOFF, false);
        values.put(Value.INJURY, false);
        values.put(Value.NO_INITIAL, true);
        values.put(Value.DISPLAY_LEAD, false);
        values.put(Value.NO_PIVOT, true);
        values.put(Value.STAR_PASS_TRIP, null);
        values.put(Value.STAR_PASS, false);
        addReference(new IndirectValueReference(this, Value.LAST_SCORE, this, IValue.PREVIOUS,
                Value.TOTAL_SCORE, true, 0));
        for (Value prop : Arrays.asList(Value.LAST_SCORE, Value.JAM_SCORE, Value.OS_OFFSET)) {
            addReference(new UpdateReference(this, Value.TOTAL_SCORE, this, prop));
        }
        addReference(new ElementReference(Value.CURRENT_TRIP, ScoringTrip.class, null));
        addReference(new UpdateReference(this, Value.NO_INITIAL, this, Value.CURRENT_TRIP));
        addReference(new UpdateReference(this, Value.DISPLAY_LEAD, this, Value.LEAD));
        addReference(new UpdateReference(this, Value.DISPLAY_LEAD, this, Value.LOST));
        addReference(new ElementReference(Value.STAR_PASS_TRIP, ScoringTrip.class, null));
        addReference(new UpdateReference(this, Value.STAR_PASS, this, Value.STAR_PASS_TRIP));
        for (ValueWithId p : team.getAll(Team.Child.POSITION)) {
            add(Child.FIELDING, new FieldingImpl(this, (Position)p));
        }
        addWriteProtection(Child.FIELDING);
        addWriteProtectionOverride(Value.CURRENT_TRIP, Flag.INTERNAL);
        getOrCreate(NChild.SCORING_TRIP, 1);
    }

    protected Object computeValue(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == Value.JAM_SCORE) {
            int sum = 0;
            for (ValueWithId trip : getAll(NChild.SCORING_TRIP)) {
                sum += ((ScoringTrip)trip).getScore();
            }
            return sum;
        }
        if (prop == Value.TOTAL_SCORE) {
            return getLastScore() + getJamScore() + getOsOffset();
        }
        if (prop == Value.NO_INITIAL) {
            if (getCurrentScoringTrip() == null) { return true; }
            return getCurrentScoringTrip().getNumber() == 1;
        }
        if (prop == Value.DISPLAY_LEAD) {
            return isLead() && !isLost();
        }
        if (prop == Value.STAR_PASS) {
            if (flag == Flag.INTERNAL || flag == Flag.FROM_AUTOSAVE) { 
                return getStarPassTrip() != null;
            } else {
                set(Value.STAR_PASS_TRIP, (Boolean)value ? getCurrentScoringTrip() : null);
                return last;
            }
        }
        if (value instanceof Integer && prop != Value.OS_OFFSET && (Integer)value < 0) { return 0; }
        return value;
    }
    protected void valueChanged(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == Value.STAR_PASS_TRIP) {
            if (last != null) {
                ((ScoringTrip)last).set(ScoringTrip.Value.AFTER_S_P, false);
            }
            if (value != null) {
                ((ScoringTrip)value).set(ScoringTrip.Value.AFTER_S_P, true);
            }
        }
        if (prop == Value.INJURY) {
            getOtherTeam().set(prop, value);
        }
    }

    protected void itemAdded(AddRemoveProperty prop, ValueWithId item) {
        if (prop == NChild.SCORING_TRIP) {
            set(Value.CURRENT_TRIP, getLast(NChild.SCORING_TRIP), Flag.INTERNAL);
        }
    }
    protected void itemRemoved(AddRemoveProperty prop, ValueWithId item) {
        if (prop == NChild.SCORING_TRIP && item == get(Value.CURRENT_TRIP)) {
            set(Value.CURRENT_TRIP, getLast(NChild.SCORING_TRIP), Flag.INTERNAL);
        }
        if (prop == NChild.SCORING_TRIP && item == get(Value.STAR_PASS_TRIP)) {
            set(Value.STAR_PASS_TRIP, ((ScoringTrip)item).getNext());
        }
    }
    public ValueWithId create(AddRemoveProperty prop, String id) {
        synchronized (coreLock) {
            if (prop == NChild.SCORING_TRIP) {
                return new ScoringTripImpl(this, id);
            }
            return null;
        }
    }

    public Jam getJam() { return (Jam)parent; }
    public Team getTeam() { return team; }
    
    public TeamJam getOtherTeam() { return getJam().getTeamJam(Team.ID_1.equals(subId) ? Team.ID_2 : Team.ID_1); }

    public boolean isRunningOrEnded() { return this == team.getRunningOrEndedTeamJam(); }
    public boolean isRunningOrUpcoming() { return this == team.getRunningOrUpcomingTeamJam(); }

    public int getLastScore() { return (Integer)get(Value.LAST_SCORE); }
    public void setLastScore(int l) { set(Value.LAST_SCORE, l); }

    public int getOsOffset() { return (Integer)get(Value.OS_OFFSET); }
    public void setOsOffset(int o) { set(Value.OS_OFFSET, o); }
    public void changeOsOffset(int c) { set(Value.OS_OFFSET, c, Flag.CHANGE); }

    public int getJamScore() { return (Integer)get(Value.JAM_SCORE); }
    public int getTotalScore() { return (Integer)get(Value.TOTAL_SCORE); }
    
    public ScoringTrip getCurrentScoringTrip() { return (ScoringTrip)get(Value.CURRENT_TRIP); }
    public void addScoringTrip() { getOrCreate(NChild.SCORING_TRIP, getCurrentScoringTrip().getNumber() + 1); }
    public void removeScoringTrip() { if (getAll(NChild.SCORING_TRIP).size() > 1) { getCurrentScoringTrip().unlink(); }}

    public boolean isLost() { return (Boolean)get(Value.LOST); }
    public boolean isLead() { return (Boolean)get(Value.LEAD); }
    public boolean isCalloff() { return (Boolean)get(Value.CALLOFF); }
    public boolean isInjury() { return (Boolean)get(Value.INJURY); }
    public boolean isDisplayLead() { return (Boolean)get(Value.DISPLAY_LEAD); }

    public boolean isStarPass() { return (Boolean)get(Value.STAR_PASS); }
    public ScoringTrip getStarPassTrip() { return (ScoringTrip)get(Value.STAR_PASS_TRIP); }
    
    public boolean hasNoPivot() { return (Boolean)get(Value.NO_PIVOT); }
    public void setNoPivot(boolean np) { set(Value.NO_PIVOT, np); }

    public Fielding getFielding (FloorPosition fp) { return (Fielding)get(Child.FIELDING, fp.toString()); }

    private Team team;
}
