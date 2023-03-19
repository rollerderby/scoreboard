package com.carolinarollergirls.scoreboard.core.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.carolinarollergirls.scoreboard.core.interfaces.BoxTrip;
import com.carolinarollergirls.scoreboard.core.interfaces.Fielding;
import com.carolinarollergirls.scoreboard.core.interfaces.FloorPosition;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Penalty;
import com.carolinarollergirls.scoreboard.core.interfaces.Position;
import com.carolinarollergirls.scoreboard.core.interfaces.Role;
import com.carolinarollergirls.scoreboard.core.interfaces.Skater;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.core.interfaces.TeamJam;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.ParentOrderedScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.event.ValueWithId;

public class FieldingImpl extends ParentOrderedScoreBoardEventProviderImpl<Fielding> implements Fielding {
    public FieldingImpl(TeamJam teamJam, Position position) {
        super(teamJam, position.getProviderId(), TeamJam.FIELDING);
        addProperties(props);
        this.teamJam = teamJam;
        game = teamJam.getTeam().getGame();
        set(POSITION, position);
        addWriteProtection(POSITION);
        setRecalculated(SKATER_NUMBER)
            .addIndirectSource(this, SKATER, Skater.ROSTER_NUMBER)
            .addSource(this, NOT_FIELDED);
        setInverseReference(BOX_TRIP, BoxTrip.FIELDING);
        addWriteProtectionOverride(BOX_TRIP, Source.NON_WS);
        addWriteProtectionOverride(CURRENT_BOX_TRIP, Source.NON_WS);
        setInverseReference(SKATER, Skater.FIELDING);
        setRecalculated(NOT_FIELDED).addSource(this, SKATER);
    }

