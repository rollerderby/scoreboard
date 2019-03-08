package com.carolinarollergirls.scoreboard.core.impl;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Map;
import java.util.HashMap;

import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.FloorPosition;
import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.Role;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.ScoringTrip;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.rules.Rule;

public class TeamImpl extends ScoreBoardEventProviderImpl implements Team {
    public TeamImpl(ScoreBoard sb, String i) {
        super(sb, Value.ID, ScoreBoard.Child.TEAM, Team.class, Value.class, Child.class, Command.class);
        set(Value.ID, i);
        for (FloorPosition fp : FloorPosition.values()) {
            add(Child.POSITION, new PositionImpl(this, fp));
        }
        addWriteProtection(Child.POSITION);
        addReference(new ElementReference(Value.RUNNING_OR_UPCOMING_TEAM_JAM, TeamJam.class, null));
        addReference(new ElementReference(Value.RUNNING_OR_ENDED_TEAM_JAM, TeamJam.class, null));
        addReference(new ElementReference(Value.LAST_ENDED_TEAM_JAM, TeamJam.class, null));
        addReference(new IndirectValueReference(this, Value.CURRENT_TRIP, this, Value.RUNNING_OR_ENDED_TEAM_JAM,
                TeamJam.Value.CURRENT_TRIP, true, null));
        addReference(new IndirectValueReference(this, Value.SCORE, this, Value.RUNNING_OR_ENDED_TEAM_JAM,
                TeamJam.Value.TOTAL_SCORE, true, 0));
        addReference(new IndirectValueReference(this, Value.JAM_SCORE, this, Value.RUNNING_OR_ENDED_TEAM_JAM,
                TeamJam.Value.JAM_SCORE, true, 0));
        addReference(new IndirectValueReference(this, Value.LAST_SCORE, this, Value.RUNNING_OR_ENDED_TEAM_JAM,
                TeamJam.Value.LAST_SCORE, true, 0));
        addReference(new IndirectValueReference(this, Value.TRIP_SCORE, this, Value.CURRENT_TRIP,
                ScoringTrip.Value.SCORE, false, 0));
        addReference(new IndirectValueReference(this, Value.LOST, this, Value.RUNNING_OR_ENDED_TEAM_JAM,
                TeamJam.Value.LOST, false, false));
        addReference(new IndirectValueReference(this, Value.LEAD, this, Value.RUNNING_OR_ENDED_TEAM_JAM,
                TeamJam.Value.LEAD, false, false));
        addReference(new IndirectValueReference(this, Value.CALLOFF, this, Value.RUNNING_OR_ENDED_TEAM_JAM,
                TeamJam.Value.CALLOFF, false, false));
        addReference(new IndirectValueReference(this, Value.INJURY, this, Value.RUNNING_OR_ENDED_TEAM_JAM,
                TeamJam.Value.INJURY, false, false));
        addReference(new IndirectValueReference(this, Value.NO_INITIAL, this, Value.RUNNING_OR_ENDED_TEAM_JAM,
                TeamJam.Value.NO_INITIAL, false, false));
        addReference(new IndirectValueReference(this, Value.DISPLAY_LEAD, this, Value.RUNNING_OR_ENDED_TEAM_JAM,
                TeamJam.Value.DISPLAY_LEAD, false, false));
        addReference(new IndirectValueReference(this, Value.NO_PIVOT, this, Value.RUNNING_OR_UPCOMING_TEAM_JAM,
                TeamJam.Value.NO_PIVOT, false, true));
        addReference(new IndirectValueReference(this, Value.STAR_PASS, this, Value.RUNNING_OR_UPCOMING_TEAM_JAM,
                TeamJam.Value.STAR_PASS, false, false));
        addReference(new IndirectValueReference(this, Value.STAR_PASS_TRIP, this, Value.RUNNING_OR_UPCOMING_TEAM_JAM,
                TeamJam.Value.STAR_PASS_TRIP, false, false));
    }

