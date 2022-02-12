package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.MirrorScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.Value;

public interface CurrentGame extends MirrorScoreBoardEventProvider<Game> {
    public void postAutosaveUpdate();

    public void load(Game g);

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<Game> GAME = new Value<>(Game.class, "Game", null, props);

    public static interface CurrentClock extends MirrorScoreBoardEventProvider<Clock> {}
    public static interface CurrentTeam extends MirrorScoreBoardEventProvider<Team> {}
    public static interface CurrentSkater extends MirrorScoreBoardEventProvider<Skater> {}
    public static interface CurrentPenalty extends MirrorScoreBoardEventProvider<Penalty> {}
    public static interface CurrentPosition extends MirrorScoreBoardEventProvider<Position> {}
    public static interface CurrentBoxTrip extends MirrorScoreBoardEventProvider<BoxTrip> {}
    public static interface CurrentPeriod extends MirrorScoreBoardEventProvider<Period> {}
    public static interface CurrentJam extends MirrorScoreBoardEventProvider<Jam> {}
    public static interface CurrentTeamJam extends MirrorScoreBoardEventProvider<TeamJam> {}
    public static interface CurrentFielding extends MirrorScoreBoardEventProvider<Fielding> {}
    public static interface CurrentScoringTrip extends MirrorScoreBoardEventProvider<ScoringTrip> {}
    public static interface CurrentTimeout extends MirrorScoreBoardEventProvider<Timeout> {}
    public static interface CurrentOfficial extends MirrorScoreBoardEventProvider<Official> {}
    public static interface CurrentExpulsion extends MirrorScoreBoardEventProvider<Expulsion> {}
}