    @Override
    protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == PENALTY_BOX && !source.isFile()) {
            if ((Boolean) value && (getCurrentBoxTrip() == null || !getCurrentBoxTrip().isCurrent())) {
                if (get(NOT_FIELDED)) {
                    return false;
                } else {
                    getTeamJam().getTeam().add(Team.BOX_TRIP, new BoxTripImpl(this));
                    if (getTeamJam().getTeam().hasFieldingAdvancePending() && isCurrent()) {
                        if (getNext().getSkater() == null && getNext().getCurrentRole() == getCurrentRole()) {
                            getNext().setSkater(getSkater());
                        } else {
                            getTeamJam().getTeam().field(getSkater(), getCurrentRole(), getTeamJam().getNext());
                        }
                        getCurrentBoxTrip().add(BoxTrip.FIELDING, getSkater().getFielding(getTeamJam().getNext()));
                    }
                }
            } else if (!(Boolean) value && getCurrentBoxTrip() != null && getCurrentBoxTrip().isCurrent()) {
                getCurrentBoxTrip().end();
            }
        }
        if (prop == NOT_FIELDED && (getSkater() != null || numberOf(BOX_TRIP) > 0)) { return false; }
        if (prop == SKATER_NUMBER) {
            if (getSkater() != null) {
                return getSkater().getRosterNumber();
            } else if (get(NOT_FIELDED)) {
                return "n/a";
            } else {
                return "?";
            }
        }
        return value;
    }
    @Override
    protected void valueChanged(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == PENALTY_BOX && isCurrent() && (Boolean) value &&
            getPosition().getFloorPosition() == FloorPosition.JAMMER && game.isInJam() &&
            !teamJam.getOtherTeam().isLead()) {
            teamJam.set(TeamJam.LOST, true);
        }
        if (prop == CURRENT_BOX_TRIP) {
            set(PENALTY_BOX, value != null && ((BoxTrip) value).isCurrent());
            updateBoxTripSymbols();
        }
        if (prop == SIT_FOR_3) {
            updateBoxTripSymbols();
            if (getSkater() != null) { getSkater().updateEligibility(); }
        }
        if (prop == NOT_FIELDED && getPosition().getFloorPosition() == FloorPosition.PIVOT) {
            teamJam.setNoPivot((Boolean) value);
        }
        if (prop == SKATER && value != null && isInBox() && !source.isFile()) {
            for (Penalty p : ((Skater) value).getUnservedPenalties()) { getCurrentBoxTrip().add(BoxTrip.PENALTY, p); }
        }
    }

    @Override
    protected void itemAdded(Child<?> prop, ValueWithId item, Source source) {
        if (prop == BOX_TRIP) {
            BoxTrip bt = (BoxTrip) item;
            if (bt.isCurrent() || getCurrentBoxTrip() == null) { set(CURRENT_BOX_TRIP, bt); }
            updateBoxTripSymbols();
        }
    }
    @Override
    protected void itemRemoved(Child<?> prop, ValueWithId item, Source source) {
        if (prop == BOX_TRIP) {
            if (item == getCurrentBoxTrip()) { set(CURRENT_BOX_TRIP, null); }
            updateBoxTripSymbols();
        }
    }

    @Override
    public void execute(Command prop, Source source) {
        if (prop == ADD_BOX_TRIP && !get(NOT_FIELDED)) {
            requestBatchStart();
            BoxTrip bt = new BoxTripImpl(this);
            bt.end();
            getTeamJam().getTeam().add(Team.BOX_TRIP, bt);
            add(BOX_TRIP, bt);
            requestBatchEnd();
        }
        if (prop == UNEND_BOX_TRIP && getCurrentBoxTrip() != null && !getCurrentBoxTrip().isCurrent()) {
            getCurrentBoxTrip().unend();
        }
    }

    @Override
    public TeamJam getTeamJam() {
        return teamJam;
    }
    @Override
    public Position getPosition() {
        return get(POSITION);
    }

    @Override
    public boolean isCurrent() {
        return (teamJam.isRunningOrUpcoming() && !teamJam.getTeam().hasFieldingAdvancePending()) ||
            teamJam.isRunningOrEnded() && teamJam.getTeam().hasFieldingAdvancePending();
    }

    @Override
    public Role getCurrentRole() {
        return getPosition().getFloorPosition().getRole(teamJam);
    }

    @Override
    public Skater getSkater() {
        return get(SKATER);
    }
    @Override
    public void setSkater(Skater s) {
        set(SKATER, s);
    }

    @Override
    public boolean isSitFor3() {
        return get(SIT_FOR_3);
    }
    @Override
    public boolean isInBox() {
        return get(PENALTY_BOX);
    }
    @Override
    public BoxTrip getCurrentBoxTrip() {
        return get(CURRENT_BOX_TRIP);
    }
    @Override
    public void updateBoxTripSymbols() {
        if (getTeamJam().getJam().isInjuryContinuation() && getTeamJam().isLead()) {
            getPrevious().updateBoxTripSymbols();
        }
        boolean considerContinuation = false;
        List<BoxTrip> trips = new ArrayList<>();
        for (BoxTrip bt : getAll(BOX_TRIP)) { trips.add(bt); }
        if (getNext() != null && getNext().getTeamJam().getJam().isInjuryContinuation() && getTeamJam().isLead()) {
            considerContinuation = true;
            for (BoxTrip bt : getNext().getAll(BOX_TRIP)) {
                if (bt.getStartFielding() == getNext() && !bt.startedAfterSP()) { trips.add(bt); }
            }
        }
        Collections.sort(trips, new Comparator<BoxTrip>() {
            @Override
            public int compare(BoxTrip b1, BoxTrip b2) {
                if (b1 == b2) { return 0; }
                if (b1 == null) { return 1; }
                return b1.compareTo(b2);
            }
        });
        StringBuilder beforeSP = new StringBuilder();
        StringBuilder afterSP = new StringBuilder();
        StringBuilder jam = new StringBuilder();
        // TODO: make symbols configurable in the ruleset
        // Key: 1 = started earlier and ended later
        // 2 = started during this, ended later
        // 3 = started with this, ended later
        // 4 = ended during this, started earlier
        // 5 = started and ended during this
        // 6 = started with and ended during this
        String[] symbols = "S,-,S,$,+,$".split(",");
        // 2015-18 symbols: "|,/,S,X,X,$"
        // pre-2015 symbols:"S,/,S,$,X,$"
        for (BoxTrip trip : trips) {
            int typeBeforeSP = 1;
            int typeAfterSP = 1;
            int typeJam = 1;
            if (this == trip.getStartFielding()) {
                if (trip.startedBetweenJams()) {
                    typeJam = 3;
                    typeBeforeSP = 3;
                } else if (trip.startedAfterSP()) {
                    typeJam = 2;
                    typeBeforeSP = 0;
                    typeAfterSP = 2;
                } else {
                    typeJam = 2;
                    typeBeforeSP = 2;
                }
            } else if (considerContinuation && getNext() == trip.getStartFielding()) {
                // team had Lead at continuation -> no SP possible
                typeJam = 2;
                typeBeforeSP = 2;
            }
            if (this == trip.getEndFielding()) {
                if (trip.endedAfterSP() && !trip.endedBetweenJams()) {
                    typeJam += 3;
                    typeAfterSP += 3;
                } else if (!trip.endedBetweenJams() || considerContinuation) {
                    typeJam += 3;
                    typeBeforeSP += 3;
                    typeAfterSP = 0;
                }
            } else if (considerContinuation && getNext() == trip.getEndFielding() && !trip.endedAfterSP() &&
                       !trip.endedBetweenJams()) {
                typeJam += 3;
                typeBeforeSP += 3;
            }
            if (typeBeforeSP > 0) { beforeSP.append(" " + symbols[typeBeforeSP - 1]); }
            if (typeAfterSP > 0) { afterSP.append(" " + symbols[typeAfterSP - 1]); }
            if (typeJam > 0) { jam.append(" " + symbols[typeJam - 1]); }
        }
        if (isSitFor3()) {
            jam.append(" 3");
            if (getTeamJam().isStarPass()) {
                afterSP.append(" 3");
            } else {
                beforeSP.append(" 3");
            }
        }
        set(BOX_TRIP_SYMBOLS_BEFORE_S_P, beforeSP.toString().trim());
        set(BOX_TRIP_SYMBOLS_AFTER_S_P, afterSP.toString().trim());
        set(BOX_TRIP_SYMBOLS, jam.toString().trim());
    }

    private TeamJam teamJam;
    private Game game;
}
