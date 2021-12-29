package com.carolinarollergirls.scoreboard.core.interfaces;

import com.carolinarollergirls.scoreboard.event.ReferenceOrderedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface CurrentPenalty extends ReferenceOrderedScoreBoardEventProvider<CurrentPenalty> {
    public int getPeriodNumber();
    public int getJamNumber();
    public String getCode();

    public boolean isServed();

    Value<Penalty> PENALTY = new Value<>(Penalty.class, "Penalty", null);
}
