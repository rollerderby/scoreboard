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
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ClockConversion;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class ClockImpl extends ScoreBoardEventProviderImpl implements Clock {
    public ClockImpl(ScoreBoard sb, String i) {
        super (sb, Value.ID, ScoreBoard.Child.CLOCK, Clock.class, Value.class, Command.class);
        set(Value.ID, i);
        //initialize types
        if (i == ID_PERIOD || i == ID_INTERMISSION) {
            addReference(new ValueReference(this, Value.NUMBER, sb, ScoreBoard.Value.CURRENT_PERIOD_NUMBER, true, 0));
        } else if (i == ID_JAM) {
            addReference(new IndirectValueReference(this, Value.NUMBER, sb, ScoreBoard.Value.CURRENT_PERIOD,
                    Period.Value.CURRENT_JAM_NUMBER, true, 0));
        } else {
            values.put(Value.NUMBER, 0);
        }
        values.put(Value.MINIMUM_TIME, 0L);
        values.put(Value.MAXIMUM_TIME, 0L);
        values.put(Value.TIME, 0L);
        values.put(Value.INVERTED_TIME, 0L);
        values.put(Value.MINIMUM_NUMBER, 0);
        values.put(Value.MAXIMUM_NUMBER, 0);
        values.put(Value.DIRECTION, false);
        values.put(Value.RUNNING, false);
        addReference(new UpdateReference(this, Value.NUMBER, this, Value.MAXIMUM_NUMBER));
        addReference(new UpdateReference(this, Value.NUMBER, this, Value.MINIMUM_NUMBER));
        addReference(new UpdateReference(this, Value.MAXIMUM_NUMBER, this, Value.MINIMUM_NUMBER));
        addReference(new UpdateReference(this, Value.TIME, this, Value.MAXIMUM_TIME));
        addReference(new UpdateReference(this, Value.TIME, this, Value.MINIMUM_TIME));
        addReference(new UpdateReference(this, Value.MAXIMUM_TIME, this, Value.MINIMUM_TIME));
        addReference(new UpdateReference(this, Value.INVERTED_TIME, this, Value.MAXIMUM_TIME));
        addReference(new UpdateReference(this, Value.INVERTED_TIME, this, Value.TIME));
        addWriteProtectionOverride(Value.INVERTED_TIME, Flag.INTERNAL);

        sb.addScoreBoardListener(new ConditionalScoreBoardListener(Rulesets.class, Rulesets.Value.CURRENT_RULESET_ID, rulesetChangeListener));
    }

    protected Object computeValue(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == Value.TIME) {
            if (isRunning() && isSyncTime()) {
                if (flag == Flag.CHANGE) {
                    value = (Long)last + ((((Long)value - (Long)last) / 1000L) * 1000L);
                }
                else if (flag != Flag.INTERNAL) {
                    value = (((Long)value / 1000L) * 1000L) + (Long)last % 1000L;
                }
            }
            if ((flag == Flag.RESET && isCountDirectionDown()) || (Long)value > getMaximumTime() + 500) {
                return getMaximumTime();
            }
            if ((flag == Flag.RESET && !isCountDirectionDown()) || (Long)value < getMinimumTime() - 500) {
                return getMinimumTime();
            }
        }
        if (prop == Value.INVERTED_TIME) {
            return getMaximumTime() - getTime();
        }
        if (prop == Value.MAXIMUM_TIME && (Long)value < getMinimumTime()) {
            return getMinimumTime();
        }
        if ((prop == Value.MAXIMUM_NUMBER || prop == Value.NUMBER) && (Integer)value < getMinimumNumber()) {
            return getMinimumNumber();
        }
        if (prop == Value.NUMBER && (Integer)value > getMaximumNumber()) {
            return getMaximumNumber();
        }
        return value;
    }
    protected void valueChanged(PermanentProperty prop, Object value, Object last, Flag flag) {
        if (prop == Value.TIME && isTimeAtEnd()) {
            stop();
        }
        if (prop == Value.MAXIMUM_TIME && isCountDirectionDown()) {
            changeTime((Long)value - (Long)last);
        }
        if (prop == Value.DIRECTION) {
            setTime(getInvertedTime());
        }
        if (prop == Value.RUNNING) {
            if ((Boolean)value) {
                updateClockTimerTask.addClock(this, flag == Flag.INTERNAL);
            } else {
                updateClockTimerTask.removeClock(this);
            }
        }
    }

    public void execute(CommandProperty prop) {
        switch((Command)prop) {
        case RESET_TIME:
            resetTime();
            break;
        case START:
            start();
            break;
        case STOP:
            stop();
            break;
        }
    }

    public void reset() {
        synchronized (coreLock) {
            stop();

            setName(getId());

            // Pull in settings.
            rulesetChangeListener.scoreBoardChange(null);

            // We hardcode the assumption that numbers count up.
            setNumber(getMinimumNumber());

            resetTime();
        }
    }

    protected ScoreBoardListener rulesetChangeListener = new ScoreBoardListener() {
        public void scoreBoardChange(ScoreBoardEvent event) {
            // Get default values from current settings or use hardcoded values
            Rulesets r = getScoreBoard().getRulesets();
            setCountDirectionDown(Boolean.parseBoolean(r.get(Rulesets.Child.CURRENT_RULE, getId() + ".ClockDirection").getValue()));
            if (getId().equals(ID_JAM) || getId().equals(ID_INTERMISSION) || getId().equals(ID_PERIOD)) {
                setMinimumNumber(0);
            } else {
                setMinimumNumber(DEFAULT_MINIMUM_NUMBER);
            }
            if (getId().equals(ID_PERIOD) || getId().equals(ID_INTERMISSION)) {
                setMaximumNumber(r.getInt(Rule.NUMBER_PERIODS));
            } else {
                setMaximumNumber(DEFAULT_MAXIMUM_NUMBER);
            }
            setMinimumTime(DEFAULT_MINIMUM_TIME);
            if (getId().equals(ID_PERIOD) || getId().equals(ID_JAM)) {
                setMaximumTime(ClockConversion.fromHumanReadable(r.get(Rulesets.Child.CURRENT_RULE, getId() + ".Duration").getValue()));
            } else {
                setMaximumTime(DEFAULT_MAXIMUM_TIME);
            }
        }
    };

    public ClockSnapshot snapshot() {
        synchronized (coreLock) {
            return new ClockSnapshotImpl(this);
        }
    }
    public void restoreSnapshot(ClockSnapshot s) {
        synchronized (coreLock) {
            if (s.getId() != getId()) { return; }
            setNumber(s.getNumber());
            setTime(s.getTime());
            if (s.isRunning()) {
                start();
            } else {
                stop();
            }
        }
    }

    public String getName() { return (String)get(Value.NAME); }
    public void setName(String n) { set(Value.NAME, n); }

    public int getNumber() { return (Integer)get(Value.NUMBER); }
    public void setNumber(int n) { set(Value.NUMBER, n); }
    public void changeNumber(int change) { set(Value.NUMBER, change, Flag.CHANGE); }

    public int getMinimumNumber() { return (Integer)get(Value.MINIMUM_NUMBER); }
    public void setMinimumNumber(int n) { set(Value.MINIMUM_NUMBER, n); }
    public void changeMinimumNumber(int change) { set(Value.MINIMUM_NUMBER, change, Flag.CHANGE); }

    public int getMaximumNumber() { return (Integer)get(Value.MAXIMUM_NUMBER); }
    public void setMaximumNumber(int n) { set(Value.MAXIMUM_NUMBER, n); }
    public void changeMaximumNumber(int change) { set(Value.MAXIMUM_NUMBER, change, Flag.CHANGE); }

    public long getTime() { return (Long)get(Value.TIME); }
    public long getInvertedTime() { return (Long)get(Value.INVERTED_TIME); }
    public long getTimeElapsed() {
        synchronized (coreLock) {
            return isCountDirectionDown()?getInvertedTime():getTime();
        }
    }
    public long getTimeRemaining() {
        synchronized (coreLock) {
            return isCountDirectionDown()?getTime():getInvertedTime();
        }
    }
    public void setTime(long ms) { set(Value.TIME, ms); }
    public void changeTime(long change) { set(Value.TIME, change, Flag.CHANGE); }
    public void elapseTime(long change) {
        synchronized (coreLock) {
            changeTime(isCountDirectionDown()?-change:change);
        }
    }
    public boolean resetTime() { return set(Value.TIME, getTime(), Flag.RESET); }
    protected boolean isDisplayChange(long current, long last) {
        //the frontend rounds values that are not full seconds to the earlier second
        //i.e. 3600ms will be displayed as 3s on a count up clock and as 4s on a count down clock.
        if (isCountDirectionDown()) {
            return Math.floor(((float)current-1)/1000) != Math.floor(((float)last-1)/1000);
        } else {
            return Math.floor((float)current/1000) != Math.floor((float)last/1000);
        }
    }

    public long getMinimumTime() { return (Long)get(Value.MINIMUM_TIME); }
    public void setMinimumTime(long ms) { set(Value.MINIMUM_TIME, ms); }
    public void changeMinimumTime(long change) { set(Value.MINIMUM_TIME, change, Flag.CHANGE); }
    public long getMaximumTime() { return (Long)get(Value.MAXIMUM_TIME); }
    public void setMaximumTime(long ms) { set(Value.MAXIMUM_TIME, ms); }
    public void changeMaximumTime(long change) { set(Value.MAXIMUM_TIME, change, Flag.CHANGE); }
    public boolean isTimeAtStart(long t) {
        synchronized (coreLock) {
            if (isCountDirectionDown()) {
                return t == getMaximumTime();
            } else {
                return t == getMinimumTime();
            }
        }
    }
    public boolean isTimeAtStart() { return isTimeAtStart(getTime()); }
    public boolean isTimeAtEnd(long t) {
        synchronized (coreLock) {
            if (isCountDirectionDown()) {
                return t == getMinimumTime();
            } else {
                return t == getMaximumTime();
            }
        }
    }
    public boolean isTimeAtEnd() { return isTimeAtEnd(getTime()); }

    public boolean isCountDirectionDown() { return ((Boolean)get(Value.DIRECTION)).booleanValue(); }
    public void setCountDirectionDown(boolean down) { set(Value.DIRECTION, down); }

    public boolean isRunning() { return (Boolean)get(Value.RUNNING); }

    public void start() { start(false); }
    public void start(boolean quickAdd) { set(Value.RUNNING, Boolean.TRUE, quickAdd ? Flag.INTERNAL : null); }
    public void stop() { set(Value.RUNNING, Boolean.FALSE); }

    public void restart() {
        synchronized (coreLock) {
            requestBatchStart();
            resetTime();
            start();
            requestBatchEnd();
        }
    }

    protected void timerTick(long delta) {
        if (!isRunning()) { return; }
        lastTime += delta;
        long newTime = isCountDirectionDown()?getTime()-delta:getTime()+delta;
        long newInvertedTime = getMaximumTime() - newTime;
        if (isDisplayChange(newTime, getTime())) {
            set(Value.TIME, newTime, Flag.INTERNAL);
        } else {
            values.put(Value.TIME, newTime);
            values.put(Value.INVERTED_TIME, newInvertedTime);
        }
    }

    protected boolean isSyncTime() {
        return Boolean.parseBoolean(getScoreBoard().getSettings().get(SETTING_SYNC));
    }

    protected boolean isMasterClock() {
        return getId() == ID_PERIOD || getId() == ID_TIMEOUT || getId() == ID_INTERMISSION;
    }

    protected long lastTime;
    protected boolean isRunning = false;

    public static UpdateClockTimerTask updateClockTimerTask = new UpdateClockTimerTask();

    public static final int DEFAULT_MINIMUM_NUMBER = 1;
    public static final int DEFAULT_MAXIMUM_NUMBER = 999;
    public static final long DEFAULT_MINIMUM_TIME = 0;
    public static final long DEFAULT_MAXIMUM_TIME = 24 * 60 * 60 * 1000; // 1 day for long time to derby
    public static final boolean DEFAULT_DIRECTION = false;   // up

    public static class ClockSnapshotImpl implements ClockSnapshot {
        private ClockSnapshotImpl(Clock clock) {
            id = clock.getId();
            number = clock.getNumber();
            time = clock.getTime();
            isRunning = clock.isRunning();
        }

        public String getId() { return id; }
        public int getNumber() { return number; }
        public long getTime() { return time; }
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
                if (c.isMasterClock()) {
                    masterClock = c;
                }
                if (c.isSyncTime() && !quickAdd) {
                    // This syncs all the clocks to change second at the same time
                    // with respect to the master clock
                    long nowMs = currentTime % 1000;
                    if (masterClock != null) {
                        nowMs = masterClock.getTime() % 1000;
                        if (masterClock.isCountDirectionDown()) {
                            nowMs = (1000 - nowMs) % 1000;
                        }
                    }

                    long timeMs = c.getTime() % 1000;
                    if (c.isCountDirectionDown()) {
                        timeMs = (1000 - timeMs) % 1000;
                    }
                    long delay = timeMs - nowMs;
                    if (Math.abs(delay) >= 500) {
                        delay = (long)(Math.signum((float)-delay) * (1000 - Math.abs(delay)));
                    }
                    c.lastTime = currentTime;
                    if (c.isCountDirectionDown()) {
                        delay = -delay;
                    }
                    c.values.put(Value.TIME, c.getTime() - delay);
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
            ArrayList<ClockImpl> clocks;
            synchronized (coreLock) {
                currentTime += update_interval;
                clocks = new ArrayList<ClockImpl>(this.clocks);
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

        public void updateTime(long time) {
            long curSystemTime = time;
            long curTicks = (curSystemTime - startSystemTime) / update_interval;
            while (curTicks > ticks) {
                ticks++;
                tick();
            }
        }

        public long getCurrentTime() {
            return currentTime;
        }

        private ScoreBoardClock scoreBoardClock = ScoreBoardClock.getInstance();
        private long currentTime = 0;
        private long startSystemTime = 0;
        private long ticks = 0;
        protected ClockImpl masterClock = null;
        ArrayList<ClockImpl> clocks = new ArrayList<ClockImpl>();
    }
}
