package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

// Roster for teams for loading in for games.
public interface PreparedTeam extends ScoreBoardEventProvider {

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Child<ValWithId> UNIFORM_COLOR = new Child<>(ValWithId.class, "UniformColor", props);
    public static final Child<PreparedSkater> SKATER = new Child<>(PreparedSkater.class, "Skater", props);

    public static interface PreparedSkater extends ScoreBoardEventProvider {}
}
