package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.NumberedChild;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

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

    Value<Jam> CURRENT_JAM = new Value<>(Jam.class, "CurrentJam", null);
    Value<Integer> CURRENT_JAM_NUMBER = new Value<>(Integer.class, "CurrentJamNumber", 0);
    Value<Jam> FIRST_JAM = new Value<>(Jam.class, "FirstJam", null);
    Value<Integer> FIRST_JAM_NUMBER = new Value<>(Integer.class, "FirstJamNumber", 0);
    Value<Boolean> RUNNING = new Value<>(Boolean.class, "Running", false);
    Value<Long> DURATION = new Value<>(Long.class, "Duration", 0L);
    Value<Long> WALLTIME_START = new Value<>(Long.class, "WalltimeStart", 0L);
    Value<Long> WALLTIME_END = new Value<>(Long.class, "WalltimeEnd", 0L);
    Value<String> LOCAL_TIME_START = new Value<>(String.class, "LocalTimeStart", "");

    Child<Timeout> TIMEOUT = new Child<>(Timeout.class, "Timeout");

    NumberedChild<Jam> JAM = new NumberedChild<>(Jam.class, "Jam");

    Command DELETE = new Command("Delete");
    Command INSERT_BEFORE = new Command("InsertBefore");
    Command INSERT_TIMEOUT = new Command("InsertTimeout");

    public static interface PeriodSnapshot {
        public String getId();
        public Jam getCurrentJam();
    }
}
