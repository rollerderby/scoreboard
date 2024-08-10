package com.carolinarollergirls.scoreboard.core.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.Queue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.carolinarollergirls.scoreboard.core.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.core.game.ClockImpl.ClockSnapshotImpl;
import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class ClockImplTests {

    private ScoreBoard sb;
    private Game g;

    private Queue<ScoreBoardEvent<?>> collectedEvents;
    public ScoreBoardListener listener = new ScoreBoardListener() {
        @Override
        public void scoreBoardChange(ScoreBoardEvent<?> event) {
            synchronized (collectedEvents) { collectedEvents.add(event); }
        }
    };

    private ClockImpl clock;
    private static String ID = Clock.ID_LINEUP;

    private void advance(long time_ms) { ScoreBoardClock.getInstance().advance(time_ms); }

    @Before
    public void setUp() throws Exception {
        ScoreBoardClock.getInstance().stop();
        GameImpl.setQuickClockThreshold(0L);
        collectedEvents = new LinkedList<>();

        sb = new ScoreBoardImpl(false);
        sb.postAutosaveUpdate();
        g = sb.getCurrentGame().get(CurrentGame.GAME);
        sb.getSettings().set(Clock.SETTING_SYNC, String.valueOf(false));

        clock = (ClockImpl) g.getClock(ID);
    }

    @After
    public void tearDown() throws Exception {
        ScoreBoardClock.getInstance().start(false);
        GameImpl.setQuickClockThreshold(1000L);
    }

    @Test
    public void testDefaults() {
        assertEquals(0, clock.getNumber());

        assertEquals(ClockImpl.DEFAULT_MAXIMUM_TIME, clock.getMaximumTime());
        assertEquals(0, clock.getTime());

        assertEquals(g.getId() + '_' + ID, clock.getId());
        assertEquals(ID, clock.getName());
        assertFalse(clock.isCountDirectionDown());
        assertFalse(clock.isRunning());

        assertEquals(sb, clock.getScoreBoard());

        assertEquals("Clock", clock.getProviderName());
        assertEquals(ID, clock.getProviderId());
        assertEquals(Clock.class, clock.getProviderClass());
    }

    @Test
    public void testRestoreSnapshot() {
        clock.setNumber(4);
        clock.setMaximumTime(1200000);
        clock.setTime(5000);
        clock.start();
        ClockImpl.ClockSnapshotImpl snapshot = (ClockSnapshotImpl) clock.snapshot();

        clock.stop();
        clock.setNumber(0);
        clock.resetTime();
        assertFalse(clock.isRunning());
        assertEquals(0, clock.getNumber());
        assertEquals(0, clock.getTime());

        // if IDs don't match no restore should be done
        snapshot.id = "OTHER_THAN_" + clock.getId();
        clock.restoreSnapshot(snapshot);
        assertFalse(clock.isRunning());
        assertEquals(0, clock.getNumber());
        assertEquals(0, clock.getTime());

        snapshot.id = clock.getId();
        clock.restoreSnapshot(snapshot);
        assertTrue(clock.isRunning());
        assertEquals(4, clock.getNumber());
        assertEquals(5000, clock.getTime());
    }

    @Test
    public void testSetting_ClockSync() {
        ClockImpl clock2 = new ClockImpl(g, Clock.ID_TIMEOUT);
        sb.getSettings().set(Clock.SETTING_SYNC, String.valueOf(true));
        clock.setMaximumTime(10000);
        clock2.setMaximumTime(10000);
        clock2.setTime(3400);
        assertEquals(3400, clock2.getTime());

        // no syncing is done if the clock is stopped
        clock.setTime(4200);
        assertEquals(4200, clock.getTime());

        // the first clock started has its time rounded
        clock2.start();
        assertEquals(3000, clock2.getTime());
        advance(400);

        // when the clocks are started the just started clock is synced to the already
        // running
        clock.start();
        assertEquals(4400, clock.getTime());

        // changes under 1s are ignored. Even if multiple changes accumulate to more
        // than 1s
        clock.changeTime(500);
        clock.changeTime(800);
        assertEquals(4400, clock.getTime());

        // changes over 1s are rounded down
        clock.changeTime(1100);
        assertEquals(5400, clock.getTime());

        // the previous statements also apply to the master clock
        clock2.changeTime(500);
        clock2.changeTime(800);
        assertEquals(3400, clock2.getTime());
        clock2.changeTime(1000);
        assertEquals(4400, clock2.getTime());

        // advancing the time affects both clocks even if less than 1s
        advance(400);
        assertEquals(4800, clock2.getTime());
        assertEquals(5800, clock.getTime());
    }

    @Test
    public void testSetName() {
        clock.addScoreBoardListener(new ConditionalScoreBoardListener<>(clock, Clock.NAME, listener));

        clock.setName("Test Clock");

        assertEquals("Test Clock", clock.getName());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent<?> event = collectedEvents.poll();
        assertEquals("Test Clock", event.getValue());
        assertEquals(ID, event.getPreviousValue());
    }

    public void testSetCountDirectionDown() {
        assertFalse(clock.isCountDirectionDown());
        clock.addScoreBoardListener(new ConditionalScoreBoardListener<>(clock, Clock.DIRECTION, listener));

        clock.setCountDirectionDown(true);
        assertTrue(clock.isCountDirectionDown());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent<?> event = collectedEvents.poll();
        assertTrue((Boolean) event.getValue());
        assertFalse((Boolean) event.getPreviousValue());

        // check idempotency
        clock.setCountDirectionDown(true);
        assertTrue(clock.isCountDirectionDown());
        assertEquals(1, collectedEvents.size());

        clock.setCountDirectionDown(false);
        assertFalse(clock.isCountDirectionDown());
    }

    @Test
    public void testChangeNumber() {
        clock.addScoreBoardListener(new ConditionalScoreBoardListener<>(clock, Clock.NUMBER, listener));

        collectedEvents.clear();

        clock.setNumber(5);
        assertEquals(5, clock.getNumber());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent<?> event = collectedEvents.poll();
        assertEquals(5, event.getValue());
        assertEquals(0, event.getPreviousValue());

        clock.changeNumber(3);
        assertEquals(8, clock.getNumber());
        assertEquals(1, collectedEvents.size());
        collectedEvents.clear();

        // validate constraint: cannot set negative number
        clock.setNumber(-2);
        assertEquals(0, clock.getNumber());
        assertEquals(1, collectedEvents.size());
        collectedEvents.clear();

        clock.setNumber(3);
        assertEquals(3, clock.getNumber());
        collectedEvents.clear();
        clock.changeNumber(6);
        assertEquals(9, clock.getNumber());
        assertEquals(1, collectedEvents.size());
        collectedEvents.clear();

        clock.setNumber(5);
        clock.changeNumber(-1);
        assertEquals(4, clock.getNumber());
        assertEquals(2, collectedEvents.size());
    }

    @Test
    public void testSetMaximumTime() {
        clock.addScoreBoardListener(new ConditionalScoreBoardListener<>(clock, Clock.MAXIMUM_TIME, listener));

        clock.setMaximumTime(0);
        collectedEvents.clear();

        clock.setMaximumTime(5000);

        // validate constraint: increase max time doesn't reset current time
        assertEquals(5000L, clock.getMaximumTime());
        assertEquals(0, clock.getTime());

        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent<?> event = collectedEvents.poll();
        assertEquals(5000L, event.getValue());
        assertEquals(0L, event.getPreviousValue());
    }

    @Test
    public void testSetMaximumTime_countDown() {
        clock.setMaximumTime(5000);
        clock.setCountDirectionDown(true);
        clock.setTime(3000);
        clock.start();

        clock.setMaximumTime(10000);
        assertEquals(8000L, clock.getTime());
        assertTrue(clock.isRunning());

        clock.setMaximumTime(1000);
        assertEquals(0L, clock.getTime());
        assertFalse(clock.isRunning());
    }

    @Test
    public void testSetMaximumTime2() {
        clock.setMaximumTime(-1000);

        // validate constraint: cannot set a max that is < min
        assertEquals(0, clock.getMaximumTime());
        assertEquals(0, clock.getTime());
    }

    @Test
    public void testChangeMaximumTime() {
        clock.addScoreBoardListener(new ConditionalScoreBoardListener<>(clock, Clock.MAXIMUM_TIME, listener));

        clock.setMaximumTime(1000);
        collectedEvents.clear();

        clock.changeMaximumTime(2000);

        assertEquals(3000L, clock.getMaximumTime());
        assertEquals(0L, clock.getTime());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent<?> event = collectedEvents.poll();
        assertEquals(3000L, event.getValue());
        assertEquals(1000L, event.getPreviousValue());
    }

    @Test
    public void testChangeTime() {
        clock.addScoreBoardListener(new ConditionalScoreBoardListener<>(clock, Clock.TIME, listener));

        clock.setMaximumTime(5000);
        collectedEvents.clear();

        clock.setTime(2000);
        assertEquals(2000L, clock.getTime());
        assertEquals(3000L, clock.getInvertedTime());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent<?> event = collectedEvents.poll();
        assertEquals(2000L, event.getValue());
        assertEquals(0L, event.getPreviousValue());

        clock.setTime(6000);
        assertEquals(5000L, clock.getTime());
        assertEquals(0L, clock.getInvertedTime());
        assertEquals(1, collectedEvents.size());
        assertEquals(5000L, collectedEvents.poll().getValue());

        clock.setTime(-600);
        assertEquals(0L, clock.getTime());
        assertEquals(5000L, clock.getInvertedTime());
        assertEquals(1, collectedEvents.size());
        clock.start();
        collectedEvents.clear();

        clock.timerTick(200);
        assertEquals(200L, clock.getTime());
        assertEquals(4800L, clock.getInvertedTime());
        assertEquals(0, collectedEvents.size());

        clock.timerTick(-201);
        assertEquals(-1L, clock.getTime());
        assertEquals(5001L, clock.getInvertedTime());
        assertEquals(1, collectedEvents.size());

        clock.setCountDirectionDown(true);
        collectedEvents.clear();
        clock.timerTick(-1);
        assertEquals(0, collectedEvents.size());

        clock.setTime(2000);
        clock.changeTime(1200);
        assertEquals(3200L, clock.getTime());
        assertEquals(1800L, clock.getInvertedTime());
        assertEquals(2, collectedEvents.size());
        collectedEvents.clear();

        clock.addScoreBoardListener(new ConditionalScoreBoardListener<>(clock, Clock.INVERTED_TIME, listener));
        clock.changeTime(-5000);
        assertEquals(0L, clock.getTime());
        assertEquals(5000L, clock.getInvertedTime());
        assertEquals(2, collectedEvents.size());
        Boolean firstEventInverted;
        event = collectedEvents.poll();
        if (event.getProperty() == Clock.TIME) {
            firstEventInverted = false;
            assertEquals(0L, event.getValue());
            assertEquals(3200L, event.getPreviousValue());
        } else {
            firstEventInverted = true;
            assertEquals(5000L, event.getValue());
            assertEquals(1800L, event.getPreviousValue());
        }
        event = collectedEvents.poll();
        if (firstEventInverted) {
            assertEquals(0L, event.getValue());
            assertEquals(3200L, event.getPreviousValue());
        } else {
            assertEquals(5000L, event.getValue());
            assertEquals(1800L, event.getPreviousValue());
        }

        clock.changeTime(5100);
        assertEquals(5100L, clock.getTime());
        assertEquals(-100L, clock.getInvertedTime());
    }

    @Test
    public void testElapseTime_countUp() {
        clock.addScoreBoardListener(new ConditionalScoreBoardListener<>(clock, Clock.TIME, listener));
        clock.setMaximumTime(5000);

        clock.setTime(2000);
        assertEquals(2000L, clock.getTimeElapsed());
        assertEquals(3000L, clock.getTimeRemaining());

        clock.elapseTime(1000);
        assertEquals(3000L, clock.getTime());
        assertEquals(3000L, clock.getTimeElapsed());
        assertEquals(2000L, clock.getTimeRemaining());
        assertEquals(2, collectedEvents.size());
    }

    @Test
    public void testElapseTime_countDown() {
        clock.setCountDirectionDown(true);
        clock.setMaximumTime(5000);

        clock.setTime(2000);
        assertEquals(3000L, clock.getTimeElapsed());
        assertEquals(2000L, clock.getTimeRemaining());

        clock.elapseTime(1000);
        assertEquals(1000L, clock.getTime());
        assertEquals(4000L, clock.getTimeElapsed());
        assertEquals(1000L, clock.getTimeRemaining());
    }

    @Test
    public void testIsTimeAtStart_countUp() {
        clock.setMaximumTime(5000);

        assertTrue(clock.isTimeAtStart());
        assertFalse(clock.isTimeAtEnd());

        clock.setTime(2000);

        assertFalse(clock.isTimeAtStart());
        assertFalse(clock.isTimeAtEnd());

        clock.setTime(5000);

        assertFalse(clock.isTimeAtStart());
        assertTrue(clock.isTimeAtEnd());
    }

    @Test
    public void testIsTimeAtStart_countDown() {
        clock.setCountDirectionDown(true);
        clock.setMaximumTime(5000);
        clock.setTime(5000);

        assertTrue(clock.isTimeAtStart());
        assertFalse(clock.isTimeAtEnd());

        clock.setTime(2000);

        assertFalse(clock.isTimeAtStart());
        assertFalse(clock.isTimeAtEnd());

        clock.setTime(0);

        assertFalse(clock.isTimeAtStart());
        assertTrue(clock.isTimeAtEnd());
    }

    @Test
    public void testResetTime() {
        clock.addScoreBoardListener(new ConditionalScoreBoardListener<>(clock, Clock.TIME, listener));
        clock.setMaximumTime(5000);

        clock.setTime(3000);

        clock.resetTime();

        assertEquals(0, clock.getTime());
        assertEquals(2, collectedEvents.size());

        clock.setTime(3000);
        clock.setCountDirectionDown(true);

        clock.resetTime();

        assertEquals(5000, clock.getTime());
    }

    public void testRunning() {
        clock.addScoreBoardListener(new ConditionalScoreBoardListener<>(clock, Clock.RUNNING, listener));
        clock.addScoreBoardListener(new ConditionalScoreBoardListener<>(clock, Clock.TIME, listener));
        clock.setMaximumTime(30000);
        assertFalse(clock.isCountDirectionDown());
        assertTrue(clock.isTimeAtStart());

        clock.start();
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent<?> event = collectedEvents.poll();
        assertEquals(Clock.RUNNING, event.getProperty());
        assertTrue((Boolean) event.getValue());
        assertFalse((Boolean) event.getPreviousValue());

        advance(1000);
        advance(500);
        advance(500);
        assertEquals(2000, clock.getTimeElapsed());
        assertEquals(2, collectedEvents.size());
        assertEquals(Clock.TIME, collectedEvents.poll().getProperty());
        assertEquals(Clock.TIME, collectedEvents.poll().getProperty());

        advance(2000);
        assertEquals(4000, clock.getTimeElapsed());
        assertEquals(1, collectedEvents.size());
        assertEquals(Clock.TIME, collectedEvents.poll().getProperty());

        clock.stop();
        assertEquals(1, collectedEvents.size());
        event = collectedEvents.poll();
        assertEquals(Clock.RUNNING, event.getProperty());
        assertFalse((Boolean) event.getValue());
        assertTrue((Boolean) event.getPreviousValue());

        clock.setCountDirectionDown(true);
        assertEquals(4000, clock.getTimeElapsed());

        clock.resetTime();
        clock.start();
        advance(2000);
        assertEquals(2000, clock.getTimeElapsed());
    }

    @Test
    public void testRunningDownHasZeroTimeEvent() {
        clock.setCountDirectionDown(true);
        clock.setMaximumTime(1000);
        clock.setTime(1000);

        clock.addScoreBoardListener(new ConditionalScoreBoardListener<>(clock, Clock.TIME, listener));
        clock.start();
        advance(200);
        advance(200);
        advance(200);
        advance(200);
        advance(200);
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent<?> event = collectedEvents.poll();
        assertEquals(0, ((Long) event.getValue()).longValue());
    }

    @Test
    public void testRestart() {
        clock.setNumber(2);
        clock.setMaximumTime(60000);
        clock.setTime(45000);
        assertFalse(clock.isRunning());

        clock.restart();
        assertTrue(clock.isRunning());
        assertEquals(2, clock.getNumber());
        assertTrue(clock.isTimeAtStart());
    }

    @Test
    public void testSetttingsEvent() {
        assertFalse(clock.isCountDirectionDown());
        assertEquals(0, clock.getTime());

        g.setRuleset(null);
        g.set(Rule.LINEUP_DIRECTION, String.valueOf(true));
        clock.rulesetChangeListener.scoreBoardChange(null);
        assertTrue(clock.isCountDirectionDown());
        assertEquals(30000, clock.getTime());
    }
}
