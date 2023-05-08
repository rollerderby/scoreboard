package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface ScoreAdjustment extends ScoreBoardEventProvider {
    public int getAmount();
    public Jam getJamRecorded();
    public boolean isRecordedInJam();
    public boolean isRecordedLastTwoMins();
    public ScoringTrip getTripAppliedTo();

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<Integer> AMOUNT = new Value<>(Integer.class, "Amount", 0, props);
    public static final Value<Jam> JAM_RECORDED = new Value<>(Jam.class, "JamRecorded", null, props);
    public static final Value<Integer> PERIOD_NUMBER_RECORDED =
        new Value<>(Integer.class, "PeriodNumberRecorded", 0, props);
    public static final Value<Integer> JAM_NUMBER_RECORDED = new Value<>(Integer.class, "JamNumberRecorded", 0, props);
    public static final Value<Boolean> RECORDED_DURING_JAM =
        new Value<>(Boolean.class, "RecordedDuringJam", false, props);
    public static final Value<Boolean> LAST_TWO_MINUTES = new Value<>(Boolean.class, "LastTwoMinutes", false, props);
    public static final Value<Boolean> OPEN = new Value<>(Boolean.class, "Open", true, props);
    public static final Value<ScoringTrip> APPLIED_TO = new Value<>(ScoringTrip.class, "AppliedTo", null, props);

    public static final Command DISCARD = new Command("Discard", props);
}
