package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.CommandProperty;
import com.carolinarollergirls.scoreboard.event.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public interface Timeout extends ScoreBoardEventProvider {
    int compareTo(Timeout other);

    public void stop();

    public TimeoutOwner getOwner();
    public boolean isReview();
    public boolean isRetained();
    public boolean isRunning();

    // @formatter:off
    PermanentProperty<TimeoutOwner> OWNER = new PermanentProperty<>(TimeoutOwner.class, "Owner", null);
    PermanentProperty<Boolean> REVIEW = new PermanentProperty<>(Boolean.class, "Review", false);
    PermanentProperty<Boolean> RETAINED_REVIEW = new PermanentProperty<>(Boolean.class, "RetainedReview", false);
    PermanentProperty<Boolean> RUNNING = new PermanentProperty<>(Boolean.class, "Running", true);
    PermanentProperty<Jam> PRECEDING_JAM = new PermanentProperty<>(Jam.class, "PrecedingJam", null);
    PermanentProperty<Integer> PRECEDING_JAM_NUMBER = new PermanentProperty<>(Integer.class, "PrecedingJamNumber", 0);
    PermanentProperty<Long> DURATION = new PermanentProperty<>(Long.class, "Duration", 0L);
    PermanentProperty<Long> PERIOD_CLOCK_ELAPSED_START = new PermanentProperty<>(Long.class, "PeriodClockElapsedStart", 0L);
    PermanentProperty<Long> PERIOD_CLOCK_ELAPSED_END = new PermanentProperty<>(Long.class, "PeriodClockElapsedEnd", 0L);
    PermanentProperty<Long> PERIOD_CLOCK_END = new PermanentProperty<>(Long.class, "PeriodClockEnd", 0L);
    PermanentProperty<Long> WALLTIME_START = new PermanentProperty<>(Long.class, "WalltimeStart", 0L);
    PermanentProperty<Long> WALLTIME_END = new PermanentProperty<>(Long.class, "WalltimeEnd", 0L);

    CommandProperty DELETE = new CommandProperty("Delete");
    // @formatter:on

    public enum Owners implements TimeoutOwner {
        NONE(""),
        OTO("O");

        Owners(String id) {
            this.id = id;
        }

        @Override
        public String getId() { return id; }
        @Override
        public String toString() { return id; }

        private String id;
    }
}
