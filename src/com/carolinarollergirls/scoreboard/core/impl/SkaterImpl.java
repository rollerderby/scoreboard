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
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.carolinarollergirls.scoreboard.core.FloorPosition;
import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.Role;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;

public class SkaterImpl extends DefaultScoreBoardEventProvider implements Skater {
    public SkaterImpl(Team t, String i, String n, String num, String flags) {
        team = t;
        setId(i);
        setName(n);
        setNumber(num);
        setFlags(flags);
        setRole(Role.BENCH);
        setBaseRole(Role.BENCH);
        setPenaltyBox(false);
    }

    public String getProviderName() { return PropertyConversion.toFrontend(Team.Child.SKATER); }
    public Class<Skater> getProviderClass() { return Skater.class; }
    public String getProviderId() { return getId(); }
    public ScoreBoardEventProvider getParent() { return team; }
    public List<Class<? extends Property>> getProperties() { return properties; }

    public Object valueFromString(PermanentProperty prop, String sValue) {
	if (prop == Value.PENALTY_BOX) { return Boolean.parseBoolean(sValue); }
	// Position and Role are not converted in order to distinguish source
	if (prop == Value.BASE_ROLE) { return Role.fromString(sValue); }
	return sValue;
    }

    public boolean set(PermanentProperty prop, Object value, Flag flag) {
	synchronized (coreLock) {
	    boolean result = false;
	    if (prop == Value.POSITION && value instanceof String) {
		team.field(this, team.getPosition(FloorPosition.fromString((String)value)));
	    } else if (prop == Value.ROLE && value instanceof String) {
		team.field(this, Role.fromString((String)value));
	    } else {
		requestBatchStart();
		Object last = get(prop);
		result = super.set(prop, value, flag);
		if(result) { 
		    if (prop == Value.PENALTY_BOX && getPosition() != null) {
			if ((Boolean)value && getPosition().getFloorPosition() == FloorPosition.JAMMER
				&& team.getLeadJammer().equals(Team.LEAD_LEAD)) {
			    team.setLeadJammer(Team.LEAD_LOST_LEAD);
			}
			getPosition().set(Position.Value.PENALTY_BOX, value);
		    }
		    if (getPosition() != null && (prop == Value.NAME || prop == Value.NUMBER || prop == Value.FLAGS)) {
			scoreBoardChange(new ScoreBoardEvent(getPosition(), Position.Value.valueOf(((Value)prop).name()), value, last));
		    }
		    if (prop == Value.BASE_ROLE && get(Value.ROLE) == last) {
			set(Value.ROLE, value);
		    }
		}
		requestBatchEnd();
	    }
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

    public Position getPosition() { return (Position)get(Value.POSITION); }
    public void setPosition(Position p) { set(Value.POSITION, p); }

    public Role getRole() { return (Role)get(Value.ROLE); }
    public void setRole(Role r) { set(Value.ROLE, r); }
    public void setRoleToBase() { setRole(getBaseRole()); }

    public Role getBaseRole() { return (Role)get(Value.BASE_ROLE); }
    public void setBaseRole(Role b) { set(Value.BASE_ROLE, b); }

    public boolean isPenaltyBox() { return (Boolean)get(Value.PENALTY_BOX); }
    public void setPenaltyBox(boolean box) { set(Value.PENALTY_BOX, box); }

    public String getFlags() { return (String)get(Value.FLAGS); }
    public void setFlags(String f) { set(Value.FLAGS, f); }

    public List<Penalty> getPenalties() {
        synchronized (coreLock) {
            return Collections.unmodifiableList(new ArrayList<Penalty>(penalties));
        }
    }
    public Penalty getFOEXPPenalty() { return foexpPenalty; }

    public void penalty(String id, String number, int period, int jam, String code) {
        synchronized (coreLock) {
            requestBatchStart();
            boolean foexp = "FO_EXP".equals(number);
            if (foexp && code != null) {
                Penalty prev = getFOEXPPenalty();
                if (prev != null) {
                    prev.set(Penalty.Value.PERIOD, period);
                    prev.set(Penalty.Value.JAM, jam);
                    prev.set(Penalty.Value.CODE, code);
                } else {
                    Penalty p = new PenaltyImpl(this, id == null ? UUID.randomUUID().toString() : id, period, jam, code);
                    p.addScoreBoardListener(this);
                    foexpPenalty = p;
                    p.set(Penalty.Value.NUMBER, number);
                }
                if (getBaseRole() == Role.BENCH) {
                    setBaseRole(Role.INELIGIBLE);
                }
            } else if (foexp && code == null) {
        	foexpPenalty.removeScoreBoardListener(this);
                scoreBoardChange(new ScoreBoardEvent(this, Child.PENALTY, foexpPenalty, true));
        	foexpPenalty = null;
        	if (getBaseRole() == Role.INELIGIBLE) {
        	    setBaseRole(Role.BENCH);
        	}
            } else if (id == null) {
                // Non FO/Exp, make sure skater has 9 or less regular penalties before adding another
                if (penalties.size() < 9 && code != null) {
                    PenaltyImpl dpm = new PenaltyImpl(this, UUID.randomUUID().toString(), period, jam, code);
                    dpm.addScoreBoardListener(this);
                    penalties.add(dpm);
                    sortPenalties();
                }
            } else {
                // Updating/Deleting existing Penalty.	Find it and process
                for (PenaltyImpl p2 : penalties) {
                    if (p2.getUuid().equals(id)) {
                        if (code != null) {
                            p2.set(Penalty.Value.PERIOD, period);
                            p2.set(Penalty.Value.JAM, jam);
                            p2.set(Penalty.Value.CODE, code);
                            sortPenalties();
                        } else {
                            penalties.remove(p2);
                            p2.removeScoreBoardListener(this);
                            sortPenalties();
                            p2.set(Penalty.Value.NUMBER, penalties.size()+1);
                            scoreBoardChange(new ScoreBoardEvent(this, Child.PENALTY, p2, true));
                        }
                        requestBatchEnd();
                        return;
                    }
                }
                // Penalty has an ID we don't have likely from the autosave, add it.
                PenaltyImpl dpm = new PenaltyImpl(this, id, period, jam, code);
                dpm.addScoreBoardListener(this);
                penalties.add(dpm);
                sortPenalties();
            }
            requestBatchEnd();
        }
    }

    private void sortPenalties() {
        Collections.sort(penalties, new Comparator<PenaltyImpl>() {

            @Override
            public int compare(PenaltyImpl a, PenaltyImpl b) {
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
            p.set(Penalty.Value.NUMBER, num);
            num++;
        }
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
            setPenaltyBox(s.isPenaltyBox());
        }
    }

    protected String id;
    protected Team team;
    protected List<PenaltyImpl> penalties = new LinkedList<PenaltyImpl>();
    protected Penalty foexpPenalty = null;

    protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	add(Value.class);
	add(Child.class);
    }};


    public class PenaltyImpl extends DefaultScoreBoardEventProvider implements Penalty {
        public PenaltyImpl(Skater s, String i, int p, int j, String c) {
            skater = s;
            values.put(Value.ID, i);
            values.put(Value.PERIOD, p);
            values.put(Value.JAM, j);
            values.put(Value.NUMBER, 0);
            values.put(Value.CODE, c);
        }
        public String getId() { return get(Value.NUMBER).toString(); }
        public String getUuid() { return (String)get(Value.ID); }
        public int getPeriod() { return (Integer)get(Value.PERIOD); }
        public int getJam() { return (Integer)get(Value.JAM); }
        public String getCode() { return (String)get(Value.CODE); }

        public String getProviderName() { return PropertyConversion.toFrontend(Skater.Child.PENALTY); }
        public Class<Penalty> getProviderClass() { return Penalty.class; }
        public String getProviderId() { return getId(); }
        public ScoreBoardEventProvider getParent() { return skater; }
        public List<Class<? extends Property>> getProperties() { return properties; }

        public boolean set(PermanentProperty prop, Object value, Flag flag) {
            synchronized (coreLock) {
                if (!(prop instanceof Value) || prop == Value.ID) { return false; }
                requestBatchStart();
                boolean result = super.set(prop, value, flag);
                    if (result && prop == Value.NUMBER) {
                        scoreBoardChange(new ScoreBoardEvent(skater, Skater.Child.PENALTY, this, false));
                }
                requestBatchEnd();
                return result;
	    }
        }
        
        protected Skater skater;

        protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
            add(Value.class);
        }};
    }

    public static class SkaterSnapshotImpl implements SkaterSnapshot {
        private SkaterSnapshotImpl(Skater skater) {
            id = skater.getId();
            position = skater.getPosition();
            role = skater.getRole();
            baseRole = skater.getBaseRole();
            box = skater.isPenaltyBox();
        }

        public String getId( ) { return id; }
        public Position getPosition() { return position; }
        public Role getRole() { return role; }
        public Role getBaseRole() { return baseRole; }
        public boolean isPenaltyBox() { return box; }

        protected String id;
        protected Position position;
        protected Role role;
        protected Role baseRole;
        protected boolean box;

    }

}
