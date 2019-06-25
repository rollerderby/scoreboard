
package com.carolinarollergirls.scoreboard.core.impl;

import static org.junit.Assert.assertEquals;
import java.util.LinkedList;
import java.util.Queue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.carolinarollergirls.scoreboard.core.Fielding;
import com.carolinarollergirls.scoreboard.core.BoxTrip;
import com.carolinarollergirls.scoreboard.core.Role;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl.BatchEvent;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class BoxTripImplTests {

    private ScoreBoard sb;

    private Team t;
    private Skater s;
    private BoxTrip bt;

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
        sb = new ScoreBoardImpl();
        sb.addScoreBoardListener(batchCounter);

        ScoreBoardClock.getInstance().stop();
        t = sb.getTeam(Team.ID_1);
        s = new SkaterImpl(t, (String)null);
        t.addSkater(s);
        sb.startJam();
        sb.stopJamTO();
        sb.startJam();
        t.field(s, Role.PIVOT);
        s.setPenaltyBox(true);
        bt = (BoxTrip) s.getFielding(t.getRunningOrUpcomingTeamJam()).getAll(Fielding.Child.BOX_TRIP).iterator().next();
        sb.stopJamTO();
        sb.startJam();
        s.setPenaltyBox(false);
        sb.stopJamTO();
        t.execute(Team.Command.ADVANCE_FIELDINGS);
        // Now going into jam 3. Skater had a box trip that started in jam 1, and ended in jam 2.
    }

    @After
    public void tearDown() throws Exception {
        ScoreBoardClock.getInstance().start(false);
        // Check all started batches were ended.
        assertEquals(0, batchLevel);
    }

    private String getBoxTripSymbols(int jam) {
        Fielding f;
        if (jam == 4) {
            f = s.getFielding(t.getRunningOrUpcomingTeamJam());
        } else {
            f = s.getFielding(sb.getCurrentPeriod().getJam(jam).getTeamJam(Team.ID_1));
        }
        return f == null ? null : (String) f.get(Fielding.Value.BOX_TRIP_SYMBOLS);
    }

    @Test
    public void testBaseCase() {
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals(" -", getBoxTripSymbols(2));
        assertEquals(" $", getBoxTripSymbols(3));
        assertEquals(null, getBoxTripSymbols(4));
    }

    @Test
    public void testStartEarlier() {
        // Start before jam.
        bt.execute(BoxTrip.Command.START_EARLIER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals(" S", getBoxTripSymbols(2));
        assertEquals(" $", getBoxTripSymbols(3));
        assertEquals(null, getBoxTripSymbols(4));

        // Can't go before the skater was present.
        bt.execute(BoxTrip.Command.START_EARLIER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals(" S", getBoxTripSymbols(2));
        assertEquals(" $", getBoxTripSymbols(3));
        assertEquals(null, getBoxTripSymbols(4));
    }

    @Test
    public void testStartLater() {
        // Start between jams.
        bt.execute(BoxTrip.Command.START_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("", getBoxTripSymbols(2));
        assertEquals(" $", getBoxTripSymbols(3));
        assertEquals(null, getBoxTripSymbols(4));

        // Start in next jam.
        bt.execute(BoxTrip.Command.START_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("", getBoxTripSymbols(2));
        assertEquals(" +", getBoxTripSymbols(3));
        assertEquals(null, getBoxTripSymbols(4));

        // Can't go beyond end of penalty.
        bt.execute(BoxTrip.Command.START_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("", getBoxTripSymbols(2));
        assertEquals(" +", getBoxTripSymbols(3));
        assertEquals(null, getBoxTripSymbols(4));
    }

    @Test
    public void testEndEarlier() {
        // End during previous.
        bt.execute(BoxTrip.Command.END_EARLIER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals(" +", getBoxTripSymbols(2));
        assertEquals("", getBoxTripSymbols(3));
        assertEquals(null, getBoxTripSymbols(4));

        // Can't go beyond start of penalty.
        bt.execute(BoxTrip.Command.END_EARLIER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals(" +", getBoxTripSymbols(2));
        assertEquals("", getBoxTripSymbols(3));
        assertEquals(null, getBoxTripSymbols(4));
    }


    @Test
    public void testEndLater() {
        // Penalty is ongoing, but skater isn't in the upcoming jam.
        bt.execute(BoxTrip.Command.END_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals(" -", getBoxTripSymbols(2));
        assertEquals(" $", getBoxTripSymbols(3));
        assertEquals(null, getBoxTripSymbols(4));

        // Field them in upcoming jam.
        t.field(s, Role.PIVOT);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals(" -", getBoxTripSymbols(2));
        assertEquals(" $", getBoxTripSymbols(3));
        assertEquals("", getBoxTripSymbols(4));
        assertEquals(false, s.getFielding(t.getRunningOrUpcomingTeamJam()).isInBox());

        // Now can mark penalty as ongoing.
        bt.execute(BoxTrip.Command.END_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals(" -", getBoxTripSymbols(2));
        assertEquals(" S", getBoxTripSymbols(3));
        assertEquals(" S", getBoxTripSymbols(4));
        assertEquals(true, s.getFielding(t.getRunningOrUpcomingTeamJam()).isInBox());

        // Can't go beyond ongoing.
        bt.execute(BoxTrip.Command.END_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals(" -", getBoxTripSymbols(2));
        assertEquals(" S", getBoxTripSymbols(3));
        assertEquals(" S", getBoxTripSymbols(4));
        assertEquals(true, s.getFielding(t.getRunningOrUpcomingTeamJam()).isInBox());
    }

    @Test
    public void testAdvanceEndToUpcomingAndBack() {
        t.field(s, Role.PIVOT);
        bt.execute(BoxTrip.Command.END_LATER);
        bt.execute(BoxTrip.Command.END_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals(" -", getBoxTripSymbols(2));
        assertEquals(" S", getBoxTripSymbols(3));
        assertEquals(" S", getBoxTripSymbols(4));

        bt.execute(BoxTrip.Command.END_EARLIER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals(" -", getBoxTripSymbols(2));
        assertEquals(" $", getBoxTripSymbols(3));
        assertEquals("", getBoxTripSymbols(4));

        bt.execute(BoxTrip.Command.END_EARLIER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals(" +", getBoxTripSymbols(2));
        assertEquals("", getBoxTripSymbols(3));
        assertEquals("", getBoxTripSymbols(4));
    }

    @Test
    public void testAdvanceStartToUpcomingAndBack() {
        t.field(s, Role.PIVOT);
        bt.execute(BoxTrip.Command.END_LATER);
        bt.execute(BoxTrip.Command.END_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals(" -", getBoxTripSymbols(2));
        assertEquals(" S", getBoxTripSymbols(3));
        assertEquals(" S", getBoxTripSymbols(4));

        bt.execute(BoxTrip.Command.START_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("", getBoxTripSymbols(2));
        assertEquals(" S", getBoxTripSymbols(3));
        assertEquals(" S", getBoxTripSymbols(4));

        bt.execute(BoxTrip.Command.START_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("", getBoxTripSymbols(2));
        assertEquals(" -", getBoxTripSymbols(3));
        assertEquals(" S", getBoxTripSymbols(4));

        bt.execute(BoxTrip.Command.START_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("", getBoxTripSymbols(2));
        assertEquals("", getBoxTripSymbols(3));
        assertEquals(" S", getBoxTripSymbols(4));

        // Can't go beyond upcoming jam.
        bt.execute(BoxTrip.Command.START_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("", getBoxTripSymbols(2));
        assertEquals("", getBoxTripSymbols(3));
        assertEquals(" S", getBoxTripSymbols(4));

        bt.execute(BoxTrip.Command.START_EARLIER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("", getBoxTripSymbols(2));
        assertEquals(" -", getBoxTripSymbols(3));
        assertEquals(" S", getBoxTripSymbols(4));

        bt.execute(BoxTrip.Command.START_EARLIER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("", getBoxTripSymbols(2));
        assertEquals(" S", getBoxTripSymbols(3));
        assertEquals(" S", getBoxTripSymbols(4));

        bt.execute(BoxTrip.Command.START_EARLIER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals(" -", getBoxTripSymbols(2));
        assertEquals(" S", getBoxTripSymbols(3));
        assertEquals(" S", getBoxTripSymbols(4));

        bt.execute(BoxTrip.Command.START_EARLIER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals(" S", getBoxTripSymbols(2));
        assertEquals(" S", getBoxTripSymbols(3));
        assertEquals(" S", getBoxTripSymbols(4));
    }

}
