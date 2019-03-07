package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public interface Jam extends NumberedScoreBoardEventProvider<Jam> {
    public void setParent(ScoreBoardEventProvider p);

    public long getDuration();
    public long getPeriodClockElapsedStart();
    public long getPeriodClockElapsedEnd();
    public long getWalltimeStart();
    public long getWalltimeEnd();

    public TeamJam getTeamJam(String id);

    public void start();
    public void stop();

    public enum Value implements PermanentProperty {
        PERIOD_NUMBER,
        DURATION,
        PERIOD_CLOCK_ELAPSED_START,
        PERIOD_CLOCK_ELAPSED_END,
        WALLTIME_START,
        WALLTIME_END;
    }
    public enum Child implements AddRemoveProperty {
        TEAM_JAM,
        PENALTY;
    }
    public enum Command implements CommandProperty {
        DELETE,
        INSERT_BEFORE;
    }
}
