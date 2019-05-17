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
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashMap;

import com.carolinarollergirls.scoreboard.core.BoxTrip;
import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.Fielding;
import com.carolinarollergirls.scoreboard.core.FloorPosition;
import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.Role;
import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.ScoringTrip;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.rules.Rule;

public class TeamImpl extends ScoreBoardEventProviderImpl implements Team {
    public TeamImpl(ScoreBoard sb, String i) {
        super(sb, Value.ID, i, ScoreBoard.Child.TEAM, Team.class, Value.class, Child.class, Command.class);
        for (FloorPosition fp : FloorPosition.values()) {
            add(Child.POSITION, new PositionImpl(this, fp));
        }
        addWriteProtection(Child.POSITION);
        setCopy(Value.CURRENT_TRIP, this, Value.RUNNING_OR_ENDED_TEAM_JAM, TeamJam.Value.CURRENT_TRIP, true);
        setCopy(Value.SCORE, this, Value.RUNNING_OR_ENDED_TEAM_JAM, TeamJam.Value.TOTAL_SCORE, true);
        setCopy(Value.JAM_SCORE, this, Value.RUNNING_OR_ENDED_TEAM_JAM, TeamJam.Value.JAM_SCORE, true);
        setCopy(Value.LAST_SCORE, this, Value.RUNNING_OR_ENDED_TEAM_JAM, TeamJam.Value.LAST_SCORE, true);
        setCopy(Value.TRIP_SCORE, this, Value.CURRENT_TRIP, ScoringTrip.Value.SCORE, false);
        setCopy(Value.LOST, this, Value.RUNNING_OR_ENDED_TEAM_JAM, TeamJam.Value.LOST, false);
        setCopy(Value.LEAD, this, Value.RUNNING_OR_ENDED_TEAM_JAM, TeamJam.Value.LEAD, false);
        setCopy(Value.CALLOFF, this, Value.RUNNING_OR_ENDED_TEAM_JAM, TeamJam.Value.CALLOFF, false);
        setCopy(Value.INJURY, this, Value.RUNNING_OR_ENDED_TEAM_JAM, TeamJam.Value.INJURY, false);
        setCopy(Value.NO_INITIAL, this, Value.RUNNING_OR_ENDED_TEAM_JAM, TeamJam.Value.NO_INITIAL, false);
        setCopy(Value.DISPLAY_LEAD, this, Value.RUNNING_OR_ENDED_TEAM_JAM, TeamJam.Value.DISPLAY_LEAD, false);
        setCopy(Value.NO_NAMED_PIVOT, this, Value.RUNNING_OR_UPCOMING_TEAM_JAM, TeamJam.Value.NO_NAMED_PIVOT, false);
        setCopy(Value.NO_PIVOT, this, Value.RUNNING_OR_UPCOMING_TEAM_JAM, TeamJam.Value.NO_PIVOT, false);
        setCopy(Value.STAR_PASS, this, Value.RUNNING_OR_ENDED_TEAM_JAM, TeamJam.Value.STAR_PASS, false);
        setCopy(Value.STAR_PASS_TRIP, this, Value.RUNNING_OR_ENDED_TEAM_JAM, TeamJam.Value.STAR_PASS_TRIP, false);

        sb.addScoreBoardListener(new ConditionalScoreBoardListener(Rulesets.class, Rulesets.Value.CURRENT_RULESET_ID, rulesetChangeListener));
    }

    @Override
    protected Object computeValue(PermanentProperty prop, Object value, Object last, Flag flag) {
        if(value instanceof Integer && (Integer)value < 0) { return 0; }
        if (prop == Value.TIMEOUTS && (Integer)value > scoreBoard.getRulesets().getInt(Rule.NUMBER_TIMEOUTS)) {
            return scoreBoard.getRulesets().getInt(Rule.NUMBER_TIMEOUTS);
        }
        if (prop == Value.OFFICIAL_REVIEWS && (Integer)value > scoreBoard.getRulesets().getInt(Rule.NUMBER_REVIEWS)) {
            return scoreBoard.getRulesets().getInt(Rule.NUMBER_REVIEWS);
        }
        if (prop == Value.TRIP_SCORE && flag != Flag.COPY && scoreBoard.isInJam() && (Integer)value > 0) {
            tripScoreTimerTask.cancel();
            tripScoreTimer.purge();
            tripScoreTimerTask = new TimerTask() {
                @Override
                public void run() {
                    execute(Command.ADD_TRIP);
                }
            };
            tripScoreTimer.schedule(tripScoreTimerTask, 4000);
        }
        return value;
    }
    @Override
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

