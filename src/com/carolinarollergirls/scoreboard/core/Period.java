package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.NumberedProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public interface Period extends NumberedScoreBoardEventProvider<Period> {
    public void truncateAfterCurrentJam();

    public boolean isRunning();
    public int getNumber();

    public Jam getJam(int j);
    public Jam getCurrentJam();
    
    public void startJam();
    public void stopJam();
    
    public enum Value implements PermanentProperty {
	RUNNING,
	DURATION,
	WALLTIME_START,
	WALLTIME_END;
    }
    public enum NChild implements NumberedProperty {
        JAM;
    }
}
