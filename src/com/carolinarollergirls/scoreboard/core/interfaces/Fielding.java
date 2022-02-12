package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.ParentOrderedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.Value;

public interface Fielding extends ParentOrderedScoreBoardEventProvider<Fielding> {
    public TeamJam getTeamJam();
    public Position getPosition();

    public boolean isCurrent();
    public Role getCurrentRole();

    public Skater getSkater();
    public void setSkater(Skater s);

    public boolean isSitFor3();
    public boolean isInBox();
    public BoxTrip getCurrentBoxTrip();
    public void updateBoxTripSymbols();

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<Skater> SKATER = new Value<>(Skater.class, "Skater", null, props);
    public static final Value<String> SKATER_NUMBER = new Value<>(String.class, "SkaterNumber", "?", props);
    public static final Value<Boolean> NOT_FIELDED = new Value<>(Boolean.class, "NotFielded", false, props);
    public static final Value<Position> POSITION = new Value<>(Position.class, "Position", null, props);
    public static final Value<Boolean> SIT_FOR_3 = new Value<>(Boolean.class, "SitFor3", false, props);
    public static final Value<Boolean> PENALTY_BOX = new Value<>(Boolean.class, "PenaltyBox", false, props);
    public static final Value<BoxTrip> CURRENT_BOX_TRIP = new Value<>(BoxTrip.class, "CurrentBoxTrip", null, props);
    public static final Value<String> BOX_TRIP_SYMBOLS = new Value<>(String.class, "BoxTripSymbols", "", props);
    public static final Value<String> BOX_TRIP_SYMBOLS_BEFORE_S_P =
        new Value<>(String.class, "BoxTripSymbolsBeforeSP", "", props);
    public static final Value<String> BOX_TRIP_SYMBOLS_AFTER_S_P =
        new Value<>(String.class, "BoxTripSymbolsAfterSP", "", props);
    public static final Value<String> ANNOTATION = new Value<>(String.class, "Annotation", "", props);

    public static final Child<BoxTrip> BOX_TRIP = new Child<>(BoxTrip.class, "BoxTrip", props);

    public static final Command ADD_BOX_TRIP = new Command("AddBoxTrip", props);
    public static final Command UNEND_BOX_TRIP = new Command("UnendBoxTrip", props);
}