    @Override
    public void execute(CommandProperty prop) {
        switch((Command)prop) {
        case ADD_TRIP:
            tripScoreTimerTask.cancel();
            getRunningOrEndedTeamJam().addScoringTrip();
            break;
        case REMOVE_TRIP:
            tripScoreTimerTask.cancel();
            getRunningOrEndedTeamJam().removeScoringTrip();
            break;
        case ADVANCE_FIELDINGS:
            advanceFieldings();
            break;
        case OFFICIAL_REVIEW:
            officialReview();
            break;
        case TIMEOUT:
            timeout();
            break;
        }
    }

    @Override
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

    @Override
    protected void itemRemoved(AddRemoveProperty prop, ValueWithId item) {
        if (prop == Child.SKATER) {
            ((Skater)item).unlink();
        }
    }

    @Override
    public ScoreBoard getScoreBoard() { return scoreBoard; }

    @Override
    public void reset() {
        synchronized (coreLock) {
            setName(DEFAULT_NAME_PREFIX + getId());
            setLogo(DEFAULT_LOGO);
            set(Value.RUNNING_OR_UPCOMING_TEAM_JAM, null);
            set(Value.RUNNING_OR_ENDED_TEAM_JAM, null);
            set(Value.LAST_ENDED_TEAM_JAM, null);
            set(Value.FIELDING_ADVANCE_PENDING, false);
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

    @Override
    public String getName() { return (String)get(Value.NAME); }
    @Override
    public void setName(String n) { set(Value.NAME, n); }

    @Override
    public void startJam() {
        synchronized (coreLock) {
            advanceFieldings(); // if this hasn't been manually triggered between jams, do it now
            updateTeamJams();
        }
    }

    @Override
    public void stopJam() {
        synchronized (coreLock) {
            requestBatchStart();
            if (isDisplayLead() && !scoreBoard.getClock(Clock.ID_JAM).isTimeAtEnd()) {
                set(Value.CALLOFF, true);
            }
            getCurrentTrip().set(ScoringTrip.Value.CURRENT, false);
            
            updateTeamJams();
            
            set(Value.FIELDING_ADVANCE_PENDING, true);
            
            Map<Skater, Role> toField = new HashMap<>();
            TeamJam upcomingTJ = getRunningOrUpcomingTeamJam();
            TeamJam endedTJ = getRunningOrEndedTeamJam();
            for (FloorPosition fp : FloorPosition.values()) {
                Skater s = endedTJ.getFielding(fp).getSkater();
                if (s != null && (endedTJ.getFielding(fp).isInBox() || s.hasUnservedPenalties())) {
                    if (fp.getRole(endedTJ) != fp.getRole(upcomingTJ)) {
                        toField.put(s, fp.getRole(endedTJ));
                    } else {
                        upcomingTJ.getFielding(fp).setSkater(s);
                        BoxTrip bt = endedTJ.getFielding(fp).getCurrentBoxTrip();
                        if (bt != null && bt.isCurrent()) {
                            bt.add(BoxTrip.Child.FIELDING, upcomingTJ.getFielding(fp));
                        }
                    }
                }
            }            
            nextReplacedBlocker = FloorPosition.PIVOT;
            for (Skater s : toField.keySet()) {
                field(s, toField.get(s), upcomingTJ);
                BoxTrip bt = s.getFielding(endedTJ).getCurrentBoxTrip();
                if (bt != null && bt.isCurrent()) {
                    bt.add(BoxTrip.Child.FIELDING, s.getFielding(upcomingTJ));
                }
            }
            
            for (ValueWithId s : getAll(Child.SKATER)) {
                ((Skater)s).updateEligibility();
            }
            requestBatchEnd();
        }
    }
    
    private void advanceFieldings() {
        if(!hasFieldingAdvancePending()) { return; }
        
        set(Value.FIELDING_ADVANCE_PENDING, false);
        
        for (ValueWithId v : getAll(Child.SKATER)) {
            Skater s = (Skater)v;
            s.set(Skater.Value.CURRENT_FIELDING, s.getFielding(getRunningOrUpcomingTeamJam()));
            s.setRole(s.getRole(getRunningOrUpcomingTeamJam()));
            s.updateEligibility();
        }
    }

    @Override
    public TeamSnapshot snapshot() {
        synchronized (coreLock) {
            return new TeamSnapshotImpl(this);
        }
    }
    @Override
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

    @Override
    public AlternateName getAlternateName(String i) { return (AlternateName)get(Child.ALTERNATE_NAME, i); }
    @Override
    public void setAlternateName(String i, String n) {
        synchronized (coreLock) {
            requestBatchStart();
            ((AlternateName)getOrCreate(Child.ALTERNATE_NAME, i)).setName(n);
            requestBatchEnd();
        }
    }
    @Override
    public void removeAlternateName(String i) { remove(Child.ALTERNATE_NAME, getAlternateName(i)); }

    @Override
    public Color getColor(String i) { return (Color)get(Child.COLOR, i); }
    @Override
    public void setColor(String i, String c) {
        synchronized (coreLock) {
            requestBatchStart();
            ((Color)getOrCreate(Child.COLOR, i)).setColor(c);
            requestBatchEnd();
        }
    }
    @Override
    public void removeColor(String i) { remove(Child.COLOR, getColor(i)); }

    @Override
    public String getLogo() { return (String)get(Value.LOGO); }
    @Override
    public void setLogo(String l) { set(Value.LOGO, l); }

    @Override
    public void timeout() {
        synchronized (coreLock) {
            if (getTimeouts() > 0) {
                getScoreBoard().setTimeoutType(this, false);
                changeTimeouts(-1);
            }
        }
    }
    @Override
    public void officialReview() {
        synchronized (coreLock) {
            if (getOfficialReviews() > 0) {
                getScoreBoard().setTimeoutType(this, true);
                changeOfficialReviews(-1);
            }
        }
    }

    @Override
    public TeamJam getRunningOrUpcomingTeamJam() { return (TeamJam)get(Value.RUNNING_OR_UPCOMING_TEAM_JAM); }
    @Override
    public TeamJam getRunningOrEndedTeamJam() { return (TeamJam)get(Value.RUNNING_OR_ENDED_TEAM_JAM); }
    @Override
    public TeamJam getLastEndedTeamJam() { return (TeamJam)get(Value.LAST_ENDED_TEAM_JAM); }
    @Override
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


    @Override
    public int getScore() { return (Integer)get(Value.SCORE); }

    @Override
    public ScoringTrip getCurrentTrip() { return (ScoringTrip)get(Value.CURRENT_TRIP); }

    @Override
    public boolean inTimeout() { return (Boolean)get(Value.IN_TIMEOUT); }
    @Override
    public void setInTimeout(boolean b) { set(Value.IN_TIMEOUT, b); }

    @Override
    public boolean inOfficialReview() { return (Boolean)get(Value.IN_OFFICIAL_REVIEW); }
    @Override
    public void setInOfficialReview(boolean b) { set(Value.IN_OFFICIAL_REVIEW, b); }

    @Override
    public boolean retainedOfficialReview() { return (Boolean)get(Value.RETAINED_OFFICIAL_REVIEW); }
    @Override
    public void setRetainedOfficialReview(boolean b) { set(Value.RETAINED_OFFICIAL_REVIEW, b); }

    @Override
    public int getTimeouts() { return (Integer)get(Value.TIMEOUTS); }
    @Override
    public void setTimeouts(int t) { set(Value.TIMEOUTS, t); }
    @Override
    public void changeTimeouts(int c) { set(Value.TIMEOUTS, c, Flag.CHANGE); } 
    @Override
    public int getOfficialReviews() { return (Integer)get(Value.OFFICIAL_REVIEWS); }
    @Override
    public void setOfficialReviews(int r) { set(Value.OFFICIAL_REVIEWS, r); }
    @Override
    public void changeOfficialReviews(int c) { set(Value.OFFICIAL_REVIEWS, c, Flag.CHANGE); }
    @Override
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

    protected ScoreBoardListener rulesetChangeListener = new ScoreBoardListener() {
        @Override
        public void scoreBoardChange(ScoreBoardEvent event) {
            setTimeouts(scoreBoard.getRulesets().getInt(Rule.NUMBER_TIMEOUTS));
            setOfficialReviews(scoreBoard.getRulesets().getInt(Rule.NUMBER_REVIEWS));
        }
    };

    @Override
    public Skater getSkater(String id) { return (Skater)get(Child.SKATER, id); }
    public Skater addSkater(String id) { return (Skater)getOrCreate(Child.SKATER, id); }
    @Override
    public Skater addSkater(String id, String n, String num, String flags) {
        synchronized (coreLock) {
            Skater s = new SkaterImpl(this, id, n, num, flags);
            addSkater(s);
            return s;
        }
    }
    @Override
    public void addSkater(Skater skater) { add(Child.SKATER, skater); }
    @Override
    public void removeSkater(String id) { remove(Child.SKATER, id); }

    @Override
    public Position getPosition(FloorPosition fp) { return fp == null ? null : (Position)get(Child.POSITION, fp.toString()); }

    @Override
    public void field(Skater s, Role r) {
        field(s, r, hasFieldingAdvancePending() ? getRunningOrEndedTeamJam() : getRunningOrUpcomingTeamJam());
    }
    @Override
    public void field(Skater s, Role r, TeamJam tj) {
        synchronized (coreLock) {
            if (s == null) { return; }
            requestBatchStart();
            if (s.getFielding(tj) != null && 
                    s.getFielding(tj).getPosition() == getPosition(FloorPosition.PIVOT)) {
                tj.setNoNamedPivot(r != Role.PIVOT);
                if ((r == Role.BLOCKER || r == Role.PIVOT) &&
                        ((tj.isRunningOrEnded() && hasFieldingAdvancePending()) ||
                                (tj.isRunningOrUpcoming() && !hasFieldingAdvancePending()))) {
                    s.setRole(r);
                }
            }
            if (s.getFielding(tj) == null || s.getRole(tj) != r) {
                Fielding f = getAvailableFielding(r, tj);
                if (r == Role.PIVOT && f != null) {
                    if (f.getSkater() != null && (tj.hasNoNamedPivot() || s.getRole(tj) == Role.BLOCKER)) {
                        // If we are moving a blocker to pivot, move the previous pivot to blocker
                        // If we are replacing a blocker from the pivot spot,
                        //  see if we have a blocker spot available for them instead
                        Fielding f2;
                        if (s.getRole(tj) == Role.BLOCKER) {
                            f2 = s.getFielding(tj);
                        } else {
                            f2 = getAvailableFielding(Role.BLOCKER, tj);
                        }
                        f2.setSkater(f.getSkater());
                    }
                    f.setSkater(s);
                    tj.setNoNamedPivot(false);
                } else if (f != null) { 
                    f.setSkater(s);
                } else { 
                    s.remove(Skater.Child.FIELDING, s.getFielding(tj));
                }
            }
            requestBatchEnd();
        }
    }
    private Fielding getAvailableFielding(Role r, TeamJam tj) {
        switch (r) {
        case JAMMER:
            if (tj.isStarPass()) {
                return tj.getFielding(FloorPosition.PIVOT);
            } else {
                return tj.getFielding(FloorPosition.JAMMER);
            }
        case PIVOT:
            if (tj.isStarPass()) {
                return null;
            } else {
                return tj.getFielding(FloorPosition.PIVOT);
            }
        case BLOCKER:
            Fielding[] fs = {tj.getFielding(FloorPosition.BLOCKER1),
                    tj.getFielding(FloorPosition.BLOCKER2),
                    tj.getFielding(FloorPosition.BLOCKER3)};
            for (Fielding f : fs) {
                if (f.getSkater() == null) { 
                    return f; 
                }
            }
            Fielding fourth = tj.getFielding(tj.isStarPass() ? FloorPosition.JAMMER : FloorPosition.PIVOT);
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
                    nextReplacedBlocker = (tj.hasNoNamedPivot() && !tj.isStarPass()) ? FloorPosition.PIVOT : FloorPosition.BLOCKER1;
                    break;
                case PIVOT:
                    nextReplacedBlocker = FloorPosition.BLOCKER1;
                    break;
                default:
                    break;
                }
            } while(tj.getFielding(nextReplacedBlocker).isInBox());
            return tj.getFielding(nextReplacedBlocker);
        default:
            return null;
        }
    }
    @Override
    public boolean hasFieldingAdvancePending() { return (Boolean)get(Value.FIELDING_ADVANCE_PENDING); }

