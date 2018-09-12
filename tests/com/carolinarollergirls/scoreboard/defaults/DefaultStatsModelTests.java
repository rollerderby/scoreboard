package com.carolinarollergirls.scoreboard.defaults;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.Arrays;

import com.carolinarollergirls.scoreboard.Clock;
import com.carolinarollergirls.scoreboard.Position;
import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.Team;
import com.carolinarollergirls.scoreboard.defaults.DefaultClockModel;
import com.carolinarollergirls.scoreboard.event.AsyncScoreBoardListener;
import com.carolinarollergirls.scoreboard.jetty.JettyServletScoreBoardController;
import com.carolinarollergirls.scoreboard.model.ClockModel;
import com.carolinarollergirls.scoreboard.model.ScoreBoardModel;
import com.carolinarollergirls.scoreboard.model.StatsModel;
import com.carolinarollergirls.scoreboard.model.TeamModel;

public class DefaultStatsModelTests {

  private ScoreBoardModel sbm;
  private StatsModel sm;
  private TeamModel team1;

  private final String ID_PREFIX = "00000000-0000-0000-0000-000000000";

  @Before
  public void setUp() throws Exception {
    DefaultClockModel.updateClockTimerTask.setPaused(true);

    ScoreBoardManager.setPropertyOverride(JettyServletScoreBoardController.class.getName() + ".html.dir", "html");
    sbm = new DefaultScoreBoardModel();
    sm = sbm.getStatsModel();

    // Add a full roster for each team.
    // Skater numbers are 100..114 and 200..214.
    for(String tid : Arrays.asList(Team.ID_1, Team.ID_2)) {
      TeamModel tm = sbm.getTeamModel(tid);
      for (int i = 0; i <= 15; i++) {
        String number = String.format("%s%02d", tid, i);
        tm.addSkaterModel(ID_PREFIX + number, "name-" + number, number, "");
      }
    }
    team1 = sbm.getTeamModel(Team.ID_1);
  }

  @After
  public void tearDown() throws Exception {
    DefaultClockModel.updateClockTimerTask.setPaused(false);
  }

  private void advance(long time_ms) {
    AsyncScoreBoardListener.waitForEvents();
    DefaultClockModel.updateClockTimerTask.advance(time_ms);
    AsyncScoreBoardListener.waitForEvents();
  }

  @Test
  public void testJamsAndPeriodsCreated() {
    // Start with no periods.
    assertEquals(0, sm.getPeriodStats().size());

    // Starting a jam creates a period and a jam.
    sbm.startJam();
    advance(1000);
    assertEquals(1, sm.getPeriodStats().size());
    StatsModel.PeriodStatsModel psm = sm.getPeriodStatsModel(1);
    assertEquals(1, psm.getJamStats().size());


    // Start the second jam and confirm it's there.
    sbm.stopJamTO();
    advance(0);
    sbm.startJam();
    advance(1000);
    assertEquals(2, psm.getJamStats().size());
  }

  @Test
  public void testJamsAndPeriodsTruncated() {
    // Put us in period 2 with 3 jams;
    ClockModel pc = sbm.getClockModel(Clock.ID_PERIOD);
    ClockModel jc = sbm.getClockModel(Clock.ID_JAM);
    pc.setNumber(2);
    for (int i = 0; i < 3; i++) {
      sbm.startJam();
      advance(1000);
      sbm.stopJamTO();
      advance(0);
    }
    assertEquals(2, sm.getPeriodStats().size());
    StatsModel.PeriodStatsModel psm = sm.getPeriodStatsModel(2);
    assertEquals(3, psm.getJamStats().size());

    // Truncate jams.
    jc.setNumber(1);
    advance(0);
    assertEquals(1, psm.getJamStats().size());
  }

