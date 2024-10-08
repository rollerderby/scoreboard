package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface PreparedOfficial extends ScoreBoardEventProvider {
    public boolean matches(String name, String league);

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<String> FULL_INFO = new Value<>(String.class, "FullInfo", "", props);
}
