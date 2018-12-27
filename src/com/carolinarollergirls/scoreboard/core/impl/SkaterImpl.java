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
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;

public class SkaterImpl extends DefaultScoreBoardEventProvider implements Skater {
    public SkaterImpl(Team t, String i, String n, String num, String flags) {
        team = t;
        setId(i);
        setName(n);
        setNumber(num);
        setFlags(flags);
    }

    public String getProviderName() { return PropertyConversion.toFrontend(Team.Child.SKATER); }
    public Class<Skater> getProviderClass() { return Skater.class; }
    public String getProviderId() { return getId(); }
    public ScoreBoardEventProvider getParent() { return team; }
    public List<Class<? extends Property>> getProperties() { return properties; }

    public Team getTeam() { return team; }

    public String getId() { return id; }
    public void setId(String i) {
        synchronized (coreLock) {
            UUID uuid;
            try {
                uuid = UUID.fromString(i);
            } catch (IllegalArgumentException iae) {
                uuid = UUID.randomUUID();
            }
            id = uuid.toString();
        }
    }

    public String getName() { return name; }
    public void setName(String n) {
        synchronized (coreLock) {
            String last = name;
            name = n;
            scoreBoardChange(new ScoreBoardEvent(this, Value.NAME, name, last));
        }
    }

    public String getNumber() { return number; }
    public void setNumber(String n) {
        synchronized (coreLock) {
            String last = number;
            number = n;
            scoreBoardChange(new ScoreBoardEvent(this, Value.NUMBER, number, last));
        }
    }

    public Position getPosition() { return position; }
    public void setPosition(Position p) {
        synchronized (coreLock) {
            if (p == position) {
                return;
            }

            Position last = position;
            position = p;
            scoreBoardChange(new ScoreBoardEvent(this, Value.POSITION, position, last));
        }
    }

    public Role getRole() { return role; }
    public void setRole(Role r) {
        synchronized (coreLock) {
            if (r == role) { return; }

            Role last = role;
            role = r;
            scoreBoardChange(new ScoreBoardEvent(this, Value.ROLE, role, last));
        }
    }
    public void setRoleToBase() { setRole(getBaseRole()); }


    public Role getBaseRole() { return baseRole; }
    public void setBaseRole(Role b) {
        synchronized (coreLock) {
            if (b == baseRole) { return; }

            Role last = baseRole;
            baseRole = b;
            scoreBoardChange(new ScoreBoardEvent(this, Value.BASE_ROLE, baseRole, last));
        }
    }

    public boolean isPenaltyBox() { return penaltyBox; }

    public void setPenaltyBox(boolean box) {
        synchronized (coreLock) {
            if (box == penaltyBox) {
                return;
            }

            requestBatchStart();

            Boolean last = new Boolean(penaltyBox);
            penaltyBox = box;
            scoreBoardChange(new ScoreBoardEvent(this, Value.PENALTY_BOX, new Boolean(penaltyBox), last));

            if (box && position.getFloorPosition() == FloorPosition.JAMMER && team.getLeadJammer().equals(Team.LEAD_LEAD)) {
                team.setLeadJammer(Team.LEAD_LOST_LEAD);
            }

            // Update Position
            if (position != null) {
        	position.setPenaltyBox(box);
            }

            requestBatchEnd();
        }
    }

    public String getFlags() { return flags; }

    public void setFlags(String f) {
        synchronized (coreLock) {
            String last = flags;
            flags = f;
            scoreBoardChange(new ScoreBoardEvent(this, Value.FLAGS, flags, last));
        }
    }

    public List<Penalty> getPenalties() {
        synchronized (coreLock) {
            return Collections.unmodifiableList(new ArrayList<Penalty>(penalties));
        }
    }
    public Penalty getFOEXPPenalty() { return foexp_penalty; }

