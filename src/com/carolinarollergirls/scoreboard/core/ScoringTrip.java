package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public interface ScoringTrip extends NumberedScoreBoardEventProvider<ScoringTrip> {
    public int getScore();
    
    public enum Value implements PermanentProperty {
        SCORE,
        AFTER_S_P,
        DURATION,
        JAM_CLOCK_START,
        JAM_CLOCK_END
    }
}
