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
import java.util.List;

import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class ClockImpl extends DefaultScoreBoardEventProvider implements Clock {
    public ClockImpl(ScoreBoard sb, String i) {
        scoreBoard = sb;
        id = i;

        sb.addScoreBoardListener(new ConditionalScoreBoardListener(Rulesets.class, Rulesets.Value.RULESET, rulesetChangeListener));

        reset();
    }

    public String getProviderName() { return PropertyConversion.toFrontend(ScoreBoard.Child.CLOCK); }
    public Class<Clock> getProviderClass() { return Clock.class; }
    public String getProviderId() { return getId(); }
    public ScoreBoardEventProvider getParent() { return scoreBoard; }
    public List<Class<? extends Property>> getProperties() { return properties; }

    public ScoreBoard getScoreBoard() { return scoreBoard; }

    public String getId() { return id; }

    public Clock getClock() { return this; }

    public void reset() {
        synchronized (coreLock) {
            stop();

            setName(id);

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
            setCountDirectionDown(r.getBoolean(r.getRule(id + ".ClockDirection")));
            if (id.equals(ID_JAM) || id.equals(ID_INTERMISSION)) {
                setMinimumNumber(0);
            } else {
                setMinimumNumber(DEFAULT_MINIMUM_NUMBER);
            }
            if (id.equals(ID_PERIOD) || id.equals(ID_INTERMISSION)) {
                setMaximumNumber(r.getInt(Rule.NUMBER_PERIODS));
            } else {
                setMaximumNumber(DEFAULT_MAXIMUM_NUMBER);
            }
            setMinimumTime(DEFAULT_MINIMUM_TIME);
            if (id.equals(ID_PERIOD) || id.equals(ID_JAM)) {
                setMaximumTime(r.getLong(r.getRule(id + ".Duration")));
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

    public String getName() { return name; }
    public void setName(String n) {
        synchronized (coreLock) {
            String last = name;
            name = n;
            scoreBoardChange(new ScoreBoardEvent(this, Value.NAME, name, last));
        }
    }

    public int getNumber() { return number; }
    public void setNumber(int n) {
        synchronized (coreLock) {
            Integer last = new Integer(number);
            number = checkNewNumber(n);
            scoreBoardChange(new ScoreBoardEvent(this, Value.NUMBER, new Integer(number), last));
        }
    }
    public void changeNumber(int change) {
        synchronized (coreLock) {
            Integer last = new Integer(number);
            number = checkNewNumber(number + change);
            scoreBoardChange(new ScoreBoardEvent(this, Value.NUMBER, new Integer(number), last));
        }
    }
    protected int checkNewNumber(int n) {
        if (n < minimumNumber) {
            return minimumNumber;
        } else if (n > maximumNumber) {
            return maximumNumber;
        } else {
            return n;
        }
    }

    public int getMinimumNumber() { return minimumNumber; }
    public void setMinimumNumber(int n) {
        synchronized (coreLock) {
            Integer last = new Integer(minimumNumber);
            minimumNumber = n;
            if (maximumNumber < minimumNumber) {
                setMaximumNumber(minimumNumber);
            }
            if (getNumber() != checkNewNumber(getNumber())) {
                setNumber(getNumber());
            }
            scoreBoardChange(new ScoreBoardEvent(this, Value.MINIMUM_NUMBER, new Integer(minimumNumber), last));
        }
    }
    public void changeMinimumNumber(int change) {
        synchronized (coreLock) {
            setMinimumNumber(minimumNumber + change);
        }
    }

    public int getMaximumNumber() { return maximumNumber; }
    public void setMaximumNumber(int n) {
        synchronized (coreLock) {
            Integer last = new Integer(maximumNumber);
            if (n < minimumNumber) {
                n = minimumNumber;
            }
            maximumNumber = n;
            if (getNumber() != checkNewNumber(getNumber())) {
                setNumber(getNumber());
            }
            scoreBoardChange(new ScoreBoardEvent(this, Value.MAXIMUM_NUMBER, new Integer(maximumNumber), last));
        }
    }
    public void changeMaximumNumber(int change) {
        synchronized (coreLock) {
            setMaximumNumber(maximumNumber + change);
        }
    }

    public long getTime() { return time; }
    public long getInvertedTime() {
        synchronized (coreLock) {
            return maximumTime - time;
        }
    }
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
    public void setTime(long ms) {
        synchronized (coreLock) {
            Long last = new Long(time);
            if (isRunning() && isSyncTime()) {
                ms = ((ms / 1000) * 1000) + (time % 1000);
            }
            time = checkNewTime(ms);
            if (isDisplayChange(time, last)) {
                scoreBoardChange(new ScoreBoardEvent(this, Value.TIME, new Long(time), last));
                scoreBoardChange(new ScoreBoardEvent(this, Value.INVERTED_TIME, new Long(maximumTime) - new Long(time), maximumTime - last));
            }
            if (isTimeAtEnd()) {
                stop();
            }
        }
    }
    public void changeTime(long change) { _changeTime(change, true); }
    protected void _changeTime(long change, boolean sync) {
        synchronized (coreLock) {
            Long last = new Long(time);
            if (sync && isRunning() && isSyncTime()) {
                change = ((change / 1000) * 1000);
            }
            time = checkNewTime(time + change);
            if (isDisplayChange(time, last)) {
                scoreBoardChange(new ScoreBoardEvent(this, Value.TIME, new Long(time), last));
                scoreBoardChange(new ScoreBoardEvent(this, Value.INVERTED_TIME, new Long(maximumTime) - new Long(time), maximumTime - last));
            }
            if(isTimeAtEnd()) {
                stop();
            }
        }
    }
    public void elapseTime(long change) {
        synchronized (coreLock) {
            changeTime(isCountDirectionDown()?-change:change);
        }
    }
    public void resetTime() {
        synchronized (coreLock) {
            if (isCountDirectionDown()) {
                setTime(getMaximumTime());
            } else {
                setTime(getMinimumTime());
            }
        }
    }
    protected long checkNewTime(long ms) {
        if (ms < minimumTime && minimumTime - ms > 500) {
            return minimumTime;
        } else if (ms > maximumTime && ms - maximumTime > 500) {
            return maximumTime;
        } else {
            return ms;
        }
    }
    protected boolean isDisplayChange(long current, long last) {
        //the frontend rounds values that are not full seconds to the earlier second
        //i.e. 3600ms will be displayed as 3s on a count up clock and as 4s on a count down clock.
        if (isCountDirectionDown()) {
            return Math.floor(((float)current-1)/1000) != Math.floor(((float)last-1)/1000);
        } else {
            return Math.floor((float)current/1000) != Math.floor((float)last/1000);
        }
    }

    public long getMinimumTime() { return minimumTime; }
    public void setMinimumTime(long ms) {
        synchronized (coreLock) {
            Long last = new Long(minimumTime);
            minimumTime = ms;
            if (maximumTime < minimumTime) {
                setMaximumTime(minimumTime);
            }
            if (getTime() != checkNewTime(getTime())) {
                setTime(getTime());
            }
            scoreBoardChange(new ScoreBoardEvent(this, Value.MINIMUM_TIME, new Long(minimumTime), last));
        }
    }
    public void changeMinimumTime(long change) {
        synchronized (coreLock) {
            setMinimumTime(minimumTime + change);
        }
    }
    public long getMaximumTime() { return maximumTime; }
    public void setMaximumTime(long ms) {
        synchronized (coreLock) {
            Long last = new Long(maximumTime);
            if (ms < minimumTime) {
                ms = minimumTime;
            }
            maximumTime = ms;
            if (isCountDirectionDown()) {
                changeTime(ms - last);
            }
            if (getTime() != checkNewTime(getTime())) {
                setTime(getTime());
            }
            scoreBoardChange(new ScoreBoardEvent(this, Value.MAXIMUM_TIME, new Long(maximumTime), last));
        }
    }
    public void changeMaximumTime(long change) {
        synchronized (coreLock) {
            setMaximumTime(maximumTime + change);
        }
    }
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

    public boolean isCountDirectionDown() { return countDown; }
    public void setCountDirectionDown(boolean down) {
        synchronized (coreLock) {
            Boolean last = new Boolean(countDown);
            countDown = down;
            scoreBoardChange(new ScoreBoardEvent(this, Value.DIRECTION, new Boolean(countDown), last));
            if (last != down) {
                setTime(getInvertedTime());
            }
        }
    }

    public boolean isRunning() { return isRunning; }

    public void start() {
        synchronized (coreLock) {
            start(false);
        }
    }
    public void start(boolean quickAdd) {
        synchronized (coreLock) {
            if (isRunning()) { return; }
            isRunning = true;
            scoreBoardChange(new ScoreBoardEvent(this, Value.RUNNING, Boolean.TRUE, Boolean.FALSE));
            updateClockTimerTask.addClock(this, quickAdd);
        }
    }
    public void stop() {
        synchronized (coreLock) {
            if (!isRunning()) { return; }
            isRunning = false;
            updateClockTimerTask.removeClock(this);
            scoreBoardChange(new ScoreBoardEvent(this, Value.RUNNING, Boolean.FALSE, Boolean.TRUE));
        }
    }

    public void startNext() {
        synchronized (coreLock) {
            requestBatchStart();
            changeNumber(1);
            resetTime();
            start();
            requestBatchEnd();
        }
    }

    protected void timerTick(long delta) {
        if (!isRunning()) { return; }
        lastTime += delta;
        _changeTime(countDown?-delta:delta, false);
    }

    protected boolean isSyncTime() {
        return Boolean.parseBoolean(getScoreBoard().getSettings().get(SETTING_SYNC));
    }

    protected boolean isMasterClock() {
        return id == ID_PERIOD || id == ID_TIMEOUT || id == ID_INTERMISSION;
    }

    protected ScoreBoard scoreBoard;

    protected String id;
    protected String name;
    protected int number;
    protected int minimumNumber;
    protected int maximumNumber;
    protected long time;
    protected long minimumTime;
    protected long maximumTime;
    protected boolean countDown;

    protected long lastTime;
    protected boolean isRunning = false;
    
    protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	add(Value.class);
    }};

    protected static Object coreLock = ScoreBoardImpl.getCoreLock();

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
                        nowMs = masterClock.time % 1000;
                        if (masterClock.countDown) {
                            nowMs = (1000 - nowMs) % 1000;
                        }
                    }

                    long timeMs = c.time % 1000;
                    if (c.countDown) {
                        timeMs = (1000 - timeMs) % 1000;
                    }
                    long delay = timeMs - nowMs;
                    if (Math.abs(delay) >= 500) {
                        delay = (long)(Math.signum((float)-delay) * (1000 - Math.abs(delay)));
                    }
                    c.lastTime = currentTime;
                    if (c.countDown) {
                        delay = -delay;
                    }
                    c.time = c.time - delay;
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