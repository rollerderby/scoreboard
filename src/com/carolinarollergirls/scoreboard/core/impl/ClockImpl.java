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
	super (sb, ScoreBoard.Child.CLOCK, Clock.class, Value.class, Command.class);
        id = i;
        //initialize types
        values.put(Value.MINIMUM_TIME, 0L);
        values.put(Value.MAXIMUM_TIME, 0L);
        values.put(Value.TIME, 0L);
        values.put(Value.NUMBER, 0);
        values.put(Value.MINIMUM_NUMBER, 0);
        values.put(Value.MAXIMUM_NUMBER, 0);
        values.put(Value.DIRECTION, Boolean.FALSE);
        values.put(Value.RUNNING, Boolean.FALSE);

        sb.addScoreBoardListener(new ConditionalScoreBoardListener(Rulesets.class, Rulesets.Value.CURRENT_RULESET_ID, rulesetChangeListener));

        reset();
    }

    public Object get(PermanentProperty prop) {
	synchronized (coreLock) {
	    if (prop == Value.INVERTED_TIME) {
		return getMaximumTime() - getTime();
	    }
	    if (prop == Value.NUMBER) {
		if (id == ID_PERIOD || id == ID_INTERMISSION) {
		    return scoreBoard.getCurrentPeriodNumber();
		} else if (id == ID_JAM) {
		    return scoreBoard.getCurrentPeriod().getCurrentJamNumber();
		}
	    }
	    return super.get(prop);
	}
    }
    
    public boolean set(PermanentProperty prop, Object value, Flag flag) {
	synchronized (coreLock) {
	    if (!(prop instanceof Value) || prop == Value.INVERTED_TIME) { return false; }
	    if (prop == Value.NUMBER) {
		if (id == ID_PERIOD) {
		    return scoreBoard.set(ScoreBoard.Value.CURRENT_PERIOD_NUMBER, value, flag);
		} else if (id == ID_JAM) {
		    return scoreBoard.getCurrentPeriod().set(Period.Value.CURRENT_JAM_NUMBER, value, flag);
		} else if (id == ID_INTERMISSION) { 
		    return false; 
		}
	    }
	    if (flag == Flag.RESET && prop == Value.TIME) { return resetTime(); }
	    requestBatchStart();
	    Object last = get(prop);
	    if (prop == Value.TIME && isRunning() && isSyncTime() && flag != Flag.CUSTOM) {
		value = (((Long)value / 1000L) * 1000L) + (flag == Flag.CHANGE ? 0L : (getTime() % 1000L));
	    }
	    Number min = null;
	    if (prop == Value.MAXIMUM_NUMBER || prop == Value.NUMBER) { min = getMinimumNumber(); }
	    if (prop == Value.MAXIMUM_TIME || prop == Value.TIME) { min = getMinimumTime(); }
	    Number max = null;
	    if (prop == Value.NUMBER) { max = getMaximumNumber(); }
	    if (prop == Value.TIME) { max =  getMaximumTime(); }
	    long tolerance = (prop == Value.TIME)? 500 : 0;
	    boolean result = set(prop, value, flag, min, max, tolerance);
	    if (result) {
		switch ((Value)prop) {
		case MINIMUM_NUMBER:
		    setMaximumNumber(getMaximumNumber()); //will check for max < min
		    //$FALL-THROUGH$
		case MAXIMUM_NUMBER:
		    setNumber(getNumber()); //will check range
		    break;
		case TIME:
	            scoreBoardChange(new ScoreBoardEvent(this, Value.INVERTED_TIME, getMaximumTime() - getTime(), getMaximumTime() - (Long)last));
	            if(isTimeAtEnd()) {
	                stop();
	            }
		    break;
		case MINIMUM_TIME:
		    setMaximumTime(getMaximumTime()); // will check for max < min
		    setTime(getTime()); // will check range
		    break;
		case MAXIMUM_TIME:
		    if (isCountDirectionDown()) {
			changeTime((Long)value - (Long)last);
		    } else {
			setTime(getTime()); // will check range
		    }
		    break;
		case DIRECTION:
		    setTime(getInvertedTime());
		    break;
		case RUNNING:
		    if ((Boolean)value) {
			updateClockTimerTask.addClock(this, flag == Flag.CUSTOM);
		    } else {
			updateClockTimerTask.removeClock(this);
		    }
		    break;
		default:
		    break;
		}
	    }
	    requestBatchEnd();
	    return result;
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
    
    public String getId() { return id; }

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
            setCountDirectionDown(Boolean.parseBoolean(r.get(Rulesets.Child.CURRENT_RULE, id + ".ClockDirection").getValue()));
            if (id.equals(ID_JAM) || id.equals(ID_INTERMISSION) || id.equals(ID_PERIOD)) {
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
                setMaximumTime(ClockConversion.fromHumanReadable(r.get(Rulesets.Child.CURRENT_RULE, id + ".Duration").getValue()));
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
    public long getInvertedTime() { 
	synchronized (coreLock) {
	    return getMaximumTime() - getTime();
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
    public void setTime(long ms) { set(Value.TIME, ms); }
    public void changeTime(long change) { set(Value.TIME, change, Flag.CHANGE); }
    public void elapseTime(long change) {
        synchronized (coreLock) {
            changeTime(isCountDirectionDown()?-change:change);
        }
    }
    public boolean resetTime() {
        synchronized (coreLock) {
            if (isCountDirectionDown()) {
                return set(Value.TIME, getMaximumTime());
            } else {
                return set(Value.TIME, getMinimumTime());
            }
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
    public void start(boolean quickAdd) { set(Value.RUNNING, Boolean.TRUE, quickAdd ? Flag.CUSTOM : null); }
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
        if (isDisplayChange(newTime, getTime())) {
            set(Value.TIME, newTime, Flag.CUSTOM);
        } else {
            values.put(Value.TIME, newTime);
        }
    }

    protected boolean isSyncTime() {
        return Boolean.parseBoolean(getScoreBoard().getSettings().get(SETTING_SYNC));
    }

    protected boolean isMasterClock() {
        return id == ID_PERIOD || id == ID_TIMEOUT || id == ID_INTERMISSION;
    }

    protected String id;

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