    protected Object computeValue(PermanentProperty prop, Object value, Object last, Flag flag) {
        if(value instanceof Integer && (Integer)value < 0) { return 0; }
        if (prop == Value.TIMEOUTS && (Integer)value > scoreBoard.getRulesets().getInt(Rule.NUMBER_TIMEOUTS)) {
            return scoreBoard.getRulesets().getInt(Rule.NUMBER_TIMEOUTS);
        }
        if (prop == Value.OFFICIAL_REVIEWS && (Integer)value > scoreBoard.getRulesets().getInt(Rule.NUMBER_REVIEWS)) {
            return scoreBoard.getRulesets().getInt(Rule.NUMBER_REVIEWS);
        }
            return value;
    }
    protected void valueChanged(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == Value.RETAINED_OFFICIAL_REVIEW && (Boolean)value && getOfficialReviews() == 0) {
            setOfficialReviews(1);
        } else if (prop == Value.LEAD && (Boolean)value && scoreBoard.isInJam()) {
            if (getCurrentTrip().getNumber() == 1) {
                getRunningOrEndedTeamJam().addScoringTrip();
            }
            String otherId = getId().equals(Team.ID_1) ? Team.ID_2 : Team.ID_1;
            Team otherTeam = getScoreBoard().getTeam(otherId);
            if (otherTeam.isLead()) {
                otherTeam.set(Value.LEAD, false);
            }
        } else if (prop == Value.STAR_PASS) {
            if (getPosition(FloorPosition.JAMMER).getSkater() != null) {
                getPosition(FloorPosition.JAMMER).getSkater().setRole(FloorPosition.JAMMER.getRole(getRunningOrUpcomingTeamJam()));
            }
            if (getPosition(FloorPosition.PIVOT).getSkater() != null) {
                getPosition(FloorPosition.PIVOT).getSkater().setRole(FloorPosition.PIVOT.getRole(getRunningOrUpcomingTeamJam()));
            }
            if ((Boolean)value && isLead()) {
                set(Value.LOST, true);
            }
        } else if ((prop == Value.CALLOFF || prop == Value.INJURY) && scoreBoard.isInJam() && (Boolean)value) {
            scoreBoard.stopJamTO();
        }
    }

    public void execute(CommandProperty prop) {
        switch((Command)prop) {
        case ADD_TRIP:
            getRunningOrEndedTeamJam().addScoringTrip();
            break;
        case REMOVE_TRIP:
            getRunningOrEndedTeamJam().removeScoringTrip();
            break;
        case OFFICIAL_REVIEW:
            officialReview();
            break;
        case TIMEOUT:
            timeout();
            break;
        }
    }

    public ValueWithId create(AddRemoveProperty prop, String id) {
        synchronized (coreLock) {
            switch((Child)prop) {
            case ALTERNATE_NAME:
                return new AlternateNameImpl(this, id, "");
            case COLOR:
                return new ColorImpl(this, id, "");
            case SKATER:
                return new SkaterImpl(this, id, "", "", "");
            case POSITION:
                return null;
            case BOX_TRIP:
                return new BoxTripImpl(this, id);
            }
            return null;
        }
    }

    protected void itemRemoved(AddRemoveProperty prop, ValueWithId item) {
        if (prop == Child.SKATER) {
            ((Skater)item).unlink();
        }
    }

    public ScoreBoard getScoreBoard() { return scoreBoard; }

    public void reset() {
        synchronized (coreLock) {
            setName(DEFAULT_NAME_PREFIX + getId());
            setLogo(DEFAULT_LOGO);
            set(Value.RUNNING_OR_UPCOMING_TEAM_JAM, null);
            set(Value.RUNNING_OR_ENDED_TEAM_JAM, null);
            set(Value.LAST_ENDED_TEAM_JAM, null);
            resetTimeouts(true);

            for (ValueWithId p : getAll(Child.POSITION)) {
                ((Position)p).reset();
            }
            removeAll(Child.BOX_TRIP);
            removeAll(Child.ALTERNATE_NAME);
            removeAll(Child.COLOR);
            removeAll(Child.SKATER);
        }
    }

    public String getName() { return (String)get(Value.NAME); }
    public void setName(String n) { set(Value.NAME, n); }

    public void startJam() {
        synchronized (coreLock) {
            updateTeamJams();
        }
    }

    public void stopJam() {
        synchronized (coreLock) {
            requestBatchStart();
            if (isDisplayLead() && !scoreBoard.getClock(Clock.ID_JAM).isTimeAtEnd()) {
                set(Value.CALLOFF, true);
            }
            
            updateTeamJams();

            Map<Skater, Role> toField = new HashMap<Skater, Role>();
            TeamJam upcomingTJ = getRunningOrUpcomingTeamJam();
            TeamJam endedTJ = getRunningOrEndedTeamJam();
            for (FloorPosition fp : FloorPosition.values()) {
                Skater s = endedTJ.getFielding(fp).getSkater();
                if (s != null && (endedTJ.getFielding(fp).isInBox() || s.hasUnservedPenalties())) {
                    if (fp.getRole(endedTJ) != fp.getRole(upcomingTJ)) {
                        toField.put(s, fp.getRole(endedTJ));
                    } else {
                        upcomingTJ.getFielding(fp).setSkater(s);
                    }
                } else if (s != null) {
                    s.set(Skater.Value.CURRENT_FIELDING, null, Flag.INTERNAL);
                }
            }            
            nextReplacedBlocker = FloorPosition.PIVOT;
            for (Skater s : toField.keySet()) {
                field(s, toField.get(s));
            }
            requestBatchEnd();
        }
    }

    public TeamSnapshot snapshot() {
        synchronized (coreLock) {
            return new TeamSnapshotImpl(this);
        }
    }
    public void restoreSnapshot(TeamSnapshot s) {
        synchronized (coreLock) {
            if (s.getId() != getId()) {	return; }
            setTimeouts(s.getTimeouts());
            setOfficialReviews(s.getOfficialReviews());
            setInTimeout(s.inTimeout());
            setInOfficialReview(s.inOfficialReview());
            for (ValueWithId skater : getAll(Child.SKATER)) {
                ((Skater)skater).restoreSnapshot(s.getSkaterSnapshot(skater.getId()));
            }
            updateTeamJams();
        }
    }

    public AlternateName getAlternateName(String i) { return (AlternateName)get(Child.ALTERNATE_NAME, i); }
    public void setAlternateName(String i, String n) {
        synchronized (coreLock) {
            requestBatchStart();
            ((AlternateName)getOrCreate(Child.ALTERNATE_NAME, i)).setName(n);
            requestBatchEnd();
        }
    }
    public void removeAlternateName(String i) { remove(Child.ALTERNATE_NAME, getAlternateName(i)); }

    public Color getColor(String i) { return (Color)get(Child.COLOR, i); }
    public void setColor(String i, String c) {
        synchronized (coreLock) {
            requestBatchStart();
            ((Color)getOrCreate(Child.COLOR, i)).setColor(c);
            requestBatchEnd();
        }
    }
    public void removeColor(String i) { remove(Child.COLOR, getColor(i)); }

    public String getLogo() { return (String)get(Value.LOGO); }
    public void setLogo(String l) { set(Value.LOGO, l); }

    public void timeout() {
        synchronized (coreLock) {
            if (getTimeouts() > 0) {
                getScoreBoard().setTimeoutType(this, false);
                changeTimeouts(-1);
            }
        }
    }
    public void officialReview() {
        synchronized (coreLock) {
            if (getOfficialReviews() > 0) {
                getScoreBoard().setTimeoutType(this, true);
                changeOfficialReviews(-1);
            }
        }
    }

    public TeamJam getRunningOrUpcomingTeamJam() { return (TeamJam)get(Value.RUNNING_OR_UPCOMING_TEAM_JAM); }
    public TeamJam getRunningOrEndedTeamJam() { return (TeamJam)get(Value.RUNNING_OR_ENDED_TEAM_JAM); }
    public TeamJam getLastEndedTeamJam() { return (TeamJam)get(Value.LAST_ENDED_TEAM_JAM); }
    public void updateTeamJams() {
        synchronized (coreLock) {
            requestBatchStart();
            set(Value.RUNNING_OR_ENDED_TEAM_JAM, scoreBoard.getCurrentPeriod().getCurrentJam().getTeamJam(getId()), Flag.INTERNAL);
            set(Value.RUNNING_OR_UPCOMING_TEAM_JAM,
                    scoreBoard.isInJam() ? getRunningOrEndedTeamJam() : getRunningOrEndedTeamJam().getNext(), Flag.INTERNAL);
            set(Value.LAST_ENDED_TEAM_JAM, getRunningOrUpcomingTeamJam().getPrevious(), Flag.INTERNAL);
            for (ValueWithId p : getAll(Child.POSITION)) {
                ((Position)p).updateCurrentFielding();
            }
            requestBatchEnd();
        }
    }


    public int getScore() { return (Integer)get(Value.SCORE); }

    public ScoringTrip getCurrentTrip() { return (ScoringTrip)get(Value.CURRENT_TRIP); }

    public boolean inTimeout() { return (Boolean)get(Value.IN_TIMEOUT); }
    public void setInTimeout(boolean b) { set(Value.IN_TIMEOUT, b); }

    public boolean inOfficialReview() { return (Boolean)get(Value.IN_OFFICIAL_REVIEW); }
    public void setInOfficialReview(boolean b) { set(Value.IN_OFFICIAL_REVIEW, b); }

    public boolean retainedOfficialReview() { return (Boolean)get(Value.RETAINED_OFFICIAL_REVIEW); }
    public void setRetainedOfficialReview(boolean b) { set(Value.RETAINED_OFFICIAL_REVIEW, b); }

    public int getTimeouts() { return (Integer)get(Value.TIMEOUTS); }
    public void setTimeouts(int t) { set(Value.TIMEOUTS, t); }
    public void changeTimeouts(int c) { set(Value.TIMEOUTS, c, Flag.CHANGE); } 
    public int getOfficialReviews() { return (Integer)get(Value.OFFICIAL_REVIEWS); }
    public void setOfficialReviews(int r) { set(Value.OFFICIAL_REVIEWS, r); }
    public void changeOfficialReviews(int c) { set(Value.OFFICIAL_REVIEWS, c, Flag.CHANGE); }
    public void resetTimeouts(boolean gameStart) {
        synchronized (coreLock) {
            setInTimeout(false);
            setInOfficialReview(false);
            if (gameStart || scoreBoard.getRulesets().getBoolean(Rule.TIMEOUTS_PER_PERIOD)) {
                setTimeouts(scoreBoard.getRulesets().getInt(Rule.NUMBER_TIMEOUTS));
            }
            if (gameStart || scoreBoard.getRulesets().getBoolean(Rule.REVIEWS_PER_PERIOD)) {
                setOfficialReviews(scoreBoard.getRulesets().getInt(Rule.NUMBER_REVIEWS));
                setRetainedOfficialReview(false);
            }
        }
    }

    public Skater getSkater(String id) { return (Skater)get(Child.SKATER, id); }
    public Skater addSkater(String id) { return (Skater)getOrCreate(Child.SKATER, id); }
    public Skater addSkater(String id, String n, String num, String flags) {
        synchronized (coreLock) {
            Skater s = new SkaterImpl(this, id, n, num, flags);
            addSkater(s);
            return s;
        }
    }
    public void addSkater(Skater skater) { add(Child.SKATER, skater); }
    public void removeSkater(String id) { remove(Child.SKATER, id); }

    public Position getPosition(FloorPosition fp) { return fp == null ? null : (Position)get(Child.POSITION, fp.toString()); }

    public void field(Skater s, Role r) {
        synchronized (coreLock) {
            if (s == null) { return; }
            requestBatchStart();
            if (s.getPosition() == getPosition(FloorPosition.PIVOT)) {
                setNoPivot(r != Role.PIVOT);
                if (r == Role.BLOCKER || r == Role.PIVOT) {
                    s.setRole(r);
                }
            }
            if (s.getRole() != r) {
                Position p = getAvailablePosition(r);
                if (r == Role.PIVOT && p != null) {
                    if (p.getSkater() != null && (hasNoPivot() || s.getRole() == Role.BLOCKER)) {
                        // If we are moving a blocker to pivot, move the previous pivot to blocker
                        // If we are replacing a blocker from the pivot spot,
                        //  see if we have a blocker spot available for them instead
                        Position p2;
                        if (s.getRole() == Role.BLOCKER) {
                            p2 = s.getPosition();
                        } else {
                            p2 = getAvailablePosition(Role.BLOCKER);
                        }
                        p2.setSkater(p.getSkater());
                    }
                    setNoPivot(false);
                }
                if (p != null) { p.setSkater(s); }
                else { s.removeCurrentFielding(); }
            }
            requestBatchEnd();
        }
    }
    private Position getAvailablePosition(Role r) {
        switch (r) {
        case JAMMER:
            if (isStarPass()) {
                return getPosition(FloorPosition.PIVOT);
            } else {
                return getPosition(FloorPosition.JAMMER);
            }
        case PIVOT:
            if (isStarPass()) {
                return null;
            } else {
                return getPosition(FloorPosition.PIVOT);
            }
        case BLOCKER:
            Position[] ps = {getPosition(FloorPosition.BLOCKER1),
                    getPosition(FloorPosition.BLOCKER2),
                    getPosition(FloorPosition.BLOCKER3)};
            for (Position p : ps) {
                if (p.getSkater() == null) { 
                    return p; 
                }
            }
            Position fourth = getPosition(isStarPass() ? FloorPosition.JAMMER : FloorPosition.PIVOT);
            if (fourth.getSkater() == null) {
                return fourth;
            }
            int tries = 0;
            do {
                if (++tries > 4) { return null; }
                switch (nextReplacedBlocker) {
                case BLOCKER1:
                    nextReplacedBlocker = FloorPosition.BLOCKER2;
                    break;
                case BLOCKER2:
                    nextReplacedBlocker = FloorPosition.BLOCKER3;
                    break;
                case BLOCKER3:
                    nextReplacedBlocker = (hasNoPivot() && !isStarPass()) ? FloorPosition.PIVOT : FloorPosition.BLOCKER1;
                    break;
                case PIVOT:
                    nextReplacedBlocker = FloorPosition.BLOCKER1;
                    break;
                default:
                    break;
                }
            } while(getPosition(nextReplacedBlocker).isPenaltyBox());
            return getPosition(nextReplacedBlocker);
        default:
            return null;
        }
    }

    public boolean isLost() { return (Boolean)get(Value.LOST); }
    public boolean isLead() { return (Boolean)get(Value.LEAD); }
    public boolean isCalloff() { return (Boolean)get(Value.CALLOFF); }
    public boolean isInjury() { return (Boolean)get(Value.INJURY); }
    public boolean isDisplayLead() { return (Boolean)get(Value.DISPLAY_LEAD); }

    public boolean isStarPass() { return (Boolean)get(Value.STAR_PASS); }
    public void setStarPass(boolean sp) { set(Value.STAR_PASS, sp); }

    public boolean hasNoPivot() { return (Boolean)get(Value.NO_PIVOT); }
    private void setNoPivot(boolean noPivot) { set(Value.NO_PIVOT, noPivot); }

    FloorPosition nextReplacedBlocker = FloorPosition.PIVOT;

    public static final String DEFAULT_NAME_PREFIX = "Team ";
    public static final String DEFAULT_LOGO = "";

    public class AlternateNameImpl extends ScoreBoardEventProviderImpl implements AlternateName {
        public AlternateNameImpl(Team t, String i, String n) {
            super(t, Value.ID, Team.Child.ALTERNATE_NAME, AlternateName.class, Value.class);
            team = t;
            set(Value.ID, i);
            setName(n);
        }
        public String getName() { return (String)get(Value.NAME); }
        public void setName(String n) { set(Value.NAME, n); }

        public Team getTeam() { return team; }

        protected Team team;
    }

    public class ColorImpl extends ScoreBoardEventProviderImpl implements Color {
        public ColorImpl(Team t, String i, String c) {
            super(t, Value.ID, Team.Child.COLOR, Color.class, Value.class);
            team = t;
            set(Value.ID, i);
            setColor(c);
        }
        public String getColor() { return (String)get(Value.COLOR); }
        public void setColor(String c) { set(Value.COLOR, c); }

        public Team getTeam() { return team; }

        protected Team team;
    }

    public static class TeamSnapshotImpl implements TeamSnapshot {
        private TeamSnapshotImpl(Team team) {
            id = team.getId();
            timeouts = team.getTimeouts();
            officialReviews = team.getOfficialReviews();
            inTimeout = team.inTimeout();
            inOfficialReview = team.inOfficialReview();
            skaterSnapshots = new HashMap<String, Skater.SkaterSnapshot>();
            for (ValueWithId skater : team.getAll(Child.SKATER)) {
                skaterSnapshots.put(skater.getId(), ((Skater)skater).snapshot());
            }
        }

        public String getId() { return id; }
        public int getTimeouts() { return timeouts; }
        public int getOfficialReviews() { return officialReviews; }
        public boolean inTimeout() { return inTimeout; }
        public boolean inOfficialReview() { return inOfficialReview; }
        public Map<String, Skater.SkaterSnapshot> getSkaterSnapshots() { return skaterSnapshots; }
        public Skater.SkaterSnapshot getSkaterSnapshot(String skater) { return skaterSnapshots.get(skater); }

        protected String id;
        protected int timeouts;
        protected int officialReviews;
        protected boolean inTimeout;
        protected boolean inOfficialReview;
        protected Map<String, Skater.SkaterSnapshot> skaterSnapshots;
    }
}
