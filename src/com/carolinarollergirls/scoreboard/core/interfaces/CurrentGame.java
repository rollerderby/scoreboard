package com.carolinarollergirls.scoreboard.core.interfaces;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface CurrentGame extends ScoreBoardEventProvider {
    public void postAutosaveUpdate();

    public void load(Game g);

    Value<Game> GAME = new Value<>(Game.class, "Game", null);

    Child<CurrentClock> CLOCK = new Child<>(CurrentClock.class, "Clock");
    Child<CurrentTeam> TEAM = new Child<>(CurrentTeam.class, "Team");
}
