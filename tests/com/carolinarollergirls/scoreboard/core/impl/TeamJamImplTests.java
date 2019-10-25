package com.carolinarollergirls.scoreboard.core.impl;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.Queue;

import org.junit.Before;
import org.junit.Test;

import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.ScoringTrip;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.core.impl.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.core.impl.TeamJamImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class TeamJamImplTests {

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


    private TeamJamImpl tj;

    @Before
    public void setUp() throws Exception {
        collectedEvents = new LinkedList<>();

        sb = new ScoreBoardImpl();
        tj = (TeamJamImpl)sb.getTeam(Team.ID_1).get(Team.Value.RUNNING_OR_UPCOMING_TEAM_JAM);
        ScoreBoardClock.getInstance().stop();
    }

    @Test
    public void testRemoveInitialPassAfterStarPass() {
        tj.getCurrentScoringTrip().set(ScoringTrip.Value.SCORE, 2);
        tj.getCurrentScoringTrip().set(ScoringTrip.Value.AFTER_S_P, true);
        assertEquals(1, tj.getCurrentScoringTrip().getNumber());
        tj.addScoringTrip();
        tj.getCurrentScoringTrip().set(ScoringTrip.Value.SCORE, 3);

        assertEquals(5, tj.getJamScore());
        assertEquals(5, tj.get(TeamJam.Value.AFTER_S_P_SCORE));
        assertEquals(2, tj.getCurrentScoringTrip().getNumber());
        assertTrue(tj.isStarPass());

        tj.getCurrentScoringTrip().getPrevious().execute(ScoringTrip.Command.REMOVE);
   
        assertEquals(3, tj.getJamScore());
        assertEquals(3, tj.get(TeamJam.Value.AFTER_S_P_SCORE));
        assertEquals(1, tj.getCurrentScoringTrip().getNumber());
        assertTrue(tj.isStarPass());
    }

    @Test
    public void testNoLostInOvertime() {
        tj.getJam().set(Jam.Value.OVERTIME, true);
        tj.set(TeamJam.Value.LOST, true);
        assertFalse(tj.isLost());
    }
}
