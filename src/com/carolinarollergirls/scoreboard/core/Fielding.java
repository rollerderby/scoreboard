package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ParentOrderedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.PermanentProperty;

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

    // @formatter:off
    PermanentProperty<Skater> SKATER = new PermanentProperty<>(Skater.class, "Skater", null);
    PermanentProperty<String> SKATER_NUMBER = new PermanentProperty<>(String.class, "SkaterNumber", "?");
    PermanentProperty<Boolean> NOT_FIELDED = new PermanentProperty<>(Boolean.class, "NotFielded", false);
    PermanentProperty<Position> POSITION = new PermanentProperty<>(Position.class, "Position", null);
    PermanentProperty<Boolean> SIT_FOR_3 = new PermanentProperty<>(Boolean.class, "SitFor3", false);
    PermanentProperty<Boolean> PENALTY_BOX = new PermanentProperty<>(Boolean.class, "PenaltyBox", false);
    PermanentProperty<BoxTrip> CURRENT_BOX_TRIP = new PermanentProperty<>(BoxTrip.class, "CurrentBoxTrip", null);
    PermanentProperty<String> BOX_TRIP_SYMBOLS = new PermanentProperty<>(String.class, "BoxTripSymbols", "");
    PermanentProperty<String> BOX_TRIP_SYMBOLS_BEFORE_S_P = new PermanentProperty<>(String.class, "BoxTripSymbolsBeforeSP", "");
    PermanentProperty<String> BOX_TRIP_SYMBOLS_AFTER_S_P = new PermanentProperty<>(String.class, "BoxTripSymbolsAfterSP", "");
    PermanentProperty<String> ANNOTATION = new PermanentProperty<>(String.class, "Annotation", "");

    AddRemoveProperty<BoxTrip> BOX_TRIP = new AddRemoveProperty<>(BoxTrip.class, "BoxTrip");

    CommandProperty ADD_BOX_TRIP = new CommandProperty("AddBoxTrip");
    CommandProperty UNEND_BOX_TRIP = new CommandProperty("UnendBoxTrip");
    // @formatter:on
}
