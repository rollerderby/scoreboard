package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public interface Settings extends ScoreBoardEventProvider {
    public String get(String k);
    // Setting to null deletes a setting.
    public void set(String k, String v);

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Child<ValWithId> SETTING = new Child<>(ValWithId.class, "Setting", props);
}
