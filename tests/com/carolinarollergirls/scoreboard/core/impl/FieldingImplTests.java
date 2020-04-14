package com.carolinarollergirls.scoreboard.core.impl;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.Queue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.carolinarollergirls.scoreboard.core.BoxTrip;
import com.carolinarollergirls.scoreboard.core.Fielding;
import com.carolinarollergirls.scoreboard.core.Role;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class FieldingImplTests {

    private ScoreBoard sb;

    private Queue<ScoreBoardEvent<?>> collectedEvents;
    public ScoreBoardListener listener = new ScoreBoardListener() {

        @Override
        public void scoreBoardChange(ScoreBoardEvent<?> event) {
            synchronized (collectedEvents) {
                collectedEvents.add(event);
            }
        }
    };

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
        collectedEvents = new LinkedList<>();

        sb = new ScoreBoardImpl();
        sb.addScoreBoardListener(batchCounter);

        ScoreBoardClock.getInstance().stop();
    }

    @After
    public void tearDown() throws Exception {
        ScoreBoardClock.getInstance().start(false);
        // Check all started batches were ended.
        assertEquals(0, batchLevel);
    }

    @Test
    public void testSortBoxTripSymbols() {
        Team t = sb.getTeam(Team.ID_1);
        Skater s = new SkaterImpl(t, (String) null);
        t.addSkater(s);
        sb.startJam();
        sb.getTeam(Team.ID_1).field(s, Role.PIVOT);
        s.setPenaltyBox(true);
        sb.stopJamTO();
        sb.startJam();
        s.setPenaltyBox(false);
        s.setPenaltyBox(true);
        sb.stopJamTO();
        sb.startJam();
        s.setPenaltyBox(false);

        Fielding f = s.getFielding(sb.getOrCreatePeriod(1).getJam(2).getTeamJam(Team.ID_1));
        f.execute(Fielding.ADD_BOX_TRIP);

        assertEquals(" $ + -", f.get(Fielding.BOX_TRIP_SYMBOLS));
    }

    @Test
    public void testAddingPastBoxTripHasRightJamNumbers() {
        Team t = sb.getTeam(Team.ID_1);
        Skater s = new SkaterImpl(t, (String) null);
        t.addSkater(s);
        sb.startJam();
        t.field(s, Role.PIVOT);
        sb.stopJamTO();
        sb.startJam();
        sb.stopJamTO();

        Fielding f = s.getFielding(sb.getOrCreatePeriod(1).getJam(1).getTeamJam(Team.ID_1));
        f.execute(Fielding.ADD_BOX_TRIP);
        BoxTrip bt = f.getAll(Fielding.BOX_TRIP).iterator().next();

        assertEquals(1, (int) bt.get(BoxTrip.START_JAM_NUMBER));
        assertEquals(1, (int) bt.get(BoxTrip.END_JAM_NUMBER));
    }

    @Test
    public void testRemovingUpcomingBoxTripAfterAdvance() {
        Team t = sb.getTeam(Team.ID_1);
        Skater s = new SkaterImpl(t, (String) null);
        t.addSkater(s);
        sb.getTeam(Team.ID_1).field(s, Role.PIVOT);
        sb.startJam();
        s.setPenaltyBox(true);
        sb.stopJamTO();
        s.setPenaltyBox(false);
        // Box trip from ended jam remains.
        Fielding f1 = s.getFielding(t.getRunningOrUpcomingTeamJam().getPrevious());
        assertEquals(1, f1.numberOf(Fielding.BOX_TRIP));

        t.execute(Team.ADVANCE_FIELDINGS);
        t.field(s, Role.PIVOT);
        Fielding f2 = s.getFielding(t.getRunningOrUpcomingTeamJam());
        assertEquals(0, f2.numberOf(Fielding.BOX_TRIP));

        // Box trip added to upcoming jam.
        s.setPenaltyBox(true);
        assertEquals(1, f1.numberOf(Fielding.BOX_TRIP));
        assertEquals(1, f2.numberOf(Fielding.BOX_TRIP));

        // And removed again.
        s.setPenaltyBox(false);
        assertEquals(1, f1.numberOf(Fielding.BOX_TRIP));
        assertEquals(0, f2.numberOf(Fielding.BOX_TRIP));
    }
}
