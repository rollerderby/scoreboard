package com.carolinarollergirls.scoreboard.core.interfaces;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface Jam extends NumberedScoreBoardEventProvider<Jam> {
    public Period getPeriod();

    public void setParent(ScoreBoardEventProvider p);

    public boolean isOvertimeJam();

    public long getDuration();
    public long getPeriodClockElapsedStart();
    public long getPeriodClockElapsedEnd();
    public long getWalltimeStart();
    public long getWalltimeEnd();

    public TeamJam getTeamJam(String id);

    public void start();
    public void stop();

    Value<Integer> PERIOD_NUMBER = new Value<>(Integer.class, "PeriodNumber", 0);
    Value<Boolean> STAR_PASS = new Value<>(Boolean.class, "StarPass", false); // true, if either team had an SP
    Value<Boolean> OVERTIME = new Value<>(Boolean.class, "Overtime", false);
    Value<Long> DURATION = new Value<>(Long.class, "Duration", 0L);
    Value<Long> PERIOD_CLOCK_ELAPSED_START = new Value<>(Long.class, "PeriodClockElapsedStart", 0L);
    Value<Long> PERIOD_CLOCK_ELAPSED_END = new Value<>(Long.class, "PeriodClockElapsedEnd", 0L);
    Value<Long> PERIOD_CLOCK_DISPLAY_END = new Value<>(Long.class, "PeriodClockDisplayEnd", 0L);
    Value<Long> WALLTIME_START = new Value<>(Long.class, "WalltimeStart", 0L);
    Value<Long> WALLTIME_END = new Value<>(Long.class, "WalltimeEnd", 0L);

    Child<TeamJam> TEAM_JAM = new Child<>(TeamJam.class, "TeamJam");
    Child<Penalty> PENALTY = new Child<>(Penalty.class, "Penalty");
    Child<Timeout> TIMEOUTS_AFTER = new Child<>(Timeout.class, "TimeoutsAfter");

    Command DELETE = new Command("Delete");
    Command INSERT_BEFORE = new Command("InsertBefore");
}
