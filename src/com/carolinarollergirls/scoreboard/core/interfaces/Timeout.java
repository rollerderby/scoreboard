package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface Timeout extends ScoreBoardEventProvider {
    int compareTo(Timeout other);

    public void stop();

    public TimeoutOwner getOwner();
    public boolean isReview();
    public boolean isRetained();
    public boolean isRunning();

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<TimeoutOwner> OWNER = new Value<>(TimeoutOwner.class, "Owner", null, props);
    public static final Value<Boolean> REVIEW = new Value<>(Boolean.class, "Review", false, props);
    public static final Value<Boolean> RETAINED_REVIEW = new Value<>(Boolean.class, "RetainedReview", false, props);
    public static final Value<Boolean> RUNNING = new Value<>(Boolean.class, "Running", true, props);
    public static final Value<Jam> PRECEDING_JAM = new Value<>(Jam.class, "PrecedingJam", null, props);
    public static final Value<Integer> PRECEDING_JAM_NUMBER =
        new Value<>(Integer.class, "PrecedingJamNumber", 0, props);
    public static final Value<Long> DURATION = new Value<>(Long.class, "Duration", 0L, props);
    public static final Value<Long> PERIOD_CLOCK_ELAPSED_START =
        new Value<>(Long.class, "PeriodClockElapsedStart", 0L, props);
    public static final Value<Long> PERIOD_CLOCK_ELAPSED_END =
        new Value<>(Long.class, "PeriodClockElapsedEnd", 0L, props);
    public static final Value<Long> PERIOD_CLOCK_END = new Value<>(Long.class, "PeriodClockEnd", 0L, props);
    public static final Value<Long> WALLTIME_START = new Value<>(Long.class, "WalltimeStart", 0L, props);
    public static final Value<Long> WALLTIME_END = new Value<>(Long.class, "WalltimeEnd", 0L, props);

    public static final Command DELETE = new Command("Delete", props);
    public static final Command INSERT_AFTER = new Command("InsertAfter", props);

    public enum Owners implements TimeoutOwner {
        NONE(""),
        OTO("O");

        Owners(String id) { this.id = id; }

        @Override
        public String getId() {
            return id;
        }
        @Override
        public String toString() {
            return id;
        }

        private String id;
    }
}
