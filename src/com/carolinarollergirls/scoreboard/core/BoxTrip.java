package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public interface BoxTrip extends ScoreBoardEventProvider {
    public void end();
    
    public boolean isCurrent();
    public Fielding getCurrentFielding();
    public Fielding getStartFielding();
    public boolean startedBetweenJams();
    public boolean startedAfterSP();
    public Fielding getEndFielding();
    public boolean endedBetweenJams();
    public boolean endedAfterSP();
    
    public enum Value implements PermanentProperty {
        ID,
        IS_CURRENT,
        CURRENT_FIELDING,
        START_FIELDING,
        START_JAM_NUMBER,
        START_BETWEEN_JAMS,
        START_AFTER_S_P,
        END_FIELDING,
        END_JAM_NUMBER,
        END_BETWEEN_JAMS,
        END_AFTER_S_P,
        WALLTIME_START,
        WALLTIME_END,
        JAM_CLOCK_START,
        JAM_CLOCK_END,
        DURATION
    }
    public enum Child implements AddRemoveProperty {
        FIELDING,
        PENALTY
    }
    public enum Command implements CommandProperty {
        START_EARLIER,
        START_LATER,
        END_EARLIER,
        END_LATER,
        DELETE
    }
}
