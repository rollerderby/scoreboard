package com.carolinarollergirls.scoreboard.core.interfaces;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.ParentOrderedScoreBoardEventProvider;
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

    Value<Skater> SKATER = new Value<>(Skater.class, "Skater", null);
    Value<String> SKATER_NUMBER = new Value<>(String.class, "SkaterNumber", "?");
    Value<Boolean> NOT_FIELDED = new Value<>(Boolean.class, "NotFielded", false);
    Value<Position> POSITION = new Value<>(Position.class, "Position", null);
    Value<Boolean> SIT_FOR_3 = new Value<>(Boolean.class, "SitFor3", false);
    Value<Boolean> PENALTY_BOX = new Value<>(Boolean.class, "PenaltyBox", false);
    Value<BoxTrip> CURRENT_BOX_TRIP = new Value<>(BoxTrip.class, "CurrentBoxTrip", null);
    Value<String> BOX_TRIP_SYMBOLS = new Value<>(String.class, "BoxTripSymbols", "");
    Value<String> BOX_TRIP_SYMBOLS_BEFORE_S_P = new Value<>(String.class, "BoxTripSymbolsBeforeSP", "");
    Value<String> BOX_TRIP_SYMBOLS_AFTER_S_P = new Value<>(String.class, "BoxTripSymbolsAfterSP", "");
    Value<String> ANNOTATION = new Value<>(String.class, "Annotation", "");

    Child<BoxTrip> BOX_TRIP = new Child<>(BoxTrip.class, "BoxTrip");

    Command ADD_BOX_TRIP = new Command("AddBoxTrip");
    Command UNEND_BOX_TRIP = new Command("UnendBoxTrip");
}
