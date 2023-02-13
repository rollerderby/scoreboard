package com.carolinarollergirls.scoreboard.core.game;

import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Jam;
import com.carolinarollergirls.scoreboard.core.interfaces.Penalty;
import com.carolinarollergirls.scoreboard.core.interfaces.Period;
import com.carolinarollergirls.scoreboard.core.interfaces.Timeout;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class PeriodImpl extends NumberedScoreBoardEventProviderImpl<Period> implements Period {
    public PeriodImpl(Game g, int p) {
        super(g, p, Game.PERIOD);
        game = g;
        addProperties(props);
        setCopy(CURRENT_JAM_NUMBER, this, CURRENT_JAM, Jam.NUMBER, true);
        setRecalculated(FIRST_JAM).addSource(this, JAM);
        setCopy(FIRST_JAM_NUMBER, this, FIRST_JAM, Jam.NUMBER, true);
        if (hasPrevious()) {
            set(CURRENT_JAM, getPrevious().get(CURRENT_JAM));
            set(SUDDEN_SCORING, getPrevious().isSuddenScoring());
        } else {
            set(CURRENT_JAM, getOrCreate(JAM, "0"));
        }
        setRecalculated(DURATION).addSource(this, WALLTIME_END).addSource(this, WALLTIME_START);
        addWriteProtectionOverride(RUNNING, Source.NON_WS);
        addWriteProtectionOverride(JAM, Source.NON_WS);
        addWriteProtectionOverride(RUNNING, Source.NON_WS);
    }

    @Override
    protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == FIRST_JAM) { return getFirst(JAM); }
        if (prop == DURATION) {
            if (getWalltimeEnd() == 0L) {
                return 0L;
            } else {
                return getWalltimeEnd() - getWalltimeStart();
            }
        }
        return value;
    }
    @Override
    protected void valueChanged(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == RUNNING && !source.isFile()) {
            if (!(Boolean) value) {
                set(WALLTIME_END, ScoreBoardClock.getInstance().getCurrentWalltime());
            } else {
                set(WALLTIME_END, 0L);
                if (get(WALLTIME_START) == 0L) {
                    set(LOCAL_TIME_START, ScoreBoardClock.getInstance().getLocalTime());
                    set(WALLTIME_START, ScoreBoardClock.getInstance().getCurrentWalltime());
                }
            }
        } else if (prop == CURRENT_JAM && hasNext() && getNext().get(CURRENT_JAM) == last) {
            getNext().set(CURRENT_JAM, (Jam) value);
        } else if (prop == PREVIOUS && value != null && numberOf(JAM) > 0) {
            getFirst(JAM).setPrevious(getPrevious().getCurrentJam());
        }
    }

    @Override
    public ScoreBoardEventProvider create(Child<? extends ScoreBoardEventProvider> prop, String id, Source source) {
        synchronized (coreLock) {
            if (prop == JAM) {
                int num = Integer.parseInt(id);
                if (num > 0 || (num == 0 && getNumber() == 0)) { return new JamImpl(this, num); }
            }
            if (prop == TIMEOUT) { return new TimeoutImpl(this, id); }
            return null;
        }
    }

    @Override
    public void execute(Command prop, Source source) {
        synchronized (coreLock) {
            if (prop == DELETE) {
                if (!isRunning()) {
                    if (this == game.getCurrentPeriod()) { game.set(Game.CURRENT_PERIOD, getPrevious()); }
                    delete(source);
                }
            } else if (prop == INSERT_BEFORE) {
                if (game.getCurrentPeriodNumber() < game.getInt(Rule.NUMBER_PERIODS))
                    game.add(ownType, new PeriodImpl(game, getNumber()));
            } else if (prop == INSERT_TIMEOUT) {
                Timeout t = new TimeoutImpl(getCurrentJam());
                t.stop();
                t.set(Timeout.DURATION, 0L);
                t.getParent().add(TIMEOUT, t); // if this period hasn't started, the timeout is added to the
                                               // previous period
            }
        }
    }

    @Override
    public void delete(Source source) {
        if (source != Source.UNLINK && numberOf(JAM) > 0) {
            Jam prevJam = getFirst(JAM).getPrevious();
            Jam nextJam = getLast(JAM).getNext();
            if (prevJam != null) {
                prevJam.setNext(nextJam);
            } else if (nextJam != null) {
                nextJam.setPrevious(null);
            }
            for (Jam j : getAll(JAM)) {
                for (Penalty p : j.getAll(Jam.PENALTY)) { p.set(Penalty.JAM, nextJam); }
            }
        }
        super.delete(source);
    }

    @Override
    public PeriodSnapshot snapshot() {
        synchronized (coreLock) { return new PeriodSnapshotImpl(this); }
    }
    @Override
    public void restoreSnapshot(PeriodSnapshot s) {
        synchronized (coreLock) {
            if (s.getId() != getId()) { return; }
            if (getCurrentJam() != s.getCurrentJam()) {
                Jam movedJam = getCurrentJam();
                remove(JAM, movedJam);
                movedJam.setParent(game);
                game.set(Game.UPCOMING_JAM, movedJam);
                set(CURRENT_JAM, s.getCurrentJam());
            }
        }
    }

    @Override
    public Game getGame() {
        return game;
    }

    @Override
    public boolean isSuddenScoring() {
        return get(SUDDEN_SCORING);
    }

    @Override
    public boolean isRunning() {
        return get(RUNNING);
    }

    @Override
    public Jam getJam(int j) {
        return get(JAM, j);
    }
    @Override
    public Jam getCurrentJam() {
        return get(CURRENT_JAM);
    }
    @Override
    public int getCurrentJamNumber() {
        return get(CURRENT_JAM_NUMBER);
    }

    @Override
    public void startJam() {
        synchronized (coreLock) {
            set(RUNNING, true);
            set(CURRENT_JAM, getCurrentJam().getNext());
            getCurrentJam().start();
        }
    }
    @Override
    public void stopJam() {
        synchronized (coreLock) { getCurrentJam().stop(); }
    }

    @Override
    public long getDuration() {
        return get(DURATION);
    }
    @Override
    public long getWalltimeStart() {
        return get(WALLTIME_START);
    }
    @Override
    public long getWalltimeEnd() {
        return get(WALLTIME_END);
    }

    private Game game;

    public static class PeriodSnapshotImpl implements PeriodSnapshot {
        private PeriodSnapshotImpl(Period period) {
            id = period.getId();
            currentJam = period.getCurrentJam();
        }

        @Override
        public String getId() {
            return id;
        }
        @Override
        public Jam getCurrentJam() {
            return currentJam;
        }

        private String id;
        private Jam currentJam;
    }
}
