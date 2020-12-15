package com.carolinarollergirls.scoreboard.core.interfaces;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface BoxTrip extends ScoreBoardEventProvider {
    public int compareTo(BoxTrip other);

    public void end();
    public void unend();

    public Team getTeam();

    public boolean isCurrent();
    public Fielding getCurrentFielding();
    public Fielding getStartFielding();
    public boolean startedBetweenJams();
    public boolean startedAfterSP();
    public Fielding getEndFielding();
    public boolean endedBetweenJams();
    public boolean endedAfterSP();

    Value<Boolean> IS_CURRENT = new Value<>(Boolean.class, "IsCurrent", false);
    Value<Fielding> CURRENT_FIELDING = new Value<>(Fielding.class, "CurrentFielding", null);
    Value<Fielding> START_FIELDING = new Value<>(Fielding.class, "StartFielding", null);
    Value<Integer> START_JAM_NUMBER = new Value<>(Integer.class, "StartJamNumber", 0);
    Value<Boolean> START_BETWEEN_JAMS = new Value<>(Boolean.class, "StartBetweenJams", false);
    Value<Boolean> START_AFTER_S_P = new Value<>(Boolean.class, "StartAfterSP", false);
    Value<Fielding> END_FIELDING = new Value<>(Fielding.class, "EndFielding", null);
    Value<Integer> END_JAM_NUMBER = new Value<>(Integer.class, "EndJamNumber", 0);
    Value<Boolean> END_BETWEEN_JAMS = new Value<>(Boolean.class, "EndBetweenJams", false);
    Value<Boolean> END_AFTER_S_P = new Value<>(Boolean.class, "EndAfterSP", false);
    Value<Long> WALLTIME_START = new Value<>(Long.class, "WalltimeStart", 0L);
    Value<Long> WALLTIME_END = new Value<>(Long.class, "WalltimeEnd", 0L);
    Value<Long> JAM_CLOCK_START = new Value<>(Long.class, "JamClockStart", 0L);
    Value<Long> JAM_CLOCK_END = new Value<>(Long.class, "JamClockEnd", 0L);
    Value<Long> DURATION = new Value<>(Long.class, "Duration", 0L);

    Child<Fielding> FIELDING = new Child<>(Fielding.class, "Fielding");
    Child<Penalty> PENALTY = new Child<>(Penalty.class, "Penalty");

    Command START_EARLIER = new Command("StartEarlier");
    Command START_LATER = new Command("StartLater");
    Command END_EARLIER = new Command("EndEarlier");
    Command END_LATER = new Command("EndLater");
    Command DELETE = new Command("Delete");
}
