package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface BoxTrip extends ScoreBoardEventProvider {
    public int compareTo(BoxTrip other);

    public void end();
    public void unend();
    public void startJam();
    public void stopJam();

    public Team getTeam();
    public Game getGame();
    public Clock getClock();

    public boolean isCurrent();
    public Fielding getCurrentFielding();
    public Fielding getStartFielding();
    public boolean startedBetweenJams();
    public boolean startedAfterSP();
    public Fielding getEndFielding();
    public boolean endedBetweenJams();
    public boolean endedAfterSP();

    public Clock.ClockSnapshot snapshot();
    public void restoreSnapshot(Clock.ClockSnapshot s);

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<Boolean> IS_CURRENT = new Value<>(Boolean.class, "IsCurrent", false, props);
    public static final Value<Fielding> CURRENT_FIELDING = new Value<>(Fielding.class, "CurrentFielding", null, props);
    public static final Value<Fielding> START_FIELDING = new Value<>(Fielding.class, "StartFielding", null, props);
    public static final Value<Integer> START_JAM_NUMBER = new Value<>(Integer.class, "StartJamNumber", 0, props);
    public static final Value<Boolean> START_BETWEEN_JAMS =
        new Value<>(Boolean.class, "StartBetweenJams", false, props);
    public static final Value<Boolean> START_AFTER_S_P = new Value<>(Boolean.class, "StartAfterSP", false, props);
    public static final Value<Fielding> END_FIELDING = new Value<>(Fielding.class, "EndFielding", null, props);
    public static final Value<Integer> END_JAM_NUMBER = new Value<>(Integer.class, "EndJamNumber", 0, props);
    public static final Value<Boolean> END_BETWEEN_JAMS = new Value<>(Boolean.class, "EndBetweenJams", false, props);
    public static final Value<Boolean> END_AFTER_S_P = new Value<>(Boolean.class, "EndAfterSP", false, props);
    public static final Value<Long> WALLTIME_START = new Value<>(Long.class, "WalltimeStart", 0L, props);
    public static final Value<Long> WALLTIME_END = new Value<>(Long.class, "WalltimeEnd", 0L, props);
    public static final Value<Long> JAM_CLOCK_START = new Value<>(Long.class, "JamClockStart", 0L, props);
    public static final Value<Long> JAM_CLOCK_END = new Value<>(Long.class, "JamClockEnd", 0L, props);
    public static final Value<Long> DURATION = new Value<>(Long.class, "Duration", 0L, props);
    public static final Value<Skater> CURRENT_SKATER = new Value<>(Skater.class, "CurrentSkater", null, props);
    public static final Value<String> ROSTER_NUMBER = new Value<>(String.class, "RosterNumber", "", props);
    public static final Value<String> PENALTY_CODES = new Value<>(String.class, "PenaltyCodes", "", props);
    public static final Value<Integer> TOTAL_PENALTIES = new Value<>(Integer.class, "TotalPenalties", null, props);
    public static final Value<Boolean> TIMING_STOPPED = new Value<>(Boolean.class, "TimingStopped", false, props);
    public static final Value<Long> TIME = new Value<>(Long.class, "Time", null, props);
    public static final Value<Integer> SHORTENED = new Value<>(Integer.class, "Shortened", 0, props);

    public static final Child<Fielding> FIELDING = new Child<>(Fielding.class, "Fielding", props);
    public static final Child<Penalty> PENALTY = new Child<>(Penalty.class, "Penalty", props);
    public static final Child<Clock> CLOCK = new Child<>(Clock.class, "Clock", props);

    public static final Command START_EARLIER = new Command("StartEarlier", props);
    public static final Command START_LATER = new Command("StartLater", props);
    public static final Command END_EARLIER = new Command("EndEarlier", props);
    public static final Command END_LATER = new Command("EndLater", props);
    public static final Command DELETE = new Command("Delete", props);
    public static final Command REMOVE_PENALTY = new Command("RemovePenalty", props);
}
