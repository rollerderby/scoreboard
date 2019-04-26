package com.carolinarollergirls.scoreboard.core.impl;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.junit.Before;
import org.junit.Test;
import com.carolinarollergirls.scoreboard.core.FloorPosition;
import com.carolinarollergirls.scoreboard.core.Role;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.impl.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.core.impl.TeamImpl;
import com.carolinarollergirls.scoreboard.core.impl.TeamImpl.TeamSnapshotImpl;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class TeamImplTests {

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


    private TeamImpl team;
    private static String ID = Team.ID_1;

    @Before
    public void setUp() throws Exception {
        collectedEvents = new LinkedList<>();

        sb = new ScoreBoardImpl();

        team = (TeamImpl)sb.getTeam(ID);
        ScoreBoardClock.getInstance().stop();
    }

    @Test
    public void testStartJam() {
        team.set(Team.Value.TRIP_SCORE, 34);
        team.set(Team.Value.LEAD, false);
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.Value.LAST_SCORE, listener));
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.Value.LEAD, listener));

        sb.startJam();

        assertEquals(0, team.get(Team.Value.TRIP_SCORE));
        assertFalse(team.isLead());
        assertEquals(1, collectedEvents.size());
    }

    @Test
    public void testStopJam() {
        sb.startJam();
        team.setStarPass(true);

        sb.stopJamTO();
        team.execute(Team.Command.ADVANCE_FIELDINGS);

        assertTrue(team.isStarPass());
        assertFalse(team.isFieldingStarPass());
    }

    @Test
    public void testRestoreSnapshot() {
        sb.startJam();
        assertFalse(team.inTimeout());
        assertFalse(team.inOfficialReview());
        assertEquals(3, team.getTimeouts());
        assertEquals(1, team.getOfficialReviews());
        TeamImpl.TeamSnapshotImpl snapshot = (TeamSnapshotImpl) team.snapshot();

        team.setInTimeout(true);
        team.setInOfficialReview(true);
        team.setTimeouts(1);
        team.setOfficialReviews(0);

        //snapshot should not be applied when id doesn't match
        snapshot.id = "OTHER";
        team.restoreSnapshot(snapshot);
        assertTrue(team.inTimeout());
        assertTrue(team.inOfficialReview());
        assertEquals(1, team.getTimeouts());
        assertEquals(0, team.getOfficialReviews());

        snapshot.id = ID;
        team.restoreSnapshot(snapshot);
        assertFalse(team.inTimeout());
        assertFalse(team.inOfficialReview());
        assertEquals(3, team.getTimeouts());
        assertEquals(1, team.getOfficialReviews());
    }

    @Test
    public void testTimeout() {
        team.setTimeouts(1);

        team.timeout();
        assertEquals(0, team.getTimeouts());
        assertEquals(team, sb.getTimeoutOwner());
        assertFalse(sb.isOfficialReview());

        team.timeout();
        assertEquals(0, team.getTimeouts());
    }

    @Test
    public void testOfficialReview() {
        team.officialReview();
        assertEquals(0, team.getOfficialReviews());
        assertEquals(team, sb.getTimeoutOwner());
        assertTrue(sb.isOfficialReview());

        team.officialReview();
        assertEquals(0, team.getOfficialReviews());
    }

    @Test
    public void testSetInTimeout() {
        assertFalse(team.inTimeout());
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.Value.IN_TIMEOUT, listener));

        team.setInTimeout(true);
        assertTrue(team.inTimeout());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertTrue((Boolean)event.getValue());
        assertFalse((Boolean)event.getPreviousValue());

        //check idempotency
        team.setInTimeout(true);
        assertTrue(team.inTimeout());
        assertEquals(0, collectedEvents.size());

        team.setInTimeout(false);
        assertFalse(team.inTimeout());
    }

    @Test
    public void testSetInOfficialReview() {
        assertFalse(team.inOfficialReview());
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.Value.IN_OFFICIAL_REVIEW, listener));

        team.setInOfficialReview(true);
        assertTrue(team.inOfficialReview());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertTrue((Boolean)event.getValue());
        assertFalse((Boolean)event.getPreviousValue());

        //check idempotency
        team.setInOfficialReview(true);
        assertTrue(team.inOfficialReview());
        assertEquals(0, collectedEvents.size());

        team.setInOfficialReview(false);
        assertFalse(team.inOfficialReview());
    }

    @Test
    public void testSetRetainedOfficialReview() {
        assertFalse(team.retainedOfficialReview());
        assertEquals(1, team.getOfficialReviews());
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.Value.RETAINED_OFFICIAL_REVIEW, listener));
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.Value.OFFICIAL_REVIEWS, listener));

        team.setRetainedOfficialReview(true);
        assertTrue(team.retainedOfficialReview());
        assertEquals(1, team.getOfficialReviews());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertTrue((Boolean)event.getValue());
        assertFalse((Boolean)event.getPreviousValue());

        //check idempotency
        team.setRetainedOfficialReview(true);
        assertTrue(team.retainedOfficialReview());
        assertEquals(0, collectedEvents.size());

        team.setRetainedOfficialReview(false);
        assertFalse(team.retainedOfficialReview());

        team.setOfficialReviews(0);
        collectedEvents.clear();
        team.setRetainedOfficialReview(true);
        assertEquals(1, team.getOfficialReviews());
        assertEquals(2, collectedEvents.size());
    }

    @Test
    public void testSetTimeouts() {
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.Value.TIMEOUTS, listener));
        sb.getRulesets().set(Rule.NUMBER_TIMEOUTS, String.valueOf(5));

        team.setTimeouts(4);
        assertEquals(4, team.getTimeouts());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertEquals(4, event.getValue());
        assertEquals(3, event.getPreviousValue());

        //values higher than maximum are clamped
        team.setTimeouts(12);
        assertEquals(5, team.getTimeouts());
        assertEquals(1, collectedEvents.size());
        assertEquals(5, collectedEvents.poll().getValue());

        //negative values are clamped
        team.setTimeouts(-2);
        assertEquals(0, team.getTimeouts());
    }

    @Test
    public void testChangeTimeouts() {
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.Value.TIMEOUTS, listener));
        assertEquals(3, team.getTimeouts());

        team.changeTimeouts(-2);
        assertEquals(1, team.getTimeouts());
        assertEquals(1, collectedEvents.size());

        team.changeTimeouts(1);
        assertEquals(2, team.getTimeouts());
    }

    @Test
    public void testSetOfficialReviews() {
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.Value.OFFICIAL_REVIEWS, listener));
        sb.getRulesets().set(Rule.NUMBER_REVIEWS, String.valueOf(5));

        team.setOfficialReviews(4);
        assertEquals(4, team.getOfficialReviews());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertEquals(4, event.getValue());
        assertEquals(1, event.getPreviousValue());

        //values higher than maximum are clamped
        team.setOfficialReviews(12);
        assertEquals(5, team.getOfficialReviews());
        assertEquals(1, collectedEvents.size());
        assertEquals(5, collectedEvents.poll().getValue());

        //negative values are clamped
        team.setOfficialReviews(-2);
        assertEquals(0, team.getOfficialReviews());
    }

    @Test
    public void testChangeOfficialReviews() {
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.Value.OFFICIAL_REVIEWS, listener));
        sb.getRulesets().set(Rule.NUMBER_REVIEWS, String.valueOf(3));
        assertEquals(1, team.getOfficialReviews());

        team.changeOfficialReviews(2);
        assertEquals(3, team.getOfficialReviews());
        assertEquals(1, collectedEvents.size());

        team.changeOfficialReviews(-1);
        assertEquals(2, team.getOfficialReviews());
    }

    @Test
    public void testResetTimeouts() {
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.Value.IN_TIMEOUT, listener));
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.Value.IN_OFFICIAL_REVIEW, listener));
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.Value.TIMEOUTS, listener));
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.Value.OFFICIAL_REVIEWS, listener));
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.Value.RETAINED_OFFICIAL_REVIEW, listener));
        team.setInTimeout(true);
        team.setInOfficialReview(true);
        team.setRetainedOfficialReview(true);
        team.setTimeouts(1);
        team.setOfficialReviews(0);
        collectedEvents.clear();

        team.resetTimeouts(false);
        assertFalse(team.inTimeout());
        assertFalse(team.inOfficialReview());
        assertEquals(1, team.getTimeouts());
        assertEquals(1, team.getOfficialReviews());
        assertFalse(team.retainedOfficialReview());
        assertEquals(4, collectedEvents.size());
        List<Property> events = new ArrayList<>();
        while(!collectedEvents.isEmpty()) {
            events.add(collectedEvents.poll().getProperty());
        }
        assertTrue(events.contains(Team.Value.IN_TIMEOUT));
        assertTrue(events.contains(Team.Value.IN_OFFICIAL_REVIEW));
        assertTrue(events.contains(Team.Value.OFFICIAL_REVIEWS));
        assertTrue(events.contains(Team.Value.RETAINED_OFFICIAL_REVIEW));

        sb.getRulesets().set(Rule.NUMBER_REVIEWS, String.valueOf(2));
        team.setInTimeout(true);
        team.setInOfficialReview(true);
        team.setRetainedOfficialReview(true);
        team.setTimeouts(1);
        team.setOfficialReviews(0);
        collectedEvents.clear();
        team.resetTimeouts(true);
        assertFalse(team.inTimeout());
        assertFalse(team.inOfficialReview());
        assertEquals(3, team.getTimeouts());
        assertEquals(2, team.getOfficialReviews());
        assertFalse(team.retainedOfficialReview());
        assertEquals(5, collectedEvents.size());
        events = new ArrayList<>();
        while(!collectedEvents.isEmpty()) {
            events.add(collectedEvents.poll().getProperty());
        }
        assertTrue(events.contains(Team.Value.IN_TIMEOUT));
        assertTrue(events.contains(Team.Value.IN_OFFICIAL_REVIEW));
        assertTrue(events.contains(Team.Value.TIMEOUTS));
        assertTrue(events.contains(Team.Value.OFFICIAL_REVIEWS));
        assertTrue(events.contains(Team.Value.RETAINED_OFFICIAL_REVIEW));

        sb.getRulesets().set(Rule.NUMBER_TIMEOUTS, String.valueOf(4));
        sb.getRulesets().set(Rule.TIMEOUTS_PER_PERIOD, String.valueOf(true));
        sb.getRulesets().set(Rule.REVIEWS_PER_PERIOD, String.valueOf(false));
        team.setRetainedOfficialReview(true);
        team.setTimeouts(1);
        team.setOfficialReviews(0);
        collectedEvents.clear();
        team.resetTimeouts(false);
        assertEquals(4, team.getTimeouts());
        assertEquals(0, team.getOfficialReviews());
        assertTrue(team.retainedOfficialReview());
        assertEquals(1, collectedEvents.size());
        assertEquals(Team.Value.TIMEOUTS, collectedEvents.poll().getProperty());
    }

    @Test
    public void testDisplayLead() {
        assertFalse(team.isLost());
        assertFalse(team.isLead());
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.Value.DISPLAY_LEAD, listener));

        team.set(Team.Value.LEAD, true);
        assertTrue(team.isDisplayLead());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertEquals(true, event.getValue());
        assertEquals(false, event.getPreviousValue());

        //check idempotency
        team.set(Team.Value.LEAD, true);
        assertTrue(team.isDisplayLead());
        assertEquals(0, collectedEvents.size());

        team.set(Team.Value.LOST, true);
        assertFalse(team.isDisplayLead());

        team.set(Team.Value.LOST, false);
        assertTrue(team.isDisplayLead());
    }

    @Test
    public void testSetStarPass() {
        assertFalse(team.isStarPass());
        sb.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.Value.STAR_PASS, listener));

        team.setStarPass(true);
        assertTrue(team.isStarPass());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertTrue((Boolean)event.getValue());
        assertFalse((Boolean)event.getPreviousValue());

        //check idempotency
        team.setStarPass(true);
        assertTrue(team.isStarPass());
        assertEquals(0, collectedEvents.size());

        team.setStarPass(false);
        assertFalse(team.isStarPass());
    }

    @Test
    public void testField() {
        Skater skater1 = team.addSkater("S1", "One", "1", "");
        Skater skater2 = team.addSkater("S2", "Two", "2", "");
        Skater skater3 = team.addSkater("S3", "Three", "3", "");
        Skater skater4 = team.addSkater("S4", "Four", "4", "");
        Skater skater5 = team.addSkater("S5", "Five", "5", "");
        Skater skater6 = team.addSkater("S6", "Six", "6", "");

        team.field(skater1, Role.JAMMER);
        team.field(skater2, Role.PIVOT);
        team.field(skater3, Role.BLOCKER);
        team.field(skater4, Role.BLOCKER);
        team.field(skater5, Role.BLOCKER);

        assertFalse(team.hasNoPivot());
        assertEquals(skater1, team.getPosition(FloorPosition.JAMMER).getSkater());
        assertEquals(team.getPosition(FloorPosition.JAMMER), skater1.getPosition());
        assertEquals(Role.JAMMER, skater1.getRole());
        assertEquals(skater2, team.getPosition(FloorPosition.PIVOT).getSkater());
        assertEquals(team.getPosition(FloorPosition.PIVOT), skater2.getPosition());
        assertEquals(Role.PIVOT, skater2.getRole());
        assertEquals(skater3, team.getPosition(FloorPosition.BLOCKER1).getSkater());
        assertEquals(team.getPosition(FloorPosition.BLOCKER1), skater3.getPosition());
        assertEquals(Role.BLOCKER, skater3.getRole());
        assertEquals(skater4, team.getPosition(FloorPosition.BLOCKER2).getSkater());
        assertEquals(team.getPosition(FloorPosition.BLOCKER2), skater4.getPosition());
        assertEquals(Role.BLOCKER, skater4.getRole());
        assertEquals(skater5, team.getPosition(FloorPosition.BLOCKER3).getSkater());
        assertEquals(team.getPosition(FloorPosition.BLOCKER3), skater5.getPosition());
        assertEquals(Role.BLOCKER, skater5.getRole());

        team.field(skater6, Role.JAMMER);

        assertEquals(skater6, team.getPosition(FloorPosition.JAMMER).getSkater());
        assertEquals(team.getPosition(FloorPosition.JAMMER), skater6.getPosition());
        assertEquals(Role.JAMMER, skater6.getRole());
        assertNull(skater1.getPosition());
        assertEquals(Role.BENCH, skater1.getRole());

        team.field(skater1, Role.BLOCKER);

        assertEquals(skater1, team.getPosition(FloorPosition.BLOCKER1).getSkater());
        assertEquals(team.getPosition(FloorPosition.BLOCKER1), skater1.getPosition());
        assertEquals(Role.BLOCKER, skater1.getRole());
        assertNull(skater3.getPosition());
        assertEquals(Role.BENCH, skater3.getRole());

        team.field(skater2, Role.BLOCKER);

        assertTrue(team.hasNoPivot());
        assertEquals(skater2, team.getPosition(FloorPosition.PIVOT).getSkater());
        assertEquals(team.getPosition(FloorPosition.PIVOT), skater2.getPosition());
        assertEquals(Role.BLOCKER, skater2.getRole());

        team.field(skater4, Role.PIVOT);

        assertFalse(team.hasNoPivot());
        assertEquals(skater4, team.getPosition(FloorPosition.PIVOT).getSkater());
        assertEquals(team.getPosition(FloorPosition.PIVOT), skater4.getPosition());
        assertEquals(Role.PIVOT, skater4.getRole());
        assertEquals(skater2, team.getPosition(FloorPosition.BLOCKER2).getSkater());
        assertEquals(team.getPosition(FloorPosition.BLOCKER2), skater2.getPosition());
        assertEquals(Role.BLOCKER, skater2.getRole());

        team.field(skater5, Role.PIVOT);

        assertFalse(team.hasNoPivot());
        assertEquals(skater5, team.getPosition(FloorPosition.PIVOT).getSkater());
        assertEquals(team.getPosition(FloorPosition.PIVOT), skater5.getPosition());
        assertEquals(Role.PIVOT, skater5.getRole());
        assertEquals(skater4, team.getPosition(FloorPosition.BLOCKER3).getSkater());
        assertEquals(team.getPosition(FloorPosition.BLOCKER3), skater4.getPosition());
        assertEquals(Role.BLOCKER, skater4.getRole());

        sb.startJam();
        assertEquals(skater6, team.getPosition(FloorPosition.JAMMER).getSkater());
        assertEquals(skater5, team.getPosition(FloorPosition.PIVOT).getSkater());
        assertEquals(skater1, team.getPosition(FloorPosition.BLOCKER1).getSkater());
        assertEquals(skater2, team.getPosition(FloorPosition.BLOCKER2).getSkater());
        assertEquals(skater4, team.getPosition(FloorPosition.BLOCKER3).getSkater());
        
        team.setStarPass(true);

        assertFalse(team.hasNoPivot());
        assertEquals(skater6, team.getPosition(FloorPosition.JAMMER).getSkater());
        assertEquals(team.getPosition(FloorPosition.JAMMER), skater6.getPosition());
        assertEquals(Role.BLOCKER, skater6.getRole());
        assertEquals(skater5, team.getPosition(FloorPosition.PIVOT).getSkater());
        assertEquals(team.getPosition(FloorPosition.PIVOT), skater5.getPosition());
        assertEquals(Role.JAMMER, skater5.getRole());

        skater6.setPenaltyBox(true);
        skater5.setPenaltyBox(true);
        skater2.setPenaltyBox(true);
        sb.stopJamTO();

        assertEquals(team.getPosition(FloorPosition.BLOCKER1), skater1.getPosition());
        assertEquals(team.getPosition(FloorPosition.BLOCKER2), skater2.getPosition());
        assertNull(skater3.getPosition());
        assertEquals(team.getPosition(FloorPosition.BLOCKER3), skater4.getPosition());
        assertEquals(team.getPosition(FloorPosition.PIVOT), skater5.getPosition());
        assertEquals(team.getPosition(FloorPosition.JAMMER), skater6.getPosition());
        assertEquals(Role.BLOCKER, skater1.getRole());
        assertEquals(Role.BLOCKER, skater2.getRole());
        assertEquals(Role.BENCH, skater3.getRole());
        assertEquals(Role.BLOCKER, skater4.getRole());
        assertEquals(Role.JAMMER, skater5.getRole());
        assertEquals(Role.BLOCKER, skater6.getRole());
        assertEquals(skater5, team.getPosition(FloorPosition.JAMMER).getSkater());
        assertNull(team.getPosition(FloorPosition.PIVOT).getSkater());
        assertEquals(skater6, team.getPosition(FloorPosition.BLOCKER1).getSkater());
        assertEquals(skater2, team.getPosition(FloorPosition.BLOCKER2).getSkater());
        assertNull(team.getPosition(FloorPosition.BLOCKER3).getSkater());
        
        skater2.setPenaltyBox(false);
        skater4.setPenaltyBox(true);

        assertEquals(team.getPosition(FloorPosition.BLOCKER1), skater1.getPosition());
        assertEquals(team.getPosition(FloorPosition.BLOCKER2), skater2.getPosition());
        assertNull(skater3.getPosition());
        assertEquals(team.getPosition(FloorPosition.BLOCKER3), skater4.getPosition());
        assertEquals(team.getPosition(FloorPosition.PIVOT), skater5.getPosition());
        assertEquals(team.getPosition(FloorPosition.JAMMER), skater6.getPosition());
        assertEquals(Role.BLOCKER, skater1.getRole());
        assertEquals(Role.BLOCKER, skater2.getRole());
        assertEquals(Role.BENCH, skater3.getRole());
        assertEquals(Role.BLOCKER, skater4.getRole());
        assertEquals(Role.JAMMER, skater5.getRole());
        assertEquals(Role.BLOCKER, skater6.getRole());
        assertEquals(skater5, team.getPosition(FloorPosition.JAMMER).getSkater());
        assertNull(team.getPosition(FloorPosition.PIVOT).getSkater());
        assertEquals(skater6, team.getPosition(FloorPosition.BLOCKER1).getSkater());
        assertNull(team.getPosition(FloorPosition.BLOCKER2).getSkater());
        assertEquals(skater4, team.getPosition(FloorPosition.BLOCKER3).getSkater());

        team.execute(Team.Command.ADVANCE_FIELDINGS);
        
        assertNull(skater1.getPosition());
        assertNull(skater2.getPosition());
        assertNull(skater3.getPosition());
        assertEquals(team.getPosition(FloorPosition.BLOCKER3), skater4.getPosition());
        assertEquals(team.getPosition(FloorPosition.JAMMER), skater5.getPosition());
        assertEquals(team.getPosition(FloorPosition.BLOCKER1), skater6.getPosition());
        assertEquals(Role.BENCH, skater1.getRole());
        assertEquals(Role.BENCH, skater2.getRole());
        assertEquals(Role.BENCH, skater3.getRole());
        assertEquals(Role.BLOCKER, skater4.getRole());
        assertEquals(Role.JAMMER, skater5.getRole());
        assertEquals(Role.BLOCKER, skater6.getRole());
        assertEquals(skater5, team.getPosition(FloorPosition.JAMMER).getSkater());
        assertNull(team.getPosition(FloorPosition.PIVOT).getSkater());
        assertEquals(skater6, team.getPosition(FloorPosition.BLOCKER1).getSkater());
        assertNull(team.getPosition(FloorPosition.BLOCKER2).getSkater());
        assertEquals(skater4, team.getPosition(FloorPosition.BLOCKER3).getSkater());

        team.field(skater1, Role.PIVOT);
        team.field(skater2, Role.BLOCKER);	
        skater6.setPenaltyBox(false);
        sb.startJam();
        sb.stopJamTO();
        team.execute(Team.Command.ADVANCE_FIELDINGS);

        assertNull(skater1.getPosition());
        assertNull(skater2.getPosition());
        assertNull(skater3.getPosition());
        assertEquals(team.getPosition(FloorPosition.BLOCKER3), skater4.getPosition());
        assertEquals(team.getPosition(FloorPosition.JAMMER), skater5.getPosition());
        assertNull(skater6.getPosition());
        assertEquals(Role.BENCH, skater1.getRole());
        assertEquals(Role.BENCH, skater2.getRole());
        assertEquals(Role.BENCH, skater3.getRole());
        assertEquals(Role.BLOCKER, skater4.getRole());
        assertEquals(Role.JAMMER, skater5.getRole());
        assertEquals(Role.BENCH, skater6.getRole());
        assertEquals(skater5, team.getPosition(FloorPosition.JAMMER).getSkater());
        assertNull(team.getPosition(FloorPosition.PIVOT).getSkater());
        assertNull(team.getPosition(FloorPosition.BLOCKER1).getSkater());
        assertNull(team.getPosition(FloorPosition.BLOCKER2).getSkater());
        assertEquals(skater4, team.getPosition(FloorPosition.BLOCKER3).getSkater());
    }
}
