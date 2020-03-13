package com.carolinarollergirls.scoreboard.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Penalty;
import com.carolinarollergirls.scoreboard.core.Period;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.Team.Value;
import com.carolinarollergirls.scoreboard.core.Timeout;
import com.carolinarollergirls.scoreboard.core.Skater.NChild;
import com.carolinarollergirls.scoreboard.core.impl.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.core.impl.ScoreBoardImpl.Button;
import com.carolinarollergirls.scoreboard.core.impl.ScoreBoardImpl.ScoreBoardSnapshot;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl.BatchEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.rules.Rule;
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
                if (event.getProperty() == BatchEvent.START) {
                    batchLevel++;
                } else if (event.getProperty() == BatchEvent.END) {
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
        assertTrue(pc.isTimeAtStart());
        assertTrue(jc.isTimeAtStart());
        assertTrue(lc.isTimeAtStart());
        assertTrue(tc.isTimeAtStart());
        assertTrue(ic.isTimeAtStart());
        collectedEvents = new LinkedList<>();
        sb.addScoreBoardListener(batchCounter);
        //Clock Sync can cause clocks to be changed when started, breaking tests.
        sb.getSettings().set(Clock.SETTING_SYNC, "False");
        sb.getSettings().set(ScoreBoard.SETTING_CLOCK_AFTER_TIMEOUT, "Lineup");
    }

    @After
    public void tearDown() throws Exception {
        ScoreBoardClock.getInstance().start(false);
        sb.reset();
        // Check all started batches were ended.
        assertEquals(0, batchLevel);
    }

    private void advance(long time_ms) {
        ScoreBoardClock.getInstance().advance(time_ms);
    }

    private void fastForwardJams(int number) {
        for (int i = 0; i < number; i++) { 
            sb.startJam();
            sb.stopJamTO();
        }
    }

    private void fastForwardPeriod() {
        ic.setTime(0);
        sb.startJam();
        pc.setTime(0);
        sb.stopJamTO();
    }

    private void checkLabels(String startLabel, String stopLabel, String timeoutLabel, String undoLabel) {
        assertEquals(startLabel, Button.START.getLabel());
        assertEquals(stopLabel, Button.STOP.getLabel());
        assertEquals(timeoutLabel, Button.TIMEOUT.getLabel());
        assertEquals(undoLabel, Button.UNDO.getLabel());
    }

    private void checkLabels(String startLabel, String stopLabel, String timeoutLabel, String undoLabel, String replaceLabel) {
        checkLabels(startLabel, stopLabel, timeoutLabel, undoLabel);
        assertEquals(replaceLabel, Button.REPLACED.getLabel());
    }

    @Test
    public void testSetInPeriod() {
        assertFalse(sb.isInPeriod());
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(sb, ScoreBoard.Value.IN_PERIOD, listener));

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
        (sb.getRulesets()).set(Rule.LINEUP_DURATION, "30000");
        lc.setMaximumTime(999999999);

        assertFalse(lc.isCountDirectionDown());
        assertFalse(sb.isInOvertime());
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(sb, ScoreBoard.Value.IN_OVERTIME, listener));

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
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(sb, ScoreBoard.Value.OFFICIAL_SCORE, listener));
        fastForwardPeriod();
        fastForwardPeriod();

        sb.setOfficialScore(true);
        assertTrue(sb.isOfficialScore());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertTrue((Boolean)event.getValue());
        assertFalse((Boolean)event.getPreviousValue());

        //check idempotency
        sb.setOfficialScore(true);
        assertTrue(sb.isOfficialScore());
        assertEquals(0, collectedEvents.size());

        sb.setOfficialScore(false);
        assertFalse(sb.isOfficialScore());
    }

    @Test
    public void testSetOfficialReview() {
        sb.timeout();
        assertFalse(sb.isOfficialReview());
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(sb, ScoreBoard.Value.OFFICIAL_REVIEW, listener));

        sb.setOfficialReview(true);
        assertTrue(sb.isOfficialReview());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertTrue((Boolean)event.getValue());
        assertFalse((Boolean)event.getPreviousValue());

        //check idempotency
        sb.setOfficialReview(true);
        assertTrue(sb.isOfficialReview());
        assertEquals(0, collectedEvents.size());

        sb.setOfficialReview(false);
        assertFalse(sb.isOfficialReview());
    }

    @Test
    public void testSetTimeoutOwner() {
        sb.timeout();
        assertEquals(Timeout.Owners.NONE, sb.getTimeoutOwner());
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(sb, ScoreBoard.Value.TIMEOUT_OWNER, listener));

        sb.setTimeoutOwner(Timeout.Owners.OTO);
        assertEquals(Timeout.Owners.OTO, sb.getTimeoutOwner());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertEquals(Timeout.Owners.OTO, event.getValue());
        assertEquals(Timeout.Owners.NONE, event.getPreviousValue());

        sb.setTimeoutOwner(Timeout.Owners.NONE);
        assertEquals(Timeout.Owners.NONE, sb.getTimeoutOwner());
        assertEquals(1, collectedEvents.size());
        event = collectedEvents.poll();
        assertEquals(Timeout.Owners.NONE, event.getValue());
        assertEquals(Timeout.Owners.OTO, event.getPreviousValue());
    }

    @Test
    public void testStartOvertime_default() {
        (sb.getRulesets()).set(Rule.OVERTIME_LINEUP_DURATION, "60000");

        fastForwardPeriod();
        fastForwardPeriod();
        assertFalse(pc.isRunning());
        assertTrue(pc.isTimeAtEnd());
        assertEquals(sb.getRulesets().getInt(Rule.NUMBER_PERIODS), pc.getNumber());
        assertFalse(jc.isRunning());
        jc.setTime(0);
        assertTrue(jc.isTimeAtEnd());
        assertFalse(lc.isRunning());
        lc.setMaximumTime(30000);
        assertFalse(tc.isRunning());
        assertTrue(ic.isRunning());

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
        fastForwardPeriod();
        fastForwardPeriod();
        sb.timeout();
        assertFalse(pc.isRunning());
        assertTrue(pc.isTimeAtEnd());
        assertEquals(sb.getRulesets().getInt(Rule.NUMBER_PERIODS), pc.getNumber());
        assertFalse(jc.isRunning());
        jc.setTime(0);
        assertTrue(jc.isTimeAtEnd());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        tc.setNumber(6);
        assertFalse(ic.isRunning());

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
        assertNotEquals(pc.getNumber(), sb.getRulesets().getInt(Rule.NUMBER_PERIODS));

        sb.startOvertime();

        assertEquals(null, sb.snapshot);
        assertFalse(sb.isInOvertime());
    }

    @Test
    public void testStartOvertime_periodRunning() {
        fastForwardPeriod();
        ic.setTime(0);
        sb.startJam();
        assertEquals(sb.getRulesets().getInt(Rule.NUMBER_PERIODS), pc.getNumber());
        assertTrue(pc.isRunning());
        ScoreBoardSnapshot saved = sb.snapshot;

        sb.startOvertime();

        assertEquals(saved, sb.snapshot);
        assertFalse(sb.isInOvertime());
    }

    @Test
    public void testStartOvertime_jamRunning() {
        fastForwardPeriod();
        ic.setTime(0);
        sb.startJam();
        pc.setTime(0);
        assertEquals(sb.getRulesets().getInt(Rule.NUMBER_PERIODS), pc.getNumber());
        assertFalse(pc.isRunning());
        assertTrue(jc.isRunning());
        ScoreBoardSnapshot saved = sb.snapshot;

        sb.startOvertime();

        assertEquals(saved, sb.snapshot);
        assertFalse(sb.isInOvertime());
    }

    @Test
    public void testStartJam_duringPeriod() {
        fastForwardJams(5);
        assertTrue(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertFalse(jc.isRunning());
        jc.setTime(34000);
        assertTrue(lc.isRunning());
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
        fastForwardJams(17);
        sb.timeout();
        assertFalse(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertFalse(jc.isRunning());
        jc.setTime(100000);
        jc.setNumber(17);
        assertFalse(lc.isRunning());
        tc.setNumber(3);
        sb.setTimeoutType(sb.getTeam(Team.ID_2), true);
        sb.setOfficialReview(true);
        assertTrue(tc.isRunning());
        assertFalse(ic.isRunning());
        assertFalse(sb.getTeam(Team.ID_2).inTimeout());
        assertTrue(sb.getTeam(Team.ID_2).inOfficialReview());

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
        assertEquals(Timeout.Owners.NONE, sb.getTimeoutOwner());
        assertFalse(sb.isOfficialReview());
        assertFalse(ic.isRunning());
        assertFalse(sb.getTeam(Team.ID_2).inTimeout());
        assertFalse(sb.getTeam(Team.ID_2).inOfficialReview());
        checkLabels(ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_START_JAM);
    }

    @Test
    public void testStartJam_fromLineupAfterTimeout() {
        fastForwardJams(22);
        sb.timeout();
        sb.stopJamTO();
        assertFalse(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertFalse(jc.isRunning());
        jc.setTime(45000);
        assertEquals(22, jc.getNumber());
        assertTrue(lc.isRunning());
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
        ic.start();
        ic.setTime(0);
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
        fastForwardJams(20);
        fastForwardPeriod();
        assertFalse(pc.isRunning());
        assertTrue(pc.isTimeAtEnd());
        assertEquals(1, pc.getNumber());
        assertFalse(jc.isRunning());
        jc.setTime(55000);
        assertEquals(21, jc.getNumber());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertEquals(1, ic.getNumber());
        assertTrue(ic.isCountDirectionDown());
        ic.setMaximumTime(900000);
        ic.setTime(55000);
        assertFalse(sb.isInPeriod());
        assertEquals(2, sb.getAll(ScoreBoard.NChild.PERIOD).size());
        assertEquals(1, sb.getCurrentPeriodNumber());

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
        assertEquals(3, sb.getAll(ScoreBoard.NChild.PERIOD).size());
        assertEquals(2, sb.getCurrentPeriodNumber());
        checkLabels(ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_START_JAM);
    }

    @Test
    public void testStartJam_earlyInIntermission() {
        fastForwardJams(20);
        fastForwardPeriod();
        assertFalse(pc.isRunning());
        assertTrue(pc.isTimeAtEnd());
        assertEquals(1, pc.getNumber());
        assertFalse(jc.isRunning());
        jc.setTime(55000);
        assertEquals(21, jc.getNumber());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertEquals(1, ic.getNumber());
        assertTrue(ic.isCountDirectionDown());
        ic.setMaximumTime(900000);
        ic.setTime(890000);
        assertTrue(ic.isRunning());
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
        fastForwardJams(8);
        sb.startJam();
        jc.setTime(74000);
        assertEquals(9, jc.getNumber());
        assertTrue(jc.isRunning());
        sb.setLabels(ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT);
        Button.UNDO.setLabel(ScoreBoard.ACTION_NONE);
        ScoreBoardSnapshot saved = sb.snapshot;

        sb.startJam();

        assertEquals(saved, sb.snapshot);
        assertTrue(jc.isRunning());
        assertEquals(9, jc.getNumber());
        assertEquals(74000, jc.getTime());
        checkLabels(ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.ACTION_NONE);
    }

    @Test
    public void testStopJam_duringPeriod() {
        sb.startJam();
        assertTrue(pc.isRunning());
        assertTrue(jc.isRunning());
        assertFalse(lc.isRunning());
        lc.setTime(50000);
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        sb.getTeam(Team.ID_1).set(Value.STAR_PASS, true);
        sb.getTeam(Team.ID_2).set(Value.LEAD, true);

        sb.stopJamTO();

        assertEquals(ScoreBoard.ACTION_STOP_JAM, sb.snapshot.getType());
        assertTrue(pc.isRunning());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertTrue(lc.isTimeAtStart());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        assertTrue(sb.getTeam(Team.ID_1).isStarPass());
        assertTrue(sb.getTeam(Team.ID_2).isLead());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_STOP_JAM);
    }

    @Test
    public void testStopJam_endOfPeriod() {
        fastForwardPeriod();
        ic.setTime(0);
        sb.startJam();
        pc.setTime(0);
        assertFalse(pc.isRunning());
        assertTrue(pc.isTimeAtEnd());
        assertEquals(2, pc.getNumber());
        assertTrue(jc.isRunning());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
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
        long dur = ClockConversion.fromHumanReadable(sb.getRulesets().get(Rule.INTERMISSION_DURATIONS).split(",")[1]);
        assertEquals(dur, ic.getMaximumTime());
        assertTrue(ic.isTimeAtStart());
        assertFalse(sb.isInPeriod());
        assertFalse(sb.isOfficialScore());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_LINEUP, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_STOP_JAM);
    }

    @Test
    public void testStopJam_endTimeoutDuringPeriod() {
        sb.startJam();
        sb.timeout();
        assertFalse(pc.isRunning());
        assertFalse(pc.isTimeAtEnd());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        lc.setTime(37000);
        assertTrue(tc.isRunning());
        tc.setNumber(4);
        assertFalse(ic.isRunning());
        sb.setTimeoutOwner(Timeout.Owners.OTO);
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
        assertEquals(Timeout.Owners.NONE, sb.getTimeoutOwner());
        assertFalse(sb.isOfficialReview());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_STOP_TO);
    }

    @Test
    public void testStopJam_endTimeoutAfterPeriod() {
        sb.startJam();
        sb.timeout();
        assertFalse(pc.isRunning());
        pc.setTime(0);
        assertTrue(pc.isTimeAtEnd());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
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
        assertEquals(Timeout.Owners.NONE, sb.getTimeoutOwner());
        assertFalse(sb.isOfficialReview());

        sb.stopJamTO();

        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertEquals(32000, tc.getTimeElapsed());
        assertEquals(8, tc.getNumber());
        assertFalse(ic.isRunning());
        assertEquals(Timeout.Owners.NONE, sb.getTimeoutOwner());
        assertFalse(sb.isOfficialReview());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_STOP_TO);
    }

    @Test
    public void testStopJam_lineupEarlyInIntermission() {
        fastForwardPeriod();
        assertFalse(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        lc.setTime(30000);
        assertFalse(tc.isRunning());
        assertTrue(ic.isCountDirectionDown());
        ic.setMaximumTime(900000);
        ic.setTime(ic.getMaximumTime() - 500);
        assertTrue(ic.isRunning());

        sb.stopJamTO();
        
        // less than 1s of intermission has passed, stopJamTO should be ignored
        assertFalse(lc.isRunning());
        assertTrue(ic.isRunning());
        
        advance(20000);
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
        fastForwardPeriod();
        assertFalse(pc.isRunning());
        assertTrue(pc.isTimeAtEnd());
        assertTrue(pc.isCountDirectionDown());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertFalse(lc.isCountDirectionDown());
        lc.setTime(30000);
        assertFalse(tc.isRunning());
        assertTrue(ic.isCountDirectionDown());
        ic.setMaximumTime(900000);
        ic.setTime(43000);
        assertTrue(ic.isRunning());

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
        String prevUndoLabel = Button.UNDO.getLabel();
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
        fastForwardJams(1);
        assertTrue(pc.isRunning());
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
        assertEquals(Timeout.Owners.NONE, sb.getTimeoutOwner());
        assertFalse(sb.isOfficialReview());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_STOP_TO, ScoreBoard.ACTION_RE_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_TIMEOUT);
    }

    @Test
    public void testTimeout_fromJam() {
        sb.startJam();
        assertTrue(pc.isRunning());
        assertTrue(jc.isRunning());
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
        fastForwardPeriod();
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertTrue(ic.isRunning());

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
        sb.setTimeoutOwner(Timeout.Owners.OTO);

        sb.timeout();

        assertEquals(ScoreBoard.ACTION_RE_TIMEOUT, sb.snapshot.getType());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertTrue(tc.isTimeAtStart());
        assertEquals(8, tc.getNumber());
        assertFalse(ic.isRunning());
        assertEquals(Timeout.Owners.NONE, sb.getTimeoutOwner());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_STOP_TO, ScoreBoard.ACTION_RE_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_RE_TIMEOUT);

        sb.setTimeoutOwner(Timeout.Owners.OTO);
        sb.timeout();

        // timeout was entered with less than 1s on the TO clock
        // and should be ignored
        assertEquals(8, tc.getNumber());
        assertEquals(Timeout.Owners.OTO, sb.getTimeoutOwner());
        
        advance(1500);
        sb.timeout();
        
        assertEquals(9, tc.getNumber());
        assertEquals(Timeout.Owners.NONE, sb.getTimeoutOwner());
    }

    @Test
    public void testSetTimeoutType() {
        sb.setTimeoutType(sb.getTeam(Team.ID_2), false);

        assertEquals(ScoreBoard.ACTION_TIMEOUT, sb.snapshot.getType());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertFalse(ic.isRunning());
        assertEquals(sb.getTeam(Team.ID_2), sb.getTimeoutOwner());
        assertFalse(sb.isOfficialReview());
        assertEquals(2, sb.getTeam(Team.ID_2).getTimeouts());

        sb.setTimeoutType(sb.getTeam(Team.ID_1), true);
        assertEquals(sb.getTeam(Team.ID_1), sb.getTimeoutOwner());
        assertTrue(sb.isOfficialReview());
        assertEquals(3, sb.getTeam(Team.ID_2).getTimeouts());
    }

    @Test
    public void testClockUndo_undo() {
        sb.startJam();
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        assertEquals(Timeout.Owners.NONE, sb.getTimeoutOwner());
        assertFalse(sb.isOfficialReview());
        assertFalse(sb.isInOvertime());
        assertTrue(sb.isInPeriod());
        sb.setLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT);
        Button.UNDO.setLabel(ScoreBoard.ACTION_NONE);

        sb.createSnapshot("TEST");
        assertEquals("TEST", sb.snapshot.getType());
        assertEquals(ScoreBoard.UNDO_PREFIX + "TEST", Button.UNDO.getLabel());

        sb.timeout();
        lc.start();
        ic.start();
        sb.setTimeoutOwner(Timeout.Owners.OTO);
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
        assertEquals(Timeout.Owners.NONE, sb.getTimeoutOwner());
        assertFalse(sb.isOfficialReview());
        assertFalse(sb.isInOvertime());
        assertTrue(sb.isInPeriod());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.ACTION_NONE);
    }

    @Test
    public void testClockUndoWithClockSync() {
        sb.getSettings().set(Clock.SETTING_SYNC, "True");
        sb.startJam();
        assertEquals(0, jc.getInvertedTime());
        assertEquals(0, pc.getInvertedTime());
        assertEquals(120000, jc.getTime());
        assertEquals(1800000, pc.getTime());
        advance(600);
        sb.clockUndo(false);


        assertEquals(0, jc.getInvertedTime());
        assertEquals(0, pc.getInvertedTime());
        assertEquals(120000, jc.getTime());
        assertEquals(1800000, pc.getTime());
    }

    @Test
    public void testClockUndo_replace() {
        sb.startJam();
        pc.elapseTime(600000);
        assertTrue(pc.isRunning());
        assertTrue(jc.isRunning());
        assertTrue(sb.isInPeriod());
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
    public void testJamStartUndoRedo() {
        sb.startJam();
        for (int i = 0; i < 5; i++){
          sb.clockUndo(false);
          // Regression test for an NPE here.
          sb.startJam();
        }
    }

    @Test
    public void testJamStartUndoAfterPeriodEnd() {
        fastForwardJams(5);
        pc.setTime(2000);
        advance(2000);
        assertFalse(pc.isRunning());
        assertEquals(5, sb.getCurrentPeriod().getAll(Period.NChild.JAM).size());

        sb.startJam();
        assertEquals(6, sb.getCurrentPeriod().getAll(Period.NChild.JAM).size());

        sb.clockUndo(false);
        assertEquals(5, sb.getCurrentPeriod().getAll(Period.NChild.JAM).size());
    }

    @Test
    public void testTimeoutCountOnUndo() {
        fastForwardJams(1);
        assertEquals(3, sb.getTeam(Team.ID_1).getTimeouts());
        assertEquals(1, sb.getTeam(Team.ID_2).getOfficialReviews());
        sb.timeout();
        sb.getTeam(Team.ID_1).timeout();
        assertEquals(2, sb.getTeam(Team.ID_1).getTimeouts());
        sb.clockUndo(false);
        assertEquals(3, sb.getTeam(Team.ID_1).getTimeouts());
        sb.getTeam(Team.ID_2).officialReview();
        assertEquals(0, sb.getTeam(Team.ID_2).getOfficialReviews());
        sb.clockUndo(false);
        assertEquals(1, sb.getTeam(Team.ID_2).getOfficialReviews());
    }

    @Test
    public void testPeriodClockEnd_duringLineup() {
        sb.getRulesets().set(Rule.INTERMISSION_DURATIONS, "5:00,15:00,5:00,60:00");

        fastForwardPeriod();
        ic.setTime(0);
        fastForwardJams(1);
        advance(0);
        String prevUndoLabel = Button.UNDO.getLabel();

        assertTrue(pc.isRunning());
        assertTrue(pc.isCountDirectionDown());
        pc.setTime(2000);
        assertEquals(2, pc.getNumber());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
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
        long dur = ClockConversion.fromHumanReadable(sb.getRulesets().get(Rule.INTERMISSION_DURATIONS).split(",")[1]);
        assertEquals(dur, ic.getTimeRemaining());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_LINEUP, ScoreBoard.ACTION_TIMEOUT, prevUndoLabel);
    }

    @Test
    public void testPeriodClockEnd_periodEndInhibitedByRuleset() {
        sb.getRulesets().set(Rule.PERIOD_END_BETWEEN_JAMS, "false");

        fastForwardJams(1);
        String prevUndoLabel = Button.UNDO.getLabel();

        assertTrue(pc.isRunning());
        assertTrue(pc.isCountDirectionDown());
        pc.setTime(2000);
        assertEquals(1, pc.getNumber());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
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
        fastForwardJams(16);
        sb.startJam();
        String prevStartLabel = Button.START.getLabel();
        String prevStopLabel = Button.STOP.getLabel();
        String prevTimeoutLabel = Button.TIMEOUT.getLabel();
        String prevUndoLabel = Button.UNDO.getLabel();
        assertTrue(pc.isRunning());
        assertTrue(pc.isCountDirectionDown());
        pc.setTime(2000);
        assertTrue(jc.isRunning());
        assertEquals(17, jc.getNumber());
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
        sb.startJam();
        String prevUndoLabel = Button.UNDO.getLabel();
        assertTrue(pc.isRunning());
        assertTrue(jc.isRunning());
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
        sb.getRulesets().set(Rule.AUTO_END_JAM, "false");
        sb.startJam();
        String prevStartLabel = Button.START.getLabel();
        String prevStopLabel = Button.STOP.getLabel();
        String prevTimeoutLabel = Button.TIMEOUT.getLabel();
        String prevUndoLabel = Button.UNDO.getLabel();

        assertTrue(pc.isRunning());
        assertTrue(jc.isRunning());
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
        
        advance(sb.getRulesets().getLong(Rule.PERIOD_DURATION));
        sb.stopJamTO();
        
        assertFalse(lc.isRunning());
    }

    @Test
    public void testIntermissionClockEnd_notLastPeriod() {
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(jc, Clock.Value.NUMBER, listener));
        fastForwardJams(19);
        fastForwardPeriod();
        String prevUndoLabel = Button.UNDO.getLabel();
        assertFalse(pc.isRunning());
        assertTrue(pc.isCountDirectionDown());
        assertTrue(pc.isTimeAtEnd());
        assertEquals(1, pc.getNumber());
        assertFalse(jc.isRunning());
        jc.setTime(4000);
        assertEquals(20, jc.getNumber());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertTrue(ic.isRunning());
        assertTrue(ic.isCountDirectionDown());
        assertEquals(1, ic.getNumber());
        ic.setTime(3000);

        collectedEvents.clear();
        advance(3000);

        assertFalse(pc.isRunning());
        assertTrue(pc.isTimeAtStart());
        assertEquals(2, pc.getNumber());
        assertFalse(jc.isRunning());
        assertTrue(jc.isTimeAtStart());
        assertEquals(20, jc.getNumber());
        assertEquals(1, collectedEvents.size());
        assertEquals(0, collectedEvents.poll().getValue());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        assertTrue(ic.isTimeAtEnd());
        assertEquals(2, ic.getNumber());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_LINEUP, ScoreBoard.ACTION_TIMEOUT, prevUndoLabel);
    }

    @Test
    public void testIntermissionClockEnd_notLastPeriodContinueCountingJams() {
        sb.getRulesets().set(Rule.JAM_NUMBER_PER_PERIOD, "false");

        fastForwardJams(19);
        fastForwardPeriod();
        assertFalse(pc.isRunning());
        assertTrue(pc.isCountDirectionDown());
        assertTrue(pc.isTimeAtEnd());
        assertFalse(jc.isRunning());
        assertEquals(20, jc.getNumber());
        jc.setTime(4000);
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertTrue(ic.isRunning());
        assertTrue(ic.isCountDirectionDown());
        assertEquals(1, ic.getNumber());
        ic.setTime(3000);

        advance(3000);

        assertEquals(20, jc.getNumber());
    }

    @Test
    public void testIntermissionClockEnd_lastPeriod() {
        fastForwardPeriod();
        ic.setTime(0);
        fastForwardJams(20);
        fastForwardPeriod();
        String prevStartLabel = Button.START.getLabel();
        String prevStopLabel = Button.STOP.getLabel();
        String prevTimeoutLabel = Button.TIMEOUT.getLabel();
        String prevUndoLabel = Button.UNDO.getLabel();
        assertFalse(pc.isRunning());
        assertTrue(pc.isCountDirectionDown());
        assertTrue(pc.isTimeAtEnd());
        assertEquals(sb.getRulesets().getInt(Rule.NUMBER_PERIODS), pc.getNumber());
        assertFalse(jc.isRunning());
        assertEquals(21, jc.getNumber());
        jc.setTime(56000);
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertTrue(ic.isRunning());
        assertTrue(ic.isCountDirectionDown());
        assertEquals(sb.getRulesets().getInt(Rule.NUMBER_PERIODS), ic.getNumber());
        ic.setTime(3000);

        advance(3000);

        assertFalse(pc.isRunning());
        assertTrue(pc.isTimeAtEnd());
        assertEquals(sb.getRulesets().getInt(Rule.NUMBER_PERIODS), pc.getNumber());
        assertFalse(jc.isRunning());
        assertEquals(21, jc.getNumber());
        assertEquals(56000, jc.getTime());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        assertTrue(ic.isTimeAtEnd());
        assertEquals(sb.getRulesets().getInt(Rule.NUMBER_PERIODS), ic.getNumber());
        checkLabels(prevStartLabel, prevStopLabel, prevTimeoutLabel, prevUndoLabel);
    }

    @Test
    public void testTimeoutInLast30s() {
        //jam ended before 30s mark, official timeout after 30s mark
        assertTrue(pc.isCountDirectionDown());
        sb.startJam();
        pc.setTime(35000);
        sb.stopJamTO();
        assertFalse((Boolean)sb.get(ScoreBoard.Value.NO_MORE_JAM));
        assertTrue(pc.isRunning());
        assertTrue(lc.isRunning());
        advance(10000);
        sb.timeout();
        advance(20000);
        sb.stopJamTO();
        assertFalse((Boolean)sb.get(ScoreBoard.Value.NO_MORE_JAM));
        assertFalse(pc.isRunning());

        //jam ended after 30s mark, official timeout
        sb.startJam();
        sb.stopJamTO();
        assertTrue((Boolean)sb.get(ScoreBoard.Value.NO_MORE_JAM));
        assertEquals(25000, pc.getTime());
        assertTrue(pc.isRunning());
        advance(1000);
        sb.timeout();
        advance(35000);
        assertTrue((Boolean)sb.get(ScoreBoard.Value.NO_MORE_JAM));
        sb.setTimeoutType(Timeout.Owners.OTO, false);
        assertTrue((Boolean)sb.get(ScoreBoard.Value.NO_MORE_JAM));
        sb.stopJamTO();
        assertTrue((Boolean)sb.get(ScoreBoard.Value.NO_MORE_JAM));
        assertTrue(pc.isRunning());

        //follow up with team timeout
        advance(2000);
        sb.setTimeoutType(sb.getTeam(Team.ID_1), false);
        assertFalse((Boolean)sb.get(ScoreBoard.Value.NO_MORE_JAM));
        advance(60000);
        sb.stopJamTO();
        assertFalse((Boolean)sb.get(ScoreBoard.Value.NO_MORE_JAM));
        assertFalse(pc.isRunning());
        assertEquals(22000, pc.getTimeRemaining());
    }

    @Test
    public void testP2StartLineupAfter() {
        // jam ended after 30s mark, no more jams.
        assertTrue(pc.isCountDirectionDown());
        sb.startJam();
        pc.setTime(2000);
        sb.stopJamTO();
        assertTrue((Boolean)sb.get(ScoreBoard.Value.NO_MORE_JAM));
        assertTrue(pc.isRunning());
        assertTrue(lc.isRunning());
        assertFalse(ic.isRunning());

        // End of jam, start intermission.
        advance(2000);
        assertFalse(pc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(ic.isRunning());

        // End intermission, go to lineup.
        ic.setTime(1000);
        advance(2000);
        sb.stopJamTO();
        assertFalse((Boolean)sb.get(ScoreBoard.Value.NO_MORE_JAM));
        assertFalse(pc.isRunning());
        assertTrue(lc.isRunning());
        assertFalse(ic.isRunning());
    }

    @Test
    public void testTimeoutsThatDontAlwaysStopPc() {
        sb.getRulesets().set(Rule.STOP_PC_ON_TO, "false");
        sb.getRulesets().set(Rule.STOP_PC_ON_OTO, "false");
        sb.getRulesets().set(Rule.STOP_PC_ON_TTO, "true");
        sb.getRulesets().set(Rule.STOP_PC_ON_OR, "true");
        sb.getRulesets().set(Rule.STOP_PC_AFTER_TO_DURATION, "120000");

        fastForwardJams(1);
        assertTrue(pc.isCountDirectionDown());
        pc.setTime(1200000);
        assertTrue(pc.isRunning());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        sb.timeout();

        assertTrue(pc.isRunning());
        assertEquals(600000, pc.getTimeElapsed());
        assertTrue(tc.isRunning());
        assertEquals(0, tc.getTimeElapsed());

        advance(2000);
        assertEquals(602000, pc.getTimeElapsed());

        sb.setTimeoutType(sb.getTeam(Team.ID_1), false);

        assertFalse(pc.isRunning());
        assertEquals(600000, pc.getTimeElapsed());
        assertTrue(tc.isRunning());

        advance(3000);
        assertEquals(600000, pc.getTimeElapsed());

        sb.setTimeoutType(Timeout.Owners.OTO, false);

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
        sb.getRulesets().set(Rule.AUTO_START, "true");
        sb.getRulesets().set(Rule.AUTO_START_JAM, "true");

        fastForwardJams(1);
        assertTrue(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isCountDirectionDown());
        assertTrue(32000 <= lc.getMaximumTime());
        assertTrue(lc.isTimeAtStart());
        assertTrue(lc.isRunning());
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
    public void testNoAutoEndJam() {
        sb.getRulesets().set(Rule.AUTO_END_JAM, "false");

        sb.startJam();
        advance(jc.getMaximumTime());

        assertFalse(jc.isRunning());
        assertTrue(sb.isInJam());
    }

    @Test
    public void testAutoStartAndEndTimeout() {
        sb.getRulesets().set(Rule.AUTO_START, "true");
        sb.getRulesets().set(Rule.AUTO_START_JAM, "false");
        sb.getRulesets().set(Rule.AUTO_START_BUFFER, "0");
        sb.getRulesets().set(Rule.AUTO_END_TTO, "true");
        sb.getRulesets().set(Rule.TTO_DURATION, "25000");

        fastForwardJams(1);
        assertTrue(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isCountDirectionDown());
        assertTrue(lc.isTimeAtStart());
        assertTrue(lc.isRunning());
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

        sb.setTimeoutType(sb.getTeam(Team.ID_2), true);

        advance(25000);

        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_STOP_TO);
    }

    @Test
    public void testPeriodJamInsertRemove() {
        fastForwardJams(4);
        
        assertEquals(2, sb.getAll(ScoreBoard.NChild.PERIOD).size());
        assertEquals(0, sb.getMinNumber(ScoreBoard.NChild.PERIOD) + 0);
        assertEquals(1, sb.getMaxNumber(ScoreBoard.NChild.PERIOD) + 0);
        Period p0 = (Period) sb.getFirst(ScoreBoard.NChild.PERIOD);
        Period p1 = (Period) sb.getLast(ScoreBoard.NChild.PERIOD);
        assertEquals(p1, sb.getCurrentPeriod());
        assertEquals(0, p0.getNumber());
        assertEquals(1, p1.getNumber());
        assertEquals(p1, p0.getNext());
        assertNull(p1.getNext());
        assertNull(p0.getPrevious());
        assertEquals(p0, p1.getPrevious());
        assertEquals(1, p0.getAll(Period.NChild.JAM).size());
        assertEquals(0, p0.getMinNumber(Period.NChild.JAM) + 0);
        assertEquals(0, p0.getMaxNumber(Period.NChild.JAM) + 0);
        assertEquals(4, p1.getAll(Period.NChild.JAM).size());
        assertEquals(1, p1.getMinNumber(Period.NChild.JAM) + 0);
        assertEquals(4, p1.getMaxNumber(Period.NChild.JAM) + 0);
        assertEquals(1, sb.getAll(Period.NChild.JAM).size());
        assertEquals(5, sb.getMinNumber(Period.NChild.JAM) + 0);
        assertEquals(5, sb.getMaxNumber(Period.NChild.JAM) + 0);
        Jam j0 = (Jam) p0.getFirst(Period.NChild.JAM);
        Jam j1 = (Jam) p1.getFirst(Period.NChild.JAM);
        Jam j2 = (Jam) p1.get(Period.NChild.JAM, 2);
        Jam j3 = (Jam) p1.get(Period.NChild.JAM, 3);
        Jam j4 = (Jam) p1.getLast(Period.NChild.JAM);
        Jam j5 = (Jam)sb.getFirst(Period.NChild.JAM);
        assertEquals(j0, p0.getCurrentJam());
        assertEquals(j4, p1.getCurrentJam());
        assertEquals(j5, sb.getUpcomingJam());
        assertEquals(0, j0.getNumber());
        assertEquals(1, j1.getNumber());
        assertEquals(2, j2.getNumber());
        assertEquals(3, j3.getNumber());
        assertEquals(4, j4.getNumber());
        assertEquals(5, j5.getNumber());
        assertEquals(j1, j0.getNext());
        assertEquals(j2, j1.getNext());
        assertEquals(j3, j2.getNext());
        assertEquals(j4, j3.getNext());
        assertEquals(j5, j4.getNext());
        assertNull(j5.getNext());
        assertNull(j0.getPrevious());
        assertEquals(j0, j1.getPrevious());
        assertEquals(j1, j2.getPrevious());
        assertEquals(j2, j3.getPrevious());
        assertEquals(j3, j4.getPrevious());
        assertEquals(j4, j5.getPrevious());
        
        sb.getCurrentPeriod().execute(Period.Command.INSERT_BEFORE);
        ((Period)sb.get(ScoreBoard.NChild.PERIOD, "1")).delete();
        
        assertEquals(2, sb.getAll(ScoreBoard.NChild.PERIOD).size());
        assertEquals(0, sb.getMinNumber(ScoreBoard.NChild.PERIOD) + 0);
        assertEquals(1, sb.getMaxNumber(ScoreBoard.NChild.PERIOD) + 0);
        p0 = (Period) sb.getFirst(ScoreBoard.NChild.PERIOD);
        p1 = (Period) sb.getLast(ScoreBoard.NChild.PERIOD);
        assertEquals(p1, sb.getCurrentPeriod());
        assertEquals(0, p0.getNumber());
        assertEquals(1, p1.getNumber());
        assertEquals(p1, p0.getNext());
        assertNull(p1.getNext());
        assertNull(p0.getPrevious());
        assertEquals(p0, p1.getPrevious());
        assertEquals(1, p0.getAll(Period.NChild.JAM).size());
        assertEquals(0, p0.getMinNumber(Period.NChild.JAM) + 0);
        assertEquals(0, p0.getMaxNumber(Period.NChild.JAM) + 0);
        assertEquals(4, p1.getAll(Period.NChild.JAM).size());
        assertEquals(1, p1.getMinNumber(Period.NChild.JAM) + 0);
        assertEquals(4, p1.getMaxNumber(Period.NChild.JAM) + 0);
        assertEquals(1, sb.getAll(Period.NChild.JAM).size());
        assertEquals(5, sb.getMinNumber(Period.NChild.JAM) + 0);
        assertEquals(5, sb.getMaxNumber(Period.NChild.JAM) + 0);
        j0 = (Jam) p0.getFirst(Period.NChild.JAM);
        j1 = (Jam) p1.getFirst(Period.NChild.JAM);
        j2 = (Jam) p1.get(Period.NChild.JAM, 2);
        j3 = (Jam) p1.get(Period.NChild.JAM, 3);
        j4 = (Jam) p1.getLast(Period.NChild.JAM);
        j5 = (Jam)sb.getFirst(Period.NChild.JAM);
        assertEquals(j0, p0.getCurrentJam());
        assertEquals(j4, p1.getCurrentJam());
        assertEquals(j5, sb.getUpcomingJam());
        assertEquals(0, j0.getNumber());
        assertEquals(1, j1.getNumber());
        assertEquals(2, j2.getNumber());
        assertEquals(3, j3.getNumber());
        assertEquals(4, j4.getNumber());
        assertEquals(5, j5.getNumber());
        assertEquals(j1, j0.getNext());
        assertEquals(j2, j1.getNext());
        assertEquals(j3, j2.getNext());
        assertEquals(j4, j3.getNext());
        assertEquals(j5, j4.getNext());
        assertNull(j5.getNext());
        assertNull(j0.getPrevious());
        assertEquals(j0, j1.getPrevious());
        assertEquals(j1, j2.getPrevious());
        assertEquals(j2, j3.getPrevious());
        assertEquals(j3, j4.getPrevious());
        assertEquals(j4, j5.getPrevious());

        sb.getCurrentPeriod().getCurrentJam().execute(Jam.Command.INSERT_BEFORE);
        ((Jam)((ScoreBoardEventProvider) sb.get(ScoreBoard.NChild.PERIOD, "1")).get(Period.NChild.JAM, 1)).delete();
        
        assertEquals(2, sb.getAll(ScoreBoard.NChild.PERIOD).size());
        assertEquals(0, sb.getMinNumber(ScoreBoard.NChild.PERIOD) + 0);
        assertEquals(1, sb.getMaxNumber(ScoreBoard.NChild.PERIOD) + 0);
        p0 = (Period) sb.getFirst(ScoreBoard.NChild.PERIOD);
        p1 = (Period) sb.getLast(ScoreBoard.NChild.PERIOD);
        assertEquals(p1, sb.getCurrentPeriod());
        assertEquals(0, p0.getNumber());
        assertEquals(1, p1.getNumber());
        assertEquals(p1, p0.getNext());
        assertNull(p1.getNext());
        assertNull(p0.getPrevious());
        assertEquals(p0, p1.getPrevious());
        assertEquals(1, p0.getAll(Period.NChild.JAM).size());
        assertEquals(0, p0.getMinNumber(Period.NChild.JAM) + 0);
        assertEquals(0, p0.getMaxNumber(Period.NChild.JAM) + 0);
        assertEquals(4, p1.getAll(Period.NChild.JAM).size());
        assertEquals(1, p1.getMinNumber(Period.NChild.JAM) + 0);
        assertEquals(4, p1.getMaxNumber(Period.NChild.JAM) + 0);
        assertEquals(1, sb.getAll(Period.NChild.JAM).size());
        assertEquals(5, sb.getMinNumber(Period.NChild.JAM) + 0);
        assertEquals(5, sb.getMaxNumber(Period.NChild.JAM) + 0);
        j0 = (Jam) p0.getFirst(Period.NChild.JAM);
        j1 = (Jam) p1.getFirst(Period.NChild.JAM);
        j2 = (Jam) p1.get(Period.NChild.JAM, 2);
        j3 = (Jam) p1.get(Period.NChild.JAM, 3);
        j4 = (Jam) p1.getLast(Period.NChild.JAM);
        j5 = (Jam)sb.getFirst(Period.NChild.JAM);
        assertEquals(j0, p0.getCurrentJam());
        assertEquals(j4, p1.getCurrentJam());
        assertEquals(j5, sb.getUpcomingJam());
        assertEquals(0, j0.getNumber());
        assertEquals(1, j1.getNumber());
        assertEquals(2, j2.getNumber());
        assertEquals(3, j3.getNumber());
        assertEquals(4, j4.getNumber());
        assertEquals(5, j5.getNumber());
        assertEquals(j1, j0.getNext());
        assertEquals(j2, j1.getNext());
        assertEquals(j3, j2.getNext());
        assertEquals(j4, j3.getNext());
        assertEquals(j5, j4.getNext());
        assertNull(j5.getNext());
        assertNull(j0.getPrevious());
        assertEquals(j0, j1.getPrevious());
        assertEquals(j1, j2.getPrevious());
        assertEquals(j2, j3.getPrevious());
        assertEquals(j3, j4.getPrevious());
        assertEquals(j4, j5.getPrevious());
    }
    
    @Test
    public void testDeleteCurrentPeriod() {
        fastForwardJams(2);
        assertTrue(pc.isCountDirectionDown());
        pc.setTime(1000);
        advance(1000);
        
        assertTrue(pc.isTimeAtEnd());
        assertFalse(pc.isRunning());
        assertEquals(2, sb.getAll(ScoreBoard.NChild.PERIOD).size());
        assertEquals(1, sb.getCurrentPeriodNumber());
        
        sb.getCurrentPeriod().execute(Period.Command.DELETE);
        
        assertEquals(1, sb.getAll(ScoreBoard.NChild.PERIOD).size());
        assertEquals(0, sb.getCurrentPeriodNumber());

        // Make sure we can start the jam.
        sb.startJam();
        assertEquals(1, sb.getCurrentPeriodNumber());
    }
    
    @Test
    public void testDeleteJam() {
        fastForwardJams(3);
        sb.startJam();
        
        Jam j2 = sb.getCurrentPeriod().getJam(2);
        j2.execute(Jam.Command.DELETE);
        
        assertEquals(3, sb.getCurrentPeriod().getAll(Period.NChild.JAM).size());

        // Make sure we can start the jam again.
        sb.stopJamTO();
        assertEquals(3, sb.getCurrentPeriod().getAll(Period.NChild.JAM).size());
        sb.startJam();
        assertEquals(4, sb.getCurrentPeriod().getAll(Period.NChild.JAM).size());
        assertEquals(4, sb.getCurrentPeriod().getCurrentJam().getNumber());
    }
    
    @Test
    public void testPenaltiesMovedOnPeriodDelete() {
        Team team = sb.getTeam(Team.ID_1);
        Skater skater = new SkaterImpl(team, UUID.randomUUID().toString());
                
        fastForwardJams(2);
        
        Penalty penalty = (Penalty)skater.getOrCreate(NChild.PENALTY, "1");
        penalty.set(Penalty.Value.JAM, sb.getCurrentPeriod().getCurrentJam());
        penalty.set(Penalty.Value.CODE, "C");
        
        fastForwardPeriod();
        ic.setTime(0);
        fastForwardJams(2);
        
        Period p2 = sb.getCurrentPeriod();
        assertEquals(2, p2.getNumber());
        assertEquals(p2.getPrevious(), penalty.getJam().getParent());
        
        p2.getPrevious().delete();
        
        assertEquals(1, p2.getNumber());
        assertEquals(p2.getFirst(Period.NChild.JAM), penalty.getJam());
    }
    
    @Test
    public void testResetDoesntAffectSettings() {
        sb.getSettings().set("foo", "bar");
        sb.reset();
        assertEquals("bar", sb.getSettings().get("foo"));
    }
}
