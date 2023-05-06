package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface Jam extends NumberedScoreBoardEventProvider<Jam> {
    public Period getPeriod();

    public void setParent(ScoreBoardEventProvider p);

    public boolean isOvertimeJam();
    public boolean isInjuryContinuation();
    public boolean isImmediateScoring();

    public long getDuration();
    public long getPeriodClockElapsedStart();
    public long getPeriodClockElapsedEnd();
    public long getWalltimeStart();
    public long getWalltimeEnd();

    public TeamJam getTeamJam(String id);

    public void start();
    public void stop();

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<Integer> PERIOD_NUMBER = new Value<>(Integer.class, "PeriodNumber", 0, props);
    public static final Value<Boolean> STAR_PASS =
        new Value<>(Boolean.class, "StarPass", false, props); // true, if either team had an SP
    public static final Value<Boolean> OVERTIME = new Value<>(Boolean.class, "Overtime", false, props);
    public static final Value<Boolean> INJURY_CONTINUATION =
        new Value<>(Boolean.class, "InjuryContinuation", false, props);
    public static final Value<Long> DURATION = new Value<>(Long.class, "Duration", 0L, props);
    public static final Value<Long> PERIOD_CLOCK_ELAPSED_START =
        new Value<>(Long.class, "PeriodClockElapsedStart", 0L, props);
    public static final Value<Long> PERIOD_CLOCK_ELAPSED_END =
        new Value<>(Long.class, "PeriodClockElapsedEnd", 0L, props);
    public static final Value<Long> PERIOD_CLOCK_DISPLAY_END =
        new Value<>(Long.class, "PeriodClockDisplayEnd", 0L, props);
    public static final Value<Long> WALLTIME_START = new Value<>(Long.class, "WalltimeStart", 0L, props);
    public static final Value<Long> WALLTIME_END = new Value<>(Long.class, "WalltimeEnd", 0L, props);

    public static final Child<TeamJam> TEAM_JAM = new Child<>(TeamJam.class, "TeamJam", props);
    public static final Child<Penalty> PENALTY = new Child<>(Penalty.class, "Penalty", props);
    public static final Child<Timeout> TIMEOUTS_AFTER = new Child<>(Timeout.class, "TimeoutsAfter", props);

    public static final Command DELETE = new Command("Delete", props);
    public static final Command INSERT_BEFORE = new Command("InsertBefore", props);
    public static final Command INSERT_TIMEOUT_AFTER = new Command("InsertTimeoutAfter", props);
}
