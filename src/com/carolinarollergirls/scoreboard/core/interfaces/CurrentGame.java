package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface CurrentGame extends ScoreBoardEventProvider {
    public void postAutosaveUpdate();

    public void load(Game g);

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<Game> GAME = new Value<>(Game.class, "Game", null, props);

    public static final Child<CurrentClock> CLOCK = new Child<>(CurrentClock.class, "Clock", props);
    public static final Child<CurrentTeam> TEAM = new Child<>(CurrentTeam.class, "Team", props);
}
