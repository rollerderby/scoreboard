package com.carolinarollergirls.scoreboard.core.impl;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.UUID;

import com.carolinarollergirls.scoreboard.core.Fielding;
import com.carolinarollergirls.scoreboard.core.Penalty;
import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.Role;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;

public class SkaterImpl extends ScoreBoardEventProviderImpl implements Skater {
    public SkaterImpl(Team t, String i, String n, String num, String flags) {
        super(t, Value.ID, Team.Child.SKATER, Skater.class, Value.class, Child.class, NChild.class);
        team = t;
        setId(i);
        setName(n);
        setNumber(num);
        setFlags(flags);
        setRole(Role.BENCH);
        setBaseRole(Role.BENCH);
        addWriteProtectionOverride(Value.CURRENT_FIELDING, Flag.INTERNAL);
        addReference(new ElementReference(Child.FIELDING, Fielding.class, Fielding.Value.SKATER));
        addReference(new IndirectValueReference(this, Value.POSITION, this, 
                Value.CURRENT_FIELDING, Fielding.Value.POSITION, true, null));
        addReference(new IndirectValueReference(this, Value.PENALTY_BOX, this, 
                Value.CURRENT_FIELDING, Fielding.Value.PENALTY_BOX, false, false));
    }

    protected Object computeValue(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == Value.ROLE && flag != Flag.INTERNAL) {
            team.field(this, (Role)value);
            return last;
        }
        return value;
    }
    protected void valueChanged(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == Value.CURRENT_FIELDING) {
            Fielding f = (Fielding)value;
            Fielding lf = (Fielding)last;
            setRole(value == null ? getBaseRole() : f.getCurrentRole());
            if (lf != null && lf.getSkater() == this) {
                if (f != null && (lf.getTeamJam().getNext() == f.getTeamJam() || lf.isCurrent())) {
                    f.setPenaltyBox(lf.getPenaltyBox());
                } 
                if (lf.isCurrent()) {
                    lf.setSkater(null);
                    lf.setPenaltyBox(false);
                }
            }
        }
        if (prop == Value.BASE_ROLE && get(Value.ROLE) == last) {
            set(Value.ROLE, value, Flag.INTERNAL);
        }
    }

    public ValueWithId create(AddRemoveProperty prop, String id) {
        synchronized (coreLock) {
            if (prop == NChild.PENALTY) { return new PenaltyImpl(this, id); }
            return null;
        }	
    }
    protected void itemAdded(AddRemoveProperty prop, ValueWithId item) {
        if (prop == NChild.PENALTY && FO_EXP_ID.equals(((Penalty)item).getProviderId()) && getBaseRole() == Role.BENCH) {
            setBaseRole(Role.INELIGIBLE);
        } else if (prop == Child.FIELDING && ((Fielding)item).isCurrent()) {
            set(Value.CURRENT_FIELDING, item, Flag.INTERNAL);
        }
    }
    protected void itemRemoved(AddRemoveProperty prop, ValueWithId item) {
        if (prop == NChild.PENALTY && FO_EXP_ID.equals(((Penalty)item).getProviderId()) && getBaseRole() == Role.INELIGIBLE) {
            setBaseRole(Role.BENCH);
        } else if (prop == Child.FIELDING && getCurrentFielding() == item) {
            set(Value.CURRENT_FIELDING, null, Flag.INTERNAL);
        }
    }

    public Team getTeam() { return team; }

    private void setId(String i) {
        UUID uuid;
        try {
            uuid = UUID.fromString(i);
        } catch (IllegalArgumentException iae) {
            uuid = UUID.randomUUID();
        }
        set(Value.ID, uuid.toString());
    }

    public String getName() { return (String)get(Value.NAME); }
    public void setName(String n) { set(Value.NAME, n); }

    public String getNumber() { return (String)get(Value.NUMBER); }
    public void setNumber(String n) { set(Value.NUMBER, n); }

    public Fielding getCurrentFielding() { return (Fielding)get(Value.CURRENT_FIELDING); }
    public void removeCurrentFielding() {
        if (getCurrentFielding() != null) {
            remove(Child.FIELDING, getCurrentFielding());
        }
    }

    public Position getPosition() { return (Position)get(Value.POSITION); }
    public void setPosition(Position p) { set(Value.POSITION, p); }

    public Role getRole() { return (Role)get(Value.ROLE); }
    public void setRole(Role r) { set(Value.ROLE, r, Flag.INTERNAL); }
    public void setRoleToBase() { setRole(getBaseRole()); }

    public Role getBaseRole() { return (Role)get(Value.BASE_ROLE); }
    public void setBaseRole(Role b) { set(Value.BASE_ROLE, b); }

    public boolean isPenaltyBox() { return (Boolean)get(Value.PENALTY_BOX); }
    public void setPenaltyBox(boolean box) { set(Value.PENALTY_BOX, box); }

    public String getFlags() { return (String)get(Value.FLAGS); }
    public void setFlags(String f) { set(Value.FLAGS, f); }

    public Penalty getPenalty(String id) { return (Penalty)get(NChild.PENALTY, id); }

    public SkaterSnapshot snapshot() {
        synchronized (coreLock) {
            return new SkaterSnapshotImpl(this);
        }
    }
    public void restoreSnapshot(SkaterSnapshot s) {
        synchronized (coreLock) {
            if (s.getId() != getId()) {	return; }
            setPosition(s.getPosition());
            setRole(s.getRole());
            setBaseRole(s.getBaseRole());
        }
    }

    protected Team team;

    public static class SkaterSnapshotImpl implements SkaterSnapshot {
        private SkaterSnapshotImpl(Skater skater) {
            id = skater.getId();
            position = skater.getPosition();
            role = skater.getRole();
            baseRole = skater.getBaseRole();
        }

        public String getId( ) { return id; }
        public Position getPosition() { return position; }
        public Role getRole() { return role; }
        public Role getBaseRole() { return baseRole; }

        protected String id;
        protected Position position;
        protected Role role;
        protected Role baseRole;
    }
}
