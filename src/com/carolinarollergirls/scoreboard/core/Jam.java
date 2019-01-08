package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public interface Jam extends ScoreBoardEventProvider {
    public int getPeriodNumber();
    public int getJamNumber();

    public long getJamClockElapsedEnd();
    public void setJamClockElapsedEnd(long t);
    public long getPeriodClockElapsedStart();
    public void setPeriodClockElapsedStart(long t);
    public long getPeriodClockElapsedEnd();
    public void setPeriodClockElapsedEnd(long t);
    public long getPeriodClockWalltimeStart();
    public void setPeriodClockWalltimeStart(long t);
    public long getPeriodClockWalltimeEnd();
    public void setPeriodClockWalltimeEnd(long t);

    public TeamJam getTeamJam(String id);

    public enum Value implements PermanentProperty {
        JAM_CLOCK_ELAPSED_END,
        PERIOD_CLOCK_ELAPSED_START,
        PERIOD_CLOCK_ELAPSED_END,
        PERIOD_CLOCK_WALLTIME_START,
        PERIOD_CLOCK_WALLTIME_END;
    }
    public enum Child implements AddRemoveProperty {
        TEAM_JAM;
    }
}
