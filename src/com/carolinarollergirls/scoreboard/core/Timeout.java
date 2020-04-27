package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface Timeout extends ScoreBoardEventProvider {
    int compareTo(Timeout other);

    public void stop();

    public TimeoutOwner getOwner();
    public boolean isReview();
    public boolean isRetained();
    public boolean isRunning();

    Value<TimeoutOwner> OWNER = new Value<>(TimeoutOwner.class, "Owner", null);
    Value<Boolean> REVIEW = new Value<>(Boolean.class, "Review", false);
    Value<Boolean> RETAINED_REVIEW = new Value<>(Boolean.class, "RetainedReview", false);
    Value<Boolean> RUNNING = new Value<>(Boolean.class, "Running", true);
    Value<Jam> PRECEDING_JAM = new Value<>(Jam.class, "PrecedingJam", null);
    Value<Integer> PRECEDING_JAM_NUMBER = new Value<>(Integer.class, "PrecedingJamNumber", 0);
    Value<Long> DURATION = new Value<>(Long.class, "Duration", 0L);
    Value<Long> PERIOD_CLOCK_ELAPSED_START = new Value<>(Long.class, "PeriodClockElapsedStart", 0L);
    Value<Long> PERIOD_CLOCK_ELAPSED_END = new Value<>(Long.class, "PeriodClockElapsedEnd", 0L);
    Value<Long> PERIOD_CLOCK_END = new Value<>(Long.class, "PeriodClockEnd", 0L);
    Value<Long> WALLTIME_START = new Value<>(Long.class, "WalltimeStart", 0L);
    Value<Long> WALLTIME_END = new Value<>(Long.class, "WalltimeEnd", 0L);

    Command DELETE = new Command("Delete");

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
