package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.NumberedChild;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.Value;

public interface Period extends NumberedScoreBoardEventProvider<Period> {
    public Game getGame();

    public PeriodSnapshot snapshot();
    public void restoreSnapshot(PeriodSnapshot s);

    public boolean isSuddenScoring();

    public boolean isRunning();

    public Jam getJam(int j);
    public Jam getInitialJam();
    public Jam getCurrentJam();
    public int getCurrentJamNumber();

    public void startJam();
    public void stopJam();

    public long getDuration();
    public long getWalltimeStart();
    public long getWalltimeEnd();

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<Jam> CURRENT_JAM = new Value<>(Jam.class, "CurrentJam", null, props);
    public static final Value<Integer> CURRENT_JAM_NUMBER = new Value<>(Integer.class, "CurrentJamNumber", 0, props);
    public static final Value<Boolean> SUDDEN_SCORING = new Value<>(Boolean.class, "SuddenScoring", false, props);
    public static final Value<Boolean> RUNNING = new Value<>(Boolean.class, "Running", false, props);
    public static final Value<Long> DURATION = new Value<>(Long.class, "Duration", 0L, props);
    public static final Value<Long> WALLTIME_START = new Value<>(Long.class, "WalltimeStart", 0L, props);
    public static final Value<Long> WALLTIME_END = new Value<>(Long.class, "WalltimeEnd", 0L, props);
    public static final Value<String> LOCAL_TIME_START = new Value<>(String.class, "LocalTimeStart", "", props);
    public static final Value<Integer> TEAM_1_PENALTY_COUNT = new Value<>(Integer.class, "Team1PenaltyCount", 0, props);
    public static final Value<Integer> TEAM_2_PENALTY_COUNT = new Value<>(Integer.class, "Team2PenaltyCount", 0, props);
    public static final Value<Integer> TEAM_1_POINTS = new Value<>(Integer.class, "Team1Points", 0, props);
    public static final Value<Integer> TEAM_2_POINTS = new Value<>(Integer.class, "Team2Points", 0, props);

    public static final Child<Timeout> TIMEOUT = new Child<>(Timeout.class, "Timeout", props);

    public static final NumberedChild<Jam> JAM = new NumberedChild<>(Jam.class, "Jam", props);

    public static final Command DELETE = new Command("Delete", props);
    public static final Command INSERT_BEFORE = new Command("InsertBefore", props);
    public static final Command INSERT_TIMEOUT = new Command("InsertTimeout", props);
    public static final Command ADD_INITIAL_JAM = new Command("AddInitialJam", props);

    public static interface PeriodSnapshot {
        public String getId();
        public Jam getCurrentJam();
    }
}
