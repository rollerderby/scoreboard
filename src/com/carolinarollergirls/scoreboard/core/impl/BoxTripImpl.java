package com.carolinarollergirls.scoreboard.core.impl;

import java.util.UUID;

import com.carolinarollergirls.scoreboard.core.BoxTrip;
import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.Fielding;
import com.carolinarollergirls.scoreboard.core.Penalty;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.event.OrderedScoreBoardEventProvider.IValue;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class BoxTripImpl extends ScoreBoardEventProviderImpl implements BoxTrip {
    public BoxTripImpl(Team t, String id) {
        super(t, Value.ID, id, Team.Child.BOX_TRIP, BoxTrip.class, Value.class, Child.class, Command.class);
        initReferences();
    }
    public BoxTripImpl(Fielding f) {
        super(f.getTeamJam().getTeam(), Value.ID, UUID.randomUUID().toString(), Team.Child.BOX_TRIP, BoxTrip.class, Value.class, Child.class, Command.class);
        set(Value.WALLTIME_START, ScoreBoardClock.getInstance().getCurrentWalltime());
        set(Value.START_AFTER_S_P, f.getTeamJam().isStarPass() && f.isCurrent());
        set(Value.START_BETWEEN_JAMS, !scoreBoard.isInJam() && !getTeam().hasFieldingAdvancePending() && f.isCurrent());
        set(Value.JAM_CLOCK_START, startedBetweenJams() ? 0L : scoreBoard.getClock(Clock.ID_JAM).getTimeElapsed());
        set(Value.IS_CURRENT, true);
        initReferences();
        add(Child.FIELDING, f);
        f.updateBoxTripSymbols();
        if (f.getSkater() != null) {
            for (Penalty p : f.getSkater().getUnservedPenalties()) {
                add(Child.PENALTY, p);
            }
        }
    }

    private void initReferences() {
        setInverseReference(Child.FIELDING, Fielding.Child.BOX_TRIP);
        setInverseReference(Child.PENALTY, Penalty.Value.BOX_TRIP);
        setRecalculated(Value.DURATION).addSource(this, Value.JAM_CLOCK_START).addSource(this, Value.JAM_CLOCK_END);
        setCopy(Value.START_JAM_NUMBER, this, Value.START_FIELDING, IValue.NUMBER, true);
        setCopy(Value.END_JAM_NUMBER, this, Value.END_FIELDING, IValue.NUMBER, true);
    }

    @Override
    public int compareTo(BoxTrip other) {
        if (other == null) { return -1; }
        if (getStartFielding() == other.getStartFielding()) {
            if (getEndFielding() == other.getEndFielding()) {
                return (int) ((Long)get(BoxTrip.Value.WALLTIME_START) - (Long)other.get(BoxTrip.Value.WALLTIME_START));
            }
            if (getEndFielding() == null) { return 1; }
            return getEndFielding().compareTo(other.getEndFielding());
        }
        if (getStartFielding() == null) { return 1; } 
        return getStartFielding().compareTo(other.getStartFielding());
        
    }
    
    @Override
    protected Object computeValue(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == Value.DURATION) {
            long time = 0;
            if (!isCurrent() && get(Value.JAM_CLOCK_START) != null && get(Value.JAM_CLOCK_END) != null) {
                for (ValueWithId f : getAll(Child.FIELDING)) {
                    if (f == getEndFielding()) {
                        time += (Long)get(Value.JAM_CLOCK_END);
                    } else {
                        time += ((Fielding)f).getTeamJam().getJam().getDuration();
                    }
                    if (f == getStartFielding()) {
                        time -= (Long)get(Value.JAM_CLOCK_START);
                    }
                }
            }
            value = time;
        }
        return value;
    }
    @Override
    protected void valueChanged(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == Value.IS_CURRENT) {
            for (ValueWithId f : getAll(Child.FIELDING)) {
                ((Fielding)f).set(Fielding.Value.CURRENT_BOX_TRIP, this);
                ((Fielding)f).set(Fielding.Value.PENALTY_BOX, value);
            }
        }
        if ((prop == Value.END_FIELDING || prop == Value.END_AFTER_S_P || prop == Value.END_BETWEEN_JAMS)
                && getEndFielding() != null) {
            getEndFielding().updateBoxTripSymbols();
        }
        if ((prop == Value.START_FIELDING || prop == Value.START_AFTER_S_P || prop == Value.START_BETWEEN_JAMS)
                && getStartFielding() != null) {
            getStartFielding().updateBoxTripSymbols();
        }
    }
    
    @Override
    protected void itemAdded(AddRemoveProperty prop, ValueWithId item) {
        if (prop == Child.FIELDING) {
            if (getStartFielding() == null || ((Fielding) item).compareTo(getStartFielding()) < 0) {
                set(Value.START_FIELDING, item);
            }
            if (getCurrentFielding() == null || ((Fielding) item).compareTo(getCurrentFielding()) > 0) {
                set(Value.CURRENT_FIELDING, item);
            }
            if (getEndFielding() != null && ((Fielding) item).compareTo(getEndFielding()) > 0) {
                set(Value.END_FIELDING, item);
            }
        }
    }
    
    @Override
    protected void itemRemoved(AddRemoveProperty prop, ValueWithId item) {
        if (prop == Child.FIELDING) {
            if (getStartFielding() == item) {
                Fielding first = null;
                for (ValueWithId f : getAll(Child.FIELDING)) {
                    if (first == null || first.compareTo((Fielding) f) > 0) {
                        first = (Fielding) f;
                    }
                }
                set(Value.START_FIELDING, first);
            }
            if (getCurrentFielding() == item) {
                Fielding last = null;
                for (ValueWithId f : getAll(Child.FIELDING)) {
                    if (last == null || last.compareTo((Fielding) f) < 0) {
                        last = (Fielding) f;
                    }
                }
                set(Value.CURRENT_FIELDING, last);
            }
            if (getEndFielding() == item) {
                Fielding last = null;
                for (ValueWithId f : getAll(Child.FIELDING)) {
                    if (last == null || last.compareTo((Fielding) f) < 0) {
                        last = (Fielding) f;
                    }
                }
                set(Value.END_FIELDING, last);
            }
        }
    }
    
    @Override
    public void execute(CommandProperty prop) {
        synchronized (coreLock) {
            requestBatchStart();
            switch((Command)prop) {
            case DELETE:
                unlink();
                ((ScoreBoardEventProviderImpl) parent).scoreBoardChange(
                        new ScoreBoardEvent(this, BatchEvent.END, Boolean.TRUE, Boolean.TRUE));
                break;
            case START_EARLIER:
                if (getStartFielding() == null) { break; }
                if (startedAfterSP()) {
                    set(Value.START_AFTER_S_P, false);
                } else if (!startedBetweenJams()) {
                    set(Value.START_BETWEEN_JAMS, true);
                } else if (add(Child.FIELDING, getStartFielding().getSkater().getFielding(
                            getStartFielding().getTeamJam().getPrevious()))) {
                    set(Value.START_BETWEEN_JAMS, false);
                    if (getStartFielding().getTeamJam().isStarPass()) {
                        set(Value.START_AFTER_S_P, true);
                    }
                }
                break;
            case START_LATER:
                if (getStartFielding() == null) { break; }
                if (getStartFielding().getTeamJam().isRunningOrUpcoming() && 
                        !scoreBoard.isInJam()) { break; }
                if (startedBetweenJams()) {
                    set(Value.START_BETWEEN_JAMS, false);
                } else if (getStartFielding() == getEndFielding()) {
                    if (endedAfterSP() && !startedAfterSP()) {
                        set(Value.START_AFTER_S_P, true);
                    }
                } else if (!startedAfterSP() && getStartFielding().getTeamJam().isStarPass()) {
                    set(Value.START_AFTER_S_P, true);
                } else if (getStartFielding() != getCurrentFielding()) {
                    remove(Child.FIELDING, getStartFielding());
                    set(Value.START_AFTER_S_P, false);
                    set(Value.START_BETWEEN_JAMS, true);
                }
                break;
            case END_EARLIER:
                if (getEndFielding() == null) {
                    end();
                } else if (endedBetweenJams()) {
                    set(Value.END_BETWEEN_JAMS, false);
                } else if (getStartFielding() == getEndFielding()) {
                    if (endedAfterSP() && !startedAfterSP()) {
                        set(Value.END_AFTER_S_P, false);
                    }
                } else if (endedAfterSP()) {
                    set(Value.END_AFTER_S_P, false);
                } else {
                    remove(Child.FIELDING, getEndFielding());
                    set(Value.END_BETWEEN_JAMS, true);
                    if (getEndFielding().getTeamJam().isStarPass()) {
                        set(Value.END_AFTER_S_P, true);
                    }
                }
                break;
            case END_LATER:
                if (getEndFielding() == null) { break; }
                if (!endedAfterSP() && getEndFielding().getTeamJam().isStarPass()) {
                    set(Value.END_AFTER_S_P, true);
                } else if (!endedBetweenJams()) {
                    set(Value.END_BETWEEN_JAMS, true);
                } else if (add(Child.FIELDING, getEndFielding().getSkater().getFielding(
                        getEndFielding().getTeamJam().getNext()))) {
                    set(Value.END_AFTER_S_P, false);
                    set(Value.END_BETWEEN_JAMS, false);
                }
                if (getEndFielding().getTeamJam().isRunningOrUpcoming() &&
                        (endedBetweenJams() || !scoreBoard.isInJam())) {
                    // moved end past the present point in the game -> make ongoing if possible
                    if (!getEndFielding().isInBox()) {
                        unend();
                    }
                }
                break;
            }
            requestBatchEnd();
        }
    }
    
    @Override
    public void end() {
        set(Value.IS_CURRENT, false);
        set(Value.WALLTIME_END, ScoreBoardClock.getInstance().getCurrentWalltime());
        if (!scoreBoard.isInJam() && getCurrentFielding().getTeamJam().isRunningOrUpcoming()) {
            if (getTeam().hasFieldingAdvancePending()) {
                getCurrentFielding().setSkater(null);
            }
            remove(Child.FIELDING, getCurrentFielding());
        }
        if (getCurrentFielding() == null) {
            // trip ended in the same interjam as it started -> ignore it
            unlink();
        } else {
            set(Value.END_FIELDING, get(Value.CURRENT_FIELDING));
            set(Value.END_BETWEEN_JAMS, !scoreBoard.isInJam() && !getTeam().hasFieldingAdvancePending()
                    && getEndFielding().getTeamJam().isRunningOrEnded());
            set(Value.END_AFTER_S_P, getEndFielding().getTeamJam().isStarPass() && getEndFielding().isCurrent());
            set(Value.JAM_CLOCK_END, endedBetweenJams() ? 0L : scoreBoard.getClock(Clock.ID_JAM).getTimeElapsed());
            getCurrentFielding().updateBoxTripSymbols();
        }
    }
    
    @Override
    public void unend() {
        set(Value.END_FIELDING, null);
        set(Value.END_BETWEEN_JAMS, false);
        set(Value.END_AFTER_S_P, false);
        set(Value.IS_CURRENT, true);
        getCurrentFielding().updateBoxTripSymbols();
    }
    
    @Override
    public Team getTeam() { return (Team)getParent(); }

    @Override
    public boolean isCurrent() { return (Boolean)get(Value.IS_CURRENT); }
    @Override
    public Fielding getCurrentFielding() { return (Fielding)get(Value.CURRENT_FIELDING); }
    @Override
    public Fielding getStartFielding() { return (Fielding)get(Value.START_FIELDING); }
    @Override
    public boolean startedBetweenJams() { return (Boolean)get(Value.START_BETWEEN_JAMS); }
    @Override
    public boolean startedAfterSP() { return (Boolean)get(Value.START_AFTER_S_P); }
    @Override
    public Fielding getEndFielding() { return (Fielding)get(Value.END_FIELDING); }
    @Override
    public boolean endedBetweenJams() { return (Boolean)get(Value.END_BETWEEN_JAMS); }
    @Override
    public boolean endedAfterSP() { return (Boolean)get(Value.END_AFTER_S_P); }
}
