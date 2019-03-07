package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public interface Penalty extends NumberedScoreBoardEventProvider<Penalty> {
    public int getPeriodNumber();
    public int getJamNumber();
    public Jam getJam();
    public String getCode();

    public enum Value implements PermanentProperty {
        TIME,
        JAM,
        PERIOD_NUMBER,
        JAM_NUMBER,
        CODE;
    }
}
