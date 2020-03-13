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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.carolinarollergirls.scoreboard.core.Fielding;
import com.carolinarollergirls.scoreboard.core.FloorPosition;
import com.carolinarollergirls.scoreboard.core.Penalty;
import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.PreparedTeam.PreparedTeamSkater;
import com.carolinarollergirls.scoreboard.core.Role;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.rules.Rule;

public class SkaterImpl extends ScoreBoardEventProviderImpl implements Skater {
    public SkaterImpl(Team t, String i) {
        super(t, (i == null ? UUID.randomUUID().toString() : i), Team.Child.SKATER, Skater.class, Value.class,
                Child.class, NChild.class);
        team = t;
        initialize();
    }
    public SkaterImpl(Team t, PreparedTeamSkater pts) {
        super(t, pts.getId(), Team.Child.SKATER, Skater.class, Value.class, Child.class, NChild.class);
        team = t;
        initialize();
        setName((String) pts.get(PreparedTeamSkater.Value.NAME));
        setNumber((String) pts.get(PreparedTeamSkater.Value.NUMBER));
        setFlags((String) pts.get(PreparedTeamSkater.Value.FLAGS));
    }
    private void initialize() {
        set(Value.BASE_ROLE, Role.BENCH);
        set(Value.ROLE, Role.BENCH);
        setInverseReference(Child.FIELDING, Fielding.Value.SKATER);
        setCopy(Value.POSITION, this, Value.CURRENT_FIELDING, Fielding.Value.POSITION, true);
        setCopy(Value.PENALTY_BOX, this, Value.CURRENT_FIELDING, Fielding.Value.PENALTY_BOX, false);
        setCopy(Value.CURRENT_BOX_SYMBOLS, this, Value.CURRENT_FIELDING, Fielding.Value.BOX_TRIP_SYMBOLS, true);
    }

    @Override
    public int compareTo(Skater other) {
        if (other == null) { return -1; }
        if (getNumber() == other.getNumber()) { return 0; }
        if (getNumber() == null) { return 1; }
        if (other.getNumber() == null) { return -1; }
        return getNumber().compareTo(other.getNumber());
    }

    @Override
    protected Object computeValue(PermanentProperty prop, Object value, Object last, Source source, Flag flag) {
        if (prop == Value.ROLE && flag != Flag.SPECIAL_CASE && !source.isFile()) {
            team.field(this, (Role) value);
            return last;
        }
        return value;
    }
    @Override
    protected void valueChanged(PermanentProperty prop, Object value, Object last, Source source, Flag flag) {
        if (prop == Value.CURRENT_FIELDING) {
            Fielding f = (Fielding) value;
            Fielding lf = (Fielding) last;
            setRole(value == null ? getBaseRole() : f.getCurrentRole());
            if (lf != null && lf.getSkater() == this) {
                if (f != null && lf.getTeamJam().getNext() == f.getTeamJam() && lf.isInBox()) {
                    f.add(Fielding.Child.BOX_TRIP, lf.getCurrentBoxTrip());
                }
                if (lf.isCurrent()) {
                    if (f != null) {
                        for (ValueWithId v : lf.getAll(Fielding.Child.BOX_TRIP)) {
                            f.add(Fielding.Child.BOX_TRIP, v);
                        }
                        lf.removeAll(Fielding.Child.BOX_TRIP);
                    }
                    lf.setSkater(null);
                }
            }
        }
        if (prop == Value.FLAGS) {
            if ("ALT".equals(value) || "BC".equals(value)) {
                set(Value.BASE_ROLE, Role.NOT_IN_GAME);
            } else if (get(Value.BASE_ROLE) == Role.NOT_IN_GAME) {
                set(Value.BASE_ROLE, Role.BENCH);
                updateEligibility();
            }
        }
        if (prop == Value.BASE_ROLE && get(Value.ROLE) == last) {
            set(Value.ROLE, value, Flag.SPECIAL_CASE);
        }
    }

    @Override
    public ValueWithId create(AddRemoveProperty prop, String id) {
        synchronized (coreLock) {
            if (prop == NChild.PENALTY) { return new PenaltyImpl(this, Integer.valueOf(id)); }
            return null;
        }
    }
    @Override
    protected void itemAdded(AddRemoveProperty prop, ValueWithId item, Source source) {
        if (prop == NChild.PENALTY) {
            Penalty p = (Penalty) item;
            if (FO_EXP_ID.equals(p.getProviderId())) {
                updateEligibility();
            } 
            if (p.getNumber() == scoreBoard.getRulesets().getInt(Rule.FO_LIMIT)) {
                Penalty fo = getPenalty(FO_EXP_ID);
                if (fo == null) {
                    fo = (Penalty)getOrCreate(NChild.PENALTY, 0);
                    fo.set(Penalty.Value.CODE, "FO");
                }
                if (fo.get(Penalty.Value.CODE) == "FO") {
                    fo.set(Penalty.Value.JAM, p.getJam());
                }
            }
            if (!p.isServed() && getRole() == Role.JAMMER && getCurrentFielding() != null
                    && !getCurrentFielding().getTeamJam().getOtherTeam().isLead() && scoreBoard.isInJam()) {
                getTeam().set(Team.Value.LOST, true);
            }
            if (!p.isServed() && !getScoreBoard().isInJam()) {
                getTeam().field(this, getRole(getTeam().getRunningOrEndedTeamJam()),
                        getTeam().getRunningOrUpcomingTeamJam());
            }
        } else if (prop == Child.FIELDING) {
            Fielding f = (Fielding) item;
            if (f.isCurrent()) {
                set(Value.CURRENT_FIELDING, item);
            }
        }
    }
    @Override
    protected void itemRemoved(AddRemoveProperty prop, ValueWithId item, Source source) {
        if (prop == NChild.PENALTY) {
            if (FO_EXP_ID.equals(((Penalty)item).getProviderId())) {
                updateEligibility();
            } else if (get(NChild.PENALTY, scoreBoard.getRulesets().getInt(Rule.FO_LIMIT)) == null) {
                Penalty fo = getPenalty(FO_EXP_ID);
                if (fo != null && fo.get(Penalty.Value.CODE) == "FO") {
                    fo.delete();
                }
            }
        } else if (prop == Child.FIELDING && getCurrentFielding() == item) {
            set(Value.CURRENT_FIELDING, null);
        }
    }

