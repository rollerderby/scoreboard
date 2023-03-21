package com.carolinarollergirls.scoreboard.core.game;

import java.util.UUID;

import com.carolinarollergirls.scoreboard.core.interfaces.BoxTrip;
import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.Fielding;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Penalty;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.event.ValueWithId;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class BoxTripImpl extends ScoreBoardEventProviderImpl<BoxTrip> implements BoxTrip {
    public BoxTripImpl(Team t, String id) {
        super(t, id, Team.BOX_TRIP);
        game = t.getGame();
        addProperties(props);
        initReferences();
    }
    public BoxTripImpl(Fielding f) {
        super(f.getTeamJam().getTeam(), UUID.randomUUID().toString(), Team.BOX_TRIP);
        game = f.getTeamJam().getTeam().getGame();
        addProperties(props);
        set(WALLTIME_START, ScoreBoardClock.getInstance().getCurrentWalltime());
        set(START_AFTER_S_P, f.getTeamJam().isStarPass() && f.isCurrent());
        set(START_BETWEEN_JAMS, !game.isInJam() && !getTeam().hasFieldingAdvancePending() && f.isCurrent());
        set(JAM_CLOCK_START, startedBetweenJams() ? 0L : game.getClock(Clock.ID_JAM).getTimeElapsed());
        set(IS_CURRENT, true);
        initReferences();
        add(FIELDING, f);
        f.updateBoxTripSymbols();
        if (f.getSkater() != null) {
            for (Penalty p : f.getSkater().getUnservedPenalties()) { add(PENALTY, p); }
        }
    }

    private void initReferences() {
        addWriteProtectionOverride(IS_CURRENT, Source.NON_WS);
        addWriteProtectionOverride(CURRENT_FIELDING, Source.ANY_INTERNAL);
        setInverseReference(FIELDING, Fielding.BOX_TRIP);
        setInverseReference(PENALTY, Penalty.BOX_TRIP);
        setRecalculated(DURATION).addSource(this, JAM_CLOCK_START).addSource(this, JAM_CLOCK_END);
        setCopy(START_JAM_NUMBER, this, START_FIELDING, Fielding.NUMBER, true);
        setCopy(END_JAM_NUMBER, this, END_FIELDING, Fielding.NUMBER, true);
    }

    @Override
    public int compareTo(BoxTrip other) {
        if (other == null) { return -1; }
        if (getStartFielding() == other.getStartFielding()) {
            if (getEndFielding() == other.getEndFielding()) {
                return (int) (get(BoxTrip.WALLTIME_START) - other.get(BoxTrip.WALLTIME_START));
            }
            if (getEndFielding() == null) { return 1; }
            return getEndFielding().compareTo(other.getEndFielding());
        }
        if (getStartFielding() == null) { return 1; }
        return getStartFielding().compareTo(other.getStartFielding());
    }

    @Override
    protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == DURATION) {
            long time = 0;
            if (!isCurrent() && get(JAM_CLOCK_START) != null && get(JAM_CLOCK_END) != null) {
                for (Fielding f : getAll(FIELDING)) {
                    if (f == getEndFielding()) {
                        time += get(JAM_CLOCK_END);
                    } else {
                        time += f.getTeamJam().getJam().getDuration();
                    }
                    if (f == getStartFielding()) { time -= get(JAM_CLOCK_START); }
                }
            }
            value = time;
        }
        return value;
    }
    @Override
    protected void valueChanged(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == IS_CURRENT) {
            for (Fielding f : getAll(FIELDING)) {
                f.set(Fielding.CURRENT_BOX_TRIP, this);
                f.set(Fielding.PENALTY_BOX, (Boolean) value);
            }
        }
        if ((prop == END_FIELDING || prop == END_AFTER_S_P || prop == END_BETWEEN_JAMS) && getEndFielding() != null) {
            getEndFielding().updateBoxTripSymbols();
        }
        if ((prop == START_FIELDING || prop == START_AFTER_S_P || prop == START_BETWEEN_JAMS) &&
            getStartFielding() != null) {
            getStartFielding().updateBoxTripSymbols();
        }
    }

    @Override
    protected void itemAdded(Child<?> prop, ValueWithId item, Source source) {
        if (prop == FIELDING) {
            Fielding f = (Fielding) item;
            if (getStartFielding() == null || f.compareTo(getStartFielding()) < 0) { set(START_FIELDING, f); }
            if (getCurrentFielding() == null || f.compareTo(getCurrentFielding()) > 0) { set(CURRENT_FIELDING, f); }
            if (getEndFielding() != null && f.compareTo(getEndFielding()) > 0) { set(END_FIELDING, f); }
        }
    }

    @Override
    protected void itemRemoved(Child<?> prop, ValueWithId item, Source source) {
        if (prop == FIELDING) {
            if (getStartFielding() == item) {
                Fielding first = null;
                for (Fielding f : getAll(FIELDING)) {
                    if (first == null || first.compareTo(f) > 0) { first = f; }
                }
                set(START_FIELDING, first);
            }
            if (getCurrentFielding() == item) {
                Fielding last = null;
                for (Fielding f : getAll(FIELDING)) {
                    if (last == null || last.compareTo(f) < 0) { last = f; }
                }
                set(CURRENT_FIELDING, last);
            }
            if (getEndFielding() == item) {
                Fielding last = null;
                for (Fielding f : getAll(FIELDING)) {
                    if (last == null || last.compareTo(f) < 0) { last = f; }
                }
                set(END_FIELDING, last);
            }
        }
    }

    @Override
    public void execute(Command prop, Source source) {
        if (prop == DELETE) {
            delete(source);
        } else if (prop == START_EARLIER) {
            if (getStartFielding() == null) { return; }
            if (startedAfterSP()) {
                set(START_AFTER_S_P, false);
            } else if (!startedBetweenJams()) {
                set(START_BETWEEN_JAMS, true);
            } else if (add(FIELDING,
                           getStartFielding().getSkater().getFielding(getStartFielding().getTeamJam().getPrevious()))) {
                set(START_BETWEEN_JAMS, false);
                if (getStartFielding().getTeamJam().isStarPass()) { set(START_AFTER_S_P, true); }
            }
        } else if (prop == START_LATER) {
            if (getStartFielding() == null) { return; }
            if (getStartFielding().getTeamJam().isRunningOrUpcoming() && !game.isInJam()) { return; }
            if (startedBetweenJams()) {
                set(START_BETWEEN_JAMS, false);
            } else if (getStartFielding() == getEndFielding()) {
                if (endedAfterSP() && !startedAfterSP()) { set(START_AFTER_S_P, true); }
            } else if (!startedAfterSP() && getStartFielding().getTeamJam().isStarPass()) {
                set(START_AFTER_S_P, true);
            } else if (getStartFielding() != getCurrentFielding()) {
                remove(FIELDING, getStartFielding());
                set(START_AFTER_S_P, false);
                set(START_BETWEEN_JAMS, true);
            }
        } else if (prop == END_EARLIER) {
            if (getEndFielding() == null) {
                end();
            } else if (endedBetweenJams()) {
                set(END_BETWEEN_JAMS, false);
            } else if (getStartFielding() == getEndFielding()) {
                if (endedAfterSP() && !startedAfterSP()) { set(END_AFTER_S_P, false); }
            } else if (endedAfterSP()) {
                set(END_AFTER_S_P, false);
            } else {
                remove(FIELDING, getEndFielding());
                set(END_BETWEEN_JAMS, true);
                if (getEndFielding().getTeamJam().isStarPass()) { set(END_AFTER_S_P, true); }
            }
        } else if (prop == END_LATER) {
            if (getEndFielding() == null) { return; }
            if (!endedAfterSP() && getEndFielding().getTeamJam().isStarPass()) {
                set(END_AFTER_S_P, true);
            } else if (!endedBetweenJams()) {
                set(END_BETWEEN_JAMS, true);
            } else {
                if (getEndFielding().getTeamJam().isRunningOrEnded() && !game.isInJam()) {
                    // user is attempting to extend trip into upcoming jam - field skater if necessary
                    getTeam().field(getEndFielding().getSkater(), getEndFielding().getCurrentRole(),
                                    getEndFielding().getTeamJam().getNext());
                }
                if (add(FIELDING, getEndFielding().getSkater().getFielding(getEndFielding().getTeamJam().getNext()))) {
                    set(END_AFTER_S_P, false);
                    set(END_BETWEEN_JAMS, false);
                }
            }
            if (getEndFielding().getTeamJam().isRunningOrUpcoming() && (endedBetweenJams() || !game.isInJam())) {
                // moved end past the present point in the game -> make ongoing
                unend();
            }
        }
    }

    @Override
    public void end() {
        set(IS_CURRENT, false);
        set(WALLTIME_END, ScoreBoardClock.getInstance().getCurrentWalltime());
        if (!game.isInJam() && getCurrentFielding().getTeamJam().isRunningOrUpcoming()) {
            if (getTeam().hasFieldingAdvancePending()) { getCurrentFielding().setSkater(null); }
            remove(FIELDING, getCurrentFielding());
        }
        if (getCurrentFielding() == null) {
            // trip ended in the same interjam as it started -> ignore it
            delete();
        } else {
            set(END_FIELDING, get(CURRENT_FIELDING));
            set(END_BETWEEN_JAMS, !game.isInJam() && !getTeam().hasFieldingAdvancePending() &&
                                      getEndFielding().getTeamJam().isRunningOrEnded());
            set(END_AFTER_S_P, getEndFielding().getTeamJam().isStarPass() && getEndFielding().isCurrent());
            set(JAM_CLOCK_END, endedBetweenJams() ? 0L : game.getClock(Clock.ID_JAM).getTimeElapsed());
            getCurrentFielding().updateBoxTripSymbols();
        }
    }

    @Override
    public void unend() {
        set(END_FIELDING, null);
        set(END_BETWEEN_JAMS, false);
        set(END_AFTER_S_P, false);
        set(IS_CURRENT, true);
        getCurrentFielding().updateBoxTripSymbols();
    }

    @Override
    public Team getTeam() {
        return (Team) getParent();
    }

    @Override
    public boolean isCurrent() {
        return get(IS_CURRENT);
    }
    @Override
    public Fielding getCurrentFielding() {
        return get(CURRENT_FIELDING);
    }
    @Override
    public Fielding getStartFielding() {
        return get(START_FIELDING);
    }
    @Override
    public boolean startedBetweenJams() {
        return get(START_BETWEEN_JAMS);
    }
    @Override
    public boolean startedAfterSP() {
        return get(START_AFTER_S_P);
    }
    @Override
    public Fielding getEndFielding() {
        return get(END_FIELDING);
    }
    @Override
    public boolean endedBetweenJams() {
        return get(END_BETWEEN_JAMS);
    }
    @Override
    public boolean endedAfterSP() {
        return get(END_AFTER_S_P);
    }

    private Game game;
}
