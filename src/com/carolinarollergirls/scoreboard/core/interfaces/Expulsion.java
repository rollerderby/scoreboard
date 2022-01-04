package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface Expulsion extends ScoreBoardEventProvider {

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<String> INFO = new Value<>(String.class, "Info", "", props);
    public static final Value<String> EXTRA_INFO = new Value<>(String.class, "ExtraInfo", "", props);
    public static final Value<Boolean> SUSPENSION = new Value<>(Boolean.class, "Suspension", false, props);
}