    @Override
    public Team getTeam() { return team; }

    @Override
    public String getName() { return (String) get(Value.NAME); }
    @Override
    public void setName(String n) { set(Value.NAME, n); }

    @Override
    public String getNumber() { return (String) get(Value.ROSTER_NUMBER); }
    @Override
    public void setNumber(String n) { set(Value.ROSTER_NUMBER, n); }

    @Override
    public Fielding getFielding(TeamJam teamJam) {
        for (FloorPosition fp : FloorPosition.values()) {
            Fielding f = (Fielding) get(Child.FIELDING, teamJam.getId() + "_" + fp.toString());
            if (f != null) { return f; }
        }
        return null;
    }
    @Override
    public Fielding getCurrentFielding() { return (Fielding) get(Value.CURRENT_FIELDING); }
    @Override
    public void removeCurrentFielding() {
        if (getCurrentFielding() != null) {
            remove(Child.FIELDING, getCurrentFielding());
        }
    }

    @Override
    public void updateFielding(TeamJam teamJam) {
        set(Skater.Value.CURRENT_FIELDING, getFielding(teamJam));
        setRole(getRole(teamJam));
        updateEligibility();
    };

    @Override
    public Position getPosition() { return (Position) get(Value.POSITION); }
    @Override
    public void setPosition(Position p) { set(Value.POSITION, p); }

    @Override
    public Role getRole() { return (Role) get(Value.ROLE); }
    @Override
    public Role getRole(TeamJam tj) {
        Fielding f = getFielding(tj);
        if (f == null) { return getBaseRole(); }
        return f.getCurrentRole();
    }
    @Override
    public void setRole(Role r) { set(Value.ROLE, r, Flag.SPECIAL_CASE); }
    @Override
    public void setRoleToBase() { setRole(getBaseRole()); }

    @Override
    public Role getBaseRole() { return (Role) get(Value.BASE_ROLE); }
    @Override
    public void setBaseRole(Role b) { set(Value.BASE_ROLE, b); }

    @Override
    public void updateEligibility() {
        if (getBaseRole() != Role.BENCH && getBaseRole() != Role.INELIGIBLE) {
            return;
        }
        if (get(NChild.PENALTY, FO_EXP_ID) != null) {
            setBaseRole(Role.INELIGIBLE);
            return;
        }
        boolean satThisPeriod = false;
        Set<TeamJam> lastN = new HashSet<>();
        TeamJam tj = getTeam().getRunningOrUpcomingTeamJam();
        int n = getTeam().hasFieldingAdvancePending() ? 5 : 4;
        while (tj != null && lastN.size() < n) {
            lastN.add(tj);
            tj = tj.getPrevious();
        }
        for (ValueWithId v : getAll(Child.FIELDING)) {
            Fielding f = (Fielding) v;
            if (f.isSitFor3()) {
                if (lastN.contains(f.getTeamJam())) {
                    setBaseRole(Role.INELIGIBLE);
                    return;
                }
                if (f.getTeamJam().getJam().getParent() == scoreBoard.getCurrentPeriod()) {
                    if (satThisPeriod) {
                        setBaseRole(Role.INELIGIBLE);
                        return;
                    } else {
                        satThisPeriod = true;
                    }
                }
            }
        }
        set(Value.BASE_ROLE, Role.BENCH);
    }

    @Override
    public boolean isPenaltyBox() { return (Boolean) get(Value.PENALTY_BOX); }
    @Override
    public void setPenaltyBox(boolean box) { set(Value.PENALTY_BOX, box); }

    @Override
    public String getFlags() { return (String) get(Value.FLAGS); }
    @Override
    public void setFlags(String f) { set(Value.FLAGS, f); }

    @Override
    public Penalty getPenalty(String id) { return (Penalty) get(NChild.PENALTY, id); }
    @Override
    public List<Penalty> getUnservedPenalties() {
        List<Penalty> usp = new ArrayList<>();
        for (ValueWithId p : getAll(NChild.PENALTY)) {
            if (!((Penalty) p).isServed()) {
                usp.add((Penalty) p);
            }
        }
        return usp;
    }
    @Override
    public boolean hasUnservedPenalties() { return getUnservedPenalties().size() > 0; }

    protected Team team;
}
