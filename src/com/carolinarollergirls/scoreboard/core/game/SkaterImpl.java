package com.carolinarollergirls.scoreboard.core.game;
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

import com.carolinarollergirls.scoreboard.core.interfaces.BoxTrip;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentSkater;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentTeam;
import com.carolinarollergirls.scoreboard.core.interfaces.Fielding;
import com.carolinarollergirls.scoreboard.core.interfaces.FloorPosition;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Penalty;
import com.carolinarollergirls.scoreboard.core.interfaces.Position;
import com.carolinarollergirls.scoreboard.core.interfaces.PreparedTeam.PreparedTeamSkater;
import com.carolinarollergirls.scoreboard.core.interfaces.Role;
import com.carolinarollergirls.scoreboard.core.interfaces.Skater;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.core.interfaces.TeamJam;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.event.ValueWithId;
import com.carolinarollergirls.scoreboard.rules.Rule;

public class SkaterImpl extends ScoreBoardEventProviderImpl<Skater> implements Skater {
    public SkaterImpl(Team t, String i) {
        super(t, (i == null ? UUID.randomUUID().toString() : i), Team.SKATER);
        team = t;
        game = t.getGame();
        initialize();
    }
    public SkaterImpl(Team t, PreparedTeamSkater pts) {
        super(t, pts.getId(), Team.SKATER);
        team = t;
        initialize();
        setName(pts.get(PreparedTeamSkater.NAME));
        setNumber(pts.get(PreparedTeamSkater.ROSTER_NUMBER));
        setFlags(pts.get(PreparedTeamSkater.FLAGS));
    }
    private void initialize() {
        addProperties(NAME, ROSTER_NUMBER, CURRENT_FIELDING, CURRENT_BOX_SYMBOLS, POSITION, ROLE, BASE_ROLE,
                PENALTY_BOX, FLAGS, FIELDING, PENALTY);
        set(BASE_ROLE, Role.BENCH);
        set(ROLE, Role.BENCH);
        setInverseReference(FIELDING, Fielding.SKATER);
        setCopy(POSITION, this, CURRENT_FIELDING, Fielding.POSITION, true);
        setCopy(PENALTY_BOX, this, CURRENT_FIELDING, Fielding.PENALTY_BOX, false);
        setCopy(CURRENT_BOX_SYMBOLS, this, CURRENT_FIELDING, Fielding.BOX_TRIP_SYMBOLS, true);
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
    protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == ROLE && flag != Flag.SPECIAL_CASE && !source.isFile()) {
            team.field(this, (Role) value);
            return last;
        }
        return value;
    }
    @Override
    protected void valueChanged(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == CURRENT_FIELDING) {
            Fielding f = (Fielding) value;
            Fielding lf = (Fielding) last;
            setRole(value == null ? getBaseRole() : f.getCurrentRole());
            if (lf != null && lf.getSkater() == this) {
                if (f != null && lf.getTeamJam().getNext() == f.getTeamJam() && lf.isInBox()) {
                    f.add(Fielding.BOX_TRIP, lf.getCurrentBoxTrip());
                }
                if (lf.isCurrent()) {
                    if (f != null) {
                        for (BoxTrip bt : lf.getAll(Fielding.BOX_TRIP)) {
                            f.add(Fielding.BOX_TRIP, bt);
                        }
                        lf.removeAll(Fielding.BOX_TRIP);
                    }
                    lf.setSkater(null);
                }
            }
        }
        if (prop == FLAGS) {
            if ("ALT".equals(value) || "BC".equals(value)) {
                set(BASE_ROLE, Role.NOT_IN_GAME);
            } else if (get(BASE_ROLE) == Role.NOT_IN_GAME) {
                set(BASE_ROLE, Role.BENCH);
                updateEligibility();
            }
        }
        if (prop == BASE_ROLE && get(ROLE) == last) {
            set(ROLE, (Role) value, Flag.SPECIAL_CASE);
        }
    }

    @Override
    public ScoreBoardEventProvider create(Child<?> prop, String id, Source source) {
        synchronized (coreLock) {
            if (prop == PENALTY) { return new PenaltyImpl(this, Integer.valueOf(id)); }
            return null;
        }
    }
    @Override
    protected void itemAdded(Child<?> prop, ValueWithId item, Source source) {
        if (prop == PENALTY) {
            Penalty p = (Penalty) item;
            if (FO_EXP_ID.equals(p.getProviderId())) {
                updateEligibility();
            }
            if (p.getNumber() == game.getInt(Rule.FO_LIMIT)) {
                Penalty fo = getPenalty(FO_EXP_ID);
                if (fo == null) {
                    fo = getOrCreate(PENALTY, 0);
                    fo.set(Penalty.CODE, "FO");
                }
                if (fo.get(Penalty.CODE) == "FO") {
                    fo.set(Penalty.JAM, p.getJam());
                }
            }
            if (!p.isServed() && getRole() == Role.JAMMER && getCurrentFielding() != null
                    && !getCurrentFielding().getTeamJam().getOtherTeam().isLead() && game.isInJam()) {
                getTeam().set(Team.LOST, true);
            }
            if (!p.isServed() && !game.isInJam()) {
                getTeam().field(this, getRole(getTeam().getRunningOrEndedTeamJam()),
                        getTeam().getRunningOrUpcomingTeamJam());
            }
        } else if (prop == FIELDING) {
            Fielding f = (Fielding) item;
            if (f.isCurrent()) {
                set(CURRENT_FIELDING, f);
            }
        }
    }
    @Override
    protected void itemRemoved(Child<?> prop, ValueWithId item, Source source) {
        if (prop == PENALTY) {
            if (FO_EXP_ID.equals(((Penalty) item).getProviderId())) {
                updateEligibility();
            } else if (get(PENALTY, game.getInt(Rule.FO_LIMIT)) == null) {
                Penalty fo = getPenalty(FO_EXP_ID);
                if (fo != null && fo.get(Penalty.CODE) == "FO") {
                    fo.delete();
                }
            }
        } else if (prop == FIELDING && getCurrentFielding() == item) {
            set(CURRENT_FIELDING, null);
        }
    }

    @Override
    public CurrentSkater getCurrentSkater() {
        CurrentTeam t = scoreBoard.getCurrentGame().get(CurrentGame.TEAM, team.getProviderId());
        return t == null ? null : t.get(CurrentTeam.SKATER, getId());
    }

    @Override
    public Team getTeam() { return team; }

    @Override
    public String getName() { return get(NAME); }
    @Override
    public void setName(String n) { set(NAME, n); }

    @Override
    public String getNumber() { return get(ROSTER_NUMBER); }
    @Override
    public void setNumber(String n) { set(ROSTER_NUMBER, n); }

    @Override
    public Fielding getFielding(TeamJam teamJam) {
        for (FloorPosition fp : FloorPosition.values()) {
            Fielding f = get(FIELDING, teamJam.getId() + "_" + fp.toString());
            if (f != null) { return f; }
        }
        return null;
    }
    @Override
    public Fielding getCurrentFielding() { return get(CURRENT_FIELDING); }
    @Override
    public void removeCurrentFielding() {
        if (getCurrentFielding() != null) {
            remove(FIELDING, getCurrentFielding());
        }
    }

    @Override
    public void updateFielding(TeamJam teamJam) {
        set(Skater.CURRENT_FIELDING, getFielding(teamJam));
        setRole(getRole(teamJam));
        updateEligibility();
    };

    @Override
    public Position getPosition() { return get(POSITION); }
    @Override
    public void setPosition(Position p) { set(POSITION, p); }

    @Override
    public Role getRole() { return get(ROLE); }
    @Override
    public Role getRole(TeamJam tj) {
        Fielding f = getFielding(tj);
        if (f == null) { return getBaseRole(); }
        return f.getCurrentRole();
    }
    @Override
    public void setRole(Role r) { set(ROLE, r, Flag.SPECIAL_CASE); }
    @Override
    public void setRoleToBase() { setRole(getBaseRole()); }

    @Override
    public Role getBaseRole() { return get(BASE_ROLE); }
    @Override
    public void setBaseRole(Role b) { set(BASE_ROLE, b); }

    @Override
    public void updateEligibility() {
        if (getBaseRole() != Role.BENCH && getBaseRole() != Role.INELIGIBLE) {
            return;
        }
        if (get(PENALTY, FO_EXP_ID) != null) {
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
        for (Fielding f : getAll(FIELDING)) {
            if (f.isSitFor3()) {
                if (lastN.contains(f.getTeamJam())) {
                    setBaseRole(Role.INELIGIBLE);
                    return;
                }
                if (f.getTeamJam().getJam().getParent() == game.getCurrentPeriod()) {
                    if (satThisPeriod) {
                        setBaseRole(Role.INELIGIBLE);
                        return;
                    } else {
                        satThisPeriod = true;
                    }
                }
            }
        }
        set(BASE_ROLE, Role.BENCH);
    }

    @Override
    public boolean isPenaltyBox() { return get(PENALTY_BOX); }
    @Override
    public void setPenaltyBox(boolean box) { set(PENALTY_BOX, box); }

    @Override
    public String getFlags() { return get(FLAGS); }
    @Override
    public void setFlags(String f) { set(FLAGS, f); }

    @Override
    public Penalty getPenalty(String id) { return get(PENALTY, id); }
    @Override
    public List<Penalty> getUnservedPenalties() {
        List<Penalty> usp = new ArrayList<>();
        for (Penalty p : getAll(PENALTY)) {
            if (!p.isServed()) {
                usp.add(p);
            }
        }
        return usp;
    }
    @Override
    public boolean hasUnservedPenalties() { return getUnservedPenalties().size() > 0; }

    protected Team team;
    private Game game;
}
