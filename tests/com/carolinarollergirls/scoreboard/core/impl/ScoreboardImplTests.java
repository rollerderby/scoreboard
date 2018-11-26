package com.carolinarollergirls.scoreboard.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.Queue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.impl.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.utils.ClockConversion;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class ScoreboardImplTests {

    private ScoreBoardImpl sb;
    private Clock pc;
    private Clock jc;
    private Clock lc;
    private Clock tc;
    private Clock ic;
    private Queue<ScoreBoardEvent> collectedEvents;
    private ScoreBoardListener listener = new ScoreBoardListener() {
        @Override
        public void scoreBoardChange(ScoreBoardEvent event) {
            synchronized(collectedEvents) {
                collectedEvents.add(event);
            }
        }
    };

    private int batchLevel;
    private ScoreBoardListener batchCounter = new ScoreBoardListener() {
        @Override
        public void scoreBoardChange(ScoreBoardEvent event) {
            synchronized(batchCounter) {
                if (event.getProperty().equals(ScoreBoardEvent.BATCH_START)) {
                    batchLevel++;
                } else if (event.getProperty().equals(ScoreBoardEvent.BATCH_END)) {
                    batchLevel--;
                }
            }
        }
    };

    @Before
    public void setUp() throws Exception {
        ScoreBoardClock.getInstance().stop();
        sb = new ScoreBoardImpl();
        pc = sb.getClock(Clock.ID_PERIOD);
        jc = sb.getClock(Clock.ID_JAM);
        lc = sb.getClock(Clock.ID_LINEUP);
        tc = sb.getClock(Clock.ID_TIMEOUT);
        ic = sb.getClock(Clock.ID_INTERMISSION);
        collectedEvents = new LinkedList<ScoreBoardEvent>();
        sb.addScoreBoardListener(batchCounter);
        //Clock Sync can cause clocks to be changed when started, breaking tests.
        sb.getSettings().set(Clock.SETTING_SYNC, "False");
        sb.getSettings().set(ScoreBoard.SETTING_CLOCK_AFTER_TIMEOUT, "Lineup");
    }

    @After
    public void tearDown() throws Exception {
        ScoreBoardClock.getInstance().start(false);
        // Check all started batches were ended.
        assertEquals(0, batchLevel);
    }

    private void advance(long time_ms) {
        ScoreBoardClock.getInstance().advance(time_ms);
    }

    private void checkLabels(String startLabel, String stopLabel, String timeoutLabel, String undoLabel) {
        assertEquals(startLabel, sb.getSettings().get(ScoreBoard.BUTTON_START));
        assertEquals(stopLabel, sb.getSettings().get(ScoreBoard.BUTTON_STOP));
        assertEquals(timeoutLabel, sb.getSettings().get(ScoreBoard.BUTTON_TIMEOUT));
        assertEquals(undoLabel, sb.getSettings().get(ScoreBoard.BUTTON_UNDO));
    }

    private void checkLabels(String startLabel, String stopLabel, String timeoutLabel, String undoLabel, String replaceLabel) {
        checkLabels(startLabel, stopLabel, timeoutLabel, undoLabel);
        assertEquals(replaceLabel, sb.getSettings().get(ScoreBoard.BUTTON_REPLACED));
    }

    @Test
    public void testSetInPeriod() {
        assertFalse(sb.isInPeriod());
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(sb, ScoreBoard.EVENT_IN_PERIOD, listener));

        sb.setInPeriod(true);
        assertTrue(sb.isInPeriod());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertTrue((Boolean)event.getValue());
        assertFalse((Boolean)event.getPreviousValue());

        //check idempotency
        sb.setInPeriod(true);
        assertTrue(sb.isInPeriod());
        assertEquals(0, collectedEvents.size());

        sb.setInPeriod(false);
        assertFalse(sb.isInPeriod());
    }

    @Test
    public void testSetInOvertime() {
        (sb.getRulesets()).set("Clock." + Clock.ID_LINEUP + ".Time", "30000");
        lc.setMaximumTime(999999999);

        assertFalse(lc.isCountDirectionDown());
        assertFalse(sb.isInOvertime());
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(sb, ScoreBoard.EVENT_IN_OVERTIME, listener));

        sb.setInOvertime(true);
        assertTrue(sb.isInOvertime());
        assertEquals(999999999, lc.getMaximumTime());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertTrue((Boolean)event.getValue());
        assertFalse((Boolean)event.getPreviousValue());

        //check idempotency
        sb.setInOvertime(true);
        assertTrue(sb.isInOvertime());
        assertEquals(0, collectedEvents.size());

        sb.setInOvertime(true);
        assertTrue(sb.isInOvertime());

        sb.setInOvertime(false);
        assertFalse(sb.isInOvertime());
        assertEquals(999999999, lc.getMaximumTime());

        //check that lineup clock maximum time is reset for countdown lineup clock
        sb.setInOvertime(true);
        lc.setCountDirectionDown(true);
        sb.setInOvertime(false);
        assertEquals(30000, lc.getMaximumTime());
    }

    @Test
    public void testSetOfficialScore() {
        assertFalse(sb.isOfficialScore());
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(sb, ScoreBoard.EVENT_OFFICIAL_SCORE, listener));

        sb.setOfficialScore(true);
        assertTrue(sb.isOfficialScore());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertTrue((Boolean)event.getValue());
        assertFalse((Boolean)event.getPreviousValue());

        //check idempotency
        sb.setOfficialScore(true);
        assertTrue(sb.isOfficialScore());
        assertEquals(1, collectedEvents.size());

        sb.setOfficialScore(false);
        assertFalse(sb.isOfficialScore());
    }

    @Test
    public void testSetOfficialReview() {
        assertFalse(sb.isOfficialReview());
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(sb, ScoreBoard.EVENT_OFFICIAL_REVIEW, listener));

        sb.setOfficialReview(true);
        assertTrue(sb.isOfficialReview());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertTrue((Boolean)event.getValue());
        assertFalse((Boolean)event.getPreviousValue());

        //check idempotency
        sb.setOfficialReview(true);
        assertTrue(sb.isOfficialReview());
        assertEquals(1, collectedEvents.size());

        sb.setOfficialReview(false);
        assertFalse(sb.isOfficialReview());
    }

    @Test
    public void testSetTimeoutOwner() {
        assertEquals("", sb.getTimeoutOwner());
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(sb, ScoreBoard.EVENT_TIMEOUT_OWNER, listener));

        sb.setTimeoutOwner("testOwner");
        assertEquals("testOwner", sb.getTimeoutOwner());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertEquals("testOwner", event.getValue());
        assertEquals("", event.getPreviousValue());

        sb.setTimeoutOwner("");
        assertEquals("", sb.getTimeoutOwner());
        assertEquals(1, collectedEvents.size());
        event = collectedEvents.poll();
        assertEquals("", event.getValue());
        assertEquals("testOwner", event.getPreviousValue());
    }

    @Test
    public void testStartOvertime_default() {
        (sb.getRulesets()).set("Clock." + Clock.ID_LINEUP + ".OvertimeTime", "60000");

        assertFalse(pc.isRunning());
        pc.setTime(0);
        assertTrue(pc.isTimeAtEnd());
        pc.setNumber(pc.getMaximumNumber());
        assertFalse(jc.isRunning());
        jc.setTime(0);
        assertTrue(jc.isTimeAtEnd());
        assertFalse(lc.isRunning());
        lc.setMaximumTime(30000);
        assertFalse(tc.isRunning());
        ic.start();

        sb.startOvertime();

        assertEquals(ScoreBoard.ACTION_OVERTIME, sb.snapshot.getType());
        assertTrue(sb.isInOvertime());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertEquals(60000, lc.getMaximumTime());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_OVERTIME);
    }

    @Test
    public void testStartOvertime_fromTimeout() {
        assertFalse(pc.isRunning());
        pc.setTime(0);
        assertTrue(pc.isTimeAtEnd());
        pc.setNumber(pc.getMaximumNumber());
        assertFalse(jc.isRunning());
        jc.setTime(0);
        assertTrue(jc.isTimeAtEnd());
        assertFalse(lc.isRunning());
        tc.start();
        tc.setNumber(6);
        ic.start();

        sb.startOvertime();

        assertEquals(ScoreBoard.ACTION_OVERTIME, sb.snapshot.getType());
        assertTrue(sb.isInOvertime());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertFalse(tc.isRunning());
        assertEquals(6, tc.getNumber());
        assertFalse(ic.isRunning());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_OVERTIME);
    }

    @Test
    public void testStartOvertime_notLastPeriod() {
        assertNotEquals(pc.getNumber(), pc.getMaximumNumber());

        sb.startOvertime();

        assertEquals(null, sb.snapshot);
        assertFalse(sb.isInOvertime());
    }

    @Test
    public void testStartOvertime_periodRunning() {
        pc.start();

        sb.startOvertime();

        assertEquals(null, sb.snapshot);
        assertFalse(sb.isInOvertime());
    }

    @Test
    public void testStartOvertime_jamRunning() {
        pc.start();

        sb.startOvertime();

        assertEquals(null, sb.snapshot);
        assertFalse(sb.isInOvertime());
    }

    @Test
    public void testStartJam_duringPeriod() {
        pc.start();
        assertEquals(1, pc.getNumber());
        assertFalse(jc.isRunning());
        jc.setTime(34000);
        jc.setNumber(5);
        lc.start();
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        sb.startJam();

        assertEquals(ScoreBoard.ACTION_START_JAM, sb.snapshot.getType());
        assertTrue(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertTrue(jc.isRunning());
        assertTrue(jc.isTimeAtStart());
        assertEquals(6, jc.getNumber());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        checkLabels(ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_START_JAM);
    }

    @Test
    public void testStartJam_fromTimeout() {
        assertFalse(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertFalse(jc.isRunning());
        jc.setTime(100000);
        jc.setNumber(17);
        assertFalse(lc.isRunning());
        tc.setNumber(3);
        sb.setTimeoutOwner("2");
        sb.setOfficialReview(true);
        tc.start();
        assertFalse(ic.isRunning());
        sb.getTeam("2").setInTimeout(true);
        sb.getTeam("2").setInOfficialReview(true);

        sb.startJam();

        assertEquals(ScoreBoard.ACTION_START_JAM, sb.snapshot.getType());
        assertTrue(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertTrue(jc.isRunning());
        assertTrue(jc.isTimeAtStart());
        assertEquals(18, jc.getNumber());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertEquals(3, tc.getNumber());
        assertEquals("", sb.getTimeoutOwner());
        assertFalse(sb.isOfficialReview());
        assertFalse(ic.isRunning());
        assertFalse(sb.getTeam("2").inTimeout());
        assertFalse(sb.getTeam("2").inOfficialReview());
        checkLabels(ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_START_JAM);
    }

    @Test
    public void testStartJam_fromLineupAfterTimeout() {
        assertFalse(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertFalse(jc.isRunning());
        jc.setTime(45000);
        jc.setNumber(22);
        lc.start();
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        sb.startJam();

        assertEquals(ScoreBoard.ACTION_START_JAM, sb.snapshot.getType());
        assertTrue(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertTrue(jc.isRunning());
        assertTrue(jc.isTimeAtStart());
        assertEquals(23, jc.getNumber());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        checkLabels(ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_START_JAM);
    }

    @Test
    public void testStartJam_startOfPeriod() {
        assertFalse(pc.isRunning());
        assertTrue(pc.isTimeAtStart());
        assertEquals(1, pc.getNumber());
        assertFalse(jc.isRunning());
        assertTrue(jc.isTimeAtStart());
        assertEquals(0, jc.getNumber());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        sb.startJam();

        assertEquals(ScoreBoard.ACTION_START_JAM, sb.snapshot.getType());
        assertTrue(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertTrue(jc.isRunning());
        assertEquals(1, jc.getNumber());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        checkLabels(ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_START_JAM);
    }

    @Test
    public void testStartJam_lateInIntermission() {
        assertFalse(pc.isRunning());
        pc.setTime(pc.getMinimumTime());
        assertTrue(pc.isTimeAtEnd());
        assertEquals(1, pc.getNumber());
        assertFalse(jc.isRunning());
        jc.setTime(55000);
        jc.setNumber(21);
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        ic.setNumber(1);
        assertTrue(ic.isCountDirectionDown());
        ic.setMaximumTime(900000);
        ic.setTime(55000);
        ic.start();
        assertFalse(sb.isInPeriod());

        sb.startJam();

        assertEquals(ScoreBoard.ACTION_START_JAM, sb.snapshot.getType());
        assertTrue(pc.isRunning());
        assertEquals(2, pc.getNumber());
        assertTrue(jc.isRunning());
        assertEquals(1, jc.getNumber());
        assertTrue(jc.isTimeAtStart());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        assertTrue(sb.isInPeriod());
        checkLabels(ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_START_JAM);
    }

    @Test
    public void testStartJam_earlyInIntermission() {
        assertFalse(pc.isRunning());
        pc.setTime(pc.getMinimumTime());
        assertTrue(pc.isTimeAtEnd());
        assertEquals(1, pc.getNumber());
        assertFalse(jc.isRunning());
        jc.setTime(55000);
        jc.setNumber(21);
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        ic.setNumber(1);
        assertTrue(ic.isCountDirectionDown());
        ic.setMaximumTime(900000);
        ic.setTime(890000);
        ic.start();
        assertFalse(sb.isInPeriod());

        sb.startJam();
        advance(1000);

        assertEquals(ScoreBoard.ACTION_START_JAM, sb.snapshot.getType());
        assertFalse(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertTrue(jc.isRunning());
        assertEquals(22, jc.getNumber());
        assertEquals(1000, jc.getTimeElapsed());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        assertTrue(sb.isInPeriod());
        checkLabels(ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_START_JAM);
    }

    @Test
    public void testStartJam_jamRunning() {
        jc.setTime(74000);
        jc.setNumber(9);
        jc.start();
        sb.setLabels(ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT);
        sb.setLabel(ScoreBoard.BUTTON_UNDO, ScoreBoard.ACTION_NONE);

        sb.startJam();

        assertEquals(null, sb.snapshot);
        assertTrue(jc.isRunning());
        assertEquals(9, jc.getNumber());
        assertEquals(74000, jc.getTime());
        checkLabels(ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.ACTION_NONE);
    }

    @Test
    public void testStopJam_duringPeriod() {
        pc.start();
        jc.start();
        assertFalse(lc.isRunning());
        lc.setTime(50000);
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        sb.getTeam("1").setStarPass(true);
        sb.getTeam("2").setLeadJammer(Team.LEAD_NO_LEAD);

        sb.stopJamTO();

        assertEquals(ScoreBoard.ACTION_STOP_JAM, sb.snapshot.getType());
        assertTrue(pc.isRunning());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertTrue(lc.isTimeAtStart());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        assertFalse(sb.getTeam("1").isStarPass());
        assertEquals(Team.LEAD_NO_LEAD, sb.getTeam("2").getLeadJammer());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_STOP_JAM);
    }

    @Test
    public void testStopJam_endOfPeriod() {
        assertFalse(pc.isRunning());
        pc.setTime(0);
        assertTrue(pc.isTimeAtEnd());
        pc.setNumber(2);
        jc.start();
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        ic.setNumber(0);
        ic.setMaximumTime(90000000);
        ic.setTime(784000);
        sb.setInPeriod(true);
        sb.setOfficialScore(true);

        sb.stopJamTO();

        assertEquals(ScoreBoard.ACTION_STOP_JAM, sb.snapshot.getType());
        assertFalse(pc.isRunning());
        assertTrue(pc.isTimeAtEnd());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertTrue(ic.isRunning());
        assertEquals(2, ic.getNumber());
        long dur = ClockConversion.fromHumanReadable(sb.getRulesets().get(ScoreBoard.RULE_INTERMISSION_DURATIONS).split(",")[1]);
        assertEquals(dur, ic.getMaximumTime());
        assertTrue(ic.isTimeAtStart());
        assertFalse(sb.isInPeriod());
        assertFalse(sb.isOfficialScore());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_LINEUP, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_STOP_JAM);
    }

    @Test
    public void testStopJam_endTimeoutDuringPeriod() {
        assertFalse(pc.isRunning());
        assertFalse(pc.isTimeAtEnd());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        lc.setTime(37000);
        tc.start();
        tc.setNumber(4);
        assertFalse(ic.isRunning());
        sb.setTimeoutOwner("O");
        sb.setOfficialReview(true);

        sb.stopJamTO();

        assertEquals(ScoreBoard.ACTION_STOP_TO, sb.snapshot.getType());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertTrue(lc.isTimeAtStart());
        assertFalse(tc.isRunning());
        assertEquals(4, tc.getNumber());
        assertFalse(ic.isRunning());
        assertEquals("", sb.getTimeoutOwner());
        assertFalse(sb.isOfficialReview());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_STOP_TO);
    }

    @Test
    public void testStopJam_endTimeoutAfterPeriod() {
        assertFalse(pc.isRunning());
        pc.setTime(0);
        assertTrue(pc.isTimeAtEnd());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        tc.start();
        tc.setNumber(3);
        assertFalse(ic.isRunning());

        sb.stopJamTO();

        assertEquals(ScoreBoard.ACTION_STOP_TO, sb.snapshot.getType());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertEquals(3, tc.getNumber());
        assertTrue(ic.isRunning());
        assertTrue(ic.isTimeAtStart());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_LINEUP, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_STOP_TO);
    }

    @Test
    public void testStopJam_endTimeoutKeepTimeoutClock() {
        sb.getSettings().set(ScoreBoard.SETTING_CLOCK_AFTER_TIMEOUT, "Timeout");
        assertFalse(pc.isRunning());
        assertFalse(pc.isTimeAtEnd());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        tc.setTime(32000);
        tc.start();
        tc.setNumber(8);
        assertFalse(ic.isRunning());
        assertEquals("", sb.getTimeoutOwner());
        assertFalse(sb.isOfficialReview());

        sb.stopJamTO();

        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertEquals(32000, tc.getTimeElapsed());
        assertEquals(8, tc.getNumber());
        assertFalse(ic.isRunning());
        assertEquals("", sb.getTimeoutOwner());
        assertFalse(sb.isOfficialReview());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_STOP_TO);
    }

    @Test
    public void testStopJam_lineupEarlyInIntermission() {
        assertFalse(pc.isRunning());
        pc.setNumber(1);
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        lc.setTime(30000);
        assertFalse(tc.isRunning());
        assertTrue(ic.isCountDirectionDown());
        ic.setMaximumTime(900000);
        ic.setTime(880000);
        ic.start();

        sb.stopJamTO();

        assertEquals(ScoreBoard.ACTION_LINEUP, sb.snapshot.getType());
        assertFalse(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertTrue(lc.isTimeAtStart());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_LINEUP);
    }

    @Test
    public void testStopJam_lineupLateInIntermission() {
        assertFalse(pc.isRunning());
        pc.setNumber(1);
        assertTrue(pc.isCountDirectionDown());
        pc.setTime(0);
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertFalse(lc.isCountDirectionDown());
        lc.setTime(30000);
        assertFalse(tc.isRunning());
        assertTrue(ic.isCountDirectionDown());
        ic.setMaximumTime(900000);
        ic.setTime(43000);
        ic.setNumber(1);
        ic.start();

        sb.stopJamTO();

        assertEquals(ScoreBoard.ACTION_LINEUP, sb.snapshot.getType());
        assertFalse(pc.isRunning());
        assertEquals(2, pc.getNumber());
        assertTrue(pc.isTimeAtStart());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertTrue(lc.isTimeAtStart());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_LINEUP);
    }

    @Test
    public void testStopJam_lineupRunning() {
        String prevUndoLabel = sb.getSettings().get(ScoreBoard.BUTTON_UNDO);
        sb.setLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_TIMEOUT);
        lc.setTime(14000);
        lc.setNumber(9);
        lc.start();

        sb.stopJamTO();

        assertEquals(null, sb.snapshot);
        assertTrue(lc.isRunning());
        assertEquals(9, lc.getNumber());
        assertEquals(14000, lc.getTime());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_TIMEOUT, prevUndoLabel);
    }

    @Test
    public void testTimeout_fromLineup() {
        pc.start();
        assertFalse(jc.isRunning());
        lc.start();
        assertFalse(tc.isRunning());
        tc.setTime(23000);
        tc.setNumber(2);
        assertFalse(ic.isRunning());

        sb.timeout();

        assertEquals(ScoreBoard.ACTION_TIMEOUT, sb.snapshot.getType());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertTrue(tc.isTimeAtStart());
        assertEquals(3, tc.getNumber());
        assertFalse(ic.isRunning());
        assertEquals("", sb.getTimeoutOwner());
        assertFalse(sb.isOfficialReview());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_STOP_TO, ScoreBoard.ACTION_RE_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_TIMEOUT);
    }

    @Test
    public void testTimeout_fromJam() {
        pc.start();
        jc.start();
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        sb.timeout();

        assertEquals(ScoreBoard.ACTION_TIMEOUT, sb.snapshot.getType());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertTrue(tc.isTimeAtStart());
        assertFalse(ic.isRunning());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_STOP_TO, ScoreBoard.ACTION_RE_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_TIMEOUT);
    }

    @Test
    public void testTimeout_fromIntermission() {
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        ic.start();

        sb.timeout();

        assertEquals(ScoreBoard.ACTION_TIMEOUT, sb.snapshot.getType());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertTrue(tc.isTimeAtStart());
        assertFalse(ic.isRunning());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_STOP_TO, ScoreBoard.ACTION_RE_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_TIMEOUT);
    }

    @Test
    public void testTimeout_AfterGame() {
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        sb.timeout();

        assertEquals(ScoreBoard.ACTION_TIMEOUT, sb.snapshot.getType());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertTrue(tc.isTimeAtStart());
        assertFalse(ic.isRunning());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_STOP_TO, ScoreBoard.ACTION_RE_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_TIMEOUT);
    }

    @Test
    public void testTimeout_fromTimeout() {
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        tc.start();
        tc.setTime(24000);
        tc.setNumber(7);
        assertFalse(ic.isRunning());
        sb.setTimeoutOwner(ScoreBoard.TIMEOUT_OWNER_NONE);

        sb.timeout();

        assertEquals(ScoreBoard.ACTION_RE_TIMEOUT, sb.snapshot.getType());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertTrue(tc.isTimeAtStart());
        assertEquals(8, tc.getNumber());
        assertFalse(ic.isRunning());
        assertEquals(ScoreBoard.TIMEOUT_OWNER_NONE, sb.getTimeoutOwner());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_STOP_TO, ScoreBoard.ACTION_RE_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_RE_TIMEOUT);

        sb.timeout();

        assertEquals("", sb.getTimeoutOwner());
    }

    @Test
    public void testSetTimeoutType() {
        sb.setTimeoutOwner("");

        sb.setTimeoutType("2", false);
        sb.getTeam("2").setTimeouts(2);

        assertEquals(ScoreBoard.ACTION_TIMEOUT, sb.snapshot.getType());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertFalse(ic.isRunning());
        assertEquals("2", sb.getTimeoutOwner());
        assertFalse(sb.isOfficialReview());
        assertEquals(2, sb.getTeam("2").getTimeouts());

        sb.setTimeoutType("1", true);
        assertEquals("1", sb.getTimeoutOwner());
        assertTrue(sb.isOfficialReview());
        assertEquals(3, sb.getTeam("2").getTimeouts());
    }

    @Test
    public void testClockUndo_undo() {
        pc.start();
        jc.start();
        sb.setInPeriod(true);
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        assertEquals("", sb.getTimeoutOwner());
        assertFalse(sb.isOfficialReview());
        assertFalse(sb.isInOvertime());
        assertTrue(sb.isInPeriod());
        sb.setLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT);
        sb.setLabel(ScoreBoard.BUTTON_UNDO, ScoreBoard.ACTION_NONE);

        sb.createSnapshot("TEST");
        assertEquals("TEST", sb.snapshot.getType());
        assertEquals(ScoreBoard.UNDO_PREFIX + "TEST", sb.getSettings().get(ScoreBoard.BUTTON_UNDO));

        pc.stop();
        jc.stop();
        lc.start();
        tc.start();
        ic.start();
        sb.setTimeoutOwner("TestOwner");
        sb.setOfficialReview(true);
        sb.setInOvertime(true);
        sb.setInPeriod(false);
        sb.setLabels(ScoreBoard.ACTION_LINEUP, ScoreBoard.ACTION_RE_TIMEOUT, ScoreBoard.ACTION_OVERTIME);
        advance(2000);

        sb.clockUndo(false);
        //need to manually advance as the stopped clock will not catch up to system time
        advance(ScoreBoardClock.getInstance().getLastRewind());
        assertTrue(pc.isRunning());
        assertEquals(2000, pc.getTimeElapsed());
        assertTrue(jc.isRunning());
        assertEquals(2000, jc.getTimeElapsed());
        assertFalse(lc.isRunning());
        assertTrue(lc.isTimeAtStart());
        assertFalse(tc.isRunning());
        assertTrue(tc.isTimeAtStart());
        assertFalse(ic.isRunning());
        assertTrue(ic.isTimeAtStart());
        assertEquals("", sb.getTimeoutOwner());
        assertFalse(sb.isOfficialReview());
        assertFalse(sb.isInOvertime());
        assertTrue(sb.isInPeriod());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.ACTION_NONE);
    }

    @Test
    public void testClockUndo_replace() {
        pc.elapseTime(600000);
        pc.start();
        jc.start();
        sb.setInPeriod(true);
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        sb.setLabels(ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT);

        sb.timeout();
        assertEquals(ScoreBoard.ACTION_TIMEOUT, sb.snapshot.getType());
        advance(2000);

        sb.clockUndo(true);
        checkLabels(ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT,
                    ScoreBoard.ACTION_NO_REPLACE, ScoreBoard.ACTION_TIMEOUT);
        sb.stopJamTO();

        advance(ScoreBoardClock.getInstance().getLastRewind());
        assertTrue(pc.isRunning());
        assertEquals(602000, pc.getTimeElapsed());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertEquals(2000, lc.getTimeElapsed());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        sb.clockUndo(true);
        sb.clockUndo(true);
        advance(ScoreBoardClock.getInstance().getLastRewind());

        assertTrue(pc.isRunning());
        assertEquals(602000, pc.getTimeElapsed());
        assertTrue(jc.isRunning());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        sb.clockUndo(true);
        sb.timeout();
        advance(ScoreBoardClock.getInstance().getLastRewind());

        assertFalse(pc.isRunning());
        assertEquals(600000, pc.getTimeElapsed());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertEquals(2000, tc.getTimeElapsed());
        assertFalse(ic.isRunning());

        sb.stopJamTO();
        advance(5000);
        sb.timeout();
        advance(3000);

        sb.clockUndo(true);
        sb.startJam();
        advance(ScoreBoardClock.getInstance().getLastRewind());

        assertTrue(pc.isRunning());
        assertEquals(603000, pc.getTimeElapsed());
        assertTrue(jc.isRunning());
        assertEquals(3000, jc.getTimeElapsed());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
    }

    @Test
    public void testTimeoutCountOnUndo() {
        assertEquals(3, sb.getTeam("1").getTimeouts());
        assertEquals(1, sb.getTeam("2").getOfficialReviews());
        pc.start();
        lc.start();
        sb.timeout();
        sb.getTeam("1").timeout();
        assertEquals(2, sb.getTeam("1").getTimeouts());
        sb.clockUndo(false);
        assertEquals(3, sb.getTeam("1").getTimeouts());
        sb.getTeam("2").officialReview();
        assertEquals(0, sb.getTeam("2").getOfficialReviews());
        sb.clockUndo(false);
        assertEquals(1, sb.getTeam("2").getOfficialReviews());
    }

    @Test
    public void testPeriodClockEnd_duringLineup() {
        sb.getRulesets().set(ScoreBoard.RULE_INTERMISSION_DURATIONS, "5:00,15:00,5:00,60:00");
        String prevUndoLabel = sb.getSettings().get(ScoreBoard.BUTTON_UNDO);

        pc.start();
        assertTrue(pc.isCountDirectionDown());
        pc.setTime(2000);
        pc.setNumber(2);
        assertFalse(jc.isRunning());
        lc.start();
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        ic.setNumber(0);
        ic.setTime(3000);

        advance(2000);

        assertFalse(pc.isRunning());
        assertEquals(2, pc.getNumber());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertTrue(ic.isRunning());
        assertTrue(ic.isTimeAtStart());
        assertEquals(2, ic.getNumber());
        long dur = ClockConversion.fromHumanReadable(sb.getRulesets().get(ScoreBoard.RULE_INTERMISSION_DURATIONS).split(",")[1]);
        assertEquals(dur, ic.getTimeRemaining());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_LINEUP, ScoreBoard.ACTION_TIMEOUT, prevUndoLabel);
    }

    @Test
    public void testPeriodClockEnd_periodEndInhibitedByRuleset() {
        sb.getRulesets().set(ScoreBoard.RULE_PERIOD_END_BETWEEN_JAMS, "false");
        String prevUndoLabel = sb.getSettings().get(ScoreBoard.BUTTON_UNDO);
        sb.setLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_TIMEOUT);

        pc.start();
        assertTrue(pc.isCountDirectionDown());
        pc.setTime(2000);
        pc.setNumber(1);
        assertFalse(jc.isRunning());
        lc.start();
        assertTrue(lc.isTimeAtStart());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        advance(2000);

        assertFalse(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertEquals(2000, lc.getTimeElapsed());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_TIMEOUT, prevUndoLabel);
    }

    @Test
    public void testPeriodClockEnd_duringJam() {
        String prevStartLabel = sb.getSettings().get(ScoreBoard.BUTTON_START);
        String prevStopLabel = sb.getSettings().get(ScoreBoard.BUTTON_STOP);
        String prevTimeoutLabel = sb.getSettings().get(ScoreBoard.BUTTON_TIMEOUT);
        String prevUndoLabel = sb.getSettings().get(ScoreBoard.BUTTON_UNDO);
        pc.start();
        assertTrue(pc.isCountDirectionDown());
        pc.setTime(2000);
        jc.start();
        jc.setNumber(17);
        assertTrue(jc.isCountDirectionDown());
        jc.setTime(10000);
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        advance(2000);

        assertFalse(pc.isRunning());
        assertTrue(jc.isRunning());
        assertEquals(17, jc.getNumber());
        assertEquals(8000, jc.getTime());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        checkLabels(prevStartLabel, prevStopLabel, prevTimeoutLabel, prevUndoLabel);
    }

    @Test
    public void testJamClockEnd_pcRemaining() {
        String prevUndoLabel = sb.getSettings().get(ScoreBoard.BUTTON_UNDO);
        pc.start();
        jc.start();
        assertTrue(jc.isCountDirectionDown());
        jc.setTime(3000);
        assertFalse(lc.isRunning());
        lc.setTime(50000);
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        advance(3000);

        assertTrue(pc.isRunning());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertTrue(lc.isTimeAtStart());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_TIMEOUT, prevUndoLabel);
    }

    @Test
    public void testJamClockEnd_autoEndDisabled() {
        sb.getRulesets().set(ScoreBoard.RULE_AUTO_END_JAM, "false");
        String prevStartLabel = sb.getSettings().get(ScoreBoard.BUTTON_START);
        String prevStopLabel = sb.getSettings().get(ScoreBoard.BUTTON_STOP);
        String prevTimeoutLabel = sb.getSettings().get(ScoreBoard.BUTTON_TIMEOUT);
        String prevUndoLabel = sb.getSettings().get(ScoreBoard.BUTTON_UNDO);

        pc.start();
        jc.start();
        assertTrue(jc.isCountDirectionDown());
        jc.setTime(3000);
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        advance(3000);

        assertTrue(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        checkLabels(prevStartLabel, prevStopLabel, prevTimeoutLabel, prevUndoLabel);
    }

    @Test
    public void testIntermissionClockEnd_notLastPeriod() {
        String prevUndoLabel = sb.getSettings().get(ScoreBoard.BUTTON_UNDO);
        assertFalse(pc.isRunning());
        assertTrue(pc.isCountDirectionDown());
        pc.setTime(0);
        pc.setNumber(1);
        assertFalse(jc.isRunning());
        jc.setTime(4000);
        jc.setNumber(20);
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        ic.start();
        assertTrue(ic.isCountDirectionDown());
        ic.setNumber(1);
        ic.setTime(3000);

        advance(3000);

        assertFalse(pc.isRunning());
        assertTrue(pc.isTimeAtStart());
        assertEquals(2, pc.getNumber());
        assertFalse(jc.isRunning());
        assertTrue(jc.isTimeAtStart());
        assertEquals(0, jc.getNumber());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        assertTrue(ic.isTimeAtEnd());
        assertEquals(1, ic.getNumber());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_LINEUP, ScoreBoard.ACTION_TIMEOUT, prevUndoLabel);
    }

    @Test
    public void testIntermissionClockEnd_notLastPeriodContinueCountingJams() {
        sb.getRulesets().set(ScoreBoard.RULE_JAM_NUMBER_PER_PERIOD, "false");

        assertFalse(pc.isRunning());
        assertTrue(pc.isCountDirectionDown());
        pc.setTime(0);
        pc.setNumber(1);
        assertFalse(jc.isRunning());
        jc.setTime(4000);
        jc.setNumber(20);
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        ic.start();
        assertTrue(ic.isCountDirectionDown());
        ic.setNumber(1);
        ic.setTime(3000);

        advance(3000);

        assertEquals(20, jc.getNumber());
    }

    @Test
    public void testIntermissionClockEnd_lastPeriod() {
        String prevStartLabel = sb.getSettings().get(ScoreBoard.BUTTON_START);
        String prevStopLabel = sb.getSettings().get(ScoreBoard.BUTTON_STOP);
        String prevTimeoutLabel = sb.getSettings().get(ScoreBoard.BUTTON_TIMEOUT);
        String prevUndoLabel = sb.getSettings().get(ScoreBoard.BUTTON_UNDO);
        assertFalse(pc.isRunning());
        assertTrue(pc.isCountDirectionDown());
        pc.setTime(0);
        pc.setNumber(pc.getMaximumNumber());
        assertFalse(jc.isRunning());
        jc.setNumber(21);
        jc.setTime(56000);
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        ic.start();
        assertTrue(ic.isCountDirectionDown());
        ic.setNumber(pc.getMaximumNumber());
        ic.setTime(3000);

        advance(3000);

        assertFalse(pc.isRunning());
        assertTrue(pc.isTimeAtEnd());
        assertEquals(pc.getMaximumNumber(), pc.getNumber());
        assertFalse(jc.isRunning());
        assertEquals(21, jc.getNumber());
        assertEquals(56000, jc.getTime());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        assertTrue(ic.isTimeAtEnd());
        assertEquals(pc.getMaximumNumber(), ic.getNumber());
        checkLabels(prevStartLabel, prevStopLabel, prevTimeoutLabel, prevUndoLabel);
    }

    @Test
    public void testTimeoutInLast30s() {
        //jam ended before 30s mark, official timeout after 30s mark
        assertTrue(pc.isCountDirectionDown());
        pc.setTime(35000);
        pc.start();
        lc.start();
        advance(10000);
        sb.timeout();
        advance(20000);
        sb.stopJamTO();
        assertFalse(pc.isRunning());

        //jam ended after 30s mark, official timeout
        sb.startJam();
        sb.stopJamTO();
        assertEquals(25000, pc.getTime());
        assertTrue(pc.isRunning());
        advance(1000);
        sb.timeout();
        advance(35000);
        sb.stopJamTO();
        assertTrue(pc.isRunning());

        //follow up with team timeout
        advance(2000);
        sb.setTimeoutType("1", false);
        advance(60000);
        sb.stopJamTO();
        assertFalse(pc.isRunning());
        assertEquals(22000, pc.getTimeRemaining());
    }

    @Test
    public void testTimeoutsThatDontAlwaysStopPc() {
        sb.getRulesets().set(ScoreBoard.RULE_STOP_PC_ON_TO, "false");
        sb.getRulesets().set(ScoreBoard.RULE_STOP_PC_ON_OTO, "false");
        sb.getRulesets().set(ScoreBoard.RULE_STOP_PC_ON_TTO, "true");
        sb.getRulesets().set(ScoreBoard.RULE_STOP_PC_ON_OR, "true");
        sb.getRulesets().set(ScoreBoard.RULE_STOP_PC_AFTER_TO_DURATION, "120000");

        assertTrue(pc.isCountDirectionDown());
        pc.setTime(1200000);
        pc.start();
        assertFalse(jc.isRunning());
        lc.start();
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        sb.timeout();

        assertTrue(pc.isRunning());
        assertEquals(600000, pc.getTimeElapsed());
        assertTrue(tc.isRunning());
        assertEquals(0, tc.getTimeElapsed());

        advance(2000);
        assertEquals(602000, pc.getTimeElapsed());

        sb.setTimeoutType("1", false);

        assertFalse(pc.isRunning());
        assertEquals(600000, pc.getTimeElapsed());
        assertTrue(tc.isRunning());

        advance(3000);
        assertEquals(600000, pc.getTimeElapsed());

        sb.setTimeoutType("O", false);

        assertTrue(pc.isRunning());
        assertEquals(605000, pc.getTimeElapsed());
        assertTrue(tc.isRunning());

        advance(115000);

        assertFalse(pc.isRunning());
        assertEquals(719800, pc.getTimeElapsed()); //tc ticks first, stopping pc, so pc's tick is skipped
        assertTrue(tc.isRunning());
    }

    @Test
    public void testAutoStartJam() {
        sb.getRulesets().set(ScoreBoard.RULE_AUTO_START, "true");
        sb.getRulesets().set(ScoreBoard.RULE_AUTO_START_JAM, "true");

        pc.start();
        assertFalse(jc.isRunning());
        assertFalse(lc.isCountDirectionDown());
        assertTrue(32000 <= lc.getMaximumTime());
        lc.setTime(0);
        lc.start();
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        advance(31000);
        assertFalse(jc.isRunning());
        advance(1000);

        assertTrue(pc.isRunning());
        assertTrue(jc.isRunning());
        assertEquals(2000, jc.getTimeElapsed());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        checkLabels(ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_START_JAM);
    }

    @Test
    public void testAutoStartAndEndTimeout() {
        sb.getRulesets().set(ScoreBoard.RULE_AUTO_START, "true");
        sb.getRulesets().set(ScoreBoard.RULE_AUTO_START_JAM, "false");
        sb.getRulesets().set(ScoreBoard.RULE_AUTO_START_BUFFER, "0");
        sb.getRulesets().set(ScoreBoard.RULE_AUTO_END_TTO, "true");
        sb.getRulesets().set(ScoreBoard.RULE_TTO_DURATION, "25000");

        pc.start();
        assertFalse(jc.isRunning());
        assertFalse(lc.isCountDirectionDown());
        lc.setTime(0);
        lc.start();
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        advance(30000);

        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertTrue(tc.isTimeAtStart());
        assertFalse(ic.isRunning());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_STOP_TO, ScoreBoard.ACTION_RE_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_TIMEOUT);

        sb.setTimeoutType("2", true);

        advance(25000);

        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_STOP_TO);
    }

    @Test
    public void testResetDoesntAffectSettings() {
        sb.getSettings().set("foo", "bar");
        sb.reset();
        assertEquals("bar", sb.getSettings().get("foo"));
    }
}
