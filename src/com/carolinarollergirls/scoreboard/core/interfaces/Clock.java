package com.carolinarollergirls.scoreboard.core.interfaces;

import java.util.ArrayList;
import java.util.Collection;

import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.Value;

public interface Clock extends ScoreBoardEventProvider {
    public ClockSnapshot snapshot();
    public void restoreSnapshot(ClockSnapshot s);

    public void start();
    public void stop();
    public void restart();

    public String getName();

    public int getNumber();
    public void setNumber(int n);
    public void changeNumber(int n);

    /**
     *
     * @return The time displayed on the clock (in ms)
     */
    public long getTime();
    public void setTime(long ms);
    /**
     * Add time to the clock.
     *
     * @param ms The amount of change (can be negative)
     */
    public void changeTime(long ms);
    /**
     *
     * @return The clock's maximum time minus the time displayed on the clock (in
     *         ms)
     */
    public long getInvertedTime();
    /**
     *
     * @return The time the clock has run (in ms). This is either the time or
     *         inverted time depending on the direction of the clock
     */
    public long getTimeElapsed();
    /**
     * Change the clock in the direction it is running. This function is the inverse
     * of changeTime(), when the clock counts down.
     *
     * @param ms The amount of change (can be negative)
     */
    public void elapseTime(long ms);
    public void resetTime();
    /**
     *
     * @return The time until the clock reaches its maximum or zero (in ms). This is
     *         the inverse of getTimeElapsed.
     */
    public long getTimeRemaining();
    public long getMaximumTime();
    public void setMaximumTime(long ms);
    public void changeMaximumTime(long ms);
    public boolean isTimeAtStart(long time);
    public boolean isTimeAtStart();
    public boolean isTimeAtEnd(long time);
    public boolean isTimeAtEnd();

    public boolean isRunning();

    public boolean isCountDirectionDown();
    public void setCountDirectionDown(boolean down);

    public long getCurrentIntermissionTime();

    public static interface ClockSnapshot {
        public String getId();
        public int getNumber();
        public long getTime();
        public boolean isRunning();
    }

    public static Collection<Property<?>> props = new ArrayList<>();

    public static final Value<String> NAME = new Value<>(String.class, "Name", "", props);
    public static final Value<Integer> NUMBER = new Value<>(Integer.class, "Number", 0, props);
    public static final Value<Long> TIME = new Value<>(Long.class, "Time", 0L, props);
    public static final Value<Long> INVERTED_TIME = new Value<>(Long.class, "InvertedTime", 0L, props);
    public static final Value<Long> MAXIMUM_TIME = new Value<>(Long.class, "MaximumTime", 0L, props);
    public static final Value<Boolean> DIRECTION = new Value<>(Boolean.class, "Direction", false, props);
    public static final Value<Boolean> RUNNING = new Value<>(Boolean.class, "Running", false, props);

    public static final Command START = new Command("Start", props);
    public static final Command STOP = new Command("Stop", props);
    public static final Command RESET_TIME = new Command("ResetTime", props);

    public static final String SETTING_SYNC = "ScoreBoard.Clock.Sync";

    public static final String ID_PERIOD = "Period";
    public static final String ID_JAM = "Jam";
    public static final String ID_LINEUP = "Lineup";
    public static final String ID_TIMEOUT = "Timeout";
    public static final String ID_INTERMISSION = "Intermission";
}
