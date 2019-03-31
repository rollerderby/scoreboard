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
import com.carolinarollergirls.scoreboard.utils.Comparators;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class BoxTripImpl extends ScoreBoardEventProviderImpl implements BoxTrip {
    public BoxTripImpl(Team t, String id) {
        super(t, Value.ID, id, Team.Child.BOX_TRIP, BoxTrip.class, Value.class, Child.class, Command.class);
        initReferences();
    }
    public BoxTripImpl(Fielding f) {
        super(f.getTeamJam().getTeam(), Value.ID, UUID.randomUUID().toString(), Team.Child.BOX_TRIP, BoxTrip.class, Value.class, Child.class, Command.class);
        set(Value.WALLTIME_START, ScoreBoardClock.getInstance().getCurrentWalltime());
        set(Value.START_AFTER_S_P, f.getTeamJam().isStarPass());
        set(Value.START_BETWEEN_JAMS, f.isCurrent() && !scoreBoard.isInJam());
        set(Value.JAM_CLOCK_START, startedBetweenJams() ? 0L : scoreBoard.getClock(Clock.ID_JAM).getTimeElapsed());
        set(Value.IS_CURRENT, true);
        initReferences();
        add(Child.FIELDING, f);
        f.updateBoxTripSymbols();
        for (Penalty p : f.getSkater().getUnservedPenalties()) {
            add(Child.PENALTY, p);
        }
    }

    private void initReferences() {
        setInverseReference(Child.FIELDING, Fielding.Child.BOX_TRIP);
        setInverseReference(Child.PENALTY, Penalty.Value.BOX_TRIP);
        setRecalculated(Value.DURATION).addSource(this, Value.JAM_CLOCK_START).addSource(this, Value.JAM_CLOCK_END);
        setCopy(Value.START_JAM_NUMBER, this, Value.START_FIELDING, IValue.NUMBER, true);
        setCopy(Value.END_JAM_NUMBER, this, Value.END_FIELDING, IValue.NUMBER, true);
    }

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
    protected void valueChanged(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == Value.IS_CURRENT && !(Boolean)value) {
            for (ValueWithId f : getAll(Child.FIELDING)) {
                ((Fielding)f).set(Fielding.Value.PENALTY_BOX, false);
            }
        }
    }
    
    protected void itemAdded(AddRemoveProperty prop, ValueWithId item) {
        if (prop == Child.FIELDING) {
            if (getStartFielding() == null || Comparators.FieldingComparator.compare((Fielding) item, getStartFielding()) < 0) {
                set(Value.START_FIELDING, item);
            }
            if (getCurrentFielding() == null || Comparators.FieldingComparator.compare((Fielding) item, getCurrentFielding()) > 0) {
                set(Value.CURRENT_FIELDING, item);
            }
            if (getEndFielding() != null && Comparators.FieldingComparator.compare((Fielding) item, getEndFielding()) > 0) {
                set(Value.END_FIELDING, item);
            }
        }
    }
    
    protected void itemRemoved(AddRemoveProperty prop, ValueWithId item) {
        if (prop == Child.FIELDING) {
            if (getStartFielding() == item) {
                Fielding first = null;
                for (ValueWithId f : getAll(Child.FIELDING)) {
                    if (first == null || Comparators.FieldingComparator.compare(first, (Fielding) f) < 0) {
                        first = (Fielding) f;
                    }
                }
                set(Value.START_FIELDING, first);
            }
            if (getCurrentFielding() == item) {
                Fielding last = null;
                for (ValueWithId f : getAll(Child.FIELDING)) {
                    if (Comparators.FieldingComparator.compare(last, (Fielding) f) > 0) {
                        last = (Fielding) f;
                    }
                }
                set(Value.CURRENT_FIELDING, last);
            }
            if (getEndFielding() == item) {
                Fielding last = null;
                for (ValueWithId f : getAll(Child.FIELDING)) {
                    if (last == null || Comparators.FieldingComparator.compare(last, (Fielding) f) > 0) {
                        last = (Fielding) f;
                    }
                }
                set(Value.START_FIELDING, last);
            }
        }
    }
    
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
                if (getStartFielding() == getEndFielding()) {
                    if (startedBetweenJams() && !endedBetweenJams()) {
                        set(Value.START_BETWEEN_JAMS, false);
                    } else if (endedAfterSP() && !startedAfterSP()) {
                        set(Value.START_AFTER_S_P, true);
                    }
                    break;
                } else {
                    if (startedBetweenJams()) {
                        set(Value.START_BETWEEN_JAMS, false);
                    } else if (!startedAfterSP() && getStartFielding().getTeamJam().isStarPass()) {
                        set(Value.START_AFTER_S_P, true);
                    } else {
                        remove(Child.FIELDING, getStartFielding());
                        set(Value.START_AFTER_S_P, false);
                        set(Value.START_BETWEEN_JAMS, true);
                    }
                }
                break;
            case END_EARLIER:
                if (getEndFielding() == null) { break; }
                if (getStartFielding() == getEndFielding()) {
                    if (endedAfterSP() && !startedAfterSP()) {
                        set(Value.END_AFTER_S_P, false);
                    }
                    break;
                } else {
                    if (endedAfterSP()) {
                        set(Value.END_AFTER_S_P, false);
                    } else if(!endedBetweenJams()) {
                        set(Value.END_BETWEEN_JAMS, true);
                    } else {
                        remove(Child.FIELDING, getEndFielding());
                        set(Value.END_BETWEEN_JAMS, false);
                        if (getEndFielding().getTeamJam().isStarPass()) {
                            set(Value.END_AFTER_S_P, true);
                        }
                    }
                }
                break;
            case END_LATER:
                if (getEndFielding() == null) { break; }
                if (endedBetweenJams()) {
                    set(Value.END_BETWEEN_JAMS, false);
                } else if (!endedAfterSP() && getEndFielding().getTeamJam().isStarPass()) {
                    set(Value.END_AFTER_S_P, true);
                } else if (add(Child.FIELDING, getStartFielding().getSkater().getFielding(
                        getStartFielding().getTeamJam().getNext()))) {
                    set(Value.END_AFTER_S_P, false);
                    set(Value.END_BETWEEN_JAMS, true);
                }
                break;
            }
            requestBatchEnd();
        }
    }
    
    public void end() {
        set(Value.IS_CURRENT, false);
        set(Value.WALLTIME_END, ScoreBoardClock.getInstance().getCurrentWalltime());
        set(Value.END_FIELDING, get(Value.CURRENT_FIELDING));
        set(Value.END_BETWEEN_JAMS, getEndFielding().isCurrent() && !scoreBoard.isInJam());
        set(Value.END_AFTER_S_P, getEndFielding().getTeamJam().isStarPass());
        set(Value.JAM_CLOCK_END, endedBetweenJams() ? 0L : scoreBoard.getClock(Clock.ID_JAM).getTimeElapsed());
        getCurrentFielding().updateBoxTripSymbols();
        if (getStartFielding() == getEndFielding() && endedBetweenJams()) {
//            || ((Long)get(Value.DURATION) < 5000L && getAll(Child.PENALTY).size() == 0)) {
            unlink();
        }
    }

    @Override
    public boolean isCurrent() { return (Boolean)get(Value.IS_CURRENT); }
    public Fielding getCurrentFielding() { return (Fielding)get(Value.CURRENT_FIELDING); }
    public Fielding getStartFielding() { return (Fielding)get(Value.START_FIELDING); }
    public boolean startedBetweenJams() { return (Boolean)get(Value.START_BETWEEN_JAMS); }
    public boolean startedAfterSP() { return (Boolean)get(Value.START_AFTER_S_P); }
    public Fielding getEndFielding() { return (Fielding)get(Value.END_FIELDING); }
    public boolean endedBetweenJams() { return (Boolean)get(Value.END_BETWEEN_JAMS); }
    public boolean endedAfterSP() { return (Boolean)get(Value.END_AFTER_S_P); }
}
