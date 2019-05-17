package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public interface Clock extends ScoreBoardEventProvider {
    public void reset();

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
     * @param ms The amount of change (can be negative)
     */
    public void changeTime(long ms);
    /**
     *
     * @return The clock's maximum time minus the time displayed on the clock (in ms)
     */
    public long getInvertedTime();
    /**
     *
     * @return The time the clock has run (in ms). This is either the time or inverted time depending on the direction of the clock
     */
    public long getTimeElapsed();
    /**
     * Change the clock in the direction it is running.
     * This function is the inverse of changeTime(), when the clock counts down.
     * @param ms The amount of change (can be negative)
     */
    public void elapseTime(long ms);
    public void resetTime();
    /**
     *
     * @return The time until the clock reaches its maximum or zero (in ms). This is the inverse of getTimeElapsed.
     */
    public long getTimeRemaining();
    public long getMinimumTime();
    public void setMinimumTime(long ms);
    public void changeMinimumTime(long ms);
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

    public static interface ClockSnapshot {
        public String getId();
        public int getNumber();
        public long getTime();
        public boolean isRunning();
    }

    public enum Value implements PermanentProperty {
        ID(String.class, ""),
        NAME(String.class, ""),
        NUMBER(Integer.class, 0),
        TIME(Long.class, 0L),
        INVERTED_TIME(Long.class, 0L),
        MINIMUM_TIME(Long.class, 0L),
        MAXIMUM_TIME(Long.class, 0L),
        DIRECTION(Boolean.class, false),
        RUNNING(Boolean.class, false);

        private Value(Class<?> t, Object dv) { type = t; defaultValue = dv; }
        private final Class<?> type;
        private final Object defaultValue;
        @Override
        public Class<?> getType() { return type; }
        @Override
        public Object getDefaultValue() { return defaultValue; }
    }
    public enum Command implements CommandProperty {
        START,
        STOP,
        RESET_TIME;
        
        @Override
        public Class<Boolean> getType() { return Boolean.class; }
    }

    public static final String SETTING_SYNC = "ScoreBoard.Clock.Sync";

    public static final String ID_PERIOD = "Period";
    public static final String ID_JAM = "Jam";
    public static final String ID_LINEUP = "Lineup";
    public static final String ID_TIMEOUT = "Timeout";
    public static final String ID_INTERMISSION = "Intermission";
}
