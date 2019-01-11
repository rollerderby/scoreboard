package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public interface Jam extends NumberedScoreBoardEventProvider<Jam> {
    public Period getPeriod();
    public int getPeriodNumber();

    public long getDuration();
    public long getPeriodClockElapsedStart();
    public long getPeriodClockElapsedEnd();
    public long getWalltimeStart();
    public long getWalltimeEnd();

    public TeamJam getTeamJam(String id);
    
    public void start();
    public void stop();

    public enum Value implements PermanentProperty {
	ID,
        DURATION,
        PERIOD_CLOCK_ELAPSED_START,
        PERIOD_CLOCK_ELAPSED_END,
        WALLTIME_START,
        WALLTIME_END;
    }
    public enum Child implements AddRemoveProperty {
        TEAM_JAM;
    }
}