    public void AddPenalty(String id, boolean foulout_explusion, int period, int jam, String code) {
        synchronized (coreLock) {
            requestBatchStart();
            if (foulout_explusion && code != null) {
                Penalty prev = foexp_penalty;
                id = UUID.randomUUID().toString();
                if (prev != null) {
                    id = prev.getId();
                }
                foexp_penalty = new PenaltyImpl(this, id, period, jam, code);
                if (getBaseRole() == Role.BENCH) {
                    setBaseRole(Role.INELIGIBLE);
                }
                scoreBoardChange(new ScoreBoardEvent(this, Value.PENALTY_FOEXP, foexp_penalty, null));
            } else if (foulout_explusion && code == null) {
                Penalty prev = foexp_penalty;
                foexp_penalty = null;
                setBaseRole(Role.BENCH);
                scoreBoardChange(new ScoreBoardEvent(this, Value.PENALTY_FOEXP, null, prev));
            } else if (id == null ) {
                id = UUID.randomUUID().toString();
                // Non FO/Exp, make sure skater has 9 or less regular penalties before adding another
                if (penalties.size() < 9) {
                    PenaltyImpl dpm = new PenaltyImpl(this, id, period, jam, code);
                    penalties.add(dpm);
                    sortPenalties();
                    scoreBoardChange(new ScoreBoardEvent(this, Child.PENALTY, dpm, false));
                }
            } else {
                // Updating/Deleting existing Penalty.	Find it and process
                for (PenaltyImpl p2 : penalties) {
                    if (p2.getId().equals(id)) {
                        if (code != null) {
                            p2.period = period;
                            p2.jam = jam;
                            p2.code = code;
                            sortPenalties();
                            scoreBoardChange(new ScoreBoardEvent(this, Child.PENALTY, p2, false));
                        } else {
                            penalties.remove(p2);
                            scoreBoardChange(new ScoreBoardEvent(this, Child.PENALTY, p2, true));
                        }
                        requestBatchEnd();
                        return;
                    }
                }
                // Penalty has an ID we don't have likely from the autosave, add it.
                PenaltyImpl dpm = new PenaltyImpl(this, id, period, jam, code);
                penalties.add(dpm);
                sortPenalties();
                scoreBoardChange(new ScoreBoardEvent(this, Child.PENALTY, dpm, false));
            }
            requestBatchEnd();
        }
    }

    private void sortPenalties() {
        Collections.sort(penalties, new Comparator<PenaltyImpl>() {

            @Override
            public int compare(PenaltyImpl a, PenaltyImpl b) {
                int periodSort = Integer.valueOf(a.period).compareTo(b.period);

                if(periodSort != 0) {
                    return periodSort;
                } else {
                    return Integer.valueOf(a.jam).compareTo(b.jam);
                }
            }

        });
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

    protected Team team;

    protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	add(Value.class);
	add(Child.class);
    }};

    protected static Object coreLock = ScoreBoardImpl.getCoreLock();

    protected String id;
    protected String name;
    protected String number;
    protected Position position = null;
    protected Role role = Role.BENCH;
    protected Role baseRole = Role.BENCH;
    protected boolean penaltyBox = false;
    protected String flags;
    protected List<PenaltyImpl> penalties = new LinkedList<PenaltyImpl>();
    protected Penalty foexp_penalty;

    protected boolean settingPositionSkater = false;

    public class PenaltyImpl extends DefaultScoreBoardEventProvider implements Penalty {
        public PenaltyImpl(Skater s, String i, int p, int j, String c) {
            skater = s;
            id = i;
            period = p;
            jam = j;
            code = c;
        }
        public String getId() { return id; }
        public int getPeriod() { return period; }
        public int getJam() { return jam; }
        public String getCode() { return code; }

        public String getProviderName() { return PropertyConversion.toFrontend(Skater.Child.PENALTY); }
        public Class<Penalty> getProviderClass() { return Penalty.class; }
        public String getProviderId() { return getId(); }
        public ScoreBoardEventProvider getParent() { return skater; }
        public List<Class<? extends Property>> getProperties() { return properties; }

        protected Skater skater;
        protected String id;
        protected int period;
        protected int jam;
        protected String code;

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
