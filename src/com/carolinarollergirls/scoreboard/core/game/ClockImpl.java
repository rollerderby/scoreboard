package com.carolinarollergirls.scoreboard.core.game;

import java.util.ArrayList;

import com.carolinarollergirls.scoreboard.core.interfaces.BoxTrip;
import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Period;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ClockConversion;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class ClockImpl extends ScoreBoardEventProviderImpl<Clock> implements Clock {
    public ClockImpl(BoxTrip bt) {
        super(bt, bt.getId(), BoxTrip.CLOCK);
        game = bt.getGame();
        subId = "";
        isPenaltyClock = true;
        addProperties(props);
        values.put(NUMBER, 0);
        setCountDirectionDown(true);
        setRecalculated(TIME).addSource(this, MAXIMUM_TIME);
        setRecalculated(INVERTED_TIME).addSource(this, MAXIMUM_TIME).addSource(this, TIME);
        setName("PenaltyClock");
        setMaximumTime(game.getLong(Rule.PENALTY_DURATION));
        resetTime();
    }
    public ClockImpl(Game g, String i) {
        super(g, g.getId() + "_" + i, Game.CLOCK);
        game = g;
        subId = i;
        addProperties(props);
        // initialize types
        if (i == ID_PERIOD || i == ID_INTERMISSION) {
            setCopy(NUMBER, g, Game.CURRENT_PERIOD_NUMBER, true);
        } else if (i == ID_JAM) {
            setCopy(NUMBER, g, Game.CURRENT_PERIOD, Period.CURRENT_JAM_NUMBER, true);
        } else {
            values.put(NUMBER, 0);
        }
        setRecalculated(MAXIMUM_TIME).addSource(g, Game.IN_SUDDEN_SCORING).addSource(g, Game.IN_OVERTIME);
        setRecalculated(TIME).addSource(this, MAXIMUM_TIME);
        setRecalculated(INVERTED_TIME).addSource(this, MAXIMUM_TIME).addSource(this, TIME);
        setName(subId);

        // Pull in settings.
        rulesetChangeListener.scoreBoardChange(null);
        resetTime();
        game.addScoreBoardListener(new ConditionalScoreBoardListener<>(Game.class, Game.RULE, rulesetChangeListener));
    }

    @Override
    public String getProviderId() {
        return subId;
    }

    @Override
    protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == TIME) {
            if (isRunning() && isSyncTime()) {
                if (flag == Flag.CHANGE) {
                    value = (Long) last + ((((Long) value - (Long) last) / 1000L) * 1000L);
                } else if (flag != Flag.SPECIAL_CASE) {
                    value = (((Long) value / 1000L) * 1000L) + (Long) last % 1000L;
                }
            }
            if ((flag == Flag.RESET && isCountDirectionDown()) ||
                (Long) value > getMaximumTime() + 500 && (!isCountDirectionDown() || source != Source.RECALCULATE)) {
                return getMaximumTime();
            }
            if ((flag == Flag.RESET && !isCountDirectionDown()) || (!isPenaltyClock && (Long) value < 0 - 500)) {
                return Long.valueOf(0);
            }
        }
        if (prop == INVERTED_TIME) { return getMaximumTime() - getTime(); }
        if (prop == MAXIMUM_TIME && subId.equals(ID_JAM) && source == Source.RECALCULATE) {
            if (game.isInJam() && game.getCurrentPeriod().getCurrentJam().isInjuryContinuation()) {
                value = last;
            } else if (game.isInSuddenScoring() && !game.isInOvertime()) {
                value = game.getLong(Rule.SUDDEN_SCORING_JAM_DURATION);
            } else {
                value = game.getLong(Rule.JAM_DURATION);
            }
        }
        if (prop == MAXIMUM_TIME && (Long) value < 0) { return Long.valueOf(0); }
        if (prop == NUMBER && (Integer) value < 0) { return 0; }
        return value;
    }
    @Override
    protected void valueChanged(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == TIME && isTimeAtEnd() && !isPenaltyClock) { stop(); }
        if (prop == MAXIMUM_TIME && isCountDirectionDown() && !source.isFile()) {
            changeTime((Long) value - (Long) last);
        }
        if (prop == DIRECTION) { setTime(getInvertedTime()); }
        if (prop == RUNNING) {
            if ((Boolean) value) {
                updateClockTimerTask.addClock(this, flag == Flag.SPECIAL_CASE);
            } else {
                updateClockTimerTask.removeClock(this);
            }
        }
    }

    @Override
    public void execute(Command prop, Source source) {
        if (prop == RESET_TIME) {
            resetTime();
        } else if (prop == START) {
            start();
        } else if (prop == STOP) {
            stop();
        }
    }

    protected ScoreBoardListener rulesetChangeListener = new ScoreBoardListener() {
        @Override
        public void scoreBoardChange(ScoreBoardEvent<?> event) {
            // Get default values from current settings or use hardcoded values
            setCountDirectionDown(Boolean.parseBoolean(game.get(Game.RULE, subId + ".ClockDirection").getValue()));
            if (subId.equals(ID_PERIOD)) {
                setMaximumTime(game.getLong(Rule.PERIOD_DURATION));
            } else if (subId.equals(ID_JAM)) {
                set(MAXIMUM_TIME, game.getLong(Rule.JAM_DURATION), Source.RECALCULATE);
            } else if (subId.equals(ID_INTERMISSION)) {
                setMaximumTime(getCurrentIntermissionTime());
            } else if (subId.equals(ID_LINEUP) && isCountDirectionDown()) {
                if (game.isInOvertime()) {
                    setMaximumTime(game.getLong(Rule.OVERTIME_LINEUP_DURATION));
                } else {
                    setMaximumTime(game.getLong(Rule.LINEUP_DURATION));
                }
            } else {
                setMaximumTime(DEFAULT_MAXIMUM_TIME);
            }
        }
    };

    @Override
    public ClockSnapshot snapshot() {
        synchronized (coreLock) { return new ClockSnapshotImpl(this); }
    }
    @Override
    public void restoreSnapshot(ClockSnapshot s) {
        synchronized (coreLock) {
            if (s.getId() != getId()) { return; }
            setNumber(s.getNumber());
            stop();
            set(TIME, isCountDirectionDown() ? getMaximumTime() - s.getTime() : s.getTime(), Flag.SPECIAL_CASE);
            if (s.isRunning()) { start(); }
        }
    }

    @Override
    public String getName() {
        return get(NAME);
    }
    public void setName(String n) { set(NAME, n); }

    @Override
    public int getNumber() {
        return get(NUMBER);
    }
    @Override
    public void setNumber(int n) {
        set(NUMBER, n);
    }
    @Override
    public void changeNumber(int change) {
        set(NUMBER, change, Flag.CHANGE);
    }

    @Override
    public long getTime() {
        return get(TIME);
    }
    @Override
    public long getInvertedTime() {
        return get(INVERTED_TIME);
    }
    @Override
    public long getTimeElapsed() {
        synchronized (coreLock) {
            return isTimeAtEnd() ? getMaximumTime() : isCountDirectionDown() ? getInvertedTime() : getTime();
        }
    }
    @Override
    public long getTimeRemaining() {
        synchronized (coreLock) { return isTimeAtEnd() ? 0L : isCountDirectionDown() ? getTime() : getInvertedTime(); }
    }
    @Override
    public void setTime(long ms) {
        set(TIME, ms);
    }
    @Override
    public void changeTime(long change) {
        set(TIME, change, Flag.CHANGE);
    }
    @Override
    public void elapseTime(long change) {
        synchronized (coreLock) { changeTime(isCountDirectionDown() ? -change : change); }
    }
    @Override
    public void resetTime() {
        set(TIME, getTime(), Flag.RESET);
    }
    protected boolean isDisplayChange(long current, long last) {
        // the frontend rounds values that are not full seconds to the earlier second
        // i.e. 3600ms will be displayed as 3s on a count up clock and as 4s on a count
        // down clock.
        if (isCountDirectionDown()) {
            return floorDiv(current - 1, 1000) != floorDiv(last - 1, 1000);
        } else {
            return floorDiv(current, 1000) != floorDiv(last, 1000);
        }
    }

    @Override
    public long getMaximumTime() {
        return get(MAXIMUM_TIME);
    }
    @Override
    public void setMaximumTime(long ms) {
        set(MAXIMUM_TIME, ms);
    }
    @Override
    public void changeMaximumTime(long change) {
        set(MAXIMUM_TIME, change, Flag.CHANGE);
    }
    @Override
    public boolean isTimeAtStart(long t) {
        synchronized (coreLock) {
            if (isCountDirectionDown()) {
                return t >= getMaximumTime();
            } else {
                return t <= 0;
            }
        }
    }
    @Override
    public boolean isTimeAtStart() {
        return isTimeAtStart(getTime());
    }
    @Override
    public boolean isTimeAtEnd(long t) {
        synchronized (coreLock) {
            if (isCountDirectionDown()) {
                return t <= 0;
            } else {
                return t >= getMaximumTime();
            }
        }
    }
    @Override
    public boolean isTimeAtEnd() {
        return isTimeAtEnd(getTime());
    }

    @Override
    public boolean isCountDirectionDown() {
        return get(DIRECTION);
    }
    @Override
    public void setCountDirectionDown(boolean down) {
        set(DIRECTION, down);
    }

    @Override
    public boolean isRunning() {
        return get(RUNNING);
    }

    @Override
    public void start() {
        start(false);
    }
    public void start(boolean quickAdd) { set(RUNNING, Boolean.TRUE, quickAdd ? Flag.SPECIAL_CASE : null); }
    @Override
    public void stop() {
        set(RUNNING, Boolean.FALSE);
    }

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
        String[] sequence = game.get(Rule.INTERMISSION_DURATIONS).split(",");
        int number = Math.min(game.getCurrentPeriodNumber(), sequence.length);
        if (number > 0) { duration = ClockConversion.fromHumanReadable(sequence[number - 1]); }
        return duration;
    }

    protected boolean isSyncTime() {
        return !isPenaltyClock && Boolean.parseBoolean(getScoreBoard().getSettings().get(SETTING_SYNC));
    }

    /* Taken from OpenJDK 1.8 to work around Oracle Java SE 8 missing this function */
    private long floorDiv(long x, long y) {
        long r = x / y;
        // if the signs are different and modulo not zero, round down
        if ((x ^ y) < 0 && (r * y != x)) { r--; }
        return r;
    }

    protected long lastTime;
    protected boolean isPenaltyClock = false;

    private Game game;
    private String subId;

    public static UpdateClockTimerTask updateClockTimerTask = new UpdateClockTimerTask();

    public static final long DEFAULT_MAXIMUM_TIME = 24 * 60 * 60 * 1000; // 1 day for long time to derby
    public static final boolean DEFAULT_DIRECTION = false;               // up

    public static class ClockSnapshotImpl implements ClockSnapshot {
        private ClockSnapshotImpl(Clock clock) {
            id = clock.getId();
            number = clock.getNumber();
            time = clock.getTimeElapsed();
            isRunning = clock.isRunning();
        }

        @Override
        public String getId() {
            return id;
        }
        @Override
        public int getNumber() {
            return number;
        }
        @Override
        public long getTime() {
            return time;
        }
        @Override
        public boolean isRunning() {
            return isRunning;
        }

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
                    if (masterClock.isCountDirectionDown()) { nowMs = (1000 - nowMs) % 1000; }

                    long timeMs = c.getTime() % 1000;
                    if (c.isCountDirectionDown()) { timeMs = (1000 - timeMs) % 1000; }
                    long delay = timeMs - nowMs;
                    if (Math.abs(delay) >= 500) { delay = (long) (Math.signum(-delay) * (1000 - Math.abs(delay))); }
                    c.lastTime = currentTime;
                    if (c.isCountDirectionDown()) { delay = -delay; }
                    c.values.put(TIME, c.getTime() - delay);
                } else {
                    c.lastTime = currentTime;
                }
                clocks.add(c);
            }
        }

        public void removeClock(ClockImpl c) {
            synchronized (coreLock) { clocks.remove(c); }
        }

        private void tick() {
            synchronized (coreLock) {
                currentTime += update_interval;
                if (!clocks.isEmpty()) {
                    ScoreBoard sb = clocks.get(0).getScoreBoard();
                    sb.runInBatch(new Runnable() {
                        @Override
                        public void run() {
                            for (ClockImpl clock : new ArrayList<>(clocks)) { clock.timerTick(update_interval); }
                        }
                    });
                }
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
