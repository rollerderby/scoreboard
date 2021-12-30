package com.carolinarollergirls.scoreboard.core.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.Queue;

import org.junit.Before;
import org.junit.Test;

import com.carolinarollergirls.scoreboard.core.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Jam;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoringTrip;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.core.interfaces.TeamJam;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Source;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class TeamJamImplTests {

    private ScoreBoard sb;

    private Queue<ScoreBoardEvent<?>> collectedEvents;
    public ScoreBoardListener listener = new ScoreBoardListener() {
        @Override
        public void scoreBoardChange(ScoreBoardEvent<?> event) {
            synchronized (collectedEvents) { collectedEvents.add(event); }
        }
    };

    private TeamJamImpl tj;

    @Before
    public void setUp() throws Exception {
        collectedEvents = new LinkedList<>();

        sb = new ScoreBoardImpl();
        sb.postAutosaveUpdate();
        tj = (TeamJamImpl) sb.getCurrentGame()
                 .get(CurrentGame.GAME)
                 .getTeam(Team.ID_1)
                 .get(Team.RUNNING_OR_UPCOMING_TEAM_JAM);
        ScoreBoardClock.getInstance().stop();
    }

    @Test
    public void testRemoveInitialPassAfterStarPass() {
        tj.getCurrentScoringTrip().set(ScoringTrip.SCORE, 2);
        tj.getCurrentScoringTrip().set(ScoringTrip.AFTER_S_P, true);
        assertEquals(1, tj.getCurrentScoringTrip().getNumber());
        tj.addScoringTrip();
        tj.getCurrentScoringTrip().set(ScoringTrip.SCORE, 3);

        assertEquals(5, tj.getJamScore());
        assertEquals(5, (int) tj.get(TeamJam.AFTER_S_P_SCORE));
        assertEquals(2, tj.getCurrentScoringTrip().getNumber());
        assertTrue(tj.isStarPass());

        tj.getCurrentScoringTrip().getPrevious().execute(ScoringTrip.REMOVE);

        assertEquals(3, tj.getJamScore());
        assertEquals(3, (int) tj.get(TeamJam.AFTER_S_P_SCORE));
        assertEquals(1, tj.getCurrentScoringTrip().getNumber());
        assertTrue(tj.isStarPass());
    }

    @Test
    public void testRemoveInitialWhenItIsTheOnlyTrip() {
        tj.getCurrentScoringTrip().set(ScoringTrip.SCORE, 2);
        assertEquals(1, tj.getCurrentScoringTrip().getNumber());
        tj.removeScoringTrip();
        // Score is now zero, but trip remains.
        assertEquals(0, tj.getJamScore());
        assertEquals(1, tj.getCurrentScoringTrip().getNumber());

        tj.getCurrentScoringTrip().set(ScoringTrip.SCORE, 2);
        tj.getCurrentScoringTrip().execute(ScoringTrip.REMOVE);
        assertEquals(0, tj.getJamScore());
        assertEquals(1, tj.getCurrentScoringTrip().getNumber());
    }

    @Test
    public void testNoLostInOvertime() {
        tj.getJam().set(Jam.OVERTIME, true);
        tj.set(TeamJam.LOST, true);
        assertFalse(tj.isLost());
    }

    @Test
    public void testLateScoreChange() {
        Game g = sb.getCurrentGame().get(CurrentGame.GAME);
        g.startJam();
        tj.getTeam().execute(Team.ADD_TRIP);
        tj.getCurrentScoringTrip().set(ScoringTrip.SCORE, 2);
        g.stopJamTO();
        g.startJam();
        g.stopJamTO();
        tj.getCurrentScoringTrip().set(ScoringTrip.SCORE, 3, Source.WS);
        assertEquals(-1, tj.getOsOffset());

        tj.getCurrentScoringTrip().set(ScoringTrip.SCORE, 0, Source.WS);
        assertEquals(2, tj.getOsOffset());

        g.set(Rule.WFTDA_LATE_SCORE_RULE, String.valueOf(false));
        tj.getCurrentScoringTrip().set(ScoringTrip.SCORE, 2, Source.WS);
        assertEquals(2, tj.getOsOffset());
    }
}
