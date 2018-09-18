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
import com.carolinarollergirls.scoreboard.event.AsyncScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.jetty.JettyServletScoreBoardController;
import com.carolinarollergirls.scoreboard.model.ClockModel;
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
		sbm.getSettingsModel().set("ScoreBoard.Clock.Sync", "False");
	}
	
	@After
	public void tearDown() throws Exception {
		ScoreBoardClock.getInstance().start(false);
		// Check all started batches were ended.
		advance(0);
		assertEquals(0, batchLevel);
	}

	private void advance(long time_ms) {
		AsyncScoreBoardListener.waitForEvents();
		ScoreBoardClock.getInstance().advance(time_ms);
		AsyncScoreBoardListener.waitForEvents();
	}
	
	@Test
	public void testSetInPeriod() {
		assertFalse(sbm.isInPeriod());
		sbm.addScoreBoardListener(new ConditionalScoreBoardListener(sbm, ScoreBoard.EVENT_IN_PERIOD, listener));
		
		sbm.setInPeriod(true);
		advance(0);
		assertTrue(sbm.isInPeriod());
		assertEquals(1, collectedEvents.size());
		ScoreBoardEvent event = collectedEvents.poll();
		assertTrue((Boolean)event.getValue());
		assertFalse((Boolean)event.getPreviousValue());
		
		//check idempotency
		sbm.setInPeriod(true);
		advance(0);
		assertTrue(sbm.isInPeriod());
		assertEquals(0, collectedEvents.size());
		
		sbm.setInPeriod(false);
		advance(0);
		assertFalse(sbm.isInPeriod());
	}

	@Test
	public void testSetInOvertime() {
		(sbm.getSettingsModel()).set("Clock." + Clock.ID_LINEUP + ".Time", "30000");
		lc.setMaximumTime(999999999);

		assertFalse(lc.isCountDirectionDown());
		assertFalse(sbm.isInOvertime());
		sbm.addScoreBoardListener(new ConditionalScoreBoardListener(sbm, ScoreBoard.EVENT_IN_OVERTIME, listener));
		
		sbm.setInOvertime(true);
		advance(0);
		assertTrue(sbm.isInOvertime());
		assertEquals(999999999, lc.getMaximumTime());
		assertEquals(1, collectedEvents.size());
		ScoreBoardEvent event = collectedEvents.poll();
		assertTrue((Boolean)event.getValue());
		assertFalse((Boolean)event.getPreviousValue());
		
		//check idempotency
		sbm.setInOvertime(true);
		advance(0);
		assertTrue(sbm.isInOvertime());
		assertEquals(0, collectedEvents.size());

		sbm.setInOvertime(true);
		advance(0);
		assertTrue(sbm.isInOvertime());

		sbm.setInOvertime(false);
		advance(0);
		assertFalse(sbm.isInOvertime());
		assertEquals(999999999, lc.getMaximumTime());

		//check that lineup clock maximum time is reset for countdown lineup clock
		sbm.setInOvertime(true);
		advance(0);
		lc.setCountDirectionDown(true);
		sbm.setInOvertime(false);
		advance(0);
		assertEquals(30000, lc.getMaximumTime());
	}

	@Test
	public void testSetOfficialScore() {
		assertFalse(sbm.isOfficialScore());
		sbm.addScoreBoardListener(new ConditionalScoreBoardListener(sbm, ScoreBoard.EVENT_OFFICIAL_SCORE, listener));
		
		sbm.setOfficialScore(true);
		advance(0);
		assertTrue(sbm.isOfficialScore());
		assertEquals(1, collectedEvents.size());
		ScoreBoardEvent event = collectedEvents.poll();
		assertTrue((Boolean)event.getValue());
		assertFalse((Boolean)event.getPreviousValue());
		
		//check idempotency
		sbm.setOfficialScore(true);
		advance(0);
		assertTrue(sbm.isOfficialScore());
		assertEquals(1, collectedEvents.size());
		
		sbm.setOfficialScore(false);
		advance(0);
		assertFalse(sbm.isOfficialScore());
	}

	@Test
	public void testSetOfficialReview() {
		assertFalse(sbm.isOfficialReview());
		sbm.addScoreBoardListener(new ConditionalScoreBoardListener(sbm, ScoreBoard.EVENT_OFFICIAL_REVIEW, listener));
		
		sbm.setOfficialReview(true);
		advance(0);
		assertTrue(sbm.isOfficialReview());
		assertEquals(1, collectedEvents.size());
		ScoreBoardEvent event = collectedEvents.poll();
		assertTrue((Boolean)event.getValue());
		assertFalse((Boolean)event.getPreviousValue());
		
		//check idempotency
		sbm.setOfficialReview(true);
		advance(0);
		assertTrue(sbm.isOfficialReview());
		assertEquals(1, collectedEvents.size());
		
		sbm.setOfficialReview(false);
		advance(0);
		assertFalse(sbm.isOfficialReview());
	}

	@Test
	public void testSetTimeoutOwner() {
		assertEquals("", sbm.getTimeoutOwner());
		sbm.addScoreBoardListener(new ConditionalScoreBoardListener(sbm, ScoreBoard.EVENT_TIMEOUT_OWNER, listener));
		
		sbm.setTimeoutOwner("testOwner");
		assertEquals("testOwner", sbm.getTimeoutOwner());
		advance(0);
		assertEquals(1, collectedEvents.size());
		ScoreBoardEvent event = collectedEvents.poll();
		assertEquals("testOwner", event.getValue());
		assertEquals("", event.getPreviousValue());
		
		sbm.setTimeoutOwner("");
		advance(0);
		assertEquals("", sbm.getTimeoutOwner());
		assertEquals(1, collectedEvents.size());
		event = collectedEvents.poll();
		assertEquals("", event.getValue());
		assertEquals("testOwner", event.getPreviousValue());
	}

	@Test
	public void testStartOvertime_default() {
		(sbm.getSettingsModel()).set("Clock." + Clock.ID_LINEUP + ".OvertimeTime", "60000");

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
		advance(0);

		assertEquals(DefaultScoreBoardModel.ACTION_OVERTIME, sbm.snapshot.getType());
		assertTrue(sbm.isInOvertime());
		assertFalse(pc.isRunning());
		assertFalse(jc.isRunning());
		assertTrue(lc.isRunning());
		assertEquals(60000, lc.getMaximumTime());
		assertFalse(tc.isRunning());
		assertFalse(ic.isRunning());
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
		advance(0);
		
		assertEquals(DefaultScoreBoardModel.ACTION_OVERTIME, sbm.snapshot.getType());
		assertTrue(sbm.isInOvertime());
		assertFalse(pc.isRunning());
		assertFalse(jc.isRunning());
		assertTrue(lc.isRunning());
		assertFalse(tc.isRunning());
		assertEquals(6, tc.getNumber());
		assertFalse(ic.isRunning());
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
		advance(0);
		
		assertEquals(DefaultScoreBoardModel.ACTION_START_JAM, sbm.snapshot.getType());
		assertTrue(pc.isRunning());
		assertEquals(1, pc.getNumber());
		assertTrue(jc.isRunning());
		assertTrue(jc.isTimeAtStart());
		assertEquals(6, jc.getNumber());
		assertFalse(lc.isRunning());
		assertFalse(tc.isRunning());
		assertFalse(ic.isRunning());
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
		advance(0);
		
		assertEquals(DefaultScoreBoardModel.ACTION_START_JAM, sbm.snapshot.getType());
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
		advance(0);
		
		assertEquals(DefaultScoreBoardModel.ACTION_START_JAM, sbm.snapshot.getType());
		assertTrue(pc.isRunning());
		assertEquals(1, pc.getNumber());
		assertTrue(jc.isRunning());
		assertTrue(jc.isTimeAtStart());
		assertEquals(23, jc.getNumber());
		assertFalse(lc.isRunning());
		assertFalse(tc.isRunning());
		assertFalse(ic.isRunning());
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
		advance(0);
		
		assertEquals(DefaultScoreBoardModel.ACTION_START_JAM, sbm.snapshot.getType());
		assertTrue(pc.isRunning());
		assertEquals(1, pc.getNumber());
		assertTrue(jc.isRunning());
		assertEquals(1, jc.getNumber());
		assertFalse(lc.isRunning());
		assertFalse(tc.isRunning());
		assertFalse(ic.isRunning());
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
		advance(0);
		
		assertEquals(DefaultScoreBoardModel.ACTION_START_JAM, sbm.snapshot.getType());
		assertTrue(pc.isRunning());
		assertEquals(2, pc.getNumber());
		assertTrue(jc.isRunning());
		assertEquals(1, jc.getNumber());
		assertTrue(jc.isTimeAtStart());
		assertFalse(lc.isRunning());
		assertFalse(tc.isRunning());
		assertFalse(ic.isRunning());
		assertTrue(sbm.isInPeriod());
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

		assertEquals(DefaultScoreBoardModel.ACTION_START_JAM, sbm.snapshot.getType());
		assertFalse(pc.isRunning());
		assertEquals(1, pc.getNumber());
		assertTrue(jc.isRunning());
		assertEquals(22, jc.getNumber());
		assertEquals(1000, jc.getTimeElapsed());
		assertFalse(lc.isRunning());
		assertFalse(tc.isRunning());
		assertFalse(ic.isRunning());
		assertTrue(sbm.isInPeriod());
	}

	@Test
	public void testStartJam_jamRunning() {
		jc.setTime(74000);
		jc.setNumber(9);
		jc.start();

		sbm.startJam();
		advance(0);

		assertEquals(null, sbm.snapshot);
		assertTrue(jc.isRunning());
		assertEquals(9, jc.getNumber());
		assertEquals(74000, jc.getTime());
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
		advance(0);
		
		assertEquals(DefaultScoreBoardModel.ACTION_STOP_JAM, sbm.snapshot.getType());
		assertTrue(pc.isRunning());
		assertFalse(jc.isRunning());
		assertTrue(lc.isRunning());
		assertTrue(lc.isTimeAtStart());
		assertFalse(tc.isRunning());
		assertFalse(ic.isRunning());
		assertFalse(sbm.getTeamModel("1").isStarPass());
		assertEquals(Team.LEAD_NO_LEAD, sbm.getTeamModel("2").getLeadJammer());
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
		advance(0);
		
		assertEquals(DefaultScoreBoardModel.ACTION_STOP_JAM, sbm.snapshot.getType());
		assertFalse(pc.isRunning());
		assertTrue(pc.isTimeAtEnd());
		assertFalse(jc.isRunning());
		assertFalse(lc.isRunning());
		assertFalse(tc.isRunning());
		assertTrue(ic.isRunning());
		assertEquals(2, ic.getNumber());
		assertEquals(sbm.getSettings().getLong("Clock." + Clock.ID_INTERMISSION + ".Time"), ic.getMaximumTime());
		assertTrue(ic.isTimeAtStart());
		assertFalse(sbm.isInPeriod());
		assertFalse(sbm.isOfficialScore());
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
		advance(0);
		
		assertEquals(DefaultScoreBoardModel.ACTION_STOP_TO, sbm.snapshot.getType());
		assertFalse(pc.isRunning());
		assertFalse(jc.isRunning());
		assertTrue(lc.isRunning());
		assertTrue(lc.isTimeAtStart());
		assertFalse(tc.isRunning());
		assertEquals(4, tc.getNumber());
		assertFalse(ic.isRunning());
		assertEquals("", sbm.getTimeoutOwner());
		assertFalse(sbm.isOfficialReview());
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
		advance(0);
		
		assertEquals(DefaultScoreBoardModel.ACTION_STOP_TO, sbm.snapshot.getType());
		assertFalse(pc.isRunning());
		assertFalse(jc.isRunning());
		assertFalse(lc.isRunning());
		assertFalse(tc.isRunning());
		assertEquals(3, tc.getNumber());
		assertTrue(ic.isRunning());
		assertTrue(ic.isTimeAtStart());
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
		advance(0);
		
		assertEquals(DefaultScoreBoardModel.ACTION_LINEUP, sbm.snapshot.getType());
		assertFalse(pc.isRunning());
		assertEquals(1, pc.getNumber());
		assertFalse(jc.isRunning());
		assertTrue(lc.isRunning());
		assertTrue(lc.isTimeAtStart());
		assertFalse(tc.isRunning());
		assertFalse(ic.isRunning());
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
		advance(0);
		
		assertEquals(DefaultScoreBoardModel.ACTION_LINEUP, sbm.snapshot.getType());
		assertFalse(pc.isRunning());
		assertEquals(2, pc.getNumber());
		assertTrue(pc.isTimeAtStart());
		assertFalse(jc.isRunning());
		assertTrue(lc.isRunning());
		assertTrue(lc.isTimeAtStart());
		assertFalse(tc.isRunning());
		assertFalse(ic.isRunning());
	}

	@Test
	public void testStopJam_lineupRunning() {
		lc.setTime(14000);
		lc.setNumber(9);
		lc.start();
		
		sbm.stopJamTO();
		advance(0);
		
		assertEquals(null, sbm.snapshot);
		assertTrue(lc.isRunning());
		assertEquals(9, lc.getNumber());
		assertEquals(14000, lc.getTime());
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
		advance(0);
		
		sbm.timeout();
		advance(0);
		
		assertEquals(DefaultScoreBoardModel.ACTION_TIMEOUT, sbm.snapshot.getType());
		assertFalse(pc.isRunning());
		assertFalse(jc.isRunning());
		assertFalse(lc.isRunning());
		assertTrue(tc.isRunning());
		assertTrue(tc.isTimeAtStart());
		assertEquals(3, tc.getNumber());
		assertFalse(ic.isRunning());
		assertEquals("", sbm.getTimeoutOwner());
		assertFalse(sbm.isOfficialReview());
	}

	@Test
	public void testTimeout_fromJam() {
		pc.start();
		jc.start();
		assertFalse(lc.isRunning());
		assertFalse(tc.isRunning());
		assertFalse(ic.isRunning());
		advance(0);
		
		sbm.timeout();
		advance(0);
		
		assertEquals(DefaultScoreBoardModel.ACTION_TIMEOUT, sbm.snapshot.getType());
		assertFalse(pc.isRunning());
		assertFalse(jc.isRunning());
		assertFalse(lc.isRunning());
		assertTrue(tc.isRunning());
		assertTrue(tc.isTimeAtStart());
		assertFalse(ic.isRunning());
	}

	@Test
	public void testTimeout_fromIntermission() {
		assertFalse(pc.isRunning());
		assertFalse(jc.isRunning());
		assertFalse(lc.isRunning());
		assertFalse(tc.isRunning());
		ic.start();
		advance(0);
		
		sbm.timeout();
		advance(0);
		
		assertEquals(DefaultScoreBoardModel.ACTION_TIMEOUT, sbm.snapshot.getType());
		assertFalse(pc.isRunning());
		assertFalse(jc.isRunning());
		assertFalse(lc.isRunning());
		assertTrue(tc.isRunning());
		assertTrue(tc.isTimeAtStart());
		assertFalse(ic.isRunning());
	}

	@Test
	public void testTimeout_AfterGame() {
		assertFalse(pc.isRunning());
		assertFalse(jc.isRunning());
		assertFalse(lc.isRunning());
		assertFalse(tc.isRunning());
		assertFalse(ic.isRunning());
		advance(0);
		
		sbm.timeout();
		advance(0);
		
		assertEquals(DefaultScoreBoardModel.ACTION_TIMEOUT, sbm.snapshot.getType());
		assertFalse(pc.isRunning());
		assertFalse(jc.isRunning());
		assertFalse(lc.isRunning());
		assertTrue(tc.isRunning());
		assertTrue(tc.isTimeAtStart());
		assertFalse(ic.isRunning());
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
		sbm.setTimeoutOwner("");
		advance(0);
		
		sbm.timeout();
		advance(0);
		
		assertEquals(DefaultScoreBoardModel.ACTION_TIMEOUT, sbm.snapshot.getType());
		assertFalse(pc.isRunning());
		assertFalse(jc.isRunning());
		assertFalse(lc.isRunning());
		assertTrue(tc.isRunning());
		assertEquals(24000, tc.getTime());
		assertEquals(7, tc.getNumber());
		assertFalse(ic.isRunning());
		assertEquals("O", sbm.getTimeoutOwner());
		
		sbm.timeout();
		advance(0);
		
		assertEquals("", sbm.getTimeoutOwner());
	}

	@Test
	public void testSetTimeoutType() {
		sbm.setTimeoutOwner("");
		advance(0);
		
		sbm.startTimeoutType("2", false);
		advance(0);

		assertEquals(DefaultScoreBoardModel.ACTION_TIMEOUT, sbm.snapshot.getType());
		assertFalse(pc.isRunning());
		assertFalse(jc.isRunning());
		assertFalse(lc.isRunning());
		assertTrue(tc.isRunning());
		assertFalse(ic.isRunning());
		assertEquals("2", sbm.getTimeoutOwner());
		assertFalse(sbm.isOfficialReview());

		sbm.startTimeoutType("1", true);
		advance(0);
		assertEquals("1", sbm.getTimeoutOwner());
		assertTrue(sbm.isOfficialReview());
	}
	
	@Test
	public void testClockUndo() {
		pc.start();
		jc.start();
		sbm.setInPeriod(true);
		advance(0);
		assertFalse(lc.isRunning());
		assertFalse(tc.isRunning());
		assertFalse(ic.isRunning());
		assertEquals("", sbm.getTimeoutOwner());
		assertFalse(sbm.isOfficialReview());
		assertFalse(sbm.isInOvertime());
		assertTrue(sbm.isInPeriod());

		sbm.createSnapshot("TEST");
		assertEquals("TEST", sbm.snapshot.getType());
		
		pc.stop();
		jc.stop();
		lc.start();
		tc.start();
		ic.start();
		sbm.setTimeoutOwner("TestOwner");
		sbm.setOfficialReview(true);
		sbm.setInOvertime(true);
		sbm.setInPeriod(false);
		advance(2000);
		
		sbm.clockUndo();
		advance(0);
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
	}
	
	@Test
	public void testPeriodClockEnd_duringLineup() {
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
		advance(0);
		
		advance(2000);
		
		assertFalse(pc.isRunning());
		assertEquals(2, pc.getNumber());
		assertFalse(jc.isRunning());
		assertFalse(lc.isRunning());
		assertFalse(tc.isRunning());
		assertTrue(ic.isRunning());
		assertTrue(ic.isTimeAtStart());
		assertEquals(2, ic.getNumber());
	}
	
	@Test
	public void testPeriodClockEnd_duringJam() {
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
		advance(0);
		
		advance(2000);
		
		assertFalse(pc.isRunning());
		assertTrue(jc.isRunning());
		assertEquals(17, jc.getNumber());
		assertEquals(8000, jc.getTime());
		assertFalse(lc.isRunning());
		assertFalse(tc.isRunning());
		assertFalse(ic.isRunning());
	}
	
	@Test
	public void testJamClockEnd() {
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
	}

	@Test
	public void testIntermissionClockEnd_notLastPeriod() {
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
		advance(0);
		
		advance(3000);
		
		assertFalse(pc.isRunning());
		assertTrue(pc.isTimeAtStart());
		assertEquals(2, pc.getNumber());
		assertFalse(jc.isRunning());
		assertEquals(0, jc.getNumber());
		assertFalse(lc.isRunning());
		assertFalse(tc.isRunning());
		assertFalse(ic.isRunning());
		assertTrue(ic.isTimeAtEnd());
		assertEquals(1, ic.getNumber());
	}

	@Test
	public void testIntermissionClockEnd_lastPeriod() {
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
		advance(0);
		
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
		advance(0);
		assertFalse(pc.isRunning());

		//jam ended after 30s mark, official timeout
		sbm.startJam();
		advance(0);
		sbm.stopJamTO();
		assertEquals(25000, pc.getTime());
		assertTrue(pc.isRunning());
		advance(1000);
		sbm.timeout();
		advance(35000);
		sbm.stopJamTO();
		advance(0);
		assertTrue(pc.isRunning());
		
		//follow up with team timeout
		advance(2000);
		sbm.startTimeoutType("1", false);
		advance(60000);
		sbm.stopJamTO();
		advance(0);
		assertFalse(pc.isRunning());
		assertEquals(22000, pc.getTimeRemaining());
	}

	@Test
	public void testResetDoesntAffectFrontendSettings() {
		sbm.getFrontendSettingsModel().set("foo", "bar");
		sbm.reset();
		assertEquals("bar", sbm.getFrontendSettings().get("foo"));
	}
}
