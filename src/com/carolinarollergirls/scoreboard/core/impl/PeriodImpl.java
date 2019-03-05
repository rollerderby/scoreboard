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
    public PeriodImpl(ScoreBoard s, String p) {
        super(s, p, IValue.NUMBER, ScoreBoard.NChild.PERIOD, Period.class, Value.class, NChild.class, Command.class);
        addReference(new IndirectValueReference(this, Value.CURRENT_JAM_NUMBER, this, Value.CURRENT_JAM, IValue.NUMBER, true, 0));
        if (hasPrevious()) {
            set(Value.CURRENT_JAM, getPrevious().get(Value.CURRENT_JAM));
        } else {
            set(Value.CURRENT_JAM, getOrCreate(NChild.JAM, "0"));
        }
        addReference(new ElementReference(Value.CURRENT_JAM, Jam.class, null));
        set(Value.WALLTIME_END, 0L);
        set(Value.WALLTIME_START, 0L);
        set(Value.RUNNING, false, Flag.FROM_AUTOSAVE);
        addReference(new UpdateReference(this, Value.DURATION, this, Value.WALLTIME_END));
        addReference(new UpdateReference(this, Value.DURATION, this, Value.WALLTIME_START));
    }

    protected Object computeValue(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == Value.DURATION) {
            if (getWalltimeEnd() == 0L) { return 0L; }
            else { return getWalltimeEnd() - getWalltimeStart(); }
        }
        return value;
    }
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
        } else if (prop == IValue.NUMBER) {
            writeProtectionOverride.remove(prop);
        }
    }

    public ValueWithId create(AddRemoveProperty prop, String id) {
        synchronized (coreLock) {
            int num = Integer.parseInt(id);
            if (prop == NChild.JAM && (num > 0 || (num == 0 && getNumber() == 0))) {
                return new JamImpl(this, id);
            }
            return null;
        }
    }
    
    public void execute(CommandProperty prop) {
        synchronized (coreLock) {
            switch((Command)prop) {
            case DELETE:
                if (!isRunning()) {
                    unlink();
                }
                break;
            case INSERT_BEFORE:
                if (scoreBoard.getCurrentPeriodNumber() < scoreBoard.getRulesets().getInt(Rule.NUMBER_PERIODS))
                    scoreBoard.add(ownType, new PeriodImpl(scoreBoard, getProviderId()));
                break;
            }
        }
    }

    public PeriodSnapshot snapshot() {
        synchronized (coreLock) {
            return new PeriodSnapshotImpl(this);
        }
    }
    public void restoreSnapshot(PeriodSnapshot s) {
        synchronized (coreLock) {
            if (s.getId() != getId()) {	return; }
            set(Value.CURRENT_JAM, s.getCurrentJam());
        }
    }

    public boolean isRunning() { return (Boolean)get(Value.RUNNING); }

    public Jam getJam(int j) { return (Jam)get(NChild.JAM, j); }
    public Jam getCurrentJam() { return (Jam)get(Value.CURRENT_JAM); }
    public int getCurrentJamNumber() { return (Integer)get(Value.CURRENT_JAM_NUMBER); }
    

    public void startJam() {
        synchronized (coreLock) {
            requestBatchStart();
            set(Value.RUNNING, true);
            set(Value.CURRENT_JAM, getCurrentJam().getNext());
            getCurrentJam().start();
            requestBatchEnd();
        }
    }
    public void stopJam() {
        synchronized (coreLock) {
            requestBatchStart();
            getCurrentJam().stop();
            requestBatchEnd();
        }
    }

    public long getDuration() { return (Long)get(Value.DURATION); }
    public long getWalltimeStart() { return (Long)get(Value.WALLTIME_START); }
    public long getWalltimeEnd() { return (Long)get(Value.WALLTIME_END); }

    public static class PeriodSnapshotImpl implements PeriodSnapshot {
        private PeriodSnapshotImpl(Period period) {
            id = period.getId();
            currentJam = period.getCurrentJam();
        }

        public String getId() { return id; }
        public Jam getCurrentJam() { return currentJam; }

        private String id;
        private Jam currentJam;
    }
}
