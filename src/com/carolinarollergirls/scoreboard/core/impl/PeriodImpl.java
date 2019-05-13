package com.carolinarollergirls.scoreboard.core.impl;

import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Period;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class PeriodImpl extends NumberedScoreBoardEventProviderImpl<Period> implements Period {
    public PeriodImpl(ScoreBoard s, int p) {
        super(s, p, ScoreBoard.NChild.PERIOD, Period.class, Value.class, NChild.class, Command.class);
        setCopy(Value.CURRENT_JAM_NUMBER, this, Value.CURRENT_JAM, IValue.NUMBER, true);
        if (hasPrevious()) {
            set(Value.CURRENT_JAM, getPrevious().get(Value.CURRENT_JAM));
        } else {
            set(Value.CURRENT_JAM, getOrCreate(NChild.JAM, "0"));
        }
        setRecalculated(Value.DURATION).addSource(this, Value.WALLTIME_END).addSource(this, Value.WALLTIME_START);
    }

    @Override
    protected Object computeValue(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == Value.DURATION) {
            if (getWalltimeEnd() == 0L) { return 0L; }
            else { return getWalltimeEnd() - getWalltimeStart(); }
        }
        return value;
    }
    @Override
    protected void valueChanged(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == Value.RUNNING && flag != Flag.FROM_AUTOSAVE) {
            if (!(Boolean)value) {
                set(Value.WALLTIME_END, ScoreBoardClock.getInstance().getCurrentWalltime());
            } else {
                set(Value.WALLTIME_END, 0L);
                if ((Long)get(Value.WALLTIME_START) == 0L) {
                    set(Value.WALLTIME_START, ScoreBoardClock.getInstance().getCurrentWalltime());
                }
            }
        } else if (prop == Value.CURRENT_JAM && hasNext() && getNext().get(Value.CURRENT_JAM) == last) {
            getNext().set(Value.CURRENT_JAM, value);
        } else if (prop == IValue.PREVIOUS && value != null && getAll(NChild.JAM).size() > 0) {
            getFirst(NChild.JAM).set(IValue.PREVIOUS, getPrevious().getCurrentJam());
        }
    }

    @Override
    public ValueWithId create(AddRemoveProperty prop, String id) {
        synchronized (coreLock) {
            int num = Integer.parseInt(id);
            if (prop == NChild.JAM && (num > 0 || (num == 0 && getNumber() == 0))) {
                return new JamImpl(this, num);
            }
            return null;
        }
    }
    
    @Override
    public void execute(CommandProperty prop) {
        synchronized (coreLock) {
            switch((Command)prop) {
            case DELETE:
                if (!isRunning()) {
                    if (this == scoreBoard.getCurrentPeriod()) {
                        scoreBoard.set(ScoreBoard.Value.CURRENT_PERIOD, getPrevious());
                    }
                    unlink();
                }
                break;
            case INSERT_BEFORE:
                if (scoreBoard.getCurrentPeriodNumber() < scoreBoard.getRulesets().getInt(Rule.NUMBER_PERIODS))
                    scoreBoard.add(ownType, new PeriodImpl(scoreBoard, getNumber()));
                break;
            }
        }
    }

    @Override
    public void unlink(boolean neighborsRemoved) {
        if (!neighborsRemoved && getAll(NChild.JAM).size() > 0) {
            if (getFirst(NChild.JAM).getPrevious() != null) {
                ((Jam) getFirst(NChild.JAM).getPrevious()).setNext((Jam) getLast(NChild.JAM).getNext());
            } else if (getLast(NChild.JAM).getNext() != null) {
                ((Jam) getLast(NChild.JAM).getNext()).setPrevious(null);
            }
        }
        super.unlink(neighborsRemoved);
    }
    
    @Override
    public PeriodSnapshot snapshot() {
        synchronized (coreLock) {
            return new PeriodSnapshotImpl(this);
        }
    }
    @Override
    public void restoreSnapshot(PeriodSnapshot s) {
        synchronized (coreLock) {
            if (s.getId() != getId()) {	return; }
            if (getCurrentJam() != s.getCurrentJam()) {
                Jam movedJam = getCurrentJam();
                remove (NChild.JAM, movedJam);
                movedJam.setParent(scoreBoard);
                scoreBoard.set(ScoreBoard.Value.UPCOMING_JAM, movedJam);
                set(Value.CURRENT_JAM, s.getCurrentJam());
            }
        }
    }

    @Override
    public boolean isRunning() { return (Boolean)get(Value.RUNNING); }

    @Override
    public Jam getJam(int j) { return (Jam)get(NChild.JAM, j); }
    @Override
    public Jam getCurrentJam() { return (Jam)get(Value.CURRENT_JAM); }
    @Override
    public int getCurrentJamNumber() { return (Integer)get(Value.CURRENT_JAM_NUMBER); }
    

    @Override
    public void startJam() {
        synchronized (coreLock) {
            requestBatchStart();
            set(Value.RUNNING, true);
            set(Value.CURRENT_JAM, getCurrentJam().getNext());
            getCurrentJam().start();
            requestBatchEnd();
        }
    }
    @Override
    public void stopJam() {
        synchronized (coreLock) {
            requestBatchStart();
            getCurrentJam().stop();
            requestBatchEnd();
        }
    }

    @Override
    public long getDuration() { return (Long)get(Value.DURATION); }
    @Override
    public long getWalltimeStart() { return (Long)get(Value.WALLTIME_START); }
    @Override
    public long getWalltimeEnd() { return (Long)get(Value.WALLTIME_END); }

    public static class PeriodSnapshotImpl implements PeriodSnapshot {
        private PeriodSnapshotImpl(Period period) {
            id = period.getId();
            currentJam = period.getCurrentJam();
        }

        @Override
        public String getId() { return id; }
        @Override
        public Jam getCurrentJam() { return currentJam; }

        private String id;
        private Jam currentJam;
    }
}
