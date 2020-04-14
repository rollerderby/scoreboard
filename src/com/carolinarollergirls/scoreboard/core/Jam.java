package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.CommandProperty;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public interface Jam extends NumberedScoreBoardEventProvider<Jam> {
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

    // @formatter:off
    PermanentProperty<Integer> PERIOD_NUMBER = new PermanentProperty<>(Integer.class, "PeriodNumber", 0);
    PermanentProperty<Boolean> STAR_PASS = new PermanentProperty<>(Boolean.class, "StarPass", false); // true, if either team had an SP
    PermanentProperty<Boolean> OVERTIME = new PermanentProperty<>(Boolean.class, "Overtime", false);
    PermanentProperty<Long> DURATION = new PermanentProperty<>(Long.class, "Duration", 0L);
    PermanentProperty<Long> PERIOD_CLOCK_ELAPSED_START = new PermanentProperty<>(Long.class, "PeriodClockElapsedStart", 0L);
    PermanentProperty<Long> PERIOD_CLOCK_ELAPSED_END = new PermanentProperty<>(Long.class, "PeriodClockElapsedEnd", 0L);
    PermanentProperty<Long> PERIOD_CLOCK_DISPLAY_END = new PermanentProperty<>(Long.class, "PeriodClockDisplayEnd", 0L);
    PermanentProperty<Long> WALLTIME_START = new PermanentProperty<>(Long.class, "WalltimeStart", 0L);
    PermanentProperty<Long> WALLTIME_END = new PermanentProperty<>(Long.class, "WalltimeEnd", 0L);

    AddRemoveProperty<TeamJam> TEAM_JAM = new AddRemoveProperty<>(TeamJam.class, "TeamJam");
    AddRemoveProperty<Penalty> PENALTY = new AddRemoveProperty<>(Penalty.class, "Penalty");
    AddRemoveProperty<Timeout> TIMEOUTS_AFTER = new AddRemoveProperty<>(Timeout.class, "TimeoutsAfter");

    CommandProperty DELETE = new CommandProperty("Delete");
    CommandProperty INSERT_BEFORE = new CommandProperty("InsertBefore");
    // @formatter:on
}
