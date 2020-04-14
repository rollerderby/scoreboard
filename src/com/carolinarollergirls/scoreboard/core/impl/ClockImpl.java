package com.carolinarollergirls.scoreboard.core.impl;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.ArrayList;
import java.util.Iterator;

import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.Period;
import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ClockConversion;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class ClockImpl extends ScoreBoardEventProviderImpl<Clock> implements Clock {
    public ClockImpl(ScoreBoard sb, String i) {
        super(sb, i, ScoreBoard.CLOCK);
        addProperties(NAME, NUMBER, TIME, INVERTED_TIME, MAXIMUM_TIME, DIRECTION, RUNNING, START, STOP, RESET_TIME);
        // initialize types
        if (i == ID_PERIOD || i == ID_INTERMISSION) {
            setCopy(NUMBER, sb, ScoreBoard.CURRENT_PERIOD_NUMBER, true);
        } else if (i == ID_JAM) {
            setCopy(NUMBER, sb, ScoreBoard.CURRENT_PERIOD, Period.CURRENT_JAM_NUMBER, true);
        } else {
            values.put(NUMBER, 0);
        }
        setRecalculated(TIME).addSource(this, MAXIMUM_TIME);
        setRecalculated(INVERTED_TIME).addSource(this, MAXIMUM_TIME).addSource(this, TIME);

        sb.addScoreBoardListener(
                new ConditionalScoreBoardListener<>(Rulesets.class, Rulesets.CURRENT_RULESET, rulesetChangeListener));
    }

    @Override
    protected Object computeValue(PermanentProperty<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == TIME) {
            if (isRunning() && isSyncTime()) {
                if (flag == Flag.CHANGE) {
                    value = (Long) last + ((((Long) value - (Long) last) / 1000L) * 1000L);
                } else if (flag != Flag.SPECIAL_CASE) {
                    value = (((Long) value / 1000L) * 1000L) + (Long) last % 1000L;
                }
            }
            if ((flag == Flag.RESET && isCountDirectionDown()) || (Long) value > getMaximumTime() + 500
                    && (!isCountDirectionDown() || source != Source.RECALCULATE)) {
                return getMaximumTime();
            }
            if ((flag == Flag.RESET && !isCountDirectionDown()) || (Long) value < 0 - 500) {
                return Long.valueOf(0);
            }
        }
        if (prop == INVERTED_TIME) {
            return getMaximumTime() - getTime();
        }
        if (prop == MAXIMUM_TIME && (Long) value < 0) {
            return Long.valueOf(0);
        }
        if (prop == NUMBER && (Integer) value < 0) {
            return 0;
        }
        return value;
    }
    @Override
    protected void valueChanged(PermanentProperty<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == TIME && isTimeAtEnd()) {
            stop();
        }
        if (prop == MAXIMUM_TIME && isCountDirectionDown() && !source.isFile()) {
            changeTime((Long) value - (Long) last);
        }
        if (prop == DIRECTION) {
            setTime(getInvertedTime());
        }
        if (prop == RUNNING) {
            if ((Boolean) value) {
                updateClockTimerTask.addClock(this, flag == Flag.SPECIAL_CASE);
            } else {
                updateClockTimerTask.removeClock(this);
            }
        }
    }

    @Override
    public void execute(CommandProperty prop, Source source) {
        if (prop == RESET_TIME) {
            resetTime();
        } else if (prop == START) {
            start();
        } else if (prop == STOP) {
            stop();
        }
    }

    @Override
    public void reset() {
        synchronized (coreLock) {
            stop();

            setName(getId());

            // Pull in settings.
            rulesetChangeListener.scoreBoardChange(null);

            // We hardcode the assumption that numbers count up.
            setNumber(0);

            resetTime();
        }
    }

    protected ScoreBoardListener rulesetChangeListener = new ScoreBoardListener() {
        @Override
        public void scoreBoardChange(ScoreBoardEvent<?> event) {
            // Get default values from current settings or use hardcoded values
            Rulesets r = getScoreBoard().getRulesets();
            setCountDirectionDown(
                    Boolean.parseBoolean(r.get(Rulesets.CURRENT_RULE, getId() + ".ClockDirection").getValue()));
            if (getId().equals(ID_PERIOD) || getId().equals(ID_JAM)) {
                setMaximumTime(ClockConversion
                        .fromHumanReadable(r.get(Rulesets.CURRENT_RULE, getId() + ".Duration").getValue()));
            } else if (getId().equals(ID_INTERMISSION)) {
                setMaximumTime(getCurrentIntermissionTime());
            } else if (getId().equals(ID_LINEUP) && isCountDirectionDown()) {
                if (getScoreBoard().isInOvertime()) {
                    setMaximumTime(r.getLong(Rule.OVERTIME_LINEUP_DURATION));
                } else {
                    setMaximumTime(r.getLong(Rule.LINEUP_DURATION));
                }
            } else {
                setMaximumTime(DEFAULT_MAXIMUM_TIME);
            }
        }
    };

    @Override
    public ClockSnapshot snapshot() {
        synchronized (coreLock) {
            return new ClockSnapshotImpl(this);
        }
    }
    @Override
    public void restoreSnapshot(ClockSnapshot s) {
        synchronized (coreLock) {
            if (s.getId() != getId()) { return; }
            setNumber(s.getNumber());
            set(TIME, s.getTime(), Flag.SPECIAL_CASE);
            if (s.isRunning()) {
                start();
            } else {
                stop();
            }
        }
    }

    @Override
    public String getName() { return get(NAME); }
    public void setName(String n) { set(NAME, n); }

    @Override
    public int getNumber() { return get(NUMBER); }
    @Override
    public void setNumber(int n) { set(NUMBER, n); }
    @Override
    public void changeNumber(int change) { set(NUMBER, change, Flag.CHANGE); }

    @Override
    public long getTime() { return get(TIME); }
    @Override
    public long getInvertedTime() { return get(INVERTED_TIME); }
    @Override
    public long getTimeElapsed() {
        synchronized (coreLock) {
            return isCountDirectionDown() ? getInvertedTime() : getTime();
        }
    }
    @Override
    public long getTimeRemaining() {
        synchronized (coreLock) {
            return isCountDirectionDown() ? getTime() : getInvertedTime();
        }
    }
    @Override
    public void setTime(long ms) { set(TIME, ms); }
    @Override
    public void changeTime(long change) { set(TIME, change, Flag.CHANGE); }
    @Override
    public void elapseTime(long change) {
        synchronized (coreLock) {
            changeTime(isCountDirectionDown() ? -change : change);
        }
    }
    @Override
    public void resetTime() { set(TIME, getTime(), Flag.RESET); }
    protected boolean isDisplayChange(long current, long last) {
        // the frontend rounds values that are not full seconds to the earlier second
        // i.e. 3600ms will be displayed as 3s on a count up clock and as 4s on a count
        // down clock.
        if (isCountDirectionDown()) {
            return Math.floor(((double) current - 1) / 1000) != Math.floor(((double) last - 1) / 1000);
        } else {
            return Math.floor((double) current / 1000) != Math.floor((double) last / 1000);
        }
    }

    @Override
    public long getMaximumTime() { return get(MAXIMUM_TIME); }
    @Override
    public void setMaximumTime(long ms) { set(MAXIMUM_TIME, ms); }
    @Override
    public void changeMaximumTime(long change) { set(MAXIMUM_TIME, change, Flag.CHANGE); }
    @Override
    public boolean isTimeAtStart(long t) {
        synchronized (coreLock) {
            if (isCountDirectionDown()) {
                return t == getMaximumTime();
            } else {
                return t == 0;
            }
        }
    }
    @Override
    public boolean isTimeAtStart() { return isTimeAtStart(getTime()); }
    @Override
    public boolean isTimeAtEnd(long t) {
        synchronized (coreLock) {
            if (isCountDirectionDown()) {
                return t == 0;
            } else {
                return t == getMaximumTime();
            }
        }
    }
    @Override
    public boolean isTimeAtEnd() { return isTimeAtEnd(getTime()); }

    @Override
    public boolean isCountDirectionDown() { return get(DIRECTION); }
    @Override
    public void setCountDirectionDown(boolean down) { set(DIRECTION, down); }

    @Override
    public boolean isRunning() { return get(RUNNING); }

    @Override
    public void start() { start(false); }
    public void start(boolean quickAdd) { set(RUNNING, Boolean.TRUE, quickAdd ? Flag.SPECIAL_CASE : null); }
    @Override
    public void stop() { set(RUNNING, Boolean.FALSE); }

    @Override
    public void restart() {
        synchronized (coreLock) {
            resetTime();
            start();
        }
    }

    protected void timerTick(long delta) {
        if (!isRunning()) { return; }
        lastTime += delta;
        long newTime = isCountDirectionDown() ? getTime() - delta : getTime() + delta;
        long newInvertedTime = getMaximumTime() - newTime;
        if (isDisplayChange(newTime, getTime())) {
            set(TIME, newTime, Flag.SPECIAL_CASE);
        } else {
            values.put(TIME, newTime);
            values.put(INVERTED_TIME, newInvertedTime);
        }
    }

    @Override
    public long getCurrentIntermissionTime() {
        long duration = DEFAULT_MAXIMUM_TIME;
        String[] sequence = getScoreBoard().getRulesets().get(Rule.INTERMISSION_DURATIONS).split(",");
        int number = Math.min(getScoreBoard().getCurrentPeriodNumber(), sequence.length);
        if (number > 0) {
            duration = ClockConversion.fromHumanReadable(sequence[number - 1]);
        }
        return duration;
    }

    protected boolean isSyncTime() { return Boolean.parseBoolean(getScoreBoard().getSettings().get(SETTING_SYNC)); }

    protected long lastTime;
    protected boolean isRunning = false;

    public static UpdateClockTimerTask updateClockTimerTask = new UpdateClockTimerTask();

    public static final long DEFAULT_MAXIMUM_TIME = 24 * 60 * 60 * 1000; // 1 day for long time to derby
    public static final boolean DEFAULT_DIRECTION = false; // up

    public static class ClockSnapshotImpl implements ClockSnapshot {
        private ClockSnapshotImpl(Clock clock) {
            id = clock.getId();
            number = clock.getNumber();
            time = clock.getTime();
            isRunning = clock.isRunning();
        }

        @Override
        public String getId() { return id; }
        @Override
        public int getNumber() { return number; }
        @Override
        public long getTime() { return time; }
        @Override
        public boolean isRunning() { return isRunning; }

        protected String id;
        protected int number;
        protected long time;
        protected boolean isRunning;
    }

    protected static class UpdateClockTimerTask implements ScoreBoardClock.ScoreBoardClockClient {
        private static long update_interval = ScoreBoardClock.CLOCK_UPDATE_INTERVAL;

        public UpdateClockTimerTask() {
            startSystemTime = scoreBoardClock.getCurrentTime();
            ScoreBoardClock.getInstance().registerClient(this);
        }

        public void addClock(ClockImpl c, boolean quickAdd) {
            synchronized (coreLock) {
                if (c.isSyncTime() && !quickAdd && !clocks.isEmpty()) {
                    // This syncs all the clocks to change second at the same time
                    // with respect to the running clocks.
                    ClockImpl masterClock = clocks.get(0);
                    long nowMs = masterClock.getTime() % 1000;
                    if (masterClock.isCountDirectionDown()) {
                        nowMs = (1000 - nowMs) % 1000;
                    }

                    long timeMs = c.getTime() % 1000;
                    if (c.isCountDirectionDown()) {
                        timeMs = (1000 - timeMs) % 1000;
                    }
                    long delay = timeMs - nowMs;
                    if (Math.abs(delay) >= 500) {
                        delay = (long) (Math.signum(-delay) * (1000 - Math.abs(delay)));
                    }
                    c.lastTime = currentTime;
                    if (c.isCountDirectionDown()) {
                        delay = -delay;
                    }
                    c.values.put(TIME, c.getTime() - delay);
                } else {
                    c.lastTime = currentTime;
                }
                clocks.add(c);
            }
        }

        public void removeClock(ClockImpl c) {
            synchronized (coreLock) {
                clocks.remove(c);
            }
        }

        private void tick() {
            Iterator<ClockImpl> i;
            @SuppressWarnings("hiding")
            ArrayList<ClockImpl> clocks;
            synchronized (coreLock) {
                currentTime += update_interval;
                clocks = new ArrayList<>(this.clocks);
            }
            ClockImpl clock;
            i = clocks.iterator();
            while (i.hasNext()) {
                clock = i.next();
                clock.requestBatchStart();
            }
            i = clocks.iterator();
            while (i.hasNext()) {
                clock = i.next();
                clock.timerTick(update_interval);
            }
            i = clocks.iterator();
            while (i.hasNext()) {
                clock = i.next();
                clock.requestBatchEnd();
            }
        }

        @Override
        public void updateTime(long time) {
            long curSystemTime = time;
            long curTicks = (curSystemTime - startSystemTime) / update_interval;
            while (curTicks > ticks) {
                ticks++;
                tick();
            }
        }

        public long getCurrentTime() { return currentTime; }

        private ScoreBoardClock scoreBoardClock = ScoreBoardClock.getInstance();
        private long currentTime = 0;
        private long startSystemTime = 0;
        private long ticks = 0;
        ArrayList<ClockImpl> clocks = new ArrayList<>();
    }
}
