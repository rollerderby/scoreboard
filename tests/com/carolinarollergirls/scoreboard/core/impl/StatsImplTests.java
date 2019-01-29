package com.carolinarollergirls.scoreboard.core.impl;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.Arrays;

import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.FloorPosition;
import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Period;
import com.carolinarollergirls.scoreboard.core.Period.Value;
import com.carolinarollergirls.scoreboard.core.Role;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.core.impl.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class StatsImplTests {

    private ScoreBoard sb;
    private Team team1;

    private final String ID_PREFIX = "00000000-0000-0000-0000-000000000";

    @Before
    public void setUp() throws Exception {
        ScoreBoardClock.getInstance().stop();

        sb = new ScoreBoardImpl();
        sb.getSettings().set(ScoreBoard.SETTING_CLOCK_AFTER_TIMEOUT, "Lineup");

        // Add a full roster for each team.
        // Skater numbers are 100..114 and 200..214.
        for(String tid : Arrays.asList(Team.ID_1, Team.ID_2)) {
            Team t = sb.getTeam(tid);
            for (int i = 0; i <= 15; i++) {
                String number = String.format("%s%02d", tid, i);
                t.addSkater(ID_PREFIX + number, "name-" + number, number, "");
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
        // Starting a jam creates a jam and its successor.
        sb.startJam();
        advance(1000);
        Period p = sb.getPeriod(1);
        assertEquals(2, p.getAll(Period.NChild.JAM).size());


        // Start the second jam and confirm it's there.
        sb.stopJamTO();
        sb.startJam();
        advance(1000);
        assertEquals(3, p.getAll(Period.NChild.JAM).size());
    }

    @Test
    public void testJamsAndPeriodsTruncated() {
        // Put us in period 2 with 3 jams;
        Clock pc = sb.getClock(Clock.ID_PERIOD);
        Clock ic = sb.getClock(Clock.ID_INTERMISSION);
        sb.startJam();
        pc.setTime(0);
        sb.stopJamTO();
        ic.setTime(0);
        for (int i = 0; i < 3; i++) {
            sb.startJam();
            advance(1000);
            sb.stopJamTO();
        }
        assertEquals(3, sb.getAll(ScoreBoard.NChild.PERIOD).size());
        Period p = sb.getPeriod(2);
        assertEquals(4, p.getAll(Period.NChild.JAM).size());

        // Truncate jams.
        p.set(Value.CURRENT_JAM_NUMBER, 1);
        p.truncateAfterCurrentJam();
        assertEquals(1, p.getAll(Period.NChild.JAM).size());
    }

    @Test
    public void testJamStartListener() {
	Skater skater1 = team1.getSkater(ID_PREFIX + "100"); 
	Skater skater2 = team1.getSkater(ID_PREFIX + "101"); 
        team1.field(skater1, Role.JAMMER);
        team1.field(skater2, Role.PIVOT);

        sb.startJam();
        advance(1000);

        // Confirm stats are as expected at start of first jam.
        Period p = sb.getPeriod(1);
        Jam j = p.getJam(1);
        TeamJam tj = j.getTeamJam(Team.ID_1);
        assertEquals(0, j.getPeriodClockElapsedStart());
        assertEquals(0, tj.getTotalScore());
        assertEquals(0, tj.getJamScore());
        assertEquals(Team.LEAD_NO_LEAD, tj.getLeadJammer());
        assertEquals(false, tj.isStarPass());
        assertEquals(skater1, tj.getFielding(FloorPosition.JAMMER).getSkater());
        assertEquals(skater2, tj.getFielding(FloorPosition.PIVOT).getSkater());
        assertEquals(null, tj.getFielding(FloorPosition.BLOCKER1).getSkater());

        // Team 1 gets lead and scores.
        team1.setLeadJammer(Team.LEAD_LEAD);
        team1.changeScore(5);

        sb.stopJamTO();
        advance(1000);
        sb.startJam();
        advance(1000);

        // Confirm stats at start of second jam.
        j = p.getJam(2);
        tj = j.getTeamJam(Team.ID_1);
        assertEquals(2000, j.getPeriodClockElapsedStart());
        assertEquals(5, tj.getTotalScore());
        assertEquals(0, tj.getJamScore());
        assertEquals(Team.LEAD_NO_LEAD, tj.getLeadJammer());
        assertEquals(false, tj.isStarPass());
        // No skaters have been entered for this jam yet.
        assertEquals(null, tj.getFielding(FloorPosition.JAMMER).getSkater());
        assertEquals(null, tj.getFielding(FloorPosition.PIVOT).getSkater());
    }

    @Test
    public void testJamStopListener() {
        sb.startJam();
        advance(1000);
        sb.stopJamTO();
        advance(1000);

        // Confirm stats are as expected at end of first jam.
        Period p = sb.getPeriod(1);
        Jam j = p.getJam(1);
        assertEquals(1000, j.getDuration());
        assertEquals(1000, j.getPeriodClockElapsedEnd());

        sb.startJam();
        advance(1000);
        sb.stopJamTO();
        advance(1000);
        j = p.getJam(2);
        assertEquals(1000, j.getDuration());
        assertEquals(3000, j.getPeriodClockElapsedEnd());
    }

    @Test
    public void testTeamEventListener() {
        sb.startJam();
        advance(1000);

        Period p = sb.getPeriod(1);
        Jam j = p.getJam(1);
        TeamJam tj = j.getTeamJam(Team.ID_1);

        // Change the score during the jam.
        team1.changeScore(5);
        assertEquals(5, tj.getTotalScore());
        assertEquals(5, tj.getJamScore());

        // Star pass during the jam.
        team1.setStarPass(true);
        assertEquals(true, tj.isStarPass());

        // Lead during the jam.
        team1.setLeadJammer(Team.LEAD_LEAD);
        assertEquals(Team.LEAD_LEAD, tj.getLeadJammer());

        sb.stopJamTO();
        advance(1000);
        // Star pass and lead still correct after jam end.
        assertEquals(true, tj.isStarPass());
        assertEquals(Team.LEAD_LEAD, tj.getLeadJammer());

        // Some points arrive after end of jam.
        team1.changeScore(4);
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
        sb.startJam();
        advance(1000);
        j = p.getJam(2);
        tj = j.getTeamJam(Team.ID_1);
        assertEquals(9, tj.getTotalScore());
        assertEquals(0, tj.getJamScore());

        // Add points during the jam, jam score is correct.
        team1.changeScore(3);
        assertEquals(12, tj.getTotalScore());
        assertEquals(3, tj.getJamScore());
    }

    @Test
    public void testSkaterEventListener() {
        // Some skater positions set before jam.
	Skater skater1 = team1.getSkater(ID_PREFIX + "100");
	Skater skater2 = team1.getSkater(ID_PREFIX + "101");
	Skater skater3 = team1.getSkater(ID_PREFIX + "102");
	Skater skater4 = team1.getSkater(ID_PREFIX + "103");
        team1.field(skater1, Role.JAMMER);
        team1.field(skater2, Role.PIVOT);

        sb.startJam();
        advance(1000);

        Period p = sb.getPeriod(1);
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
        assertEquals(true, tj.getFielding(FloorPosition.JAMMER).getPenaltyBox());

        // Skater exits the box.
        skater1.setPenaltyBox(false);
        assertEquals(false, tj.getFielding(FloorPosition.JAMMER).getPenaltyBox());

        // Jam ends.
        sb.stopJamTO();
        advance(1000);
        // New jammer does not replace jammer from previous jam.
        team1.field(skater4, Role.JAMMER);
        assertEquals(skater1, tj.getFielding(FloorPosition.JAMMER).getSkater());
    }

}
