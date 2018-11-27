package com.carolinarollergirls.scoreboard.core.impl;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.carolinarollergirls.scoreboard.core.Rulesets;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.impl.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.core.impl.TeamImpl;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class TeamImplTests {

    private ScoreBoard sbMock;
    private Rulesets rulesetsMock;
    private Team otherTeamMock;

    private int maxNumberTimeouts = 3;
    private boolean timeoutsPerPeriod = false;
    private int maxNumberReviews = 1;
    private boolean reviewsPerPeriod = true;

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
    private static String ID = "TEST";

    @Before
    public void setUp() throws Exception {
        collectedEvents = new LinkedList<ScoreBoardEvent>();

        sbMock = Mockito.mock(ScoreBoardImpl.class);

        rulesetsMock = Mockito.mock(Rulesets.class);
        otherTeamMock = Mockito.mock(TeamImpl.class);

        Mockito
        .when(sbMock.getRulesets())
        .thenReturn(rulesetsMock);

        Mockito
        .when(sbMock.getTeam(Mockito.anyString()))
        .thenReturn(otherTeamMock);

        Mockito
        .when(rulesetsMock.getInt(Rule.NUMBER_TIMEOUTS))
        .thenAnswer(new Answer<Integer>() {
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                return maxNumberTimeouts;
            }
        });

        Mockito
        .when(rulesetsMock.getBoolean(Rule.TIMEOUTS_PER_PERIOD))
        .thenAnswer(new Answer<Boolean>() {
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                return timeoutsPerPeriod;
            }
        });
        Mockito
        .when(rulesetsMock.getInt(Rule.NUMBER_REVIEWS))
        .thenAnswer(new Answer<Integer>() {
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                return maxNumberReviews;
            }
        });

        Mockito
        .when(rulesetsMock.getBoolean(Rule.REVIEWS_PER_PERIOD))
        .thenAnswer(new Answer<Boolean>() {
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                return reviewsPerPeriod;
            }
        });

        team = new TeamImpl(sbMock, ID);
        ScoreBoardClock.getInstance().stop();
    }

    @Test
    public void testStartJam() {
        team.setScore(34);
        team.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.EVENT_LAST_SCORE, listener));

        team.startJam();

        assertEquals(34, team.getLastScore());
        assertEquals(1, collectedEvents.size());
    }

    @Test
    public void testStopJam() {
        team.setLeadJammer(Team.LEAD_LOST_LEAD);
        team.setStarPass(true);
        team.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.EVENT_LEAD_JAMMER, listener));
        team.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.EVENT_STAR_PASS, listener));

        team.stopJam();

        assertEquals(Team.LEAD_NO_LEAD, team.getLeadJammer());
        assertEquals(false, team.isStarPass());
        assertEquals(2, collectedEvents.size());
        boolean leadJammerSeen = false;
        boolean starPassSeen = false;
        while (!collectedEvents.isEmpty()) {
            String event = collectedEvents.poll().getProperty();
            if (event == Team.EVENT_LEAD_JAMMER) { leadJammerSeen = true; }
            if (event == Team.EVENT_STAR_PASS) { starPassSeen = true; }
        }
        assertTrue(leadJammerSeen && starPassSeen);
    }

    @Test
    public void testRestoreSnapshot() {
        assertEquals(Team.LEAD_NO_LEAD, team.getLeadJammer());
        assertFalse(team.isStarPass());
        assertEquals(0, team.getScore());
        assertEquals(0, team.getLastScore());
        assertFalse(team.inTimeout());
        assertFalse(team.inOfficialReview());
        assertEquals(3, team.getTimeouts());
        assertEquals(1, team.getOfficialReviews());
        Team.TeamSnapshot snapshot = team.snapshot();

        team.setLeadJammer(Team.LEAD_LOST_LEAD);
        team.setStarPass(true);
        team.setScore(5);
        team.setLastScore(3);
        team.setInTimeout(true);
        team.setInOfficialReview(true);
        team.setTimeouts(1);
        team.setOfficialReviews(0);

        //snapshot should not be applied when id doesn't match
        team.id = "OTHER";
        team.restoreSnapshot(snapshot);
        assertEquals(Team.LEAD_LOST_LEAD, team.getLeadJammer());
        assertTrue(team.isStarPass());
        assertEquals(5, team.getScore());
        assertEquals(3, team.getLastScore());
        assertTrue(team.inTimeout());
        assertTrue(team.inOfficialReview());
        assertEquals(1, team.getTimeouts());
        assertEquals(0, team.getOfficialReviews());

        team.id = "TEST";
        team.restoreSnapshot(snapshot);
        assertEquals(Team.LEAD_NO_LEAD, team.getLeadJammer());
        assertFalse(team.isStarPass());
        assertEquals(5, team.getScore()); //score is not reset
        assertEquals(0, team.getLastScore());
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
        Mockito.verify(sbMock).setTimeoutType("TEST", false);

        team.timeout();
        Mockito.verify(sbMock, Mockito.times(1)).setTimeoutType("TEST", false);
    }

    @Test
    public void testOfficialReview() {
        team.officialReview();
        assertEquals(0, team.getOfficialReviews());
        Mockito.verify(sbMock).setTimeoutType("TEST", true);

        team.officialReview();
        Mockito.verify(sbMock, Mockito.times(1)).setTimeoutType("TEST", true);
    }

    @Test
    public void testSetScore() {
        team.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.EVENT_SCORE, listener));

        team.setScore(5);
        assertEquals(5, team.getScore());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertEquals(5, event.getValue());
        assertEquals(0, event.getPreviousValue());
        assertEquals(0, team.getLastScore());

        //setting a value below lastScore changes lastScore
        team.setLastScore(4);
        team.setScore(3);
        assertEquals(3, team.getLastScore());

        //negative values are clamped
        collectedEvents.clear();
        team.setScore(-1);
        assertEquals(0,team.getScore());
        assertEquals(1, collectedEvents.size());
        assertEquals(0, collectedEvents.poll().getValue());
    }

    @Test
    public void testChangeScore() {
        team.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.EVENT_SCORE, listener));

        team.setScore(5);
        team.changeScore(3);
        assertEquals(8, team.getScore());
        assertEquals(2, collectedEvents.size());

        team.changeScore(-5);
        assertEquals(3, team.getScore());
    }

    @Test
    public void testSetLastScore() {
        team.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.EVENT_LAST_SCORE, listener));
        team.setScore(10);

        team.setLastScore(5);
        assertEquals(5, team.getLastScore());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertEquals(5, event.getValue());
        assertEquals(0, event.getPreviousValue());

        //values higher than score are clamped
        team.setLastScore(12);
        assertEquals(10, team.getLastScore());
        assertEquals(1, collectedEvents.size());
        assertEquals(10, collectedEvents.poll().getValue());

        //negative values are clamped
        team.setLastScore(-2);
        assertEquals(0, team.getLastScore());
    }

    @Test
    public void testChangeLastScore() {
        team.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.EVENT_LAST_SCORE, listener));

        team.setScore(10);
        team.setLastScore(5);
        team.changeLastScore(3);
        assertEquals(8, team.getLastScore());
        assertEquals(2, collectedEvents.size());

        team.changeLastScore(-5);
        assertEquals(3, team.getLastScore());
    }

    @Test
    public void testSetInTimeout() {
        assertFalse(team.inTimeout());
        team.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.EVENT_IN_TIMEOUT, listener));

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
        team.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.EVENT_IN_OFFICIAL_REVIEW, listener));

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
        team.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.EVENT_RETAINED_OFFICIAL_REVIEW, listener));
        team.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.EVENT_OFFICIAL_REVIEWS, listener));

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
        team.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.EVENT_TIMEOUTS, listener));
        maxNumberTimeouts = 5;

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
        team.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.EVENT_TIMEOUTS, listener));
        assertEquals(3, team.getTimeouts());

        team.changeTimeouts(-2);
        assertEquals(1, team.getTimeouts());
        assertEquals(1, collectedEvents.size());

        team.changeTimeouts(1);
        assertEquals(2, team.getTimeouts());
    }

    @Test
    public void testSetOfficialReviews() {
        team.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.EVENT_OFFICIAL_REVIEWS, listener));
        maxNumberReviews = 5;

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
        team.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.EVENT_OFFICIAL_REVIEWS, listener));
        maxNumberReviews = 3;
        assertEquals(1, team.getOfficialReviews());

        team.changeOfficialReviews(2);
        assertEquals(3, team.getOfficialReviews());
        assertEquals(1, collectedEvents.size());

        team.changeOfficialReviews(-1);
        assertEquals(2, team.getOfficialReviews());
    }

    @Test
    public void testResetTimeouts() {
        team.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.EVENT_IN_TIMEOUT, listener));
        team.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.EVENT_IN_OFFICIAL_REVIEW, listener));
        team.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.EVENT_TIMEOUTS, listener));
        team.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.EVENT_OFFICIAL_REVIEWS, listener));
        team.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.EVENT_RETAINED_OFFICIAL_REVIEW, listener));
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
        List<String> events = new ArrayList<String>();
        while(!collectedEvents.isEmpty()) {
            events.add(collectedEvents.poll().getProperty());
        }
        assertTrue(events.contains(Team.EVENT_IN_TIMEOUT));
        assertTrue(events.contains(Team.EVENT_IN_OFFICIAL_REVIEW));
        assertTrue(events.contains(Team.EVENT_OFFICIAL_REVIEWS));
        assertTrue(events.contains(Team.EVENT_RETAINED_OFFICIAL_REVIEW));

        maxNumberReviews = 2;
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
        events = new ArrayList<String>();
        while(!collectedEvents.isEmpty()) {
            events.add(collectedEvents.poll().getProperty());
        }
        assertTrue(events.contains(Team.EVENT_IN_TIMEOUT));
        assertTrue(events.contains(Team.EVENT_IN_OFFICIAL_REVIEW));
        assertTrue(events.contains(Team.EVENT_TIMEOUTS));
        assertTrue(events.contains(Team.EVENT_OFFICIAL_REVIEWS));
        assertTrue(events.contains(Team.EVENT_RETAINED_OFFICIAL_REVIEW));

        maxNumberTimeouts = 4;
        timeoutsPerPeriod = true;
        reviewsPerPeriod = false;
        team.setRetainedOfficialReview(true);
        team.setTimeouts(1);
        team.setOfficialReviews(0);
        collectedEvents.clear();
        team.resetTimeouts(false);
        assertEquals(4, team.getTimeouts());
        assertEquals(0, team.getOfficialReviews());
        assertTrue(team.retainedOfficialReview());
        assertEquals(1, collectedEvents.size());
        assertEquals(Team.EVENT_TIMEOUTS, collectedEvents.poll().getProperty());
    }

    @Test
    public void testSetLeadJammer() {
        assertEquals(Team.LEAD_NO_LEAD, team.getLeadJammer());
        team.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.EVENT_LEAD_JAMMER, listener));

        team.setLeadJammer(Team.LEAD_LEAD);
        assertEquals(Team.LEAD_LEAD, team.getLeadJammer());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertEquals(Team.LEAD_LEAD, event.getValue());
        assertEquals(Team.LEAD_NO_LEAD, event.getPreviousValue());

        //check idempotency
        team.setLeadJammer(Team.LEAD_LEAD);
        assertEquals(Team.LEAD_LEAD, team.getLeadJammer());
        assertEquals(1, collectedEvents.size());

        team.setLeadJammer(Team.LEAD_LOST_LEAD);
        assertEquals(Team.LEAD_LOST_LEAD, team.getLeadJammer());

        team.setLeadJammer(Team.LEAD_NO_LEAD);
        assertEquals(Team.LEAD_NO_LEAD, team.getLeadJammer());
    }

    @Test
    public void testSetStarPass() {
        assertFalse(team.isStarPass());
        team.addScoreBoardListener(new ConditionalScoreBoardListener(team, Team.EVENT_STAR_PASS, listener));

        team.setStarPass(true);
        assertTrue(team.isStarPass());
        assertEquals(1, collectedEvents.size());
        ScoreBoardEvent event = collectedEvents.poll();
        assertTrue((Boolean)event.getValue());
        assertFalse((Boolean)event.getPreviousValue());

        //check idempotency
        team.setStarPass(true);
        assertTrue(team.isStarPass());
        assertEquals(1, collectedEvents.size());

        team.setStarPass(false);
        assertFalse(team.isStarPass());
    }
}
