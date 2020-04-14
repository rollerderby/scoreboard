package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.CommandProperty;
import com.carolinarollergirls.scoreboard.event.NumberedProperty;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.PermanentProperty;

public interface Period extends NumberedScoreBoardEventProvider<Period> {
    public PeriodSnapshot snapshot();
    public void restoreSnapshot(PeriodSnapshot s);

    public boolean isRunning();

    public Jam getJam(int j);
    public Jam getCurrentJam();
    public int getCurrentJamNumber();

    public void startJam();
    public void stopJam();

    public long getDuration();
    public long getWalltimeStart();
    public long getWalltimeEnd();

    PermanentProperty<Jam> CURRENT_JAM = new PermanentProperty<>(Jam.class, "CurrentJam", null);
    PermanentProperty<Integer> CURRENT_JAM_NUMBER = new PermanentProperty<>(Integer.class, "CurrentJamNumber", 0);
    PermanentProperty<Jam> FIRST_JAM = new PermanentProperty<>(Jam.class, "FirstJam", null);
    PermanentProperty<Integer> FIRST_JAM_NUMBER = new PermanentProperty<>(Integer.class, "FirstJamNumber", 0);
    PermanentProperty<Boolean> RUNNING = new PermanentProperty<>(Boolean.class, "Running", false);
    PermanentProperty<Long> DURATION = new PermanentProperty<>(Long.class, "Duration", 0L);
    PermanentProperty<Long> WALLTIME_START = new PermanentProperty<>(Long.class, "WalltimeStart", 0L);
    PermanentProperty<Long> WALLTIME_END = new PermanentProperty<>(Long.class, "WalltimeEnd", 0L);

    AddRemoveProperty<Timeout> TIMEOUT = new AddRemoveProperty<>(Timeout.class, "Timeout");

    NumberedProperty<Jam> JAM = new NumberedProperty<>(Jam.class, "Jam");

    CommandProperty DELETE = new CommandProperty("Delete");
    CommandProperty INSERT_BEFORE = new CommandProperty("InsertBefore");
    CommandProperty INSERT_TIMEOUT = new CommandProperty("InsertTimeout");

    public static interface PeriodSnapshot {
        public String getId();
        public Jam getCurrentJam();
    }
}
