package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.NumberedProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public interface Period extends NumberedScoreBoardEventProvider<Period> {
    public PeriodSnapshot snapshot();
    public void restoreSnapshot(PeriodSnapshot s);

    public boolean isRunning();
    public int getNumber();

    public Jam getJam(int j);
    public Jam getCurrentJam();
    public int getCurrentJamNumber();

    public void startJam();
    public void stopJam();

    public long getDuration();
    public long getWalltimeStart();
    public long getWalltimeEnd();

    public enum Value implements PermanentProperty {
        CURRENT_JAM,
        CURRENT_JAM_NUMBER,
        RUNNING,
        DURATION,
        WALLTIME_START,
        WALLTIME_END;
    }
    public enum NChild implements NumberedProperty {
        JAM;
    }
    public enum Command implements CommandProperty {
        DELETE,
        INSERT_BEFORE;
    }

    public static interface PeriodSnapshot {
        public String getId();
        public Jam getCurrentJam();
    }
}
