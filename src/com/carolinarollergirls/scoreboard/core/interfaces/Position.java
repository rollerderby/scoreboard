package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface Position extends ScoreBoardEventProvider {
    public void updateCurrentFielding();

    public Team getTeam();
    public FloorPosition getFloorPosition();
    public Skater getSkater();
    public void setSkater(Skater s);
    public Fielding getCurrentFielding();
    public void setCurrentFielding(Fielding f);
    public boolean isPenaltyBox();
    public void setPenaltyBox(boolean box);

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<Fielding> CURRENT_FIELDING = new Value<>(Fielding.class, "CurrentFielding", null, props);
    public static final Value<String> CURRENT_BOX_SYMBOLS = new Value<>(String.class, "CurrentBoxSymbols", "", props);
    public static final Value<String> CURRENT_PENALTIES = new Value<>(String.class, "CurrentPenalties", "", props);
    public static final Value<String> ANNOTATION = new Value<>(String.class, "Annotation", "", props);
    public static final Value<Skater> SKATER = new Value<>(Skater.class, "Skater", null, props);
    public static final Value<String> NAME = new Value<>(String.class, "Name", "", props);
    public static final Value<String> ROSTER_NUMBER = new Value<>(String.class, "RosterNumber", "", props);
    public static final Value<String> FLAGS = new Value<>(String.class, "Flags", "", props);
    public static final Value<Boolean> PENALTY_BOX = new Value<>(Boolean.class, "PenaltyBox", false, props);
    public static final Value<Boolean> HAS_UNSERVED = new Value<>(Boolean.class, "HasUnserved", false, props);

    public static final Command CLEAR = new Command("Clear", props);
}
