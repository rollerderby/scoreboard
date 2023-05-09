package com.carolinarollergirls.scoreboard.core.game;

import java.util.UUID;

import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Jam;
import com.carolinarollergirls.scoreboard.core.interfaces.Period;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.core.interfaces.Timeout;
import com.carolinarollergirls.scoreboard.core.interfaces.TimeoutOwner;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class TimeoutImpl extends ScoreBoardEventProviderImpl<Timeout> implements Timeout {
    public TimeoutImpl(Period p, String id) {
        super(p, id, Period.TIMEOUT);
        game = p.getGame();
        initReferences();
        if ("noTimeout".equals(id)) {
            set(RUNNING, false);
            set(READONLY, true);
        }
    }

    public TimeoutImpl(Jam precedingJam) {
        super(precedingJam.getParent(), UUID.randomUUID().toString(), Period.TIMEOUT);
        game = precedingJam.getPeriod().getGame();
        initReferences();
        set(PRECEDING_JAM, precedingJam);
        set(WALLTIME_START, ScoreBoardClock.getInstance().getCurrentWalltime());
        set(PERIOD_CLOCK_ELAPSED_START, game.getClock(Clock.ID_PERIOD).getTimeElapsed());
    }

    private void initReferences() {
        addProperties(props);
        set(OWNER, Owners.NONE);
        setInverseReference(PRECEDING_JAM, Jam.TIMEOUTS_AFTER);
        setCopy(PRECEDING_JAM_NUMBER, this, PRECEDING_JAM, Jam.NUMBER, true);
    }

    @Override
    public int compareTo(Timeout other) {
        int result = 0;
        if (get(PRECEDING_JAM) != null && other.get(PRECEDING_JAM) != null) {
            result = get(PRECEDING_JAM).compareTo(other.get(PRECEDING_JAM));
        }
        if (result == 0) { result = (int) (get(WALLTIME_START) - other.get(WALLTIME_START)); }
        return result;
    }

    @Override
    protected void valueChanged(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == OWNER) {
            if (last instanceof Team) { ((Team) last).remove(Team.TIME_OUT, this); }
            if (value instanceof Team) { ((Team) value).add(Team.TIME_OUT, this); }
            if (get(PRECEDING_JAM) == game.getCurrentPeriod().getCurrentJam()) {
                game.set(Game.NO_MORE_JAM, game.get(Game.NO_MORE_JAM), Source.RECALCULATE);
            }
        }
        if (prop == REVIEW && getOwner() instanceof Team) {
            ((Team) getOwner()).recountTimeouts();
            if (get(PRECEDING_JAM) == game.getCurrentPeriod().getCurrentJam()) {
                game.set(Game.NO_MORE_JAM, game.get(Game.NO_MORE_JAM), Source.RECALCULATE);
            }
        }
        if (prop == RETAINED_REVIEW && getOwner() instanceof Team) { ((Team) getOwner()).recountTimeouts(); }
        if (prop == PRECEDING_JAM) {
            if (value != null && ((Jam) value).getParent() != getParent()) {
                getParent().remove(Period.TIMEOUT, this);
                parent = ((Jam) value).getParent();
                getParent().add(Period.TIMEOUT, this);
            }
            if (getOwner() instanceof Team) { ((Team) getOwner()).recountTimeouts(); }
            if (game.getCurrentPeriod() != null &&
                (value == game.getCurrentPeriod().getCurrentJam() || last == game.getCurrentPeriod().getCurrentJam())) {
                game.set(Game.NO_MORE_JAM, game.get(Game.NO_MORE_JAM), Source.RECALCULATE);
            }
        }
    }

    @Override
    public void delete(Source source) {
        if (get(OWNER) instanceof Team) { ((Team) get(OWNER)).remove(Team.TIME_OUT, this); }
        super.delete(source);
    }

    @Override
    public void execute(Command prop, Source source) {
        synchronized (coreLock) {
            if (prop == DELETE) {
                if (!isRunning()) { delete(source); }
            } else if (prop == INSERT_AFTER) {
                Timeout newTo = new TimeoutImpl(get(PRECEDING_JAM));
                newTo.set(RUNNING, false);
                newTo.set(WALLTIME_START, get(WALLTIME_END));
                newTo.set(WALLTIME_END, get(WALLTIME_END));
                newTo.set(PERIOD_CLOCK_ELAPSED_START, get(PERIOD_CLOCK_ELAPSED_END));
                newTo.set(PERIOD_CLOCK_ELAPSED_END, get(PERIOD_CLOCK_ELAPSED_END));
                newTo.set(PERIOD_CLOCK_END, get(PERIOD_CLOCK_END));
                getParent().add(Period.TIMEOUT, newTo);
            }
        }
    }

    @Override
    public void stop() {
        set(RUNNING, false);
        set(DURATION, game.getClock(Clock.ID_TIMEOUT).getTimeElapsed());
        set(WALLTIME_END, ScoreBoardClock.getInstance().getCurrentWalltime());
        set(PERIOD_CLOCK_ELAPSED_END, game.getClock(Clock.ID_PERIOD).getTimeElapsed());
        set(PERIOD_CLOCK_END, game.getClock(Clock.ID_PERIOD).getTime());
    }

    @Override
    public TimeoutOwner getOwner() {
        return get(OWNER);
    }

    @Override
    public boolean isReview() {
        return get(REVIEW);
    }

    @Override
    public boolean isRetained() {
        return get(RETAINED_REVIEW);
    }

    @Override
    public boolean isRunning() {
        return get(RUNNING);
    }

    private Game game;
}
