package com.carolinarollergirls.scoreboard.json;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;
import com.carolinarollergirls.scoreboard.defaults.DefaultScoreBoardModel;
import com.carolinarollergirls.scoreboard.jetty.JettyServletScoreBoardController;
import com.carolinarollergirls.scoreboard.view.Clock;
import com.carolinarollergirls.scoreboard.view.Position;
import com.carolinarollergirls.scoreboard.view.Team;

public class ScoreBoardJSONListenerTests {

	private DefaultScoreBoardModel sbm;
	private JSONStateManager jsm;

	private Map<String, Object> state;
	private JSONStateListener jsonListener = new JSONStateListener() {
		@Override
		public void sendUpdates(Map<String, Object> s, Set<String> changed) {
			state = s;
		}
	};

	@Before
	public void setUp() throws Exception {
		ScoreBoardClock.getInstance().stop();
		ScoreBoardManager.setPropertyOverride(JettyServletScoreBoardController.class.getName() + ".html.dir", "html");
		sbm = new DefaultScoreBoardModel();

		jsm = new JSONStateManager();
		new ScoreBoardJSONListener(sbm, jsm);
		jsm.register(jsonListener);
	}

	@After
	public void tearDown() throws Exception {
		// Make sure events are still flowing through the ScoreBoardJSONListener.
		sbm.getFrontendSettingsModel().set("teardownTest", "foo");
		advance(0);
		assertEquals("foo", state.get("ScoreBoard.FrontendSettings.teardownTest"));
		ScoreBoardClock.getInstance().start(false);
	}

	private void advance(long time_ms) {
		ScoreBoardClock.getInstance().advance(time_ms);
		jsm.waitForSent();
	}

	@Test
	public void testScoreBoardEvents() {
		assertEquals("00000000-0000-0000-0000-000000000000", state.get("ScoreBoard.Ruleset"));
		assertEquals(false, state.get("ScoreBoard.InPeriod"));
		assertEquals(false, state.get("ScoreBoard.InOvertime"));
		assertEquals(false, state.get("ScoreBoard.OfficialScore"));
		assertEquals(false, state.get("ScoreBoard.OfficialReview"));

		sbm.setInPeriod(true);
		advance(0);
		assertEquals(true, state.get("ScoreBoard.InPeriod"));

		sbm.setInOvertime(true);
		advance(0);
		assertEquals(true, state.get("ScoreBoard.InOvertime"));

		sbm.setOfficialScore(true);
		advance(0);
		assertEquals(true, state.get("ScoreBoard.OfficialScore"));

		sbm.setOfficialReview(true);
		advance(0);
		assertEquals(true, state.get("ScoreBoard.OfficialReview"));
	}

	@Test
	public void testTeamEvents() {
		sbm.startJam();
		advance(0);

		sbm.getTeamModel("1").changeScore(5);
		advance(0);
		assertEquals(0, state.get("ScoreBoard.Team(1).LastScore"));
		assertEquals(5, state.get("ScoreBoard.Team(1).Score"));
		assertEquals(5, state.get("ScoreBoard.Team(1).JamScore"));

		sbm.getTeamModel("1").setStarPass(true);
		advance(0);
		assertEquals(true, state.get("ScoreBoard.Team(1).StarPass"));

		sbm.getTeamModel("1").setLeadJammer(Team.LEAD_LEAD);
		advance(0);
		assertEquals("Lead", state.get("ScoreBoard.Team(1).LeadJammer"));

		sbm.getTeamModel("1").setTimeouts(2);
		sbm.getTeamModel("1").setOfficialReviews(1);
		sbm.getTeamModel("1").setRetainedOfficialReview(true);
		advance(0);
		assertEquals(2, state.get("ScoreBoard.Team(1).Timeouts"));
		assertEquals(1, state.get("ScoreBoard.Team(1).OfficialReviews"));
		assertEquals(true, state.get("ScoreBoard.Team(1).RetainedOfficialReview"));

		sbm.getTeamModel("1").setInOfficialReview(true);
		sbm.getTeamModel("1").setInTimeout(false);
		advance(0);
		assertEquals(true, state.get("ScoreBoard.Team(1).InOfficialReview"));
		assertEquals(false, state.get("ScoreBoard.Team(1).InTimeout"));

		sbm.getTeamModel("1").setInOfficialReview(false);
		sbm.getTeamModel("1").setInTimeout(true);
		advance(0);
		assertEquals(false, state.get("ScoreBoard.Team(1).InOfficialReview"));
		assertEquals(true, state.get("ScoreBoard.Team(1).InTimeout"));

		sbm.getTeamModel("1").setName("ATeam");
		sbm.getTeamModel("1").setLogo("ATeamLogo");
		advance(0);
		assertEquals("ATeam", state.get("ScoreBoard.Team(1).Name"));
		assertEquals("ATeamLogo", state.get("ScoreBoard.Team(1).Logo"));

		sbm.getTeamModel("1").setAlternateNameModel("overlay", "AT");
		advance(0);
		assertEquals("AT", state.get("ScoreBoard.Team(1).AlternateName(overlay)"));

		sbm.getTeamModel("1").setAlternateNameModel("overlay", "AT");
		advance(0);
		assertEquals("AT", state.get("ScoreBoard.Team(1).AlternateName(overlay)"));
		sbm.getTeamModel("1").removeAlternateNameModel("overlay");
		advance(0);
		assertEquals(null, state.get("ScoreBoard.Team(1).AlternateName(overlay)"));

		sbm.getTeamModel("1").setColorModel("overlay", "red");
		advance(0);
		assertEquals("red", state.get("ScoreBoard.Team(1).Color(overlay)"));
		sbm.getTeamModel("1").removeColorModel("overlay");
		advance(0);
		assertEquals(null, state.get("ScoreBoard.Team(1).Color(overlay)"));
	}