  @Test
  public void testJamStartListener() {
    team1.getSkaterModel(ID_PREFIX + "100").setPosition(Position.ID_JAMMER);
    team1.getSkaterModel(ID_PREFIX + "101").setPosition(Position.ID_PIVOT);
    advance(0);

    sbm.startJam();
    advance(1000);

    // Confirm stats are as expected at start of first jam.
    StatsModel.PeriodStatsModel psm = sm.getPeriodStatsModel(1);
    StatsModel.JamStatsModel jsm = psm.getJamStatsModel(1);
    StatsModel.TeamStatsModel tsm1 = jsm.getTeamStatsModel(Team.ID_1);
    assertEquals(0, jsm.getPeriodClockElapsedStart());
    assertEquals(0, tsm1.getTotalScore());
    assertEquals(0, tsm1.getJamScore());
    assertEquals(Team.LEAD_NO_LEAD, tsm1.getLeadJammer());
    assertEquals(false, tsm1.getStarPass());
    assertEquals(3, tsm1.getTimeouts());
    assertEquals(1, tsm1.getOfficialReviews());
    assertEquals(Position.ID_JAMMER, tsm1.getSkaterStatsModel(ID_PREFIX + "100").getPosition());
    assertEquals(Position.ID_PIVOT, tsm1.getSkaterStatsModel(ID_PREFIX + "101").getPosition());
    assertEquals(null, tsm1.getSkaterStatsModel(ID_PREFIX + "114"));

    // Team 1 gets lead and scores.
    team1.setLeadJammer(Team.LEAD_LEAD);
    team1.changeScore(5);
    advance(0);

    sbm.stopJamTO();
    advance(1000);
    sbm.startJam();
    advance(1000);

    // Confirm stats at start of second jam.
    jsm = psm.getJamStatsModel(2);
    tsm1 = jsm.getTeamStatsModel(Team.ID_1);
    assertEquals(2000, jsm.getPeriodClockElapsedStart());
    assertEquals(5, tsm1.getTotalScore());
    assertEquals(0, tsm1.getJamScore());
    assertEquals(Team.LEAD_NO_LEAD, tsm1.getLeadJammer());
    assertEquals(false, tsm1.getStarPass());
    assertEquals(3, tsm1.getTimeouts());
    assertEquals(1, tsm1.getOfficialReviews());
    // No skaters have been entered for this jam yet.
    assertEquals(null, tsm1.getSkaterStatsModel(ID_PREFIX + "100"));
    assertEquals(null, tsm1.getSkaterStatsModel(ID_PREFIX + "101"));
  }

  @Test
  public void testJamStopListener() {
    sbm.startJam();
    advance(1000);
    sbm.stopJamTO();
    advance(1000);

    // Confirm stats are as expected at end of first jam.
    StatsModel.PeriodStatsModel psm = sm.getPeriodStatsModel(1);
    StatsModel.JamStatsModel jsm = psm.getJamStatsModel(1);
    assertEquals(1000, jsm.getJamClockElapsedEnd());
    assertEquals(1000, jsm.getPeriodClockElapsedEnd());

    sbm.startJam();
    advance(1000);
    sbm.stopJamTO();
    advance(1000);
    jsm = psm.getJamStatsModel(2);
    assertEquals(1000, jsm.getJamClockElapsedEnd());
    assertEquals(3000, jsm.getPeriodClockElapsedEnd());
  }

