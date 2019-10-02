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
import com.carolinarollergirls.scoreboard.core.PreparedTeam;
import com.carolinarollergirls.scoreboard.core.PreparedTeam.PreparedTeamSkater;
import com.carolinarollergirls.scoreboard.core.Role;
import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.ScoringTrip;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.core.Timeout;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

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
        setCopy(Value.NO_PIVOT, this, Value.RUNNING_OR_UPCOMING_TEAM_JAM, TeamJam.Value.NO_PIVOT, false);
        setCopy(Value.STAR_PASS, this, Value.RUNNING_OR_ENDED_TEAM_JAM, TeamJam.Value.STAR_PASS, false);
        setCopy(Value.STAR_PASS_TRIP, this, Value.RUNNING_OR_ENDED_TEAM_JAM, TeamJam.Value.STAR_PASS_TRIP, false);
        setRecalculated(Value.IN_TIMEOUT).addIndirectSource(sb, ScoreBoard.Value.CURRENT_TIMEOUT, Timeout.Value.OWNER)
            .addIndirectSource(sb, ScoreBoard.Value.CURRENT_TIMEOUT, Timeout.Value.REVIEW)
            .addIndirectSource(sb, ScoreBoard.Value.CURRENT_TIMEOUT, Timeout.Value.RUNNING);
        setRecalculated(Value.IN_OFFICIAL_REVIEW).addIndirectSource(sb, ScoreBoard.Value.CURRENT_TIMEOUT, Timeout.Value.OWNER)
            .addIndirectSource(sb, ScoreBoard.Value.CURRENT_TIMEOUT, Timeout.Value.REVIEW)
            .addIndirectSource(sb, ScoreBoard.Value.CURRENT_TIMEOUT, Timeout.Value.RUNNING);
        addWriteProtectionOverride(Value.TIMEOUTS, Flag.INTERNAL);
        addWriteProtectionOverride(Value.OFFICIAL_REVIEWS, Flag.INTERNAL);
        addWriteProtectionOverride(Value.LAST_REVIEW, Flag.INTERNAL);
        setCopy(Value.RETAINED_OFFICIAL_REVIEW, this, Value.LAST_REVIEW, Timeout.Value.RETAINED_REVIEW, false);

        sb.addScoreBoardListener(new ConditionalScoreBoardListener(Rulesets.class, Rulesets.Value.CURRENT_RULESET, rulesetChangeListener));
    }

    @Override
    protected Object computeValue(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == Value.IN_TIMEOUT) {
            Timeout t = scoreBoard.getCurrentTimeout(); 
            return t.isRunning() && this == t.getOwner() && !t.isReview();
        }
        if (prop == Value.IN_OFFICIAL_REVIEW) {
            Timeout t = scoreBoard.getCurrentTimeout(); 
            return t.isRunning() && this == t.getOwner() && t.isReview();
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
        if (prop == Value.NO_INITIAL && flag != Flag.COPY) {
            if (!(Boolean)value && (Boolean)last) {
                execute(Command.ADD_TRIP);
            } else if ((Boolean)value && !(Boolean)last && getCurrentTrip().getNumber() == 2
                    && (Integer)get(Value.JAM_SCORE) == 0) {
                execute(Command.REMOVE_TRIP);
            }
        }
        return value;
    }
    @Override
    protected void valueChanged(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == Value.LEAD && (Boolean)value && scoreBoard.isInJam()) {
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
            case SKATER:
                return new SkaterImpl(this, id);
            case BOX_TRIP:
                return new BoxTripImpl(this, id);
            default:
                return null;
            }
        }
    }
    
    @Override
    protected void itemAdded(AddRemoveProperty prop, ValueWithId item) {
        if (prop == Child.TIME_OUT) {
            recountTimeouts();
        }
    }

    @Override
    protected void itemRemoved(AddRemoveProperty prop, ValueWithId item) {
        if (prop == Child.SKATER) {
            ((Skater)item).unlink();
        }
        if (prop == Child.TIME_OUT) {
            recountTimeouts();
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

            for (ValueWithId p : getAll(Child.POSITION)) {
                ((Position)p).reset();
            }
            removeAll(Child.BOX_TRIP);
            removeAll(Child.ALTERNATE_NAME);
            removeAll(Child.COLOR);
            removeAll(Child.SKATER);
            removeAll(Child.TIME_OUT);
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
            for (ValueWithId skater : getAll(Child.SKATER)) {
                ((Skater)skater).restoreSnapshot(s.getSkaterSnapshot(skater.getId()));
            }
            updateTeamJams();
        }
    }

    @Override
    public String getAlternateName(String i) { return get(Child.ALTERNATE_NAME, i).getValue(); }
    @Override
    public String getAlternateName(AlternateNameId id) { return getAlternateName(id.toString()); }
    @Override
    public void setAlternateName(String i, String n) {
        synchronized (coreLock) {
            add(Child.ALTERNATE_NAME, new ValWithId(i, n));
        }
    }
    @Override
    public void removeAlternateName(String i) { remove(Child.ALTERNATE_NAME, i); }

    @Override
    public String getColor(String i) { return get(Child.COLOR, i).getValue(); }
    @Override
    public void setColor(String i, String c) {
        synchronized (coreLock) {
            add(Child.COLOR, new ValWithId(i, c));
        }
    }
    @Override
    public void removeColor(String i) { remove(Child.COLOR, i); }

    @Override
    public String getLogo() { return (String)get(Value.LOGO); }
    @Override
    public void setLogo(String l) { set(Value.LOGO, l); }

 
    @Override
    public void loadPreparedTeam(PreparedTeam pt) {
        synchronized (coreLock) {
            setLogo((String)pt.get(PreparedTeam.Value.LOGO));
            setName((String)pt.get(PreparedTeam.Value.NAME));
            for (ValueWithId v : pt.getAll(PreparedTeam.Child.ALTERNATE_NAME)) {
                setAlternateName(v.getId(), v.getValue());
            }
            for (ValueWithId v : pt.getAll(PreparedTeam.Child.COLOR)) {
                setColor(v.getId(), v.getValue());
            }
            for (ValueWithId v : pt.getAll(PreparedTeam.Child.SKATER)) {
                addSkater(new SkaterImpl(this, (PreparedTeamSkater)v));
            }
        }
    }

    @Override
    public void timeout() {
        synchronized (coreLock) {
            if (getTimeouts() > 0) {
                getScoreBoard().setTimeoutType(this, false);
            }
        }
    }
    @Override
    public void officialReview() {
        synchronized (coreLock) {
            if (getOfficialReviews() > 0) {
                getScoreBoard().setTimeoutType(this, true);
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
            set(Value.RUNNING_OR_ENDED_TEAM_JAM, scoreBoard.getCurrentPeriod().getCurrentJam().getTeamJam(getId()), Flag.INTERNAL);
            set(Value.RUNNING_OR_UPCOMING_TEAM_JAM,
                    scoreBoard.isInJam() ? getRunningOrEndedTeamJam() : getRunningOrEndedTeamJam().getNext(), Flag.INTERNAL);
            set(Value.LAST_ENDED_TEAM_JAM, getRunningOrUpcomingTeamJam().getPrevious(), Flag.INTERNAL);
            for (ValueWithId p : getAll(Child.POSITION)) {
                ((Position)p).updateCurrentFielding();
            }
        }
    }


    @Override
    public int getScore() { return (Integer)get(Value.SCORE); }

    @Override
    public ScoringTrip getCurrentTrip() { return (ScoringTrip)get(Value.CURRENT_TRIP); }

    @Override
    public boolean inTimeout() { return (Boolean)get(Value.IN_TIMEOUT); }

    @Override
    public boolean inOfficialReview() { return (Boolean)get(Value.IN_OFFICIAL_REVIEW); }

    @Override
    public boolean retainedOfficialReview() { return (Boolean)get(Value.RETAINED_OFFICIAL_REVIEW); }
    @Override
    public void setRetainedOfficialReview(boolean b) { set(Value.RETAINED_OFFICIAL_REVIEW, b); }

    @Override
    public int getTimeouts() { return (Integer)get(Value.TIMEOUTS); }
    @Override
    public int getOfficialReviews() { return (Integer)get(Value.OFFICIAL_REVIEWS); }

    @Override
    public void recountTimeouts() {
        boolean toPerPeriod = scoreBoard.getRulesets().getBoolean(Rule.TIMEOUTS_PER_PERIOD);
        boolean revPerPeriod = scoreBoard.getRulesets().getBoolean(Rule.REVIEWS_PER_PERIOD);
        int toCount = scoreBoard.getRulesets().getInt(Rule.NUMBER_TIMEOUTS);
        int revCount = scoreBoard.getRulesets().getInt(Rule.NUMBER_REVIEWS);
        int retainsLeft = scoreBoard.getRulesets().getInt(Rule.NUMBER_RETAINS);
        Timeout lastReview = null;

        for (ValueWithId v : getAll(Child.TIME_OUT)) {
            Timeout t = (Timeout)v;
            if (t.isReview()) {
                if (!revPerPeriod || t.getParent() == scoreBoard.getCurrentPeriod()) {
                    if (retainsLeft > 0 && t.isRetained()) {
                        retainsLeft--;
                    } else if (revCount > 0){
                        revCount--;
                    }
                    if (lastReview == null || t.compareTo(lastReview) > 0) {
                        lastReview = t;
                    }
                }
            } else {
                if (toCount > 0 && (!toPerPeriod || t.getParent() == scoreBoard.getCurrentPeriod())) {
                    toCount--;
                }
            }
        }
        set(Value.TIMEOUTS, toCount, Flag.INTERNAL);
        set(Value.OFFICIAL_REVIEWS, revCount, Flag.INTERNAL);
        set(Value.LAST_REVIEW, lastReview, Flag.INTERNAL);
    }
    
    protected ScoreBoardListener rulesetChangeListener = new ScoreBoardListener() {
        @Override
        public void scoreBoardChange(ScoreBoardEvent event) {
            recountTimeouts();
        }
};
    @Override
    public Skater getSkater(String id) { return (Skater)get(Child.SKATER, id); }
    public Skater addSkater(String id) { return (Skater)getOrCreate(Child.SKATER, id); }
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
            if (s.getFielding(tj) != null && 
                    s.getFielding(tj).getPosition() == getPosition(FloorPosition.PIVOT)) {
                tj.setNoPivot(r != Role.PIVOT);
                if ((r == Role.BLOCKER || r == Role.PIVOT) &&
                        ((tj.isRunningOrEnded() && hasFieldingAdvancePending()) ||
                                (tj.isRunningOrUpcoming() && !hasFieldingAdvancePending()))) {
                    s.setRole(r);
                }
            }
            if (s.getFielding(tj) == null || s.getRole(tj) != r) {
                Fielding f = getAvailableFielding(r, tj);
                if (r == Role.PIVOT && f != null) {
                    if (f.getSkater() != null && (tj.hasNoPivot() || s.getRole(tj) == Role.BLOCKER)) {
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
                    tj.setNoPivot(false);
                } else if (f != null) { 
                    f.setSkater(s);
                } else { 
                    s.remove(Skater.Child.FIELDING, s.getFielding(tj));
                }
            }
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
                    nextReplacedBlocker = (tj.hasNoPivot() && !tj.isStarPass()) ? FloorPosition.PIVOT : FloorPosition.BLOCKER1;
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
    public boolean hasNoPivot() { return (Boolean)get(Value.NO_PIVOT); }

    FloorPosition nextReplacedBlocker = FloorPosition.PIVOT;
    
    private Timer tripScoreTimer = new Timer();
    private TimerTask tripScoreTimerTask = new TimerTask() {
        @Override
        public void run() {} // dummy, so the variable is not null at the first score entry
    };

    public static final String DEFAULT_NAME_PREFIX = "Team ";
    public static final String DEFAULT_LOGO = "";

    public static class TeamSnapshotImpl implements TeamSnapshot {
        private TeamSnapshotImpl(Team team) {
            id = team.getId();
            skaterSnapshots = new HashMap<>();
            for (ValueWithId skater : team.getAll(Child.SKATER)) {
                skaterSnapshots.put(skater.getId(), ((Skater)skater).snapshot());
            }
        }

        @Override
        public String getId() { return id; }
        @Override
        public Map<String, Skater.SkaterSnapshot> getSkaterSnapshots() { return skaterSnapshots; }
        @Override
        public Skater.SkaterSnapshot getSkaterSnapshot(String skater) { return skaterSnapshots.get(skater); }

        protected String id;
        protected Map<String, Skater.SkaterSnapshot> skaterSnapshots;
    }
}
