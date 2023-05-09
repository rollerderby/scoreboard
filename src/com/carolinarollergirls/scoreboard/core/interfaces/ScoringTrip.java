package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.Value;

public interface ScoringTrip extends NumberedScoreBoardEventProvider<ScoringTrip> {
    public int getScore();
    public boolean isAfterSP();
    public String getAnnotation();

    public int tryApplyScoreAdjustment(ScoreAdjustment adjustment);

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<Integer> SCORE = new Value<>(Integer.class, "Score", 0, props);
    public static final Value<Boolean> AFTER_S_P = new Value<>(Boolean.class, "AfterSP", false, props);
    public static final Value<Boolean> CURRENT = new Value<>(Boolean.class, "Current", false, props);
    public static final Value<Long> DURATION = new Value<>(Long.class, "Duration", 0L, props);
    public static final Value<Long> JAM_CLOCK_START = new Value<>(Long.class, "JamClockStart", 0L, props);
    public static final Value<Long> JAM_CLOCK_END = new Value<>(Long.class, "JamClockEnd", 0L, props);
    public static final Value<String> ANNOTATION = new Value<>(String.class, "Annotation", "", props);

    public static final Command INSERT_BEFORE = new Command("InsertBefore", props);
    public static final Command REMOVE = new Command("Remove", props);
}