	@Test
	public void testSkaterAndPositionEvents() {
		sbm.startJam();
		advance(0);

		String id = "00000000-0000-0000-0000-000000000001";

		sbm.getTeamModel("1").addSkaterModel(id, "Uno", "01", "");
		advance(0);
		assertEquals("Uno", state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Name"));
		assertEquals("01", state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Number"));
		assertEquals("", state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Flags"));
		assertEquals("Bench", state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Position"));
		assertEquals(false, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).PenaltyBox"));

		sbm.getTeamModel("1").getSkaterModel(id).setPosition(Position.ID_JAMMER);
		sbm.getTeamModel("1").getSkaterModel(id).setPenaltyBox(true);
		advance(0);
		assertEquals("Jammer", state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Position"));
		assertEquals(true, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).PenaltyBox"));
		assertEquals("00000000-0000-0000-0000-000000000001", state.get("ScoreBoard.Team(1).Position(Jammer).Skater"));
		assertEquals(true, state.get("ScoreBoard.Team(1).Position(Jammer).PenaltyBox"));

		sbm.getTeamModel("1").removeSkaterModel(id);
		advance(0);
		assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Name"));
		assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Number"));
		assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Flags"));
		assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Position"));
		assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).PenaltyBox"));
		assertEquals(null, state.get("ScoreBoard.Team(1).Position(Jammer).Skater"));
		assertEquals(false, state.get("ScoreBoard.Team(1).Position(Jammer).PenaltyBox"));
	}

	@Test
	public void testPenaltyEvents() {
		String sid = "00000000-0000-0000-0000-000000000001";
		String pid = "00000000-0000-0000-0000-000000000002";

		sbm.getTeamModel("1").addSkaterModel(sid, "Uno", "01", "");
		sbm.penalty("1", sid, pid, false, 1, 2, "X");
		advance(0);
		assertEquals(pid, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).Id"));
		assertEquals(1, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).Period"));
		assertEquals(2, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).Jam"));
		assertEquals("X", state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).Code"));

		sbm.penalty("1", sid, pid, false, 1, 2, null);
		advance(0);
		assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).Id"));
		assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).Period"));
		assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).Jam"));
		assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(1).Code"));

		sbm.penalty("1", sid, null, true, 1, 2, "B");
		advance(0);
		assertEquals(1, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(FO_EXP).Period"));
		assertEquals(2, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(FO_EXP).Jam"));
		assertEquals("B", state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(FO_EXP).Code"));

		sbm.penalty("1", sid, null, true, 1, 2, null);
		advance(0);
		assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(FO_EXP).Period"));
		assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(FO_EXP).Period"));
		assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(FO_EXP).Jam"));
		assertEquals(null, state.get("ScoreBoard.Team(1).Skater(00000000-0000-0000-0000-000000000001).Penalty(FO_EXP).Code"));
	}

	@Test
	public void testStatsEvents() {
		String id = "00000000-0000-0000-0000-000000000001";

		sbm.getTeamModel("1").addSkaterModel(id, "Uno", "01", "");
		sbm.getTeamModel("1").getSkaterModel(id).setPosition(Position.ID_JAMMER);
		sbm.startJam();
		advance(2000);

		assertEquals(0L, state.get("ScoreBoard.Stats.Period(1).Jam(1).PeriodClockElapsedStart"));
		assertEquals(0, state.get("ScoreBoard.Stats.Period(1).Jam(1).Team(1).JamScore"));
		assertEquals(0, state.get("ScoreBoard.Stats.Period(1).Jam(1).Team(1).TotalScore"));
		assertEquals("NoLead", state.get("ScoreBoard.Stats.Period(1).Jam(1).Team(1).LeadJammer"));
		assertEquals(false, state.get("ScoreBoard.Stats.Period(1).Jam(1).Team(1).StarPass"));
		assertEquals(1, state.get("ScoreBoard.Stats.Period(1).Jam(1).Team(1).OfficialReviews"));
		assertEquals(3, state.get("ScoreBoard.Stats.Period(1).Jam(1).Team(1).Timeouts"));
		assertEquals("00000000-0000-0000-0000-000000000001", state.get("ScoreBoard.Stats.Period(1).Jam(1).Team(1).Skater(00000000-0000-0000-0000-000000000001).Id"));
		assertEquals(false, state.get("ScoreBoard.Stats.Period(1).Jam(1).Team(1).Skater(00000000-0000-0000-0000-000000000001).PenaltyBox"));
		assertEquals("Jammer", state.get("ScoreBoard.Stats.Period(1).Jam(1).Team(1).Skater(00000000-0000-0000-0000-000000000001).Position"));

		sbm.getTeamModel("1").getSkaterModel(id).setPosition(Position.ID_BENCH);
		advance(0);
		assertEquals(null, state.get("ScoreBoard.Stats.Period(1).Jam(1).Team(1).Skater(00000000-0000-0000-0000-000000000001).Id"));
		assertEquals(null, state.get("ScoreBoard.Stats.Period(1).Jam(1).Team(1).Skater(00000000-0000-0000-0000-000000000001).PenaltyBox"));
		assertEquals(null, state.get("ScoreBoard.Stats.Period(1).Jam(1).Team(1).Skater(00000000-0000-0000-0000-000000000001).Position"));

		sbm.getTeamModel("1").getSkaterModel(id).setPosition(Position.ID_JAMMER);
		advance(0);
		assertEquals("Jammer", state.get("ScoreBoard.Stats.Period(1).Jam(1).Team(1).Skater(00000000-0000-0000-0000-000000000001).Position"));
		sbm.getTeamModel("1").removeSkaterModel(id);
		advance(0);
		assertEquals(null, state.get("ScoreBoard.Stats.Period(1).Jam(1).Team(1).Skater(00000000-0000-0000-0000-000000000001).Id"));
		assertEquals(null, state.get("ScoreBoard.Stats.Period(1).Jam(1).Team(1).Skater(00000000-0000-0000-0000-000000000001).PenaltyBox"));
		assertEquals(null, state.get("ScoreBoard.Stats.Period(1).Jam(1).Team(1).Skater(00000000-0000-0000-0000-000000000001).Position"));

		sbm.stopJamTO();
		advance(1000);
		assertEquals(2000L, state.get("ScoreBoard.Stats.Period(1).Jam(1).JamClockElapsedEnd"));
		assertEquals(2000L, state.get("ScoreBoard.Stats.Period(1).Jam(1).PeriodClockElapsedEnd"));

		sbm.getClockModel(Clock.ID_PERIOD).setNumber(2);
		sbm.getClockModel(Clock.ID_JAM).setNumber(3);
		sbm.startJam();
		advance(1000);
		sbm.stopJamTO();
		advance(1000);
		sbm.startJam();
		advance(1000);
		sbm.stopJamTO();
		advance(1000);
		assertEquals(3000L, state.get("ScoreBoard.Stats.Period(2).Jam(4).PeriodClockElapsedStart"));
		assertEquals(5000L, state.get("ScoreBoard.Stats.Period(2).Jam(5).PeriodClockElapsedStart"));
		// Remove a jam.
		sbm.getClockModel(Clock.ID_JAM).setNumber(4);
		advance(0);
		assertEquals(3000L, state.get("ScoreBoard.Stats.Period(2).Jam(4).PeriodClockElapsedStart"));
		assertEquals(null, state.get("ScoreBoard.Stats.Period(2).Jam(5).PeriodClockElapsedStart"));
		// Remove a period.
		sbm.getClockModel(Clock.ID_PERIOD).setNumber(1);
		advance(0);
		assertEquals(null, state.get("ScoreBoard.Stats.Period(2).Jam(4).PeriodClockElapsedStart"));
		assertEquals(null, state.get("ScoreBoard.Stats.Period(2).Jam(5).PeriodClockElapsedStart"));

	}

}
