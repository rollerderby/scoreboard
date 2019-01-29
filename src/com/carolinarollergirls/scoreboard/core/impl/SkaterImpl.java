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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.carolinarollergirls.scoreboard.core.Fielding;
import com.carolinarollergirls.scoreboard.core.FloorPosition;
import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.Role;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;

public class SkaterImpl extends ScoreBoardEventProviderImpl implements Skater {
    public SkaterImpl(Team t, String i, String n, String num, String flags) {
	super(t, Team.Child.SKATER, Skater.class, Value.class, Child.class);
        team = t;
        setId(i);
        setName(n);
        setNumber(num);
        setFlags(flags);
        setRole(Role.BENCH);
        setBaseRole(Role.BENCH);
        addReference(new IndirectPropertyReference(this, Value.POSITION, this, 
        	Value.CURRENT_FIELDING, Fielding.Value.POSITION, false, null));
        addReference(new IndirectPropertyReference(this, Value.PENALTY_BOX, this, 
        	Value.CURRENT_FIELDING, Fielding.Value.PENALTY_BOX, false, false));
        values.put(Value.SORT_PENALTIES, true);
    }

    public Object valueFromString(PermanentProperty prop, String sValue) {
	if (prop == Value.BASE_ROLE || prop == Value.ROLE) { return Role.fromString(sValue); }
	if (prop == Value.POSITION) { return team.getPosition(FloorPosition.fromString(sValue)); }
	return super.valueFromString(prop, sValue);
    }
    public boolean set(PermanentProperty prop, Object value, Flag flag) {
	synchronized (coreLock) {
	    if (prop == Value.ROLE && flag != Flag.CUSTOM) {
		team.field(this, (Role)value);
		return true;
	    } else {
		return super.set(prop, value, flag);
	    }
	}
    }
    protected void valueChanged(PermanentProperty prop, Object value, Object last) {
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
	    set(Value.ROLE, value, Flag.CUSTOM);
	}
	if (prop == Value.SORT_PENALTIES && (Boolean)value) {
	    sortPenalties();
	}
    }
    
    public ValueWithId create(AddRemoveProperty prop, String id) {
	synchronized (coreLock) {
	    if (prop == Child.PENALTY) { return new PenaltyImpl(this, id); }
	    return null;
	}	
    }
    public boolean add(AddRemoveProperty prop, ValueWithId item) {
	synchronized (coreLock) {
	    requestBatchStart();
	    boolean result = super.add(prop, item);
	    if (result) {
		if (prop == Child.PENALTY) {
		    if (FO_EXP_ID.equals(((Penalty)item).getProviderId())) {
			if (getBaseRole() == Role.BENCH) {
			    setBaseRole(Role.INELIGIBLE);
			}
		    } else {
			sortPenalties();
		    }
		} else if (prop == Child.FIELDING && ((Fielding)item).isCurrent()) {
		    set(Value.CURRENT_FIELDING, item);
		}
	    }
	    requestBatchEnd();
	    return result;
	}
    }
    public boolean remove(AddRemoveProperty prop, ValueWithId item) {
	synchronized (coreLock) {
	    requestBatchStart();
	    boolean result;
	    if (prop == Child.PENALTY) {
		Set<String> oldIds = null;
		oldIds = new HashSet<String>(children.get(prop).keySet());
		result = super.removeSilent(prop, item);
		if (result) {
		    if (FO_EXP_ID.equals(((Penalty)item).getProviderId())) {
			scoreBoardChange(new ScoreBoardEvent(this, prop, item, true));
			if (getBaseRole() == Role.INELIGIBLE) {
			    setBaseRole(Role.BENCH);
			}
		    } else {
			sortPenalties(oldIds);
		    }
		}
	    } else {
		result = super.remove(prop,  item);
		if (result && prop == Child.FIELDING) {
		    if (getCurrentFielding() == item) {
			set(Value.CURRENT_FIELDING, null);
		    }
		}
	    }
	    requestBatchEnd();
	    return result;
	}
    }
    
    public Team getTeam() { return team; }

    public String getId() { return id; }
    private void setId(String i) {
	UUID uuid;
	try {
	    uuid = UUID.fromString(i);
	} catch (IllegalArgumentException iae) {
	    uuid = UUID.randomUUID();
	}
	id = uuid.toString();
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
    public void setRole(Role r) { set(Value.ROLE, r, Flag.CUSTOM); }
    public void setRoleToBase() { setRole(getBaseRole()); }

    public Role getBaseRole() { return (Role)get(Value.BASE_ROLE); }
    public void setBaseRole(Role b) { set(Value.BASE_ROLE, b); }

    public boolean isPenaltyBox() { return (Boolean)get(Value.PENALTY_BOX); }
    public void setPenaltyBox(boolean box) { set(Value.PENALTY_BOX, box); }

    public String getFlags() { return (String)get(Value.FLAGS); }
    public void setFlags(String f) { set(Value.FLAGS, f); }

    public Penalty getPenalty(String id) { return (Penalty)get(Child.PENALTY, id); }

    public void sortPenalties() {
	sortPenalties(new HashSet<String>(children.get(Child.PENALTY).keySet()));
    }
    private void sortPenalties(Set<String> oldIds) {
	if (!(Boolean)get(Value.SORT_PENALTIES)) { return; }
	requestBatchStart();
	List<Penalty> penalties = new ArrayList<Penalty>();
	for (ValueWithId p : getAll(Child.PENALTY)) {
	    if (!FO_EXP_ID.equals(((Penalty)p).getProviderId())) {
		penalties.add((Penalty)p);
	    } else {
		oldIds.remove(FO_EXP_ID);
	    }
	}
        Collections.sort(penalties, new Comparator<Penalty>() {
            @Override
            public int compare(Penalty a, Penalty b) {
                int periodSort = Integer.valueOf(a.getPeriod()).compareTo(b.getPeriod());

                if(periodSort != 0) {
                    return periodSort;
                } else {
                    return Integer.valueOf(a.getJam()).compareTo(b.getJam());
                }
            }
        });
        
        int num = 1;
        for (Penalty p : penalties) {
            String n = String.valueOf(num);
            children.get(Child.PENALTY).put(n, p);
            p.set(Penalty.Value.NUMBER, n);
            oldIds.remove(n);
            num++;
        }
        for (String i : oldIds) {
            //old entry has not been overwritten - remove it
            children.get(Child.PENALTY).remove(i);
            scoreBoardChange(new ScoreBoardEvent(this, Child.PENALTY, new PenaltyImpl(this, i), true));
        }
        requestBatchEnd();
    }
    
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

    protected String id;
    protected Team team;


    public class PenaltyImpl extends ScoreBoardEventProviderImpl implements Penalty {
        public PenaltyImpl(Skater s, String n) {
            super(s, Skater.Child.PENALTY, Penalty.class, Value.class);
            skater = s;
            values.put(Value.NUMBER, n);
            values.put(Value.ID, UUID.randomUUID().toString());
            values.put(Value.PERIOD, 0);
            values.put(Value.JAM, 0);
        }
        public String getId() { return (String)get(Value.ID); }
        public int getPeriod() { return (Integer)get(Value.PERIOD); }
        public int getJam() { return (Integer)get(Value.JAM); }
        public String getCode() { return (String)get(Value.CODE); }

        public String getProviderId() { return (String)get(Value.NUMBER); }

        public boolean set(PermanentProperty prop, Object value, Flag flag) {
            synchronized (coreLock) {
                if (!(prop instanceof Value) || (prop == Value.ID && flag != Flag.FORCE)) { return false; }
                requestBatchStart();
                boolean result = super.set(prop, value, flag);
                if (result) { 
                    if (prop == Value.NUMBER) {
                	scoreBoardChange(new ScoreBoardEvent(skater, Skater.Child.PENALTY, this, false));
                    } else if (prop == Value.PERIOD || prop == Value.JAM) {
                	skater.sortPenalties();
                    }
                }
                requestBatchEnd();
                return result;
	    }
        }
        
        protected Skater skater;
    }

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
