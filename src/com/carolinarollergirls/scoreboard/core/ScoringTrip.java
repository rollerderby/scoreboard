package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.CommandProperty;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.PermanentProperty;

public interface ScoringTrip extends NumberedScoreBoardEventProvider<ScoringTrip> {
    public int getScore();

    // @formatter:off
    PermanentProperty<Integer> SCORE = new PermanentProperty<>(Integer.class, "Score", 0);
    PermanentProperty<Boolean> AFTER_S_P = new PermanentProperty<>(Boolean.class, "AfterSP", false);
    PermanentProperty<Boolean> CURRENT = new PermanentProperty<>(Boolean.class, "Current", false);
    PermanentProperty<Long> DURATION = new PermanentProperty<>(Long.class, "Duration", 0L);
    PermanentProperty<Long> JAM_CLOCK_START = new PermanentProperty<>(Long.class, "JamClockStart", 0L);
    PermanentProperty<Long> JAM_CLOCK_END = new PermanentProperty<>(Long.class, "JamClockEnd", 0L);
    PermanentProperty<String> ANNOTATION = new PermanentProperty<>(String.class, "Annotation", "");

    CommandProperty INSERT_BEFORE = new CommandProperty("InsertBefore");
    CommandProperty REMOVE = new CommandProperty("Remove");
    // @formatter:on
}
