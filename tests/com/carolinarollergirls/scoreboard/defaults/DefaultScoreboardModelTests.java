package com.carolinarollergirls.scoreboard.defaults;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.Queue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.jetty.JettyServletScoreBoardController;
import com.carolinarollergirls.scoreboard.model.ClockModel;
import com.carolinarollergirls.scoreboard.utils.ClockConversion;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;
import com.carolinarollergirls.scoreboard.view.Clock;
import com.carolinarollergirls.scoreboard.view.ScoreBoard;
import com.carolinarollergirls.scoreboard.view.Team;

public class DefaultScoreboardModelTests {

    private DefaultScoreBoardModel sbm;
    private ClockModel pc;
    private ClockModel jc;
    private ClockModel lc;
    private ClockModel tc;
    private ClockModel ic;
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
        ScoreBoardManager.setPropertyOverride(JettyServletScoreBoardController.class.getName() + ".html.dir", "html");
        sbm = new DefaultScoreBoardModel();
        pc = sbm.getClockModel(Clock.ID_PERIOD);
        jc = sbm.getClockModel(Clock.ID_JAM);
        lc = sbm.getClockModel(Clock.ID_LINEUP);
        tc = sbm.getClockModel(Clock.ID_TIMEOUT);
        ic = sbm.getClockModel(Clock.ID_INTERMISSION);
        collectedEvents = new LinkedList<ScoreBoardEvent>();
        sbm.addScoreBoardListener(batchCounter);
        //Clock Sync can cause clocks to be changed when started, breaking tests.
        sbm.getSettingsModel().set(Clock.SETTING_SYNC, "False");
        sbm.getSettingsModel().set(ScoreBoard.SETTING_CLOCK_AFTER_TIMEOUT, "Lineup");
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
        assertEquals(startLabel, sbm.getSettings().get(ScoreBoard.BUTTON_START));
        assertEquals(stopLabel, sbm.getSettings().get(ScoreBoard.BUTTON_STOP));
        assertEquals(timeoutLabel, sbm.getSettings().get(ScoreBoard.BUTTON_TIMEOUT));
        assertEquals(undoLabel, sbm.getSettings().get(ScoreBoard.BUTTON_UNDO));
    }

    private void checkLabels(String startLabel, String stopLabel, String timeoutLabel, String undoLabel, String replaceLabel) {
        checkLabels(startLabel, stopLabel, timeoutLabel, undoLabel);
        assertEquals(replaceLabel, sbm.getSettings().get(ScoreBoard.BUTTON_REPLACED));
    }

    @Test
    public void testSetInPeriod() {
        assertFalse(sbm.isInPeriod());
        sbm.addScoreBoardListener(new ConditionalScoreBoardListener(sbm, ScoreBoard.EVENT_IN_PERIOD, listener));

        sbm.setInPeriod(true);
        assertTrue(sbm.isInPeriod());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertTrue((Boolean)event.getValue());
        assertFalse((Boolean)event.getPreviousValue());

        //check idempotency
        sbm.setInPeriod(true);
        assertTrue(sbm.isInPeriod());
        assertEquals(0, collectedEvents.size());

        sbm.setInPeriod(false);
        assertFalse(sbm.isInPeriod());
    }

    @Test
    public void testSetInOvertime() {
        (sbm.getRulesetsModel()).set("Clock." + Clock.ID_LINEUP + ".Time", "30000");
        lc.setMaximumTime(999999999);

        assertFalse(lc.isCountDirectionDown());
        assertFalse(sbm.isInOvertime());
        sbm.addScoreBoardListener(new ConditionalScoreBoardListener(sbm, ScoreBoard.EVENT_IN_OVERTIME, listener));

        sbm.setInOvertime(true);
        assertTrue(sbm.isInOvertime());
        assertEquals(999999999, lc.getMaximumTime());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertTrue((Boolean)event.getValue());
        assertFalse((Boolean)event.getPreviousValue());

        //check idempotency
        sbm.setInOvertime(true);
        assertTrue(sbm.isInOvertime());
        assertEquals(0, collectedEvents.size());

        sbm.setInOvertime(true);
        assertTrue(sbm.isInOvertime());

        sbm.setInOvertime(false);
        assertFalse(sbm.isInOvertime());
        assertEquals(999999999, lc.getMaximumTime());

        //check that lineup clock maximum time is reset for countdown lineup clock
        sbm.setInOvertime(true);
        lc.setCountDirectionDown(true);
        sbm.setInOvertime(false);
        assertEquals(30000, lc.getMaximumTime());
    }

    @Test
    public void testSetOfficialScore() {
        assertFalse(sbm.isOfficialScore());
        sbm.addScoreBoardListener(new ConditionalScoreBoardListener(sbm, ScoreBoard.EVENT_OFFICIAL_SCORE, listener));

        sbm.setOfficialScore(true);
        assertTrue(sbm.isOfficialScore());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertTrue((Boolean)event.getValue());
        assertFalse((Boolean)event.getPreviousValue());

        //check idempotency
        sbm.setOfficialScore(true);
        assertTrue(sbm.isOfficialScore());
        assertEquals(1, collectedEvents.size());

        sbm.setOfficialScore(false);
        assertFalse(sbm.isOfficialScore());
    }

    @Test
    public void testSetOfficialReview() {
        assertFalse(sbm.isOfficialReview());
        sbm.addScoreBoardListener(new ConditionalScoreBoardListener(sbm, ScoreBoard.EVENT_OFFICIAL_REVIEW, listener));

        sbm.setOfficialReview(true);
        assertTrue(sbm.isOfficialReview());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertTrue((Boolean)event.getValue());
        assertFalse((Boolean)event.getPreviousValue());

        //check idempotency
        sbm.setOfficialReview(true);
        assertTrue(sbm.isOfficialReview());
        assertEquals(1, collectedEvents.size());

        sbm.setOfficialReview(false);
        assertFalse(sbm.isOfficialReview());
    }

    @Test
    public void testSetTimeoutOwner() {
        assertEquals("", sbm.getTimeoutOwner());
        sbm.addScoreBoardListener(new ConditionalScoreBoardListener(sbm, ScoreBoard.EVENT_TIMEOUT_OWNER, listener));

        sbm.setTimeoutOwner("testOwner");
        assertEquals("testOwner", sbm.getTimeoutOwner());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertEquals("testOwner", event.getValue());
        assertEquals("", event.getPreviousValue());

        sbm.setTimeoutOwner("");
        assertEquals("", sbm.getTimeoutOwner());
        assertEquals(1, collectedEvents.size());
        event = collectedEvents.poll();
        assertEquals("", event.getValue());
        assertEquals("testOwner", event.getPreviousValue());
    }

    @Test
    public void testStartOvertime_default() {
        (sbm.getRulesetsModel()).set("Clock." + Clock.ID_LINEUP + ".OvertimeTime", "60000");

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

        sbm.startOvertime();

        assertEquals(ScoreBoard.ACTION_OVERTIME, sbm.snapshot.getType());
        assertTrue(sbm.isInOvertime());
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

        sbm.startOvertime();

        assertEquals(ScoreBoard.ACTION_OVERTIME, sbm.snapshot.getType());
        assertTrue(sbm.isInOvertime());
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

        sbm.startOvertime();

        assertEquals(null, sbm.snapshot);
        assertFalse(sbm.isInOvertime());
    }

    @Test
    public void testStartOvertime_periodRunning() {
        pc.start();

        sbm.startOvertime();

        assertEquals(null, sbm.snapshot);
        assertFalse(sbm.isInOvertime());
    }

    @Test
    public void testStartOvertime_jamRunning() {
        pc.start();

        sbm.startOvertime();

        assertEquals(null, sbm.snapshot);
        assertFalse(sbm.isInOvertime());
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

        sbm.startJam();

        assertEquals(ScoreBoard.ACTION_START_JAM, sbm.snapshot.getType());
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
        sbm.setTimeoutOwner("2");
        sbm.setOfficialReview(true);
        tc.start();
        assertFalse(ic.isRunning());
        sbm.getTeamModel("2").setInTimeout(true);
        sbm.getTeamModel("2").setInOfficialReview(true);

        sbm.startJam();

        assertEquals(ScoreBoard.ACTION_START_JAM, sbm.snapshot.getType());
        assertTrue(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertTrue(jc.isRunning());
        assertTrue(jc.isTimeAtStart());
        assertEquals(18, jc.getNumber());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertEquals(3, tc.getNumber());
        assertEquals("", sbm.getTimeoutOwner());
        assertFalse(sbm.isOfficialReview());
        assertFalse(ic.isRunning());
        assertFalse(sbm.getTeamModel("2").inTimeout());
        assertFalse(sbm.getTeamModel("2").inOfficialReview());
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

        sbm.startJam();

        assertEquals(ScoreBoard.ACTION_START_JAM, sbm.snapshot.getType());
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

        sbm.startJam();

        assertEquals(ScoreBoard.ACTION_START_JAM, sbm.snapshot.getType());
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
        assertFalse(sbm.isInPeriod());

        sbm.startJam();

        assertEquals(ScoreBoard.ACTION_START_JAM, sbm.snapshot.getType());
        assertTrue(pc.isRunning());
        assertEquals(2, pc.getNumber());
        assertTrue(jc.isRunning());
        assertEquals(1, jc.getNumber());
        assertTrue(jc.isTimeAtStart());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        assertTrue(sbm.isInPeriod());
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
        assertFalse(sbm.isInPeriod());

        sbm.startJam();
        advance(1000);

        assertEquals(ScoreBoard.ACTION_START_JAM, sbm.snapshot.getType());
        assertFalse(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertTrue(jc.isRunning());
        assertEquals(22, jc.getNumber());
        assertEquals(1000, jc.getTimeElapsed());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        assertTrue(sbm.isInPeriod());
        checkLabels(ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_START_JAM);
    }

    @Test
    public void testStartJam_jamRunning() {
        jc.setTime(74000);
        jc.setNumber(9);
        jc.start();
        sbm.setLabels(ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT);
        sbm.setLabel(ScoreBoard.BUTTON_UNDO, ScoreBoard.ACTION_NONE);

        sbm.startJam();

        assertEquals(null, sbm.snapshot);
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
        sbm.getTeamModel("1").setStarPass(true);
        sbm.getTeamModel("2").setLeadJammer(Team.LEAD_NO_LEAD);

        sbm.stopJamTO();

        assertEquals(ScoreBoard.ACTION_STOP_JAM, sbm.snapshot.getType());
        assertTrue(pc.isRunning());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertTrue(lc.isTimeAtStart());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        assertFalse(sbm.getTeamModel("1").isStarPass());
        assertEquals(Team.LEAD_NO_LEAD, sbm.getTeamModel("2").getLeadJammer());
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
        sbm.setInPeriod(true);
        sbm.setOfficialScore(true);

        sbm.stopJamTO();

        assertEquals(ScoreBoard.ACTION_STOP_JAM, sbm.snapshot.getType());
        assertFalse(pc.isRunning());
        assertTrue(pc.isTimeAtEnd());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertTrue(ic.isRunning());
        assertEquals(2, ic.getNumber());
        long dur = ClockConversion.fromHumanReadable(sbm.getRulesets().get(ScoreBoard.RULE_INTERMISSION_DURATIONS).split(",")[1]);
        assertEquals(dur, ic.getMaximumTime());
        assertTrue(ic.isTimeAtStart());
        assertFalse(sbm.isInPeriod());
        assertFalse(sbm.isOfficialScore());
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
        sbm.setTimeoutOwner("O");
        sbm.setOfficialReview(true);

        sbm.stopJamTO();

        assertEquals(ScoreBoard.ACTION_STOP_TO, sbm.snapshot.getType());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertTrue(lc.isTimeAtStart());
        assertFalse(tc.isRunning());
        assertEquals(4, tc.getNumber());
        assertFalse(ic.isRunning());
        assertEquals("", sbm.getTimeoutOwner());
        assertFalse(sbm.isOfficialReview());
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

        sbm.stopJamTO();

        assertEquals(ScoreBoard.ACTION_STOP_TO, sbm.snapshot.getType());
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
        sbm.getSettingsModel().set(ScoreBoard.SETTING_CLOCK_AFTER_TIMEOUT, "Timeout");
        assertFalse(pc.isRunning());
        assertFalse(pc.isTimeAtEnd());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        tc.setTime(32000);
        tc.start();
        tc.setNumber(8);
        assertFalse(ic.isRunning());
        assertEquals("", sbm.getTimeoutOwner());
        assertFalse(sbm.isOfficialReview());

        sbm.stopJamTO();

        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertEquals(32000, tc.getTimeElapsed());
        assertEquals(8, tc.getNumber());
        assertFalse(ic.isRunning());
        assertEquals("", sbm.getTimeoutOwner());
        assertFalse(sbm.isOfficialReview());
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

        sbm.stopJamTO();

        assertEquals(ScoreBoard.ACTION_LINEUP, sbm.snapshot.getType());
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

        sbm.stopJamTO();

        assertEquals(ScoreBoard.ACTION_LINEUP, sbm.snapshot.getType());
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
        String prevUndoLabel = sbm.getSettings().get(ScoreBoard.BUTTON_UNDO);
        sbm.setLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_TIMEOUT);
        lc.setTime(14000);
        lc.setNumber(9);
        lc.start();

        sbm.stopJamTO();

        assertEquals(null, sbm.snapshot);
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

        sbm.timeout();

        assertEquals(ScoreBoard.ACTION_TIMEOUT, sbm.snapshot.getType());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertTrue(tc.isTimeAtStart());
        assertEquals(3, tc.getNumber());
        assertFalse(ic.isRunning());
        assertEquals("", sbm.getTimeoutOwner());
        assertFalse(sbm.isOfficialReview());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_STOP_TO, ScoreBoard.ACTION_RE_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_TIMEOUT);
    }

    @Test
    public void testTimeout_fromJam() {
        pc.start();
        jc.start();
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        sbm.timeout();

        assertEquals(ScoreBoard.ACTION_TIMEOUT, sbm.snapshot.getType());
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

        sbm.timeout();

        assertEquals(ScoreBoard.ACTION_TIMEOUT, sbm.snapshot.getType());
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

        sbm.timeout();

        assertEquals(ScoreBoard.ACTION_TIMEOUT, sbm.snapshot.getType());
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
        sbm.setTimeoutOwner(ScoreBoard.TIMEOUT_OWNER_NONE);

        sbm.timeout();

        assertEquals(ScoreBoard.ACTION_RE_TIMEOUT, sbm.snapshot.getType());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertTrue(tc.isTimeAtStart());
        assertEquals(8, tc.getNumber());
        assertFalse(ic.isRunning());
        assertEquals(ScoreBoard.TIMEOUT_OWNER_NONE, sbm.getTimeoutOwner());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_STOP_TO, ScoreBoard.ACTION_RE_TIMEOUT, ScoreBoard.UNDO_PREFIX + ScoreBoard.ACTION_RE_TIMEOUT);

        sbm.timeout();

        assertEquals("", sbm.getTimeoutOwner());
    }

    @Test
    public void testSetTimeoutType() {
        sbm.setTimeoutOwner("");

        sbm.setTimeoutType("2", false);
        sbm.getTeamModel("2").setTimeouts(2);

        assertEquals(ScoreBoard.ACTION_TIMEOUT, sbm.snapshot.getType());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertFalse(ic.isRunning());
        assertEquals("2", sbm.getTimeoutOwner());
        assertFalse(sbm.isOfficialReview());
        assertEquals(2, sbm.getTeam("2").getTimeouts());

        sbm.setTimeoutType("1", true);
        assertEquals("1", sbm.getTimeoutOwner());
        assertTrue(sbm.isOfficialReview());
        assertEquals(3, sbm.getTeam("2").getTimeouts());
    }

    @Test
    public void testClockUndo_undo() {
        pc.start();
        jc.start();
        sbm.setInPeriod(true);
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        assertEquals("", sbm.getTimeoutOwner());
        assertFalse(sbm.isOfficialReview());
        assertFalse(sbm.isInOvertime());
        assertTrue(sbm.isInPeriod());
        sbm.setLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT);
        sbm.setLabel(ScoreBoard.BUTTON_UNDO, ScoreBoard.ACTION_NONE);

        sbm.createSnapshot("TEST");
        assertEquals("TEST", sbm.snapshot.getType());
        assertEquals(ScoreBoard.UNDO_PREFIX + "TEST", sbm.getSettings().get(ScoreBoard.BUTTON_UNDO));

        pc.stop();
        jc.stop();
        lc.start();
        tc.start();
        ic.start();
        sbm.setTimeoutOwner("TestOwner");
        sbm.setOfficialReview(true);
        sbm.setInOvertime(true);
        sbm.setInPeriod(false);
        sbm.setLabels(ScoreBoard.ACTION_LINEUP, ScoreBoard.ACTION_RE_TIMEOUT, ScoreBoard.ACTION_OVERTIME);
        advance(2000);

        sbm.clockUndo(false);
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
        assertEquals("", sbm.getTimeoutOwner());
        assertFalse(sbm.isOfficialReview());
        assertFalse(sbm.isInOvertime());
        assertTrue(sbm.isInPeriod());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT, ScoreBoard.ACTION_NONE);
    }

    @Test
    public void testClockUndo_replace() {
        pc.elapseTime(600000);
        pc.start();
        jc.start();
        sbm.setInPeriod(true);
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        sbm.setLabels(ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT);

        sbm.timeout();
        assertEquals(ScoreBoard.ACTION_TIMEOUT, sbm.snapshot.getType());
        advance(2000);

        sbm.clockUndo(true);
        checkLabels(ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_STOP_JAM, ScoreBoard.ACTION_TIMEOUT,
                    ScoreBoard.ACTION_NO_REPLACE, ScoreBoard.ACTION_TIMEOUT);
        sbm.stopJamTO();

        advance(ScoreBoardClock.getInstance().getLastRewind());
        assertTrue(pc.isRunning());
        assertEquals(602000, pc.getTimeElapsed());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertEquals(2000, lc.getTimeElapsed());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        sbm.clockUndo(true);
        sbm.clockUndo(true);
        advance(ScoreBoardClock.getInstance().getLastRewind());

        assertTrue(pc.isRunning());
        assertEquals(602000, pc.getTimeElapsed());
        assertTrue(jc.isRunning());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        sbm.clockUndo(true);
        sbm.timeout();
        advance(ScoreBoardClock.getInstance().getLastRewind());

        assertFalse(pc.isRunning());
        assertEquals(600000, pc.getTimeElapsed());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertEquals(2000, tc.getTimeElapsed());
        assertFalse(ic.isRunning());

        sbm.stopJamTO();
        advance(5000);
        sbm.timeout();
        advance(3000);

        sbm.clockUndo(true);
        sbm.startJam();
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
        assertEquals(3, sbm.getTeam("1").getTimeouts());
        assertEquals(1, sbm.getTeam("2").getOfficialReviews());
        pc.start();
        lc.start();
        sbm.timeout();
        sbm.getTeamModel("1").timeout();
        assertEquals(2, sbm.getTeam("1").getTimeouts());
        sbm.clockUndo(false);
        assertEquals(3, sbm.getTeam("1").getTimeouts());
        sbm.getTeamModel("2").officialReview();
        assertEquals(0, sbm.getTeam("2").getOfficialReviews());
        sbm.clockUndo(false);
        assertEquals(1, sbm.getTeam("2").getOfficialReviews());
    }

    @Test
    public void testPeriodClockEnd_duringLineup() {
        sbm.getRulesetsModel().set(ScoreBoard.RULE_INTERMISSION_DURATIONS, "5:00,15:00,5:00,60:00");
        String prevUndoLabel = sbm.getSettings().get(ScoreBoard.BUTTON_UNDO);

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
        long dur = ClockConversion.fromHumanReadable(sbm.getRulesets().get(ScoreBoard.RULE_INTERMISSION_DURATIONS).split(",")[1]);
        assertEquals(dur, ic.getTimeRemaining());
        checkLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_LINEUP, ScoreBoard.ACTION_TIMEOUT, prevUndoLabel);
    }

    @Test
    public void testPeriodClockEnd_periodEndInhibitedByRuleset() {
        sbm.getRulesetsModel().set(ScoreBoard.RULE_PERIOD_END_BETWEEN_JAMS, "false");
        String prevUndoLabel = sbm.getSettings().get(ScoreBoard.BUTTON_UNDO);
        sbm.setLabels(ScoreBoard.ACTION_START_JAM, ScoreBoard.ACTION_NONE, ScoreBoard.ACTION_TIMEOUT);

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
        String prevStartLabel = sbm.getSettings().get(ScoreBoard.BUTTON_START);
        String prevStopLabel = sbm.getSettings().get(ScoreBoard.BUTTON_STOP);
        String prevTimeoutLabel = sbm.getSettings().get(ScoreBoard.BUTTON_TIMEOUT);
        String prevUndoLabel = sbm.getSettings().get(ScoreBoard.BUTTON_UNDO);
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
        String prevUndoLabel = sbm.getSettings().get(ScoreBoard.BUTTON_UNDO);
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
        sbm.getRulesetsModel().set(ScoreBoard.RULE_AUTO_END_JAM, "false");
        String prevStartLabel = sbm.getSettings().get(ScoreBoard.BUTTON_START);
        String prevStopLabel = sbm.getSettings().get(ScoreBoard.BUTTON_STOP);
        String prevTimeoutLabel = sbm.getSettings().get(ScoreBoard.BUTTON_TIMEOUT);
        String prevUndoLabel = sbm.getSettings().get(ScoreBoard.BUTTON_UNDO);

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
        String prevUndoLabel = sbm.getSettings().get(ScoreBoard.BUTTON_UNDO);
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
        sbm.getRulesetsModel().set(ScoreBoard.RULE_JAM_NUMBER_PER_PERIOD, "false");

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
        String prevStartLabel = sbm.getSettings().get(ScoreBoard.BUTTON_START);
        String prevStopLabel = sbm.getSettings().get(ScoreBoard.BUTTON_STOP);
        String prevTimeoutLabel = sbm.getSettings().get(ScoreBoard.BUTTON_TIMEOUT);
        String prevUndoLabel = sbm.getSettings().get(ScoreBoard.BUTTON_UNDO);
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
        sbm.timeout();
        advance(20000);
        sbm.stopJamTO();
        assertFalse(pc.isRunning());

        //jam ended after 30s mark, official timeout
        sbm.startJam();
        sbm.stopJamTO();
        assertEquals(25000, pc.getTime());
        assertTrue(pc.isRunning());
        advance(1000);
        sbm.timeout();
        advance(35000);
        sbm.stopJamTO();
        assertTrue(pc.isRunning());

        //follow up with team timeout
        advance(2000);
        sbm.setTimeoutType("1", false);
        advance(60000);
        sbm.stopJamTO();
        assertFalse(pc.isRunning());
        assertEquals(22000, pc.getTimeRemaining());
    }

    @Test
    public void testTimeoutsThatDontAlwaysStopPc() {
        sbm.getRulesetsModel().set(ScoreBoard.RULE_STOP_PC_ON_TO, "false");
        sbm.getRulesetsModel().set(ScoreBoard.RULE_STOP_PC_ON_OTO, "false");
        sbm.getRulesetsModel().set(ScoreBoard.RULE_STOP_PC_ON_TTO, "true");
        sbm.getRulesetsModel().set(ScoreBoard.RULE_STOP_PC_ON_OR, "true");
        sbm.getRulesetsModel().set(ScoreBoard.RULE_STOP_PC_AFTER_TO_DURATION, "120000");

        assertTrue(pc.isCountDirectionDown());
        pc.setTime(1200000);
        pc.start();
        assertFalse(jc.isRunning());
        lc.start();
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        sbm.timeout();

        assertTrue(pc.isRunning());
        assertEquals(600000, pc.getTimeElapsed());
        assertTrue(tc.isRunning());
        assertEquals(0, tc.getTimeElapsed());

        advance(2000);
        assertEquals(602000, pc.getTimeElapsed());

        sbm.setTimeoutType("1", false);

        assertFalse(pc.isRunning());
        assertEquals(600000, pc.getTimeElapsed());
        assertTrue(tc.isRunning());

        advance(3000);
        assertEquals(600000, pc.getTimeElapsed());

        sbm.setTimeoutType("O", false);

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
        sbm.getRulesetsModel().set(ScoreBoard.RULE_AUTO_START, "true");
        sbm.getRulesetsModel().set(ScoreBoard.RULE_AUTO_START_JAM, "true");

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
        sbm.getRulesetsModel().set(ScoreBoard.RULE_AUTO_START, "true");
        sbm.getRulesetsModel().set(ScoreBoard.RULE_AUTO_START_JAM, "false");
        sbm.getRulesetsModel().set(ScoreBoard.RULE_AUTO_START_BUFFER, "0");
        sbm.getRulesetsModel().set(ScoreBoard.RULE_AUTO_END_TTO, "true");
        sbm.getRulesetsModel().set(ScoreBoard.RULE_TTO_DURATION, "25000");

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

        sbm.setTimeoutType("2", true);

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
        sbm.getSettingsModel().set("foo", "bar");
        sbm.reset();
        assertEquals("bar", sbm.getSettings().get("foo"));
    }
}
