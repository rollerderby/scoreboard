package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface OfficialPosition extends ScoreBoardEventProvider {
    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<String> NAME = new Value<>(String.class, "Name", "", props);
    public static final Value<Team> TEAM = new Value<>(Team.class, "Team", null, props);
    public static final Value<Boolean> REMOVABLE = new Value<>(Boolean.class, "Removable", true, props);
    public static final Value<Official> CURRENT_OFFICIAL = new Value<>(Official.class, "CurrentOfficial", null, props);
    public static final Value<String> CURRENT_OFFICIAL_NAME =
        new Value<>(String.class, "CurrentOfficialName", "", props);

    public static final Child<Penalty> PENALTIES = new Child<>(Penalty.class, "Penalties", props);

    public static final Command REMOVE = new Command("Remove", props);
}
