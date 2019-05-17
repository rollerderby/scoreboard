package com.carolinarollergirls.scoreboard.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.Queue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.impl.ClockImpl;
import com.carolinarollergirls.scoreboard.core.impl.ClockImpl.ClockSnapshotImpl;
import com.carolinarollergirls.scoreboard.core.impl.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class ClockImplTests {

    private ScoreBoard sb;

    private Queue<ScoreBoardEvent> collectedEvents;
    public ScoreBoardListener listener = new ScoreBoardListener() {

        @Override
        public void scoreBoardChange(ScoreBoardEvent event) {
            synchronized(collectedEvents) {
                collectedEvents.add(event);
            }
        }
    };


    private ClockImpl clock;
    private static String ID = Clock.ID_LINEUP;

    private void advance(long time_ms) {
        ScoreBoardClock.getInstance().advance(time_ms);
    }

    @Before
    public void setUp() throws Exception {
        ScoreBoardClock.getInstance().stop();
        collectedEvents = new LinkedList<>();

        sb = new ScoreBoardImpl();
        sb.getSettings().set(Clock.SETTING_SYNC, String.valueOf(false));

        clock = (ClockImpl) sb.getClock(ID);
    }

    @After
    public void tearDown() throws Exception {
        ScoreBoardClock.getInstance().start(false);
    }

    @Test
    public void testDefaults() {
        assertEquals(0, clock.getNumber());

        assertEquals(ClockImpl.DEFAULT_MINIMUM_TIME, clock.getMinimumTime());
        assertEquals(ClockImpl.DEFAULT_MAXIMUM_TIME, clock.getMaximumTime());
        assertEquals(ClockImpl.DEFAULT_MINIMUM_TIME, clock.getTime());

        assertEquals(ID, clock.getId());
        assertEquals(ID, clock.getName());
        assertFalse(clock.isMasterClock());
        assertFalse(clock.isCountDirectionDown());
        assertFalse(clock.isRunning());

        assertEquals(sb, clock.getScoreBoard());

        assertEquals("Clock", clock.getProviderName());
        assertEquals(ID, clock.getProviderId());
        assertEquals(Clock.class, clock.getProviderClass());
    }

    @Test
    public void testReset() {
        clock.setNumber(4);
        clock.setMaximumTime(1200000);
        clock.setTime(5000);
        clock.setCountDirectionDown(true);

        clock.reset();

        assertFalse(clock.isCountDirectionDown());
        assertEquals(ClockImpl.DEFAULT_MINIMUM_TIME, clock.getMinimumTime());
        assertEquals(ClockImpl.DEFAULT_MAXIMUM_TIME, clock.getMaximumTime());
        assertEquals(0, clock.getNumber());
        assertTrue(clock.isTimeAtStart());
    }

    @Test
    public void testRestoreSnapshot() {
        clock.setNumber(4);
        clock.setMaximumTime(1200000);
        clock.setTime(5000);
        clock.start();
        ClockImpl.ClockSnapshotImpl snapshot = (ClockSnapshotImpl) clock.snapshot();

        clock.reset();
        assertFalse(clock.isRunning());
        assertEquals(0, clock.getNumber());
        assertEquals(ClockImpl.DEFAULT_MINIMUM_TIME, clock.getTime());

        //if IDs don't match no restore should be done
        snapshot.id = "OTHER";
        clock.restoreSnapshot(snapshot);
        assertFalse(clock.isRunning());
        assertEquals(0, clock.getNumber());
        assertEquals(ClockImpl.DEFAULT_MINIMUM_TIME, clock.getTime());

        snapshot.id = ID;
        clock.restoreSnapshot(snapshot);
        assertTrue(clock.isRunning());
        assertEquals(4, clock.getNumber());
        assertEquals(5000, clock.getTime());
    }

    @Test
    public void testSetting_ClockSync() {
        //add a master clock
        ClockImpl clock2 = new ClockImpl(sb, Clock.ID_TIMEOUT);
        sb.getSettings().set(Clock.SETTING_SYNC, String.valueOf(true));
        clock.setMaximumTime(10000);
        clock2.setMaximumTime(10000);
        clock2.setTime(3400);
        assertEquals(3400, clock2.getTime());

        //no syncing is done if the clock is stopped
        clock.setTime(4200);
        assertEquals(4200, clock.getTime());

        //when the clocks are started the non-master clock is synced to the master clock
        clock2.start();
        clock.start();
        assertEquals(4400, clock.getTime());

        //changes under 1s are ignored. Even if multiple changes accumulate to more than 1s
        clock.changeTime(500);
        clock.changeTime(800);
        assertEquals(4400, clock.getTime());

        //changes over 1s are rounded down
        clock.changeTime(1100);
        assertEquals(5400, clock.getTime());

        //the previous statements also apply to the master clock
        clock2.changeTime(500);
        clock2.changeTime(800);
        assertEquals(3400, clock2.getTime());
        clock2.changeTime(1000);
        assertEquals(4400, clock2.getTime());

        //advancing the time affects both clocks even if less than 1s
        advance(400);
        assertEquals(4800, clock2.getTime());
        assertEquals(5800, clock.getTime());
    }

    @Test
    public void testSetName() {
        clock.addScoreBoardListener(new ConditionalScoreBoardListener(clock, Clock.Value.NAME, listener));

        clock.setName("Test Clock");

        assertEquals("Test Clock", clock.getName());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertEquals("Test Clock", event.getValue());
        assertEquals(ID, event.getPreviousValue());
    }

    public void testSetCountDirectionDown() {
        assertFalse(clock.isCountDirectionDown());
        clock.addScoreBoardListener(new ConditionalScoreBoardListener(clock, Clock.Value.DIRECTION, listener));

        clock.setCountDirectionDown(true);
        assertTrue(clock.isCountDirectionDown());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertTrue((Boolean)event.getValue());
        assertFalse((Boolean)event.getPreviousValue());

        //check idempotency
        clock.setCountDirectionDown(true);
        assertTrue(clock.isCountDirectionDown());
        assertEquals(1, collectedEvents.size());

        clock.setCountDirectionDown(false);
        assertFalse(clock.isCountDirectionDown());
    }

    @Test
    public void testChangeNumber() {
        clock.addScoreBoardListener(new ConditionalScoreBoardListener(clock, Clock.Value.NUMBER, listener));

        collectedEvents.clear();

        clock.setNumber(5);
        assertEquals(5, clock.getNumber());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
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
    public void testSetMinimumTime() {
        clock.addScoreBoardListener(new ConditionalScoreBoardListener(clock, Clock.Value.MINIMUM_TIME, listener));
        clock.addScoreBoardListener(new ConditionalScoreBoardListener(clock, Clock.Value.MAXIMUM_TIME, listener));
        clock.addScoreBoardListener(new ConditionalScoreBoardListener(clock, Clock.Value.TIME, listener));

        clock.setMaximumTime(ClockImpl.DEFAULT_MINIMUM_TIME);
        collectedEvents.clear();
        clock.setMinimumTime(1000);

        // validate constraint: max > min
        assertEquals(1000, clock.getMinimumTime());
        assertEquals(1000, clock.getMaximumTime());
        assertEquals(1000, clock.getTime());

        assertEquals(3, collectedEvents.size());
        ScoreBoardEvent event;
        while (!collectedEvents.isEmpty()) {
            event = collectedEvents.poll();
            assertEquals(1000, (long)event.getValue());
            assertEquals(ClockImpl.DEFAULT_MINIMUM_TIME, (long)event.getPreviousValue());
        }
    }

    @Test
    public void testSetMinimumTime2() {
        clock.addScoreBoardListener(new ConditionalScoreBoardListener(clock, Clock.Value.MINIMUM_TIME, listener));

        clock.setMaximumTime(2000);
        clock.setMinimumTime(2000);
        clock.setMinimumTime(1000);

        // validate constraint: reducing min time doesn't reset max or current time
        assertEquals(1000, clock.getMinimumTime());
        assertEquals(2000, clock.getMaximumTime());
        assertEquals(2000, clock.getTime());
    }

    @Test
    public void testSetMinimumTime3() {
        clock.setMaximumTime(2000);
        clock.setMinimumTime(1000);

        // validate constraint: time cannot be less than min time
        assertEquals(1000, clock.getMinimumTime());
        assertEquals(2000, clock.getMaximumTime());
        assertEquals(1000, clock.getTime());

    }

    @Test
    public void testSetMaximumTime() {
        clock.addScoreBoardListener(new ConditionalScoreBoardListener(clock, Clock.Value.MAXIMUM_TIME, listener));

        clock.setMaximumTime(0);
        collectedEvents.clear();

        clock.setMaximumTime(5000);

        // validate constraint: increase max time doesn't reset min or current time
        assertEquals(0, clock.getMinimumTime());
        assertEquals(5000, clock.getMaximumTime());
        assertEquals(0, clock.getTime());

        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertEquals(5000, (long)event.getValue());
        assertEquals(0, (long)event.getPreviousValue());
    }

    @Test
    public void testSetMaximumTime_countDown() {
        clock.setMaximumTime(5000);
        clock.setCountDirectionDown(true);
        clock.setTime(3000);
        clock.start();

        clock.setMaximumTime(10000);
        assertEquals(8000, clock.getTime());
        assertTrue(clock.isRunning());

        clock.setMaximumTime(1000);
        assertEquals(0, clock.getTime());
        assertFalse(clock.isRunning());
    }

    @Test
    public void testSetMaximumTime2() {
        clock.setMinimumTime(2000);
        clock.setMaximumTime(1000);

        // validate constraint: cannot set a max that is < min
        assertEquals(2000, clock.getMinimumTime());
        assertEquals(2000, clock.getMaximumTime());
        assertEquals(2000, clock.getTime());
    }

    @Test
    public void testChangeMaximumTime() {
        clock.addScoreBoardListener(new ConditionalScoreBoardListener(clock, Clock.Value.MAXIMUM_TIME, listener));

        clock.setMaximumTime(1000);
        collectedEvents.clear();

        clock.changeMaximumTime(2000);

        assertEquals(0, clock.getMinimumTime());
        assertEquals(3000, clock.getMaximumTime());
        assertEquals(0, clock.getTime());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertEquals(3000, (long)event.getValue());
        assertEquals(1000, (long)event.getPreviousValue());
    }

    @Test
    public void testChangeMinimumTime() {
        clock.addScoreBoardListener(new ConditionalScoreBoardListener(clock, Clock.Value.MINIMUM_TIME, listener));

        clock.setMaximumTime(5000);
        clock.setMinimumTime(5000);
        collectedEvents.clear();

        clock.changeMinimumTime(2000);

        assertEquals(7000, clock.getMinimumTime());
        assertEquals(7000, clock.getMaximumTime());
        assertEquals(7000, clock.getTime());

        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertEquals(7000, (long)event.getValue());
        assertEquals(5000, (long)event.getPreviousValue());
    }

    @Test
    public void testChangeTime() {
        clock.addScoreBoardListener(new ConditionalScoreBoardListener(clock, Clock.Value.TIME, listener));

        clock.setMaximumTime(5000);
        clock.setMinimumTime(1000);
        collectedEvents.clear();

        clock.setTime(2000);
        assertEquals(2000, clock.getTime());
        assertEquals(3000, clock.getInvertedTime());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertEquals(2000, (long)event.getValue());
        assertEquals(1000, (long)event.getPreviousValue());

        clock.setTime(6000);
        assertEquals(5000, clock.getTime());
        assertEquals(0, clock.getInvertedTime());
        assertEquals(1, collectedEvents.size());
        assertEquals(5000, (long)collectedEvents.poll().getValue());

        clock.setTime(400);
        assertEquals(1000, clock.getTime());
        assertEquals(4000, clock.getInvertedTime());
        assertEquals(1, collectedEvents.size());
        clock.start();
        collectedEvents.clear();

        clock.timerTick(200);
        assertEquals(1200, clock.getTime());
        assertEquals(3800, clock.getInvertedTime());
        assertEquals(0, collectedEvents.size());

        clock.timerTick(-201);
        assertEquals(999, clock.getTime());
        assertEquals(1, collectedEvents.size());

        clock.setCountDirectionDown(true);
        collectedEvents.clear();
        clock.timerTick(-1);
        assertEquals(0, collectedEvents.size());

        clock.setTime(2000);
        clock.changeTime(1200);
        assertEquals(3200, clock.getTime());
        assertEquals(1800, clock.getInvertedTime());
        assertEquals(2, collectedEvents.size());
        collectedEvents.clear();

        clock.addScoreBoardListener(new ConditionalScoreBoardListener(clock, Clock.Value.INVERTED_TIME, listener));
        clock.changeTime(-5000);
        assertEquals(1000, clock.getTime());
        assertEquals(4000, clock.getInvertedTime());
        assertEquals(2, collectedEvents.size());
        Boolean firstEventInverted;
        event = collectedEvents.poll();
        if (event.getProperty() == Clock.Value.TIME) {
            firstEventInverted = false;
            assertEquals(1000, (long)event.getValue());
            assertEquals(3200, (long)event.getPreviousValue());
        } else {
            firstEventInverted = true;
            assertEquals(4000, (long)event.getValue());
            assertEquals(1800, (long)event.getPreviousValue());
        }
        event = collectedEvents.poll();
        if (firstEventInverted) {
            assertEquals(1000, (long)event.getValue());
            assertEquals(3200, (long)event.getPreviousValue());
        } else {
            assertEquals(4000, (long)event.getValue());
            assertEquals(1800, (long)event.getPreviousValue());
        }

        clock.changeTime(4100);
        assertEquals(5100, clock.getTime());
        assertEquals(-100, clock.getInvertedTime());
    }

    @Test
    public void testElapseTime_countUp() {
        clock.addScoreBoardListener(new ConditionalScoreBoardListener(clock, Clock.Value.TIME, listener));
        clock.setMaximumTime(5000);

        clock.setTime(2000);
        assertEquals(2000, clock.getTimeElapsed());
        assertEquals(3000, clock.getTimeRemaining());

        clock.elapseTime(1000);
        assertEquals(3000, clock.getTime());
        assertEquals(3000, clock.getTimeElapsed());
        assertEquals(2000, clock.getTimeRemaining());
        assertEquals(2, collectedEvents.size());
    }

    @Test
    public void testElapseTime_countDown() {
        clock.setCountDirectionDown(true);
        clock.setMaximumTime(5000);

        clock.setTime(2000);
        assertEquals(3000, clock.getTimeElapsed());
        assertEquals(2000, clock.getTimeRemaining());

        clock.elapseTime(1000);
        assertEquals(1000, clock.getTime());
        assertEquals(4000, clock.getTimeElapsed());
        assertEquals(1000, clock.getTimeRemaining());
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
        clock.addScoreBoardListener(new ConditionalScoreBoardListener(clock, Clock.Value.TIME, listener));
        clock.setMaximumTime(5000);
        clock.setMinimumTime(1000);

        clock.setTime(3000);

        clock.resetTime();

        assertEquals(1000, clock.getTime());
        assertEquals(3, collectedEvents.size());

        clock.setTime(3000);
        clock.setCountDirectionDown(true);

        clock.resetTime();

        assertEquals(5000, clock.getTime());
    }

    public void testRunning() {
        clock.addScoreBoardListener(new ConditionalScoreBoardListener(clock, Clock.Value.RUNNING, listener));
        clock.addScoreBoardListener(new ConditionalScoreBoardListener(clock, Clock.Value.TIME, listener));
        clock.setMaximumTime(30000);
        assertFalse(clock.isCountDirectionDown());
        assertTrue(clock.isTimeAtStart());

        clock.start();
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertEquals(Clock.Value.RUNNING, event.getProperty());
        assertTrue((Boolean)event.getValue());
        assertFalse((Boolean)event.getPreviousValue());

        advance(1000);
        advance(500);
        advance(500);
        assertEquals(2000, clock.getTimeElapsed());
        assertEquals(2, collectedEvents.size());
        assertEquals(Clock.Value.TIME, collectedEvents.poll().getProperty());
        assertEquals(Clock.Value.TIME, collectedEvents.poll().getProperty());

        advance(2000);
        assertEquals(4000, clock.getTimeElapsed());
        assertEquals(1, collectedEvents.size());
        assertEquals(Clock.Value.TIME, collectedEvents.poll().getProperty());

        clock.stop();
        assertEquals(1, collectedEvents.size());
        event = collectedEvents.poll();
        assertEquals(Clock.Value.RUNNING, event.getProperty());
        assertFalse((Boolean)event.getValue());
        assertTrue((Boolean)event.getPreviousValue());

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

        clock.addScoreBoardListener(new ConditionalScoreBoardListener(clock, Clock.Value.TIME, listener));
        clock.start();
        advance(200);
        advance(200);
        advance(200);
        advance(200);
        advance(200);
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertEquals(0, ((Long)event.getValue()).longValue());
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

        sb.getRulesets().set(Rule.LINEUP_DIRECTION, String.valueOf(true));
        clock.rulesetChangeListener.scoreBoardChange(null);
        assertTrue(clock.isCountDirectionDown());
        assertEquals(86400000, clock.getTime());
    }
}
