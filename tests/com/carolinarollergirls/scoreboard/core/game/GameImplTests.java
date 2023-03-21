package com.carolinarollergirls.scoreboard.core.game;

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

import com.carolinarollergirls.scoreboard.core.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.core.game.GameImpl.Button;
import com.carolinarollergirls.scoreboard.core.game.GameImpl.GameSnapshot;
import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame;
import com.carolinarollergirls.scoreboard.core.interfaces.FloorPosition;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Jam;
import com.carolinarollergirls.scoreboard.core.interfaces.Penalty;
import com.carolinarollergirls.scoreboard.core.interfaces.Period;
import com.carolinarollergirls.scoreboard.core.interfaces.Role;
import com.carolinarollergirls.scoreboard.core.interfaces.Rulesets;
import com.carolinarollergirls.scoreboard.core.interfaces.Rulesets.Ruleset;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.Skater;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.core.interfaces.TeamJam;
import com.carolinarollergirls.scoreboard.core.interfaces.Timeout;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Flag;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ClockConversion;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public class GameImplTests {

    private ScoreBoardImpl sb;
    private GameImpl g;
    private Clock pc;
    private Clock jc;
    private Clock lc;
    private Clock tc;
    private Clock ic;
    private Queue<ScoreBoardEvent<?>> collectedEvents;
    private ScoreBoardListener listener = new ScoreBoardListener() {
        @Override
        public void scoreBoardChange(ScoreBoardEvent<?> event) {
            synchronized (collectedEvents) { collectedEvents.add(event); }
        }
    };

    private final String ID_PREFIX = "00000000-0000-0000-0000-000000000";

    private int batchLevel;
    private ScoreBoardListener batchCounter = new ScoreBoardListener() {
        @Override
        public void scoreBoardChange(ScoreBoardEvent<?> event) {
            synchronized (batchCounter) {
                if (event.getProperty() == ScoreBoardEventProviderImpl.BATCH_START) {
                    batchLevel++;
                } else if (event.getProperty() == ScoreBoardEventProviderImpl.BATCH_END) {
                    batchLevel--;
                }
            }
        }
    };

    @Before
    public void setUp() throws Exception {
        ScoreBoardClock.getInstance().stop();
        sb = new ScoreBoardImpl();
        sb.postAutosaveUpdate();
        g = (GameImpl) sb.getCurrentGame().get(CurrentGame.GAME);
        pc = g.getClock(Clock.ID_PERIOD);
        jc = g.getClock(Clock.ID_JAM);
        lc = g.getClock(Clock.ID_LINEUP);
        tc = g.getClock(Clock.ID_TIMEOUT);
        ic = g.getClock(Clock.ID_INTERMISSION);
        assertTrue(pc.isTimeAtStart());
        assertTrue(jc.isTimeAtStart());
        assertTrue(lc.isTimeAtStart());
        assertTrue(tc.isTimeAtStart());
        assertTrue(ic.isTimeAtStart());
        collectedEvents = new LinkedList<>();
        sb.addScoreBoardListener(batchCounter);
        // Clock Sync can cause clocks to be changed when started, breaking tests.
        sb.getSettings().set(Clock.SETTING_SYNC, "False");
        sb.getSettings().set(ScoreBoard.SETTING_CLOCK_AFTER_TIMEOUT, "Lineup");
    }

    @After
    public void tearDown() throws Exception {
        ScoreBoardClock.getInstance().start(false);
        // Check all started batches were ended.
        assertEquals(0, batchLevel);
    }

    private void advance(long time_ms) { ScoreBoardClock.getInstance().advance(time_ms); }

    private void fastForwardJams(int number) {
        for (int i = 0; i < number; i++) {
            g.startJam();
            g.stopJamTO();
        }
    }

    private void fastForwardPeriod() {
        ic.setTime(0);
        g.startJam();
        pc.setTime(0);
        g.stopJamTO();
    }

    private void checkLabels(String startLabel, String stopLabel, String timeoutLabel, String undoLabel) {
        assertEquals(startLabel, g.getLabel(Button.START));
        assertEquals(stopLabel, g.getLabel(Button.STOP));
        assertEquals(timeoutLabel, g.getLabel(Button.TIMEOUT));
        assertEquals(undoLabel, g.getLabel(Button.UNDO));
    }

    private void checkLabels(String startLabel, String stopLabel, String timeoutLabel, String undoLabel,
                             String replaceLabel) {
        checkLabels(startLabel, stopLabel, timeoutLabel, undoLabel);
        assertEquals(replaceLabel, g.getLabel(Button.REPLACED));
    }

    @Test
    public void testSetInPeriod() {
        assertFalse(g.isInPeriod());
        g.addScoreBoardListener(new ConditionalScoreBoardListener<>(g, Game.IN_PERIOD, listener));

        g.setInPeriod(true);
        assertTrue(g.isInPeriod());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent<?> event = collectedEvents.poll();
        assertTrue((Boolean) event.getValue());
        assertFalse((Boolean) event.getPreviousValue());

        // check idempotency
        g.setInPeriod(true);
        assertTrue(g.isInPeriod());
        assertEquals(0, collectedEvents.size());

        g.setInPeriod(false);
        assertFalse(g.isInPeriod());
    }

    @Test
    public void testSetInOvertime() {
        g.set(Rule.LINEUP_DURATION, "30000");
        lc.setMaximumTime(999999999);

        assertFalse(lc.isCountDirectionDown());
        assertFalse(g.isInOvertime());
        g.addScoreBoardListener(new ConditionalScoreBoardListener<>(g, Game.IN_OVERTIME, listener));

        g.setInOvertime(true);
        assertTrue(g.isInOvertime());
        assertEquals(999999999, lc.getMaximumTime());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent<?> event = collectedEvents.poll();
        assertTrue((Boolean) event.getValue());
        assertFalse((Boolean) event.getPreviousValue());

        // check idempotency
        g.setInOvertime(true);
        assertTrue(g.isInOvertime());
        assertEquals(0, collectedEvents.size());

        g.setInOvertime(true);
        assertTrue(g.isInOvertime());

        g.setInOvertime(false);
        assertFalse(g.isInOvertime());
        assertEquals(999999999, lc.getMaximumTime());

        // check that lineup clock maximum time is reset for countdown lineup clock
        g.setInOvertime(true);
        lc.setCountDirectionDown(true);
        g.setInOvertime(false);
        assertEquals(30000, lc.getMaximumTime());
    }

    @Test
    public void testSetOfficialScore() {
        assertFalse(g.isOfficialScore());
        g.addScoreBoardListener(new ConditionalScoreBoardListener<>(g, Game.OFFICIAL_SCORE, listener));
        fastForwardPeriod();
        fastForwardPeriod();

        g.setOfficialScore(true);
        assertTrue(g.isOfficialScore());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent<?> event = collectedEvents.poll();
        assertTrue((Boolean) event.getValue());
        assertFalse((Boolean) event.getPreviousValue());

        // check idempotency
        g.setOfficialScore(true);
        assertTrue(g.isOfficialScore());
        assertEquals(0, collectedEvents.size());

        g.setOfficialScore(false);
        assertFalse(g.isOfficialScore());
    }

    @Test
    public void testSetOfficialReview() {
        g.timeout();
        assertFalse(g.isOfficialReview());
        g.addScoreBoardListener(new ConditionalScoreBoardListener<>(g, Game.OFFICIAL_REVIEW, listener));

        g.setOfficialReview(true);
        assertTrue(g.isOfficialReview());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent<?> event = collectedEvents.poll();
        assertTrue((Boolean) event.getValue());
        assertFalse((Boolean) event.getPreviousValue());

        // check idempotency
        g.setOfficialReview(true);
        assertTrue(g.isOfficialReview());
        assertEquals(0, collectedEvents.size());

        g.setOfficialReview(false);
        assertFalse(g.isOfficialReview());
    }

    @Test
    public void testSetTimeoutOwner() {
        g.timeout();
        assertEquals(Timeout.Owners.NONE, g.getTimeoutOwner());
        g.addScoreBoardListener(new ConditionalScoreBoardListener<>(g, Game.TIMEOUT_OWNER, listener));

        g.setTimeoutOwner(Timeout.Owners.OTO);
        assertEquals(Timeout.Owners.OTO, g.getTimeoutOwner());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent<?> event = collectedEvents.poll();
        assertEquals(Timeout.Owners.OTO, event.getValue());
        assertEquals(Timeout.Owners.NONE, event.getPreviousValue());

        g.setTimeoutOwner(Timeout.Owners.NONE);
        assertEquals(Timeout.Owners.NONE, g.getTimeoutOwner());
        assertEquals(1, collectedEvents.size());
        event = collectedEvents.poll();
        assertEquals(Timeout.Owners.NONE, event.getValue());
        assertEquals(Timeout.Owners.OTO, event.getPreviousValue());
    }

    @Test
    public void testStartOvertime_default() {
        g.set(Rule.OVERTIME_LINEUP_DURATION, "60000");

        fastForwardPeriod();
        fastForwardPeriod();
        assertFalse(pc.isRunning());
        assertTrue(pc.isTimeAtEnd());
        assertEquals(g.getInt(Rule.NUMBER_PERIODS), pc.getNumber());
        assertFalse(jc.isRunning());
        jc.setTime(0);
        assertTrue(jc.isTimeAtEnd());
        assertFalse(lc.isRunning());
        lc.setMaximumTime(30000);
        assertFalse(tc.isRunning());
        assertTrue(ic.isRunning());

        g.startOvertime();

        assertEquals(Game.ACTION_OVERTIME, g.snapshot.getType());
        assertTrue(g.isInOvertime());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertEquals(60000, lc.getMaximumTime());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_NONE, Game.ACTION_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_OVERTIME);
    }

    @Test
    public void testStartOvertime_fromTimeout() {
        fastForwardPeriod();
        fastForwardPeriod();
        g.timeout();
        assertFalse(pc.isRunning());
        assertTrue(pc.isTimeAtEnd());
        assertEquals(g.getInt(Rule.NUMBER_PERIODS), pc.getNumber());
        assertFalse(jc.isRunning());
        jc.setTime(0);
        assertTrue(jc.isTimeAtEnd());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        tc.setNumber(6);
        assertFalse(ic.isRunning());

        g.startOvertime();

        assertEquals(Game.ACTION_OVERTIME, g.snapshot.getType());
        assertTrue(g.isInOvertime());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertFalse(tc.isRunning());
        assertEquals(6, tc.getNumber());
        assertFalse(ic.isRunning());
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_NONE, Game.ACTION_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_OVERTIME);
    }

    @Test
    public void testStartOvertime_notLastPeriod() {
        assertNotEquals(pc.getNumber(), g.getInt(Rule.NUMBER_PERIODS));

        g.startOvertime();

        assertEquals(null, g.snapshot);
        assertFalse(g.isInOvertime());
    }

    @Test
    public void testStartOvertime_periodRunning() {
        fastForwardPeriod();
        ic.setTime(0);
        g.startJam();
        assertEquals(g.getInt(Rule.NUMBER_PERIODS), pc.getNumber());
        assertTrue(pc.isRunning());
        GameSnapshot saved = g.snapshot;

        g.startOvertime();

        assertEquals(saved, g.snapshot);
        assertFalse(g.isInOvertime());
    }

    @Test
    public void testStartOvertime_jamRunning() {
        fastForwardPeriod();
        ic.setTime(0);
        g.startJam();
        pc.setTime(0);
        assertEquals(g.getInt(Rule.NUMBER_PERIODS), pc.getNumber());
        assertFalse(pc.isRunning());
        assertTrue(jc.isRunning());
        GameSnapshot saved = g.snapshot;

        g.startOvertime();

        assertEquals(saved, g.snapshot);
        assertFalse(g.isInOvertime());
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

        g.startJam();

        assertEquals(Game.ACTION_START_JAM, g.snapshot.getType());
        assertTrue(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertTrue(jc.isRunning());
        assertTrue(jc.isTimeAtStart());
        assertEquals(6, jc.getNumber());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        checkLabels(Game.ACTION_NONE, Game.ACTION_STOP_JAM, Game.ACTION_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_START_JAM);
    }

    @Test
    public void testStartJam_injuryContiunuation() {
        fastForwardJams(6);
        assertTrue(pc.isRunning());
        assertEquals(1, pc.getNumber());

        g.startJam();
        advance(35000);
        TeamJam tj1 = g.getCurrentPeriod().getCurrentJam().getTeamJam(Team.ID_1);
        tj1.set(TeamJam.LEAD, true);
        tj1.addScoringTrip();
        g.timeout();
        tj1.set(TeamJam.INJURY, true);

        g.set(Game.INJURY_CONTINUATION_UPCOMING, true);
        g.startJam();

        assertEquals(Game.ACTION_START_JAM, g.snapshot.getType());
        assertTrue(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertTrue(jc.isRunning());
        assertTrue(jc.isTimeAtStart());
        assertEquals(120000 - 35000, jc.getMaximumTime());
        assertEquals(8, jc.getNumber());
        assertTrue(tj1.getNext().isLead());
        assertFalse(tj1.getOtherTeam().getNext().isLead());
        assertEquals(3, tj1.getCurrentScoringTrip().getNumber());
        assertEquals(4, tj1.getNext().getCurrentScoringTrip().getNumber());
        assertEquals(1, tj1.getOtherTeam().getNext().getCurrentScoringTrip().getNumber());

        g.stopJamTO();
        advance(30000);
        g.startJam();
        assertEquals(120000, jc.getMaximumTime());
    }

    @Test
    public void testStartJam_suddenScoring() {
        g.set(Game.IN_SUDDEN_SCORING, true);
        g.startJam();

        assertTrue(jc.isRunning());
        assertEquals(60000, jc.getMaximumTime());
    }

    @Test
    public void testStartJam_fromTimeout() {
        fastForwardJams(17);
        g.timeout();
        assertFalse(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertFalse(jc.isRunning());
        jc.setTime(100000);
        jc.setNumber(17);
        assertFalse(lc.isRunning());
        tc.setNumber(3);
        g.setTimeoutType(g.getTeam(Team.ID_2), true);
        g.setOfficialReview(true);
        assertTrue(tc.isRunning());
        assertFalse(ic.isRunning());
        assertFalse(g.getTeam(Team.ID_2).inTimeout());
        assertTrue(g.getTeam(Team.ID_2).inOfficialReview());

        g.startJam();

        assertEquals(Game.ACTION_START_JAM, g.snapshot.getType());
        assertTrue(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertTrue(jc.isRunning());
        assertTrue(jc.isTimeAtStart());
        assertEquals(18, jc.getNumber());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertEquals(3, tc.getNumber());
        assertEquals(Timeout.Owners.NONE, g.getTimeoutOwner());
        assertFalse(g.isOfficialReview());
        assertFalse(ic.isRunning());
        assertFalse(g.getTeam(Team.ID_2).inTimeout());
        assertFalse(g.getTeam(Team.ID_2).inOfficialReview());
        checkLabels(Game.ACTION_NONE, Game.ACTION_STOP_JAM, Game.ACTION_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_START_JAM);
    }

    @Test
    public void testStartJam_fromLineupAfterTimeout() {
        fastForwardJams(22);
        g.timeout();
        g.stopJamTO();
        assertFalse(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertFalse(jc.isRunning());
        jc.setTime(45000);
        assertEquals(22, jc.getNumber());
        assertTrue(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        g.startJam();

        assertEquals(Game.ACTION_START_JAM, g.snapshot.getType());
        assertTrue(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertTrue(jc.isRunning());
        assertTrue(jc.isTimeAtStart());
        assertEquals(23, jc.getNumber());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        checkLabels(Game.ACTION_NONE, Game.ACTION_STOP_JAM, Game.ACTION_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_START_JAM);
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

        g.startJam();

        assertEquals(Game.ACTION_START_JAM, g.snapshot.getType());
        assertTrue(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertTrue(jc.isRunning());
        assertEquals(1, jc.getNumber());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        checkLabels(Game.ACTION_NONE, Game.ACTION_STOP_JAM, Game.ACTION_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_START_JAM);
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
        assertFalse(g.isInPeriod());
        assertEquals(2, g.numberOf(Game.PERIOD));
        assertEquals(1, g.getCurrentPeriodNumber());

        g.startJam();

        assertEquals(Game.ACTION_START_JAM, g.snapshot.getType());
        assertTrue(pc.isRunning());
        assertEquals(2, pc.getNumber());
        assertTrue(jc.isRunning());
        assertEquals(1, jc.getNumber());
        assertTrue(jc.isTimeAtStart());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        assertTrue(g.isInPeriod());
        assertEquals(3, g.numberOf(Game.PERIOD));
        assertEquals(2, g.getCurrentPeriodNumber());
        checkLabels(Game.ACTION_NONE, Game.ACTION_STOP_JAM, Game.ACTION_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_START_JAM);
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
        assertFalse(g.isInPeriod());

        g.startJam();
        advance(1000);

        assertEquals(Game.ACTION_START_JAM, g.snapshot.getType());
        assertFalse(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertTrue(jc.isRunning());
        assertEquals(22, jc.getNumber());
        assertEquals(1000, jc.getTimeElapsed());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        assertTrue(g.isInPeriod());
        checkLabels(Game.ACTION_NONE, Game.ACTION_STOP_JAM, Game.ACTION_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_START_JAM);
    }

    @Test
    public void testStartJam_jamRunning() {
        fastForwardJams(8);
        g.startJam();
        jc.setTime(74000);
        assertEquals(9, jc.getNumber());
        assertTrue(jc.isRunning());
        g.setLabels(Game.ACTION_NONE, Game.ACTION_STOP_JAM, Game.ACTION_TIMEOUT);
        g.setLabel(Button.UNDO, Game.ACTION_NONE);
        GameSnapshot saved = g.snapshot;

        g.startJam();

        assertEquals(saved, g.snapshot);
        assertTrue(jc.isRunning());
        assertEquals(9, jc.getNumber());
        assertEquals(74000, jc.getTime());
        checkLabels(Game.ACTION_NONE, Game.ACTION_STOP_JAM, Game.ACTION_TIMEOUT, Game.ACTION_NONE);
    }

    @Test
    public void testStopJam_duringPeriod() {
        g.startJam();
        assertTrue(pc.isRunning());
        assertTrue(jc.isRunning());
        assertFalse(lc.isRunning());
        lc.setTime(50000);
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        g.getTeam(Team.ID_1).set(Team.STAR_PASS, true);
        g.getTeam(Team.ID_2).set(Team.LEAD, true);

        g.stopJamTO();

        assertEquals(Game.ACTION_STOP_JAM, g.snapshot.getType());
        assertTrue(pc.isRunning());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertTrue(lc.isTimeAtStart());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        assertTrue(g.getTeam(Team.ID_1).isStarPass());
        assertTrue(g.getTeam(Team.ID_2).isLead());
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_NONE, Game.ACTION_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_STOP_JAM);
    }

    @Test
    public void testStopJam_endOfPeriod() {
        fastForwardPeriod();
        ic.setTime(0);
        g.startJam();
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
        g.setInPeriod(true);
        g.setOfficialScore(true);

        g.stopJamTO();

        assertEquals(Game.ACTION_STOP_JAM, g.snapshot.getType());
        assertFalse(pc.isRunning());
        assertTrue(pc.isTimeAtEnd());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertTrue(ic.isRunning());
        assertEquals(2, ic.getNumber());
        long dur = ClockConversion.fromHumanReadable(g.get(Rule.INTERMISSION_DURATIONS).split(",")[1]);
        assertEquals(dur, ic.getMaximumTime());
        assertTrue(ic.isTimeAtStart());
        assertFalse(g.isInPeriod());
        assertFalse(g.isOfficialScore());
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_LINEUP, Game.ACTION_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_STOP_JAM);
    }

    @Test
    public void testStopJam_endTimeoutDuringPeriod() {
        g.startJam();
        g.timeout();
        assertFalse(pc.isRunning());
        assertFalse(pc.isTimeAtEnd());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        lc.setTime(37000);
        assertTrue(tc.isRunning());
        tc.setNumber(4);
        assertFalse(ic.isRunning());
        g.setTimeoutOwner(Timeout.Owners.OTO);
        g.setOfficialReview(true);

        g.stopJamTO();

        assertEquals(Game.ACTION_STOP_TO, g.snapshot.getType());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertTrue(lc.isTimeAtStart());
        assertFalse(tc.isRunning());
        assertEquals(4, tc.getNumber());
        assertFalse(ic.isRunning());
        assertEquals(Timeout.Owners.NONE, g.getTimeoutOwner());
        assertFalse(g.isOfficialReview());
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_NONE, Game.ACTION_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_STOP_TO);
    }

    @Test
    public void testStopJam_endTimeoutAfterPeriod() {
        g.startJam();
        g.timeout();
        assertFalse(pc.isRunning());
        pc.setTime(0);
        assertTrue(pc.isTimeAtEnd());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        tc.setNumber(3);
        assertFalse(ic.isRunning());

        g.stopJamTO();

        assertEquals(Game.ACTION_STOP_TO, g.snapshot.getType());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertEquals(3, tc.getNumber());
        assertTrue(ic.isRunning());
        assertTrue(ic.isTimeAtStart());
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_LINEUP, Game.ACTION_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_STOP_TO);
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
        assertEquals(Timeout.Owners.NONE, g.getTimeoutOwner());
        assertFalse(g.isOfficialReview());

        g.stopJamTO();

        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertEquals(32000, tc.getTimeElapsed());
        assertEquals(8, tc.getNumber());
        assertFalse(ic.isRunning());
        assertEquals(Timeout.Owners.NONE, g.getTimeoutOwner());
        assertFalse(g.isOfficialReview());
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_NONE, Game.ACTION_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_STOP_TO);
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

        g.stopJamTO();

        // less than 1s of intermission has passed, stopJamTO should be ignored
        assertFalse(lc.isRunning());
        assertTrue(ic.isRunning());

        advance(20000);
        g.stopJamTO();

        assertEquals(Game.ACTION_LINEUP, g.snapshot.getType());
        assertFalse(pc.isRunning());
        assertEquals(1, pc.getNumber());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertTrue(lc.isTimeAtStart());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_NONE, Game.ACTION_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_LINEUP);
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

        g.stopJamTO();

        assertEquals(Game.ACTION_LINEUP, g.snapshot.getType());
        assertFalse(pc.isRunning());
        assertEquals(2, pc.getNumber());
        assertTrue(pc.isTimeAtStart());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertTrue(lc.isTimeAtStart());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_NONE, Game.ACTION_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_LINEUP);
    }

    @Test
    public void testStopJam_lineupRunning() {
        String prevUndoLabel = g.getLabel(Button.UNDO);
        g.setLabels(Game.ACTION_START_JAM, Game.ACTION_NONE, Game.ACTION_TIMEOUT);
        lc.setTime(14000);
        lc.setNumber(9);
        lc.start();

        g.stopJamTO();

        assertEquals(null, g.snapshot);
        assertTrue(lc.isRunning());
        assertEquals(9, lc.getNumber());
        assertEquals(14000, lc.getTime());
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_NONE, Game.ACTION_TIMEOUT, prevUndoLabel);
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

        g.timeout();

        assertEquals(Game.ACTION_TIMEOUT, g.snapshot.getType());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertTrue(tc.isTimeAtStart());
        assertEquals(3, tc.getNumber());
        assertFalse(ic.isRunning());
        assertEquals(Timeout.Owners.NONE, g.getTimeoutOwner());
        assertFalse(g.isOfficialReview());
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_STOP_TO, Game.ACTION_RE_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_TIMEOUT);
    }

    @Test
    public void testTimeout_fromJam() {
        g.startJam();
        assertTrue(pc.isRunning());
        assertTrue(jc.isRunning());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        g.timeout();

        assertEquals(Game.ACTION_TIMEOUT, g.snapshot.getType());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertTrue(tc.isTimeAtStart());
        assertFalse(ic.isRunning());
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_STOP_TO, Game.ACTION_RE_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_TIMEOUT);
    }

    @Test
    public void testTimeout_fromIntermission() {
        fastForwardPeriod();
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertTrue(ic.isRunning());

        g.timeout();

        assertEquals(Game.ACTION_TIMEOUT, g.snapshot.getType());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertTrue(tc.isTimeAtStart());
        assertFalse(ic.isRunning());
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_STOP_TO, Game.ACTION_RE_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_TIMEOUT);
    }

    @Test
    public void testTimeout_AfterGame() {
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        g.timeout();

        assertEquals(Game.ACTION_TIMEOUT, g.snapshot.getType());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertTrue(tc.isTimeAtStart());
        assertFalse(ic.isRunning());
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_STOP_TO, Game.ACTION_RE_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_TIMEOUT);
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
        g.setTimeoutOwner(Timeout.Owners.OTO);

        g.timeout();

        assertEquals(Game.ACTION_RE_TIMEOUT, g.snapshot.getType());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertTrue(tc.isTimeAtStart());
        assertEquals(8, tc.getNumber());
        assertFalse(ic.isRunning());
        assertEquals(Timeout.Owners.NONE, g.getTimeoutOwner());
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_STOP_TO, Game.ACTION_RE_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_RE_TIMEOUT);

        g.setTimeoutOwner(Timeout.Owners.OTO);
        g.timeout();

        // timeout was entered with less than 1s on the TO clock
        // and should be ignored
        assertEquals(8, tc.getNumber());
        assertEquals(Timeout.Owners.OTO, g.getTimeoutOwner());

        advance(1500);
        g.timeout();

        assertEquals(9, tc.getNumber());
        assertEquals(Timeout.Owners.NONE, g.getTimeoutOwner());
    }

    @Test
    public void testSetTimeoutType() {
        g.setTimeoutType(g.getTeam(Team.ID_2), false);

        assertEquals(Game.ACTION_TIMEOUT, g.snapshot.getType());
        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertFalse(ic.isRunning());
        assertEquals(g.getTeam(Team.ID_2), g.getTimeoutOwner());
        assertFalse(g.isOfficialReview());
        assertEquals(2, g.getTeam(Team.ID_2).getTimeouts());

        g.setTimeoutType(g.getTeam(Team.ID_1), true);
        assertEquals(g.getTeam(Team.ID_1), g.getTimeoutOwner());
        assertTrue(g.isOfficialReview());
        assertEquals(3, g.getTeam(Team.ID_2).getTimeouts());
    }

    @Test
    public void testClockUndo_undo() {
        g.startJam();
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        assertEquals(Timeout.Owners.NONE, g.getTimeoutOwner());
        assertFalse(g.isOfficialReview());
        assertFalse(g.isInOvertime());
        assertTrue(g.isInPeriod());
        g.setLabels(Game.ACTION_START_JAM, Game.ACTION_STOP_JAM, Game.ACTION_TIMEOUT);
        g.setLabel(Button.UNDO, Game.ACTION_NONE);

        g.createSnapshot("TEST");
        assertEquals("TEST", g.snapshot.getType());
        assertEquals(Game.UNDO_PREFIX + "TEST", g.getLabel(Button.UNDO));

        g.timeout();
        lc.start();
        ic.start();
        g.setTimeoutOwner(Timeout.Owners.OTO);
        g.setOfficialReview(true);
        g.setInOvertime(true);
        g.setInPeriod(false);
        g.setLabels(Game.ACTION_LINEUP, Game.ACTION_RE_TIMEOUT, Game.ACTION_OVERTIME);
        advance(2000);

        g.clockUndo(false);
        // need to manually advance as the stopped clock will not catch up to system
        // time
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
        assertEquals(Timeout.Owners.NONE, g.getTimeoutOwner());
        assertFalse(g.isOfficialReview());
        assertFalse(g.isInOvertime());
        assertTrue(g.isInPeriod());
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_STOP_JAM, Game.ACTION_TIMEOUT, Game.ACTION_NONE);
    }

    @Test
    public void testClockUndoWithClockSync() {
        sb.getSettings().set(Clock.SETTING_SYNC, "True");
        assertEquals(0, jc.getInvertedTime());
        assertEquals(0, pc.getInvertedTime());
        assertEquals(120000, jc.getTime());
        assertEquals(1800000, pc.getTime());
        g.startJam();
        advance(600);
        g.clockUndo(false);

        assertEquals(0, jc.getInvertedTime());
        assertEquals(0, pc.getInvertedTime());
        assertEquals(120000, jc.getTime());
        assertEquals(1800000, pc.getTime());
    }

    @Test
    public void testClockUndo_replace() {
        g.startJam();
        pc.elapseTime(600000);
        assertTrue(pc.isRunning());
        assertTrue(jc.isRunning());
        assertTrue(g.isInPeriod());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        g.setLabels(Game.ACTION_NONE, Game.ACTION_STOP_JAM, Game.ACTION_TIMEOUT);

        g.timeout();
        assertEquals(Game.ACTION_TIMEOUT, g.snapshot.getType());
        advance(2000);

        g.clockUndo(true);
        checkLabels(Game.ACTION_NONE, Game.ACTION_STOP_JAM, Game.ACTION_TIMEOUT, Game.ACTION_NO_REPLACE,
                    Game.ACTION_TIMEOUT);
        g.stopJamTO();

        advance(ScoreBoardClock.getInstance().getLastRewind());
        assertTrue(pc.isRunning());
        assertEquals(602000, pc.getTimeElapsed());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertEquals(2000, lc.getTimeElapsed());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        g.clockUndo(true);
        g.clockUndo(true);
        advance(ScoreBoardClock.getInstance().getLastRewind());

        assertTrue(pc.isRunning());
        assertEquals(602000, pc.getTimeElapsed());
        assertTrue(jc.isRunning());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        g.clockUndo(true);
        g.timeout();
        advance(ScoreBoardClock.getInstance().getLastRewind());

        assertFalse(pc.isRunning());
        assertEquals(600000, pc.getTimeElapsed());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertEquals(2000, tc.getTimeElapsed());
        assertFalse(ic.isRunning());

        g.stopJamTO();
        advance(5000);
        g.timeout();
        advance(3000);

        g.clockUndo(true);
        g.startJam();
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
        g.startJam();
        for (int i = 0; i < 5; i++) {
            g.clockUndo(false);
            // Regression test for an NPE here.
            g.startJam();
        }
    }

    @Test
    public void testJamStartUndoAfterPeriodEnd() {
        fastForwardJams(5);
        pc.setTime(2000);
        advance(2000);
        assertFalse(pc.isRunning());
        assertEquals(5, g.getCurrentPeriod().numberOf(Period.JAM));

        g.startJam();
        assertEquals(6, g.getCurrentPeriod().numberOf(Period.JAM));

        g.clockUndo(false);
        assertEquals(5, g.getCurrentPeriod().numberOf(Period.JAM));
    }

    @Test
    public void testTimeoutCountOnUndo() {
        fastForwardJams(1);
        assertEquals(3, g.getTeam(Team.ID_1).getTimeouts());
        assertEquals(1, g.getTeam(Team.ID_2).getOfficialReviews());
        g.timeout();
        g.getTeam(Team.ID_1).timeout();
        assertEquals(2, g.getTeam(Team.ID_1).getTimeouts());
        g.clockUndo(false);
        assertEquals(3, g.getTeam(Team.ID_1).getTimeouts());
        g.getTeam(Team.ID_2).officialReview();
        assertEquals(0, g.getTeam(Team.ID_2).getOfficialReviews());
        g.clockUndo(false);
        assertEquals(1, g.getTeam(Team.ID_2).getOfficialReviews());
    }

    @Test
    public void testPeriodClockEnd_duringLineup() {
        g.set(Rule.INTERMISSION_DURATIONS, "5:00,15:00,5:00,60:00");

        fastForwardPeriod();
        ic.setTime(0);
        fastForwardJams(1);
        advance(0);
        String prevUndoLabel = g.getLabel(Button.UNDO);

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
        long dur = ClockConversion.fromHumanReadable(g.get(Rule.INTERMISSION_DURATIONS).split(",")[1]);
        assertEquals(dur, ic.getTimeRemaining());
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_LINEUP, Game.ACTION_TIMEOUT, prevUndoLabel);
    }

    @Test
    public void testPeriodClockEnd_periodEndInhibitedByRuleset() {
        g.set(Rule.PERIOD_END_BETWEEN_JAMS, "false");

        fastForwardJams(1);
        String prevUndoLabel = g.getLabel(Button.UNDO);

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
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_NONE, Game.ACTION_TIMEOUT, prevUndoLabel);
    }

    @Test
    public void testPeriodClockEnd_duringJam() {
        fastForwardJams(16);
        g.startJam();
        String prevStartLabel = g.getLabel(Button.START);
        String prevStopLabel = g.getLabel(Button.STOP);
        String prevTimeoutLabel = g.getLabel(Button.TIMEOUT);
        String prevUndoLabel = g.getLabel(Button.UNDO);
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
        sb.getSettings().set(ScoreBoard.SETTING_AUTO_END_JAM, "true");
        g.startJam();
        String prevStartLabel = g.getLabel(Button.START);
        String prevStopLabel = g.getLabel(Button.STOP);
        String prevTimeoutLabel = g.getLabel(Button.TIMEOUT);
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
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_NONE, Game.ACTION_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_STOP_JAM);

        g.clockUndo(false);
        advance(1000);

        assertTrue(pc.isRunning());
        assertFalse(jc.isRunning());
        assertTrue(jc.isCountDirectionDown());
        assertTrue(jc.isTimeAtEnd());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        checkLabels(prevStartLabel, prevStopLabel, prevTimeoutLabel, Game.ACTION_NONE);
    }

    @Test
    public void testJamClockEnd_autoEndDisabled() {
        sb.getSettings().set(ScoreBoard.SETTING_AUTO_END_JAM, "false");
        g.startJam();
        String prevStartLabel = g.getLabel(Button.START);
        String prevStopLabel = g.getLabel(Button.STOP);
        String prevTimeoutLabel = g.getLabel(Button.TIMEOUT);
        String prevUndoLabel = g.getLabel(Button.UNDO);

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

        advance(g.getLong(Rule.PERIOD_DURATION));
        g.stopJamTO();

        assertFalse(lc.isRunning());
    }

    @Test
    public void testIntermissionClockEnd_notLastPeriod() {
        g.addScoreBoardListener(new ConditionalScoreBoardListener<>(jc, Clock.NUMBER, listener));
        fastForwardJams(19);
        fastForwardPeriod();
        String prevUndoLabel = g.getLabel(Button.UNDO);
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
        assertEquals(0, collectedEvents.size());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        assertTrue(ic.isTimeAtEnd());
        assertEquals(2, ic.getNumber());
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_LINEUP, Game.ACTION_TIMEOUT, prevUndoLabel);
    }

    @Test
    public void testIntermissionClockEnd_notLastPeriodContinueCountingJams() {
        g.set(Rule.JAM_NUMBER_PER_PERIOD, "false");

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
        String prevStartLabel = g.getLabel(Button.START);
        String prevStopLabel = g.getLabel(Button.STOP);
        String prevTimeoutLabel = g.getLabel(Button.TIMEOUT);
        String prevUndoLabel = g.getLabel(Button.UNDO);
        assertFalse(pc.isRunning());
        assertTrue(pc.isCountDirectionDown());
        assertTrue(pc.isTimeAtEnd());
        assertEquals(g.getInt(Rule.NUMBER_PERIODS), pc.getNumber());
        assertFalse(jc.isRunning());
        assertEquals(21, jc.getNumber());
        jc.setTime(56000);
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertTrue(ic.isRunning());
        assertTrue(ic.isCountDirectionDown());
        assertEquals(g.getInt(Rule.NUMBER_PERIODS), ic.getNumber());
        ic.setTime(3000);

        advance(3000);

        assertFalse(pc.isRunning());
        assertTrue(pc.isTimeAtEnd());
        assertEquals(g.getInt(Rule.NUMBER_PERIODS), pc.getNumber());
        assertFalse(jc.isRunning());
        assertEquals(21, jc.getNumber());
        assertEquals(56000, jc.getTime());
        assertFalse(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        assertTrue(ic.isTimeAtEnd());
        assertEquals(g.getInt(Rule.NUMBER_PERIODS), ic.getNumber());
        checkLabels(prevStartLabel, prevStopLabel, prevTimeoutLabel, prevUndoLabel);
    }

    @Test
    public void testTimeoutInLast30s() {
        // jam ended before 30s mark, official timeout after 30s mark
        assertTrue(pc.isCountDirectionDown());
        g.startJam();
        pc.setTime(35000);
        g.stopJamTO();
        assertFalse(g.get(Game.NO_MORE_JAM));
        assertTrue(pc.isRunning());
        assertTrue(lc.isRunning());
        advance(10000);
        g.timeout();
        advance(20000);
        g.stopJamTO();
        assertFalse(g.get(Game.NO_MORE_JAM));
        assertFalse(pc.isRunning());

        // jam ended after 30s mark, official timeout
        g.startJam();
        g.stopJamTO();
        assertTrue(g.get(Game.NO_MORE_JAM));
        assertEquals(25000, pc.getTime());
        assertTrue(pc.isRunning());
        advance(1000);
        g.timeout();
        advance(35000);
        assertTrue(g.get(Game.NO_MORE_JAM));
        g.setTimeoutType(Timeout.Owners.OTO, false);
        assertTrue(g.get(Game.NO_MORE_JAM));
        g.stopJamTO();
        assertTrue(g.get(Game.NO_MORE_JAM));
        assertTrue(pc.isRunning());

        // follow up with team timeout
        advance(2000);
        g.setTimeoutType(g.getTeam(Team.ID_1), false);
        assertFalse(g.get(Game.NO_MORE_JAM));
        advance(60000);
        g.stopJamTO();
        assertFalse(g.get(Game.NO_MORE_JAM));
        assertFalse(pc.isRunning());
        assertEquals(22000, pc.getTimeRemaining());
    }

    @Test
    public void testP2StartLineupAfter() {
        // jam ended after 30s mark, no more jams.
        assertTrue(pc.isCountDirectionDown());
        g.startJam();
        pc.setTime(2000);
        g.stopJamTO();
        assertTrue(g.get(Game.NO_MORE_JAM));
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
        g.stopJamTO();
        assertFalse(g.get(Game.NO_MORE_JAM));
        assertFalse(pc.isRunning());
        assertTrue(lc.isRunning());
        assertFalse(ic.isRunning());
    }

    @Test
    public void testTimeoutsThatDontAlwaysStopPc() {
        g.set(Rule.STOP_PC_ON_TO, "false");
        g.set(Rule.STOP_PC_ON_OTO, "false");
        g.set(Rule.STOP_PC_ON_TTO, "true");
        g.set(Rule.STOP_PC_ON_OR, "true");
        g.set(Rule.STOP_PC_AFTER_TO_DURATION, "120000");

        fastForwardJams(1);
        assertTrue(pc.isCountDirectionDown());
        pc.setTime(1200000);
        assertTrue(pc.isRunning());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());

        g.timeout();

        assertTrue(pc.isRunning());
        assertEquals(600000, pc.getTimeElapsed());
        assertTrue(tc.isRunning());
        assertEquals(0, tc.getTimeElapsed());

        advance(2000);
        assertEquals(602000, pc.getTimeElapsed());

        g.setTimeoutType(g.getTeam(Team.ID_1), false);

        assertFalse(pc.isRunning());
        assertEquals(600000, pc.getTimeElapsed());
        assertTrue(tc.isRunning());

        advance(3000);
        assertEquals(600000, pc.getTimeElapsed());

        g.setTimeoutType(Timeout.Owners.OTO, false);

        assertTrue(pc.isRunning());
        assertEquals(605000, pc.getTimeElapsed());
        assertTrue(tc.isRunning());

        advance(115000);

        assertFalse(pc.isRunning());
        assertEquals(719800, pc.getTimeElapsed()); // tc ticks first, stopping pc, so pc's tick is skipped
        assertTrue(tc.isRunning());
    }

    @Test
    public void testAutoStartJam() {
        sb.getSettings().set(ScoreBoard.SETTING_AUTO_START, Clock.ID_JAM);

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
        checkLabels(Game.ACTION_NONE, Game.ACTION_STOP_JAM, Game.ACTION_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_START_JAM);
    }

    @Test
    public void testNoAutoEndJam() {
        sb.getSettings().set(ScoreBoard.SETTING_AUTO_END_JAM, "false");

        g.startJam();
        advance(jc.getMaximumTime());

        assertFalse(jc.isRunning());
        assertTrue(g.isInJam());
    }

    @Test
    public void testAutoStartAndEndTimeout() {
        sb.getSettings().set(ScoreBoard.SETTING_AUTO_START, Clock.ID_TIMEOUT);
        sb.getSettings().set(ScoreBoard.SETTING_AUTO_START_BUFFER, "0");
        sb.getSettings().set(ScoreBoard.SETTING_AUTO_END_TTO, "true");
        g.set(Rule.TTO_DURATION, "25000");

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
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_STOP_TO, Game.ACTION_RE_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_TIMEOUT);

        g.setTimeoutType(g.getTeam(Team.ID_2), false);

        advance(25000);

        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertTrue(lc.isRunning());
        assertFalse(tc.isRunning());
        assertFalse(ic.isRunning());
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_NONE, Game.ACTION_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_STOP_TO);

        advance(30000);

        g.setTimeoutType(g.getTeam(Team.ID_2), true);

        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertTrue(tc.isTimeAtStart());
        assertFalse(ic.isRunning());
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_STOP_TO, Game.ACTION_RE_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_TIMEOUT);

        advance(25000);

        assertFalse(pc.isRunning());
        assertFalse(jc.isRunning());
        assertFalse(lc.isRunning());
        assertTrue(tc.isRunning());
        assertFalse(ic.isRunning());
        checkLabels(Game.ACTION_START_JAM, Game.ACTION_STOP_TO, Game.ACTION_RE_TIMEOUT,
                    Game.UNDO_PREFIX + Game.ACTION_TIMEOUT);
    }

    @Test
    public void testPeriodJamInsertRemove() {
        fastForwardJams(4);

        assertEquals(2, g.numberOf(Game.PERIOD));
        assertEquals(0, g.getMinNumber(Game.PERIOD) + 0);
        assertEquals(1, g.getMaxNumber(Game.PERIOD) + 0);
        Period p0 = g.getFirst(Game.PERIOD);
        Period p1 = g.getLast(Game.PERIOD);
        assertEquals(p1, g.getCurrentPeriod());
        assertEquals(0, p0.getNumber());
        assertEquals(1, p1.getNumber());
        assertEquals(p1, p0.getNext());
        assertNull(p1.getNext());
        assertNull(p0.getPrevious());
        assertEquals(p0, p1.getPrevious());
        assertEquals(1, p0.numberOf(Period.JAM));
        assertEquals(0, p0.getMinNumber(Period.JAM) + 0);
        assertEquals(0, p0.getMaxNumber(Period.JAM) + 0);
        assertEquals(4, p1.numberOf(Period.JAM));
        assertEquals(1, p1.getMinNumber(Period.JAM) + 0);
        assertEquals(4, p1.getMaxNumber(Period.JAM) + 0);
        assertEquals(1, g.numberOf(Period.JAM));
        assertEquals(5, g.getMinNumber(Period.JAM) + 0);
        assertEquals(5, g.getMaxNumber(Period.JAM) + 0);
        Jam j0 = p0.getFirst(Period.JAM);
        Jam j1 = p1.getFirst(Period.JAM);
        Jam j2 = p1.get(Period.JAM, 2);
        Jam j3 = p1.get(Period.JAM, 3);
        Jam j4 = p1.getLast(Period.JAM);
        Jam j5 = g.getFirst(Period.JAM);
        assertEquals(j0, p0.getCurrentJam());
        assertEquals(j4, p1.getCurrentJam());
        assertEquals(j5, g.getUpcomingJam());
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

        g.getCurrentPeriod().execute(Period.INSERT_BEFORE);
        g.get(Game.PERIOD, "1").delete();

        assertEquals(2, g.numberOf(Game.PERIOD));
        assertEquals(0, g.getMinNumber(Game.PERIOD) + 0);
        assertEquals(1, g.getMaxNumber(Game.PERIOD) + 0);
        p0 = g.getFirst(Game.PERIOD);
        p1 = g.getLast(Game.PERIOD);
        assertEquals(p1, g.getCurrentPeriod());
        assertEquals(0, p0.getNumber());
        assertEquals(1, p1.getNumber());
        assertEquals(p1, p0.getNext());
        assertNull(p1.getNext());
        assertNull(p0.getPrevious());
        assertEquals(p0, p1.getPrevious());
        assertEquals(1, p0.numberOf(Period.JAM));
        assertEquals(0, p0.getMinNumber(Period.JAM) + 0);
        assertEquals(0, p0.getMaxNumber(Period.JAM) + 0);
        assertEquals(4, p1.numberOf(Period.JAM));
        assertEquals(1, p1.getMinNumber(Period.JAM) + 0);
        assertEquals(4, p1.getMaxNumber(Period.JAM) + 0);
        assertEquals(1, g.numberOf(Period.JAM));
        assertEquals(5, g.getMinNumber(Period.JAM) + 0);
        assertEquals(5, g.getMaxNumber(Period.JAM) + 0);
        j0 = p0.getFirst(Period.JAM);
        j1 = p1.getFirst(Period.JAM);
        j2 = p1.get(Period.JAM, 2);
        j3 = p1.get(Period.JAM, 3);
        j4 = p1.getLast(Period.JAM);
        j5 = g.getFirst(Period.JAM);
        assertEquals(j0, p0.getCurrentJam());
        assertEquals(j4, p1.getCurrentJam());
        assertEquals(j5, g.getUpcomingJam());
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

        g.getCurrentPeriod().getCurrentJam().execute(Jam.INSERT_BEFORE);
        ((ScoreBoardEventProvider) g.get(Game.PERIOD, "1")).get(Period.JAM, 1).delete();

        assertEquals(2, g.numberOf(Game.PERIOD));
        assertEquals(0, g.getMinNumber(Game.PERIOD) + 0);
        assertEquals(1, g.getMaxNumber(Game.PERIOD) + 0);
        p0 = g.getFirst(Game.PERIOD);
        p1 = g.getLast(Game.PERIOD);
        assertEquals(p1, g.getCurrentPeriod());
        assertEquals(0, p0.getNumber());
        assertEquals(1, p1.getNumber());
        assertEquals(p1, p0.getNext());
        assertNull(p1.getNext());
        assertNull(p0.getPrevious());
        assertEquals(p0, p1.getPrevious());
        assertEquals(1, p0.numberOf(Period.JAM));
        assertEquals(0, p0.getMinNumber(Period.JAM) + 0);
        assertEquals(0, p0.getMaxNumber(Period.JAM) + 0);
        assertEquals(4, p1.numberOf(Period.JAM));
        assertEquals(1, p1.getMinNumber(Period.JAM) + 0);
        assertEquals(4, p1.getMaxNumber(Period.JAM) + 0);
        assertEquals(1, g.numberOf(Period.JAM));
        assertEquals(5, g.getMinNumber(Period.JAM) + 0);
        assertEquals(5, g.getMaxNumber(Period.JAM) + 0);
        j0 = p0.getFirst(Period.JAM);
        j1 = p1.getFirst(Period.JAM);
        j2 = p1.get(Period.JAM, 2);
        j3 = p1.get(Period.JAM, 3);
        j4 = p1.getLast(Period.JAM);
        j5 = g.getFirst(Period.JAM);
        assertEquals(j0, p0.getCurrentJam());
        assertEquals(j4, p1.getCurrentJam());
        assertEquals(j5, g.getUpcomingJam());
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
        assertEquals(2, g.numberOf(Game.PERIOD));
        assertEquals(1, g.getCurrentPeriodNumber());

        g.getCurrentPeriod().execute(Period.DELETE);

        assertEquals(1, g.numberOf(Game.PERIOD));
        assertEquals(0, g.getCurrentPeriodNumber());

        // Make sure we can start the jam.
        g.startJam();
        assertEquals(1, g.getCurrentPeriodNumber());
    }

    @Test
    public void testDeleteJam() {
        fastForwardJams(3);
        g.startJam();

        Jam j2 = g.getCurrentPeriod().getJam(2);
        j2.execute(Jam.DELETE);

        assertEquals(3, g.getCurrentPeriod().numberOf(Period.JAM));

        // Make sure we can start the jam again.
        g.stopJamTO();
        assertEquals(3, g.getCurrentPeriod().numberOf(Period.JAM));
        g.startJam();
        assertEquals(4, g.getCurrentPeriod().numberOf(Period.JAM));
        assertEquals(4, g.getCurrentPeriod().getCurrentJam().getNumber());
    }

    @Test
    public void testPenaltiesMovedOnPeriodDelete() {
        Team team = g.getTeam(Team.ID_1);
        Skater skater = new SkaterImpl(team, UUID.randomUUID().toString());

        fastForwardJams(2);

        Penalty penalty = skater.getOrCreate(Skater.PENALTY, "1");
        penalty.set(Penalty.JAM, g.getCurrentPeriod().getCurrentJam());
        penalty.set(Penalty.CODE, "C");

        fastForwardPeriod();
        ic.setTime(0);
        fastForwardJams(2);

        Period p2 = g.getCurrentPeriod();
        assertEquals(2, p2.getNumber());
        assertEquals(p2.getPrevious(), penalty.getJam().getParent());

        p2.getPrevious().delete();

        assertEquals(1, p2.getNumber());
        assertEquals(p2.getFirst(Period.JAM), penalty.getJam());
    }

    @Test
    public void testJamsAndPeriodsCreated() {
        // Starting a jam creates a jam and its successor.
        g.startJam();
        advance(1000);
        Period p = g.getOrCreatePeriod(1);
        assertEquals(1, p.numberOf(Period.JAM));

        // Start the second jam and confirm it's there.
        g.stopJamTO();
        g.startJam();
        advance(1000);
        assertEquals(2, p.numberOf(Period.JAM));
    }

    @Test
    public void testJamsAndPeriodsTruncated() {
        // Put us in period 2 with 3 jams;
        g.startJam();
        pc.setTime(0);
        g.stopJamTO();
        ic.setTime(0);
        for (int i = 0; i < 3; i++) {
            g.startJam();
            advance(1000);
            g.stopJamTO();
        }
        assertEquals(3, g.numberOf(Game.PERIOD)); // 0, 1, and 2
        Period p = g.getOrCreatePeriod(2);
        assertEquals(3, p.numberOf(Period.JAM));
    }

    @Test
    public void testJamStartListener() {
        Team team1 = g.getTeam(Team.ID_1);
        Skater skater1 = team1.getSkater(ID_PREFIX + "100");
        Skater skater2 = team1.getSkater(ID_PREFIX + "101");
        team1.field(skater1, Role.JAMMER);
        team1.field(skater2, Role.PIVOT);

        g.startJam();
        advance(1000);

        // Confirm stats are as expected at start of first jam.
        Period p = g.getOrCreatePeriod(1);
        Jam j = p.getJam(1);
        TeamJam tj = j.getTeamJam(Team.ID_1);
        assertEquals(0, j.getPeriodClockElapsedStart());
        assertEquals(0, tj.getTotalScore());
        assertEquals(0, tj.getJamScore());
        assertEquals(false, tj.isLost());
        assertEquals(false, tj.isLead());
        assertEquals(false, tj.isLead());
        assertEquals(false, tj.isCalloff());
        assertEquals(false, tj.isInjury());
        assertEquals(false, tj.isStarPass());
        assertEquals(null, tj.getStarPassTrip());
        assertEquals(skater1, tj.getFielding(FloorPosition.JAMMER).getSkater());
        assertEquals(skater2, tj.getFielding(FloorPosition.PIVOT).getSkater());
        assertEquals(null, tj.getFielding(FloorPosition.BLOCKER1).getSkater());

        // Team 1 gets lead and scores.
        team1.set(Team.LEAD, true);
        team1.set(Team.TRIP_SCORE, 5, Flag.CHANGE);
        assertEquals(5, tj.getTotalScore());

        g.stopJamTO();
        advance(1000);
        g.startJam();
        advance(1000);

        // Confirm stats at start of second jam.
        j = p.getJam(2);
        tj = j.getTeamJam(Team.ID_1);
        assertEquals(2000, j.getPeriodClockElapsedStart());
        assertEquals(5, tj.getTotalScore());
        assertEquals(0, tj.getJamScore());
        assertEquals(false, tj.isLead());
        assertEquals(false, tj.isStarPass());
        // No skaters have been entered for this jam yet.
        assertEquals(null, tj.getFielding(FloorPosition.JAMMER).getSkater());
        assertEquals(null, tj.getFielding(FloorPosition.PIVOT).getSkater());
    }

    @Test
    public void testJamStopListener() {
        g.startJam();
        advance(1000);
        g.stopJamTO();
        advance(1000);

        // Confirm stats are as expected at end of first jam.
        Period p = g.getOrCreatePeriod(1);
        Jam j = p.getJam(1);
        assertEquals(1000, j.getDuration());
        assertEquals(1000, j.getPeriodClockElapsedEnd());

        g.startJam();
        advance(1000);
        g.stopJamTO();
        advance(1000);
        j = p.getJam(2);
        assertEquals(1000, j.getDuration());
        assertEquals(3000, j.getPeriodClockElapsedEnd());
    }

    @Test
    public void testTeamEventListener() {
        Team team1 = g.getTeam(Team.ID_1);
        g.startJam();
        advance(1000);

        Period p = g.getOrCreatePeriod(1);
        Jam j = p.getJam(1);
        TeamJam tj = j.getTeamJam(Team.ID_1);

        // Lead during the jam.
        team1.set(Team.LEAD, true);
        assertTrue(tj.isDisplayLead());

        // Change the score during the jam.
        team1.set(Team.TRIP_SCORE, 5, Flag.CHANGE);
        assertEquals(5, tj.getTotalScore());
        assertEquals(5, tj.getJamScore());

        // Star pass during the jam.
        team1.set(Team.STAR_PASS_TRIP, team1.get(Team.CURRENT_TRIP));
        assertEquals(true, tj.isStarPass());

        g.stopJamTO();
        advance(1000);
        // Star pass and lead still correct after jam end.
        assertEquals(true, tj.isStarPass());
        assertEquals(false, tj.isDisplayLead());

        // Some points arrive after end of jam.
        team1.set(Team.TRIP_SCORE, 4, Flag.CHANGE);
        assertEquals(9, tj.getTotalScore());
        assertEquals(9, tj.getJamScore());

        // Timeout at end of jam.
        team1.timeout();
        advance(1000);
        assertEquals(2, tj.getTeam().getTimeouts());

        // Official review at end of jam.
        team1.officialReview();
        advance(1000);
        assertEquals(0, tj.getTeam().getOfficialReviews());

        // Start jam 2, confirm score.
        g.startJam();
        advance(1000);
        j = p.getJam(2);
        tj = j.getTeamJam(Team.ID_1);
        assertEquals(9, tj.getTotalScore());
        assertEquals(0, tj.getJamScore());

        // Add points during the jam, jam score is correct.
        team1.set(Team.TRIP_SCORE, 3, Flag.CHANGE);
        assertEquals(12, tj.getTotalScore());
        assertEquals(3, tj.getJamScore());
    }

    @Test
    public void testSkaterEventListener() {
        Team team1 = g.getTeam(Team.ID_1);
        // Some skater positions set before jam.
        Skater skater1 = team1.getOrCreate(Team.SKATER, ID_PREFIX + "100");
        Skater skater2 = team1.getOrCreate(Team.SKATER, ID_PREFIX + "101");
        Skater skater3 = team1.getOrCreate(Team.SKATER, ID_PREFIX + "102");
        Skater skater4 = team1.getOrCreate(Team.SKATER, ID_PREFIX + "103");
        team1.field(skater1, Role.JAMMER);
        team1.field(skater2, Role.PIVOT);

        g.startJam();
        advance(1000);

        Period p = g.getOrCreatePeriod(1);
        Jam j = p.getJam(1);
        TeamJam tj = j.getTeamJam(Team.ID_1);

        // Blocker is added after the jam starts. All positions in place.
        team1.getPosition(FloorPosition.BLOCKER1).setSkater(skater3);
        assertEquals(skater1, tj.getFielding(FloorPosition.JAMMER).getSkater());
        assertEquals(skater2, tj.getFielding(FloorPosition.PIVOT).getSkater());
        assertEquals(skater3, tj.getFielding(FloorPosition.BLOCKER1).getSkater());

        // Skater was actually on bench.
        team1.field(skater3, Role.BENCH);
        assertEquals(null, tj.getFielding(FloorPosition.BLOCKER1).getSkater());

        // Skater goes to the box.
        skater1.setPenaltyBox(true);
        assertEquals(true, tj.getFielding(FloorPosition.JAMMER).isInBox());

        // Skater exits the box.
        skater1.setPenaltyBox(false);
        assertEquals(false, tj.getFielding(FloorPosition.JAMMER).isInBox());

        // Jam ends.
        g.stopJamTO();
        team1.execute(Team.ADVANCE_FIELDINGS);
        advance(1000);
        // New jammer does not replace jammer from previous jam.
        team1.field(skater4, Role.JAMMER);
        assertEquals(skater1, tj.getFielding(FloorPosition.JAMMER).getSkater());
    }
    @Test
    public void testTripAutoAdvance() {
        Team team1 = g.getTeam(Team.ID_1);
        g.startJam();
        advance(1000);

        Period p = g.getOrCreatePeriod(1);
        Jam j = p.getJam(1);
        TeamJam tj = j.getTeamJam(Team.ID_1);

        // Add some points without adding a trip or lead.
        team1.set(Team.TRIP_SCORE, 4, Flag.CHANGE);
        assertEquals(2, tj.getCurrentScoringTrip().getNumber());

        g.stopJamTO();
        advance(1000);
        g.startJam();
        advance(1000);
        j = p.getJam(2);
        tj = j.getTeamJam(Team.ID_1);
        g.stopJamTO();
        advance(1000);

        // Add some points between jams without adding a trip or lead.
        team1.set(Team.TRIP_SCORE, 4, Flag.CHANGE);
        assertEquals(2, tj.getCurrentScoringTrip().getNumber());

        g.startJam();
        g.setInOvertime(true);
        advance(1000);
        j = p.getJam(3);
        tj = j.getTeamJam(Team.ID_1);

        // Add some points without adding a trip or lead during an overtime jam
        assertEquals(1, tj.getCurrentScoringTrip().getNumber());
        team1.set(Team.TRIP_SCORE, 1);
        assertEquals(1, tj.getCurrentScoringTrip().getNumber());
    }

    @Test
    public void testChangingRuleset() {
        Rulesets rulesets = sb.getRulesets();
        Ruleset root = rulesets.getRuleset(Rulesets.ROOT_ID);
        String id1 = UUID.randomUUID().toString();
        Ruleset child = rulesets.addRuleset("child", root, id1);
        assertEquals(root, child.getParentRuleset());
        assertEquals(2, g.getInt(Rule.NUMBER_PERIODS));
        assertEquals(1800000, g.getLong(Rule.PERIOD_DURATION));
        assertEquals(root, g.getRuleset());
        assertEquals("WFTDA", g.getRulesetName());

        child.add(Ruleset.RULE, new ValWithId(Rule.NUMBER_PERIODS.toString(), "5"));
        g.setRuleset(child);
        assertEquals(5, g.getInt(Rule.NUMBER_PERIODS));
        assertEquals(1800000, g.getLong(Rule.PERIOD_DURATION));
        assertEquals(id1, g.getRuleset().getId());
        assertEquals("child", g.getRulesetName());

        g.setRuleset(root);
        assertEquals(2, g.getInt(Rule.NUMBER_PERIODS));
        assertEquals(1800000, g.getLong(Rule.PERIOD_DURATION));
        assertEquals(root, g.getRuleset());
        assertEquals("WFTDA", g.getRulesetName());

        g.set(Rule.NUMBER_PERIODS, "6");
        assertEquals(6, g.getInt(Rule.NUMBER_PERIODS));

        g.set(Rule.NUMBER_PERIODS, "zz");
        assertEquals(6, g.getInt(Rule.NUMBER_PERIODS));
    }
}
