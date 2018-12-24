package com.carolinarollergirls.scoreboard.core.impl;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.Arrays;

import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.FloorPosition;
import com.carolinarollergirls.scoreboard.core.Role;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Stats;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.impl.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class StatsImplTests {

    private ScoreBoard sb;
    private Stats stats;
    private Team team1;

    private final String ID_PREFIX = "00000000-0000-0000-0000-000000000";

    @Before
    public void setUp() throws Exception {
        ScoreBoardClock.getInstance().stop();

        sb = new ScoreBoardImpl();
        sb.getSettings().set(ScoreBoard.SETTING_CLOCK_AFTER_TIMEOUT, "Lineup");
        stats = sb.getStats();

        // Add a full roster for each team.
        // Skater numbers are 100..114 and 200..214.
        for(String tid : Arrays.asList(Team.ID_1, Team.ID_2)) {
            Team tm = sb.getTeam(tid);
            for (int i = 0; i <= 15; i++) {
                String number = String.format("%s%02d", tid, i);
                tm.addSkater(ID_PREFIX + number, "name-" + number, number, "");
            }
        }
        team1 = sb.getTeam(Team.ID_1);
    }

    @After
    public void tearDown() throws Exception {
        ScoreBoardClock.getInstance().start(false);
    }

    private void advance(long time_ms) {
        ScoreBoardClock.getInstance().advance(time_ms);
    }

    @Test
    public void testJamsAndPeriodsCreated() {
        // Start with no periods.
        assertEquals(0, stats.getPeriodStats().size());

        // Starting a jam creates a period and a jam.
        sb.startJam();
        advance(1000);
        assertEquals(1, stats.getPeriodStats().size());
        Stats.PeriodStats psm = stats.getPeriodStats(1);
        assertEquals(1, psm.getJamStats().size());


        // Start the second jam and confirm it's there.
        sb.stopJamTO();
        sb.startJam();
        advance(1000);
        assertEquals(2, psm.getJamStats().size());
    }

    @Test
    public void testJamsAndPeriodsTruncated() {
        // Put us in period 2 with 3 jams;
        Clock pc = sb.getClock(Clock.ID_PERIOD);
        Clock jc = sb.getClock(Clock.ID_JAM);
        pc.setNumber(2);
        for (int i = 0; i < 3; i++) {
            sb.startJam();
            advance(1000);
            sb.stopJamTO();
        }
        assertEquals(2, stats.getPeriodStats().size());
        Stats.PeriodStats psm = stats.getPeriodStats(2);
        assertEquals(3, psm.getJamStats().size());

        // Truncate jams.
        jc.setNumber(1);
        assertEquals(1, psm.getJamStats().size());
    }

    @Test
    public void testJamStartListener() {
        team1.field(team1.getSkater(ID_PREFIX + "100"), Role.JAMMER);
        team1.field(team1.getSkater(ID_PREFIX + "101"), Role.PIVOT);

        sb.startJam();
        advance(1000);

        // Confirm stats are as expected at start of first jam.
        Stats.PeriodStats psm = stats.getPeriodStats(1);
        Stats.JamStats jsm = psm.getJamStats(1);
        Stats.TeamStats tsm1 = jsm.getTeamStats(Team.ID_1);
        assertEquals(0, jsm.getPeriodClockElapsedStart());
        assertEquals(0, tsm1.getTotalScore());
        assertEquals(0, tsm1.getJamScore());
        assertEquals(Team.LEAD_NO_LEAD, tsm1.getLeadJammer());
        assertEquals(false, tsm1.getStarPass());
        assertEquals(3, tsm1.getTimeouts());
        assertEquals(1, tsm1.getOfficialReviews());
        assertEquals(FloorPosition.JAMMER.toString(), tsm1.getSkaterStats(ID_PREFIX + "100").getPosition());
        assertEquals(FloorPosition.PIVOT.toString(), tsm1.getSkaterStats(ID_PREFIX + "101").getPosition());
        assertEquals(null, tsm1.getSkaterStats(ID_PREFIX + "114"));

        // Team 1 gets lead and scores.
        team1.setLeadJammer(Team.LEAD_LEAD);
        team1.changeScore(5);

        sb.stopJamTO();
        advance(1000);
        sb.startJam();
        advance(1000);

        // Confirm stats at start of second jam.
        jsm = psm.getJamStats(2);
        tsm1 = jsm.getTeamStats(Team.ID_1);
        assertEquals(2000, jsm.getPeriodClockElapsedStart());
        assertEquals(5, tsm1.getTotalScore());
        assertEquals(0, tsm1.getJamScore());
        assertEquals(Team.LEAD_NO_LEAD, tsm1.getLeadJammer());
        assertEquals(false, tsm1.getStarPass());
        assertEquals(3, tsm1.getTimeouts());
        assertEquals(1, tsm1.getOfficialReviews());
        // No skaters have been entered for this jam yet.
        assertEquals(null, tsm1.getSkaterStats(ID_PREFIX + "100"));
        assertEquals(null, tsm1.getSkaterStats(ID_PREFIX + "101"));
    }

    @Test
    public void testJamStopListener() {
        sb.startJam();
        advance(1000);
        sb.stopJamTO();
        advance(1000);

        // Confirm stats are as expected at end of first jam.
        Stats.PeriodStats psm = stats.getPeriodStats(1);
        Stats.JamStats jsm = psm.getJamStats(1);
        assertEquals(1000, jsm.getJamClockElapsedEnd());
        assertEquals(1000, jsm.getPeriodClockElapsedEnd());

        sb.startJam();
        advance(1000);
        sb.stopJamTO();
        advance(1000);
        jsm = psm.getJamStats(2);
        assertEquals(1000, jsm.getJamClockElapsedEnd());
        assertEquals(3000, jsm.getPeriodClockElapsedEnd());
    }

    @Test
    public void testTeamEventListener() {
        sb.startJam();
        advance(1000);

        Stats.PeriodStats psm = stats.getPeriodStats(1);
        Stats.JamStats jsm = psm.getJamStats(1);
        Stats.TeamStats tsm1 = jsm.getTeamStats(Team.ID_1);

        // Change the score during the jam.
        team1.changeScore(5);
        assertEquals(5, tsm1.getTotalScore());
        assertEquals(5, tsm1.getJamScore());

        // Star pass during the jam.
        team1.setStarPass(true);
        assertEquals(true, tsm1.getStarPass());

        // Lead during the jam.
        team1.setLeadJammer(Team.LEAD_LEAD);
        assertEquals(Team.LEAD_LEAD, tsm1.getLeadJammer());

        sb.stopJamTO();
        advance(1000);
        // Star pass and lead still correct after jam end.
        assertEquals(true, tsm1.getStarPass());
        assertEquals(Team.LEAD_LEAD, tsm1.getLeadJammer());

        // Some points arrive after end of jam.
        team1.changeScore(4);
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
        sb.startJam();
        advance(1000);
        jsm = psm.getJamStats(2);
        tsm1 = jsm.getTeamStats(Team.ID_1);
        assertEquals(9, tsm1.getTotalScore());
        assertEquals(0, tsm1.getJamScore());

        // Add points during the jam, jam score is correct.
        team1.changeScore(3);
        assertEquals(12, tsm1.getTotalScore());
        assertEquals(3, tsm1.getJamScore());
    }

    @Test
    public void testSkaterEventListener() {
        // Some skater positions set before jam.
        team1.field(team1.getSkater(ID_PREFIX + "100"), Role.JAMMER);
        team1.field(team1.getSkater(ID_PREFIX + "101"), Role.PIVOT);

        sb.startJam();
        advance(1000);

        Stats.PeriodStats psm = stats.getPeriodStats(1);
        Stats.JamStats jsm = psm.getJamStats(1);
        Stats.TeamStats tsm1 = jsm.getTeamStats(Team.ID_1);

        // Blocker is added after the jam starts. All positions in place.
        team1.field(team1.getSkater(ID_PREFIX + "102"), team1.getPosition(FloorPosition.BLOCKER1));
        assertEquals(FloorPosition.JAMMER.toString(), tsm1.getSkaterStats(ID_PREFIX + "100").getPosition());
        assertEquals(FloorPosition.PIVOT.toString(), tsm1.getSkaterStats(ID_PREFIX + "101").getPosition());
        assertEquals(FloorPosition.BLOCKER1.toString(), tsm1.getSkaterStats(ID_PREFIX + "102").getPosition());

        // Skater was actually on bench.
        team1.field(team1.getSkater(ID_PREFIX + "102"), Role.BENCH);
        assertEquals(null, tsm1.getSkaterStats(ID_PREFIX + "102"));

        // Skater goes to the box.
        team1.getSkater(ID_PREFIX + "100").setPenaltyBox(true);
        assertEquals(true, tsm1.getSkaterStats(ID_PREFIX + "100").getPenaltyBox());

        // Skater exits the box.
        team1.getSkater(ID_PREFIX + "100").setPenaltyBox(false);
        assertEquals(false, tsm1.getSkaterStats(ID_PREFIX + "100").getPenaltyBox());

        // Jam ends.
        sb.stopJamTO();
        advance(1000);
        // New jammer does not replace jammer from previous jam.
        team1.field(team1.getSkater(ID_PREFIX + "103"), Role.JAMMER);
        assertEquals(FloorPosition.JAMMER.toString(), tsm1.getSkaterStats(ID_PREFIX + "100").getPosition());
    }

}
