package com.carolinarollergirls.scoreboard.core.impl;

import static org.junit.Assert.assertEquals;
import java.util.LinkedList;
import java.util.Queue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.carolinarollergirls.scoreboard.core.Fielding;
import com.carolinarollergirls.scoreboard.core.Role;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl.BatchEvent;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class FieldingImplTests {

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

    private int batchLevel;
    private ScoreBoardListener batchCounter = new ScoreBoardListener() {
        @Override
        public void scoreBoardChange(ScoreBoardEvent event) {
            synchronized(batchCounter) {
                if (event.getProperty() == BatchEvent.START) {
                    batchLevel++;
                } else if (event.getProperty() == BatchEvent.END) {
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
        Skater s = sb.getTeam(Team.ID_1).addSkater("SkaterId", "Name", "3", null);
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
        f.execute(Fielding.Command.ADD_BOX_TRIP);
        
        assertEquals(" $ + -", f.get(Fielding.Value.BOX_TRIP_SYMBOLS));
    }
}
