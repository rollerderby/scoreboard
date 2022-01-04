package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ReferenceOrderedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface CurrentPenalty extends ReferenceOrderedScoreBoardEventProvider<CurrentPenalty> {
    public int getPeriodNumber();
    public int getJamNumber();
    public String getCode();

    public boolean isServed();

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<Penalty> PENALTY = new Value<>(Penalty.class, "Penalty", null, props);
}