    @Override
    public boolean isLost() { return (Boolean)get(Value.LOST); }
    @Override
    public boolean isLead() { return (Boolean)get(Value.LEAD); }
    @Override
    public boolean isCalloff() { return (Boolean)get(Value.CALLOFF); }
    @Override
    public boolean isInjury() { return (Boolean)get(Value.INJURY); }
    @Override
    public boolean isDisplayLead() { return (Boolean)get(Value.DISPLAY_LEAD); }

    protected boolean isFieldingStarPass() {
        if (hasFieldingAdvancePending()) {
            return getRunningOrEndedTeamJam().isStarPass();
        } else {
            return getRunningOrUpcomingTeamJam().isStarPass();
        }
    }
    @Override
    public boolean isStarPass() { return (Boolean)get(Value.STAR_PASS); }
    public void setStarPass(boolean sp) { set(Value.STAR_PASS, sp); }

    @Override
    public boolean hasNoNamedPivot() { return (Boolean)get(Value.NO_NAMED_PIVOT); }

    FloorPosition nextReplacedBlocker = FloorPosition.PIVOT;
    
    private Timer tripScoreTimer = new Timer();
    private TimerTask tripScoreTimerTask = new TimerTask() {
        @Override
        public void run() {} // dummy, so the variable is not null at the first score entry
    };

