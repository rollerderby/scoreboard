package com.carolinarollergirls.scoreboard.core;

import com.carolinarollergirls.scoreboard.event.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.CommandProperty;
import com.carolinarollergirls.scoreboard.event.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

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

    // @formatter:off
    PermanentProperty<Boolean> IS_CURRENT = new PermanentProperty<>(Boolean.class, "IsCurrent", false);
    PermanentProperty<Fielding> CURRENT_FIELDING = new PermanentProperty<>(Fielding.class, "CurrentFielding", null);
    PermanentProperty<Fielding> START_FIELDING = new PermanentProperty<>(Fielding.class, "StartFielding", null);
    PermanentProperty<Integer> START_JAM_NUMBER = new PermanentProperty<>(Integer.class, "StartJamNumber", 0);
    PermanentProperty<Boolean> START_BETWEEN_JAMS = new PermanentProperty<>(Boolean.class, "StartBetweenJams", false);
    PermanentProperty<Boolean> START_AFTER_S_P = new PermanentProperty<>(Boolean.class, "StartAfterSP", false);
    PermanentProperty<Fielding> END_FIELDING = new PermanentProperty<>(Fielding.class, "EndFielding", null);
    PermanentProperty<Integer> END_JAM_NUMBER = new PermanentProperty<>(Integer.class, "EndJamNumber", 0);
    PermanentProperty<Boolean> END_BETWEEN_JAMS = new PermanentProperty<>(Boolean.class, "EndBetweenJams", false);
    PermanentProperty<Boolean> END_AFTER_S_P = new PermanentProperty<>(Boolean.class, "EndAfterSP", false);
    PermanentProperty<Long> WALLTIME_START = new PermanentProperty<>(Long.class, "WalltimeStart", 0L);
    PermanentProperty<Long> WALLTIME_END = new PermanentProperty<>(Long.class, "WalltimeEnd", 0L);
    PermanentProperty<Long> JAM_CLOCK_START = new PermanentProperty<>(Long.class, "JamClockStart", 0L);
    PermanentProperty<Long> JAM_CLOCK_END = new PermanentProperty<>(Long.class, "JamClockEnd", 0L);
    PermanentProperty<Long> DURATION = new PermanentProperty<>(Long.class, "Duration", 0L);

    AddRemoveProperty<Fielding> FIELDING = new AddRemoveProperty<>(Fielding.class, "Fielding");
    AddRemoveProperty<Penalty> PENALTY = new AddRemoveProperty<>(Penalty.class, "Penalty");

    CommandProperty START_EARLIER = new CommandProperty("StartEarlier");
    CommandProperty START_LATER = new CommandProperty("StartLater");
    CommandProperty END_EARLIER = new CommandProperty("EndEarlier");
    CommandProperty END_LATER = new CommandProperty("EndLater");
    CommandProperty DELETE = new CommandProperty("Delete");
    // @formatter:on
}
