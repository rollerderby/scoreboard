package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public interface Period extends ScoreBoardEventProvider {
    public void ensureAtLeastNJams(int n);
    public void truncateAfterNJams(int n);

    public int getPeriodNumber();
    public boolean isRunning();

    public Jam getJam(int j);
    
    public enum Value implements PermanentProperty {
	RUNNING,
	DURATION,
	WALLTIME_START,
	WALLTIME_END;
    }
    public enum Child implements AddRemoveProperty {
        JAM;
    }
}
