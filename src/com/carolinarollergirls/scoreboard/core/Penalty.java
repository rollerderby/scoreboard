package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;

public interface Penalty extends ScoreBoardEventProvider {
    public int getPeriodNumber();
    public int getJamNumber();
    public Jam getJam();
    public String getCode();

    public enum Value implements PermanentProperty {
        ID,
        TIME,
        JAM,
        PERIOD_NUMBER,
        JAM_NUMBER,
        NUMBER,
        CODE;
    }
}
