package com.carolinarollergirls.scoreboard.core.interfaces;

import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface ScoringTrip extends NumberedScoreBoardEventProvider<ScoringTrip> {
    public int getScore();
    public boolean isAfterSP();
    public String getAnnotation();

    Value<Integer> SCORE = new Value<>(Integer.class, "Score", 0);
    Value<Boolean> AFTER_S_P = new Value<>(Boolean.class, "AfterSP", false);
    Value<Boolean> CURRENT = new Value<>(Boolean.class, "Current", false);
    Value<Long> DURATION = new Value<>(Long.class, "Duration", 0L);
    Value<Long> JAM_CLOCK_START = new Value<>(Long.class, "JamClockStart", 0L);
    Value<Long> JAM_CLOCK_END = new Value<>(Long.class, "JamClockEnd", 0L);
    Value<String> ANNOTATION = new Value<>(String.class, "Annotation", "");

    Command INSERT_BEFORE = new Command("InsertBefore");
    Command REMOVE = new Command("Remove");
}