    public static final String DEFAULT_NAME_PREFIX = "Team ";
    public static final String DEFAULT_LOGO = "";

    public class AlternateNameImpl extends ScoreBoardEventProviderImpl implements AlternateName {
        public AlternateNameImpl(Team t, String i, String n) {
            super(t, Value.ID, i, Team.Child.ALTERNATE_NAME, AlternateName.class, Value.class);
            team = t;
            setName(n);
        }
        @Override
        public String getName() { return (String)get(Value.NAME); }
        @Override
        public void setName(String n) { set(Value.NAME, n); }

        @Override
        public Team getTeam() { return team; }

        protected Team team;
    }

    public class ColorImpl extends ScoreBoardEventProviderImpl implements Color {
        public ColorImpl(Team t, String i, String c) {
            super(t, Value.ID, i, Team.Child.COLOR, Color.class, Value.class);
            team = t;
            setColor(c);
        }
        @Override
        public String getColor() { return (String)get(Value.COLOR); }
        @Override
        public void setColor(String c) { set(Value.COLOR, c); }

        @Override
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
            skaterSnapshots = new HashMap<>();
            for (ValueWithId skater : team.getAll(Child.SKATER)) {
                skaterSnapshots.put(skater.getId(), ((Skater)skater).snapshot());
            }
        }

        @Override
        public String getId() { return id; }
        @Override
        public int getTimeouts() { return timeouts; }
        @Override
        public int getOfficialReviews() { return officialReviews; }
        @Override
        public boolean inTimeout() { return inTimeout; }
        @Override
        public boolean inOfficialReview() { return inOfficialReview; }
        @Override
        public Map<String, Skater.SkaterSnapshot> getSkaterSnapshots() { return skaterSnapshots; }
        @Override
        public Skater.SkaterSnapshot getSkaterSnapshot(String skater) { return skaterSnapshots.get(skater); }

        protected String id;
        protected int timeouts;
        protected int officialReviews;
        protected boolean inTimeout;
        protected boolean inOfficialReview;
        protected Map<String, Skater.SkaterSnapshot> skaterSnapshots;
    }
}