  @Test
  public void testTeamEventListener() {
    sbm.startJam();
    advance(1000);

    StatsModel.PeriodStatsModel psm = sm.getPeriodStatsModel(1);
    StatsModel.JamStatsModel jsm = psm.getJamStatsModel(1);
    StatsModel.TeamStatsModel tsm1 = jsm.getTeamStatsModel(Team.ID_1);

    // Change the score during the jam.
    team1.changeScore(5);
    advance(0);
    assertEquals(5, tsm1.getTotalScore());
    assertEquals(5, tsm1.getJamScore());

    // Star pass during the jam.
    team1.setStarPass(true);
    advance(0);
    assertEquals(true, tsm1.getStarPass());

    // Lead during the jam.
    team1.setLeadJammer(Team.LEAD_LEAD);
    advance(0);
    assertEquals(Team.LEAD_LEAD, tsm1.getLeadJammer());

    sbm.stopJamTO();
    advance(1000);
    // Star pass and lead still correct after jam end.
    assertEquals(true, tsm1.getStarPass());
    assertEquals(Team.LEAD_LEAD, tsm1.getLeadJammer());

    // Some points arrive after end of jam.
    team1.changeScore(4);
    advance(0);
    assertEquals(9, tsm1.getTotalScore());
    assertEquals(9, tsm1.getJamScore());

    // Timeout at end of jam.
    team1.timeout();
    advance(1000);
    assertEquals(2, tsm1.getTimeouts());

    // Official review at end of jam.
    team1.officialReview();
    advance(1000);
    assertEquals(0, tsm1.getOfficialReviews());

    // Start jam 2, confirm score.
    sbm.startJam();
    advance(1000);
    jsm = psm.getJamStatsModel(2);
    tsm1 = jsm.getTeamStatsModel(Team.ID_1);
    assertEquals(9, tsm1.getTotalScore());
    assertEquals(0, tsm1.getJamScore());

    // Add points during the jam, jam score is correct.
    team1.changeScore(3);
    advance(0);
    assertEquals(12, tsm1.getTotalScore());
    assertEquals(3, tsm1.getJamScore());
  }

  @Test
  public void testSkaterEventListener() {
    // Some skater positions set before jam.
    team1.getSkaterModel(ID_PREFIX + "100").setPosition(Position.ID_JAMMER);
    team1.getSkaterModel(ID_PREFIX + "101").setPosition(Position.ID_PIVOT);
    advance(0);

    sbm.startJam();
    advance(1000);

    StatsModel.PeriodStatsModel psm = sm.getPeriodStatsModel(1);
    StatsModel.JamStatsModel jsm = psm.getJamStatsModel(1);
    StatsModel.TeamStatsModel tsm1 = jsm.getTeamStatsModel(Team.ID_1);

    // Blocker is added after the jam stats. All positions in place.
    team1.getSkaterModel(ID_PREFIX + "102").setPosition(Position.ID_BLOCKER1);
    advance(0);
    assertEquals(Position.ID_JAMMER, tsm1.getSkaterStatsModel(ID_PREFIX + "100").getPosition());
    assertEquals(Position.ID_PIVOT, tsm1.getSkaterStatsModel(ID_PREFIX + "101").getPosition());
    assertEquals(Position.ID_BLOCKER1, tsm1.getSkaterStatsModel(ID_PREFIX + "102").getPosition());

    // Skater was actually on bench.
    team1.getSkaterModel(ID_PREFIX + "102").setPosition(Position.ID_BENCH);
    advance(0);
    assertEquals(null, tsm1.getSkaterStatsModel(ID_PREFIX + "102"));

    // Skater goes to the box.
    team1.getSkaterModel(ID_PREFIX + "100").setPenaltyBox(true);
    advance(0);
    assertEquals(true, tsm1.getSkaterStatsModel(ID_PREFIX + "100").getPenaltyBox());

    // Skater exits the box.
    team1.getSkaterModel(ID_PREFIX + "100").setPenaltyBox(false);
    advance(0);
    assertEquals(false, tsm1.getSkaterStatsModel(ID_PREFIX + "100").getPenaltyBox());

    // Jam ends.
    sbm.stopJamTO();
    advance(1000);
    // New jammer does not replace jammer from previous jam.
    team1.getSkaterModel(ID_PREFIX + "103").setPosition(Position.ID_JAMMER);
    advance(0);
    assertEquals(Position.ID_JAMMER, tsm1.getSkaterStatsModel(ID_PREFIX + "100").getPosition());
  }

}
