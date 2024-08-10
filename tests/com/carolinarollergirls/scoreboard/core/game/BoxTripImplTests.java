package com.carolinarollergirls.scoreboard.core.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.carolinarollergirls.scoreboard.core.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.core.interfaces.BoxTrip;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame;
import com.carolinarollergirls.scoreboard.core.interfaces.Fielding;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Penalty;
import com.carolinarollergirls.scoreboard.core.interfaces.Role;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.Skater;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Source;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.rules.Rule;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class BoxTripImplTests {

    private ScoreBoard sb;
    private Game g;
    private Team t;
    private Skater s;
    private BoxTrip bt;

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
        GameImpl.setQuickClockThreshold(0L);
        sb = new ScoreBoardImpl(false);
        sb.postAutosaveUpdate();
        sb.getSettings().set(ScoreBoard.SETTING_USE_LT, "true");
        g = sb.getCurrentGame().get(CurrentGame.GAME);
        sb.addScoreBoardListener(batchCounter);

        ScoreBoardClock.getInstance().stop();
        t = g.getTeam(Team.ID_1);
        s = new SkaterImpl(t, (String) null);
        t.addSkater(s);
        g.startJam();
        g.stopJamTO();
        g.startJam();
        t.field(s, Role.PIVOT);
        s.setPenaltyBox(true);
        bt = s.getFielding(t.getRunningOrUpcomingTeamJam()).get(Fielding.CURRENT_BOX_TRIP);
        g.stopJamTO();
        g.startJam();
        s.setPenaltyBox(false);
        g.stopJamTO();
        t.execute(Team.ADVANCE_FIELDINGS);
        // Now going into jam 4. Skater had a box trip that started in jam 2, and ended
        // in jam 3.
    }

    @After
    public void tearDown() throws Exception {
        ScoreBoardClock.getInstance().start(false);
        GameImpl.setQuickClockThreshold(1000L);
        // Check all started batches were ended.
        assertEquals(0, batchLevel);
    }

    private void advance(long time_ms) { ScoreBoardClock.getInstance().advance(time_ms); }

    private String getBoxTripSymbols(int jam) {
        Fielding f;
        if (jam == 4) {
            f = s.getFielding(t.getRunningOrUpcomingTeamJam());
        } else {
            f = s.getFielding(g.getCurrentPeriod().getJam(jam).getTeamJam(Team.ID_1));
        }
        return f == null ? null : f.get(Fielding.BOX_TRIP_SYMBOLS);
    }

    @Test
    public void testBaseCase() {
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("-", getBoxTripSymbols(2));
        assertEquals("$", getBoxTripSymbols(3));
        assertEquals(null, getBoxTripSymbols(4));
    }

    @Test
    public void testStartEarlier() {
        // Start before jam.
        bt.execute(BoxTrip.START_EARLIER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("S", getBoxTripSymbols(2));
        assertEquals("$", getBoxTripSymbols(3));
        assertEquals(null, getBoxTripSymbols(4));

        // Can't go before the skater was present.
        bt.execute(BoxTrip.START_EARLIER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("S", getBoxTripSymbols(2));
        assertEquals("$", getBoxTripSymbols(3));
        assertEquals(null, getBoxTripSymbols(4));
    }

    @Test
    public void testStartLater() {
        // Start between jams.
        bt.execute(BoxTrip.START_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("", getBoxTripSymbols(2));
        assertEquals("$", getBoxTripSymbols(3));
        assertEquals(null, getBoxTripSymbols(4));

        // Start in next jam.
        bt.execute(BoxTrip.START_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("", getBoxTripSymbols(2));
        assertEquals("+", getBoxTripSymbols(3));
        assertEquals(null, getBoxTripSymbols(4));

        // Can't go beyond end of penalty.
        bt.execute(BoxTrip.START_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("", getBoxTripSymbols(2));
        assertEquals("+", getBoxTripSymbols(3));
        assertEquals(null, getBoxTripSymbols(4));
    }

    @Test
    public void testEndEarlier() {
        // End between Jams
        bt.execute(BoxTrip.END_EARLIER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("-", getBoxTripSymbols(2));
        assertEquals("", getBoxTripSymbols(3));
        assertEquals(null, getBoxTripSymbols(4));

        // End during previous.
        bt.execute(BoxTrip.END_EARLIER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("+", getBoxTripSymbols(2));
        assertEquals("", getBoxTripSymbols(3));
        assertEquals(null, getBoxTripSymbols(4));

        // Can't go beyond start of penalty.
        bt.execute(BoxTrip.END_EARLIER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("+", getBoxTripSymbols(2));
        assertEquals("", getBoxTripSymbols(3));
        assertEquals(null, getBoxTripSymbols(4));
    }

    @Test
    public void testEndLater() {
        bt.execute(BoxTrip.END_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("-", getBoxTripSymbols(2));
        assertEquals("S", getBoxTripSymbols(3));
        assertEquals(null, getBoxTripSymbols(4));

        bt.execute(BoxTrip.END_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("-", getBoxTripSymbols(2));
        assertEquals("S", getBoxTripSymbols(3));
        assertEquals("S", getBoxTripSymbols(4));
        assertEquals(true, s.getFielding(t.getRunningOrUpcomingTeamJam()).isInBox());

        // Can't go beyond ongoing.
        bt.execute(BoxTrip.END_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("-", getBoxTripSymbols(2));
        assertEquals("S", getBoxTripSymbols(3));
        assertEquals("S", getBoxTripSymbols(4));
        assertEquals(true, s.getFielding(t.getRunningOrUpcomingTeamJam()).isInBox());
    }

    @Test
    public void testAdvanceEndToUpcomingAndBack() {
        t.field(s, Role.PIVOT);
        bt.execute(BoxTrip.END_LATER);
        bt.execute(BoxTrip.END_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("-", getBoxTripSymbols(2));
        assertEquals("S", getBoxTripSymbols(3));
        assertEquals("S", getBoxTripSymbols(4));

        bt.execute(BoxTrip.END_EARLIER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("-", getBoxTripSymbols(2));
        assertEquals("S", getBoxTripSymbols(3));
        assertEquals("", getBoxTripSymbols(4));

        bt.execute(BoxTrip.END_EARLIER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("-", getBoxTripSymbols(2));
        assertEquals("$", getBoxTripSymbols(3));
        assertEquals("", getBoxTripSymbols(4));

        bt.execute(BoxTrip.END_EARLIER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("-", getBoxTripSymbols(2));
        assertEquals("", getBoxTripSymbols(3));
        assertEquals("", getBoxTripSymbols(4));

        bt.execute(BoxTrip.END_EARLIER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("+", getBoxTripSymbols(2));
        assertEquals("", getBoxTripSymbols(3));
        assertEquals("", getBoxTripSymbols(4));
    }

    @Test
    public void testAdvanceStartToUpcomingAndBack() {
        t.field(s, Role.PIVOT);
        bt.execute(BoxTrip.END_LATER);
        bt.execute(BoxTrip.END_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("-", getBoxTripSymbols(2));
        assertEquals("S", getBoxTripSymbols(3));
        assertEquals("S", getBoxTripSymbols(4));

        bt.execute(BoxTrip.START_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("", getBoxTripSymbols(2));
        assertEquals("S", getBoxTripSymbols(3));
        assertEquals("S", getBoxTripSymbols(4));

        bt.execute(BoxTrip.START_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("", getBoxTripSymbols(2));
        assertEquals("-", getBoxTripSymbols(3));
        assertEquals("S", getBoxTripSymbols(4));

        bt.execute(BoxTrip.START_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("", getBoxTripSymbols(2));
        assertEquals("", getBoxTripSymbols(3));
        assertEquals("S", getBoxTripSymbols(4));

        // Can't go beyond upcoming jam.
        bt.execute(BoxTrip.START_LATER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("", getBoxTripSymbols(2));
        assertEquals("", getBoxTripSymbols(3));
        assertEquals("S", getBoxTripSymbols(4));

        bt.execute(BoxTrip.START_EARLIER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("", getBoxTripSymbols(2));
        assertEquals("-", getBoxTripSymbols(3));
        assertEquals("S", getBoxTripSymbols(4));

        bt.execute(BoxTrip.START_EARLIER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("", getBoxTripSymbols(2));
        assertEquals("S", getBoxTripSymbols(3));
        assertEquals("S", getBoxTripSymbols(4));

        bt.execute(BoxTrip.START_EARLIER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("-", getBoxTripSymbols(2));
        assertEquals("S", getBoxTripSymbols(3));
        assertEquals("S", getBoxTripSymbols(4));

        bt.execute(BoxTrip.START_EARLIER);
        assertEquals(null, getBoxTripSymbols(1));
        assertEquals("S", getBoxTripSymbols(2));
        assertEquals("S", getBoxTripSymbols(3));
        assertEquals("S", getBoxTripSymbols(4));
    }

    @Test
    public void testStartStopTimer() {
        g.execute(Game.START_BOX_TRIP);
        assertEquals(1, g.numberOf(Team.BOX_TRIP));
        BoxTrip newBt = g.getAll(Team.BOX_TRIP).iterator().next();
        assertNotNull(newBt.getClock());
        assertFalse(newBt.getClock().isRunning());
        assertEquals(30000, g.getLong(Rule.PENALTY_DURATION));
        assertEquals(30000, newBt.getClock().getTimeRemaining());

        g.startJam();
        assertTrue(newBt.getClock().isRunning());
        advance(5000);
        g.stopJamTO();
        assertFalse(newBt.getClock().isRunning());
        assertEquals(25000, newBt.getClock().getTimeRemaining());

        g.startJam();
        assertTrue(newBt.getClock().isRunning());
        newBt.set(BoxTrip.TIMING_STOPPED, true);
        assertFalse(newBt.getClock().isRunning());
        newBt.set(BoxTrip.TIMING_STOPPED, false);
        assertTrue(newBt.getClock().isRunning());
        newBt.set(BoxTrip.TIMING_STOPPED, true);
        assertFalse(newBt.getClock().isRunning());
        g.stopJamTO();
        assertFalse(newBt.getClock().isRunning());
        g.startJam();
        assertFalse(newBt.getClock().isRunning());
        g.stopJamTO();
        newBt.set(BoxTrip.TIMING_STOPPED, false);
        assertFalse(newBt.getClock().isRunning());
        g.startJam();
        assertTrue(newBt.getClock().isRunning());
    }

    @Test
    public void testStartDuringJam() {
        g.startJam();
        g.execute(Game.START_BOX_TRIP);
        BoxTrip newBt = g.getAll(Team.BOX_TRIP).iterator().next();
        assertNotNull(newBt.getClock());
        assertTrue(newBt.getClock().isRunning());
    }

    @Test
    public void testAssignFieldingAndPenalties() {
        g.execute(Game.START_BOX_TRIP);
        BoxTrip newBt = g.getAll(Team.BOX_TRIP).iterator().next();
        assertNotNull(newBt.getClock());
        assertEquals(0, s.numberOf(Skater.PENALTY));
        assertEquals(0, (int) s.get(Skater.PENALTY_COUNT));
        assertEquals(0, s.getUnservedPenalties().size());

        t.field(s, Role.BLOCKER);
        newBt.set(BoxTrip.CURRENT_FIELDING, s.getCurrentFielding(), Source.WS);
        assertNotNull(newBt.getCurrentFielding());
        assertEquals(s.getCurrentFielding(), newBt.getCurrentFielding());
        assertEquals(1, s.numberOf(Skater.PENALTY));
        assertEquals(0, g.numberOf(Team.BOX_TRIP));
        assertEquals(30000L, newBt.getClock().getMaximumTime());
        assertEquals(30000L, newBt.getClock().getTimeRemaining());

        s.getOrCreate(Skater.PENALTY, 1);
        assertEquals(1, s.numberOf(Skater.PENALTY));
        assertEquals(30000L, newBt.getClock().getMaximumTime());
        assertEquals(30000L, newBt.getClock().getTimeRemaining());

        Penalty p = new PenaltyImpl(s, 2);
        s.add(Skater.PENALTY, p);
        assertEquals(60000L, newBt.getClock().getMaximumTime());
        assertEquals(60000L, newBt.getClock().getTimeRemaining());

        p.delete();
        assertEquals(30000L, newBt.getClock().getMaximumTime());
        assertEquals(30000L, newBt.getClock().getTimeRemaining());

        g.startJam();
        advance(5000L);
        p = new PenaltyImpl(s, 2);
        s.add(Skater.PENALTY, p);
        assertEquals(60000L, newBt.getClock().getMaximumTime());
        assertEquals(55000L, newBt.getClock().getTimeRemaining());

        advance(30000L);
        assertTrue(newBt.getClock().isRunning());
        assertEquals(25000L, newBt.getClock().getTimeRemaining());

        p.delete();
        assertEquals(30000L, newBt.getClock().getMaximumTime());
        assertEquals(0L, newBt.getClock().getTimeRemaining());

        advance(5000L);
        p = new PenaltyImpl(s, 2);
        s.add(Skater.PENALTY, p);
        assertEquals(60000L, newBt.getClock().getMaximumTime());
        assertEquals(20000L, newBt.getClock().getTimeRemaining());
    }

    @Test
    public void testStartWithPenalties() {
        s.add(Skater.PENALTY, new PenaltyImpl(s, 2));
        s.add(Skater.PENALTY, new PenaltyImpl(s, 3));
        t.field(s, Role.BLOCKER);
        g.execute(Game.START_BOX_TRIP);
        BoxTrip newBt = g.getAll(Team.BOX_TRIP).iterator().next();
        newBt.set(BoxTrip.CURRENT_FIELDING, s.getCurrentFielding(), Source.WS);

        assertEquals(60000L, newBt.getClock().getMaximumTime());
    }

    @Test
    public void testJammerSwapSimple() {
        Team t2 = g.getTeam(Team.ID_2);
        t2.execute(Team.ADVANCE_FIELDINGS);
        Skater s2 = t2.getOrCreate(Team.SKATER, "Skater2");
        t2.field(s2, Role.JAMMER);
        t.field(s, Role.JAMMER);
        g.startJam();
        s.add(Skater.PENALTY, new PenaltyImpl(s, 2));
        s2.add(Skater.PENALTY, new PenaltyImpl(s2, 1));

        g.execute(Game.START_BOX_TRIP);
        BoxTrip bt1 = g.getAll(Team.BOX_TRIP).iterator().next();
        bt1.set(BoxTrip.CURRENT_FIELDING, s.getCurrentFielding(), Source.WS);
        advance(7000L);

        assertEquals(23000L, bt1.getClock().getTimeRemaining());
        assertTrue(s.getCurrentFielding().isInBox());

        g.execute(Game.START_JAMMER_BOX_TRIP);
        BoxTrip bt2 = s2.getCurrentFielding().getCurrentBoxTrip();
        assertNotNull(bt2.getClock());
        assertTrue(s2.getCurrentFielding().isInBox());
        assertEquals(0L, bt1.getClock().getTimeRemaining());
        assertEquals(7000L, bt2.getClock().getTimeRemaining());
    }

    @Test
    public void testJammerSwapJammerToExPivot() {
        Team t2 = g.getTeam(Team.ID_2);
        t2.execute(Team.ADVANCE_FIELDINGS);
        Skater s2 = t2.getOrCreate(Team.SKATER, "Skater2");
        t2.field(s2, Role.PIVOT);
        t.field(s, Role.JAMMER);
        g.startJam();
        t2.set(Team.STAR_PASS, true);
        s.add(Skater.PENALTY, new PenaltyImpl(s, 2));
        s2.add(Skater.PENALTY, new PenaltyImpl(s2, 1));

        g.execute(Game.START_BOX_TRIP);
        BoxTrip bt1 = g.getAll(Team.BOX_TRIP).iterator().next();
        bt1.set(BoxTrip.CURRENT_FIELDING, s.getCurrentFielding(), Source.WS);
        advance(7000L);

        assertEquals(23000L, bt1.getClock().getTimeRemaining());
        assertTrue(s.getCurrentFielding().isInBox());

        g.execute(Game.START_JAMMER_BOX_TRIP);
        BoxTrip bt2 = s2.getCurrentFielding().getCurrentBoxTrip();
        assertNotNull(bt2.getClock());
        assertTrue(s2.getCurrentFielding().isInBox());
        assertEquals(0L, bt1.getClock().getTimeRemaining());
        assertEquals(7000L, bt2.getClock().getTimeRemaining());
    }

    @Test
    public void testJammerSwapExPivotToJammer() {
        Team t2 = g.getTeam(Team.ID_2);
        t2.execute(Team.ADVANCE_FIELDINGS);
        Skater s2 = t2.getOrCreate(Team.SKATER, "Skater2");
        t2.field(s2, Role.JAMMER);
        t.field(s, Role.PIVOT);
        g.startJam();
        t.set(Team.STAR_PASS, true);
        s.add(Skater.PENALTY, new PenaltyImpl(s, 2));
        s2.add(Skater.PENALTY, new PenaltyImpl(s2, 1));

        g.execute(Game.START_BOX_TRIP);
        BoxTrip bt1 = g.getAll(Team.BOX_TRIP).iterator().next();
        bt1.set(BoxTrip.CURRENT_FIELDING, s.getCurrentFielding(), Source.WS);
        advance(7000L);

        assertEquals(23000L, bt1.getClock().getTimeRemaining());
        assertTrue(s.getCurrentFielding().isInBox());

        g.execute(Game.START_JAMMER_BOX_TRIP);
        BoxTrip bt2 = s2.getCurrentFielding().getCurrentBoxTrip();
        assertNotNull(bt2.getClock());
        assertTrue(s2.getCurrentFielding().isInBox());
        assertEquals(0L, bt1.getClock().getTimeRemaining());
        assertEquals(7000L, bt2.getClock().getTimeRemaining());
    }

    @Test
    public void testJammerSwapExPivotToExPivot() {
        Team t2 = g.getTeam(Team.ID_2);
        t2.execute(Team.ADVANCE_FIELDINGS);
        Skater s2 = t2.getOrCreate(Team.SKATER, "Skater2");
        t2.field(s2, Role.PIVOT);
        t.field(s, Role.PIVOT);
        g.startJam();
        t.set(Team.STAR_PASS, true);
        t2.set(Team.STAR_PASS, true);
        s.add(Skater.PENALTY, new PenaltyImpl(s, 2));
        s2.add(Skater.PENALTY, new PenaltyImpl(s2, 1));

        g.execute(Game.START_BOX_TRIP);
        BoxTrip bt1 = g.getAll(Team.BOX_TRIP).iterator().next();
        bt1.set(BoxTrip.CURRENT_FIELDING, s.getCurrentFielding(), Source.WS);
        advance(7000L);

        assertEquals(23000L, bt1.getClock().getTimeRemaining());
        assertTrue(s.getCurrentFielding().isInBox());

        g.execute(Game.START_JAMMER_BOX_TRIP);
        BoxTrip bt2 = s2.getCurrentFielding().getCurrentBoxTrip();
        assertNotNull(bt2.getClock());
        assertTrue(s2.getCurrentFielding().isInBox());
        assertEquals(0L, bt1.getClock().getTimeRemaining());
        assertEquals(7000L, bt2.getClock().getTimeRemaining());
    }

    @Test
    public void testJammerSwapDoubleFirstToSingle() {
        Team t2 = g.getTeam(Team.ID_2);
        t2.execute(Team.ADVANCE_FIELDINGS);
        Skater s2 = t2.getOrCreate(Team.SKATER, "Skater2");
        t2.field(s2, Role.JAMMER);
        t.field(s, Role.JAMMER);
        g.startJam();
        s.add(Skater.PENALTY, new PenaltyImpl(s, 2));
        s.add(Skater.PENALTY, new PenaltyImpl(s, 3));
        s2.add(Skater.PENALTY, new PenaltyImpl(s2, 1));

        g.execute(Game.START_BOX_TRIP);
        BoxTrip bt1 = g.getAll(Team.BOX_TRIP).iterator().next();
        bt1.set(BoxTrip.CURRENT_FIELDING, s.getCurrentFielding(), Source.WS);
        advance(7000L);

        assertEquals(53000L, bt1.getClock().getTimeRemaining());
        assertTrue(s.getCurrentFielding().isInBox());

        g.execute(Game.START_JAMMER_BOX_TRIP);
        BoxTrip bt2 = s2.getCurrentFielding().getCurrentBoxTrip();
        assertNotNull(bt2.getClock());
        assertTrue(s2.getCurrentFielding().isInBox());
        assertEquals(23000L, bt1.getClock().getTimeRemaining());
        assertEquals(0L, bt2.getClock().getTimeRemaining());
    }

    @Test
    public void testJammerSwapDoubleSecondToSingle() {
        Team t2 = g.getTeam(Team.ID_2);
        t2.execute(Team.ADVANCE_FIELDINGS);
        Skater s2 = t2.getOrCreate(Team.SKATER, "Skater2");
        t2.field(s2, Role.JAMMER);
        t.field(s, Role.JAMMER);
        g.startJam();
        s.add(Skater.PENALTY, new PenaltyImpl(s, 2));
        s.add(Skater.PENALTY, new PenaltyImpl(s, 3));
        s2.add(Skater.PENALTY, new PenaltyImpl(s2, 1));

        g.execute(Game.START_BOX_TRIP);
        BoxTrip bt1 = g.getAll(Team.BOX_TRIP).iterator().next();
        bt1.set(BoxTrip.CURRENT_FIELDING, s.getCurrentFielding(), Source.WS);
        advance(41000L);

        assertEquals(19000L, bt1.getClock().getTimeRemaining());
        assertTrue(s.getCurrentFielding().isInBox());

        g.execute(Game.START_JAMMER_BOX_TRIP);
        BoxTrip bt2 = s2.getCurrentFielding().getCurrentBoxTrip();
        assertNotNull(bt2.getClock());
        assertTrue(s2.getCurrentFielding().isInBox());
        assertEquals(0L, bt1.getClock().getTimeRemaining());
        assertEquals(11000L, bt2.getClock().getTimeRemaining());
    }

    @Test
    public void testJammerSwapDoubleFirstToDouble() {
        Team t2 = g.getTeam(Team.ID_2);
        t2.execute(Team.ADVANCE_FIELDINGS);
        Skater s2 = t2.getOrCreate(Team.SKATER, "Skater2");
        t2.field(s2, Role.JAMMER);
        t.field(s, Role.JAMMER);
        g.startJam();
        s.add(Skater.PENALTY, new PenaltyImpl(s, 2));
        s.add(Skater.PENALTY, new PenaltyImpl(s, 3));
        s2.add(Skater.PENALTY, new PenaltyImpl(s2, 1));
        s2.add(Skater.PENALTY, new PenaltyImpl(s2, 2));

        g.execute(Game.START_BOX_TRIP);
        BoxTrip bt1 = g.getAll(Team.BOX_TRIP).iterator().next();
        bt1.set(BoxTrip.CURRENT_FIELDING, s.getCurrentFielding(), Source.WS);
        advance(7000L);

        assertEquals(53000L, bt1.getClock().getTimeRemaining());
        assertTrue(s.getCurrentFielding().isInBox());

        g.execute(Game.START_JAMMER_BOX_TRIP);
        BoxTrip bt2 = s2.getCurrentFielding().getCurrentBoxTrip();
        assertNotNull(bt2.getClock());
        assertTrue(s2.getCurrentFielding().isInBox());
        assertEquals(0L, bt1.getClock().getTimeRemaining());
        assertEquals(7000L, bt2.getClock().getTimeRemaining());
    }

    @Test
    public void testJammerSwapDoubleSecondToDouble() {
        Team t2 = g.getTeam(Team.ID_2);
        t2.execute(Team.ADVANCE_FIELDINGS);
        Skater s2 = t2.getOrCreate(Team.SKATER, "Skater2");
        t2.field(s2, Role.JAMMER);
        t.field(s, Role.JAMMER);
        g.startJam();
        s.add(Skater.PENALTY, new PenaltyImpl(s, 2));
        s.add(Skater.PENALTY, new PenaltyImpl(s, 3));
        s2.add(Skater.PENALTY, new PenaltyImpl(s2, 1));
        s2.add(Skater.PENALTY, new PenaltyImpl(s2, 2));

        g.execute(Game.START_BOX_TRIP);
        BoxTrip bt1 = g.getAll(Team.BOX_TRIP).iterator().next();
        bt1.set(BoxTrip.CURRENT_FIELDING, s.getCurrentFielding(), Source.WS);
        advance(41000L);

        assertEquals(19000L, bt1.getClock().getTimeRemaining());
        assertTrue(s.getCurrentFielding().isInBox());

        g.execute(Game.START_JAMMER_BOX_TRIP);
        BoxTrip bt2 = s2.getCurrentFielding().getCurrentBoxTrip();
        assertNotNull(bt2.getClock());
        assertTrue(s2.getCurrentFielding().isInBox());
        assertEquals(0L, bt1.getClock().getTimeRemaining());
        assertEquals(41000L, bt2.getClock().getTimeRemaining());
    }

    @Test
    public void testJammerSwapLateSecond() {
        Team t2 = g.getTeam(Team.ID_2);
        t2.execute(Team.ADVANCE_FIELDINGS);
        Skater s2 = t2.getOrCreate(Team.SKATER, "Skater2");
        t2.field(s2, Role.JAMMER);
        t.field(s, Role.JAMMER);
        g.startJam();
        s.add(Skater.PENALTY, new PenaltyImpl(s, 2));
        s.add(Skater.PENALTY, new PenaltyImpl(s, 3));
        s2.add(Skater.PENALTY, new PenaltyImpl(s2, 1));

        g.execute(Game.START_BOX_TRIP);
        BoxTrip bt1 = g.getAll(Team.BOX_TRIP).iterator().next();
        bt1.set(BoxTrip.CURRENT_FIELDING, s.getCurrentFielding(), Source.WS);
        advance(7000L);

        assertEquals(53000L, bt1.getClock().getTimeRemaining());
        assertTrue(s.getCurrentFielding().isInBox());

        g.execute(Game.START_JAMMER_BOX_TRIP);
        BoxTrip bt2 = s2.getCurrentFielding().getCurrentBoxTrip();
        assertNotNull(bt2.getClock());
        assertTrue(s2.getCurrentFielding().isInBox());
        assertEquals(23000L, bt1.getClock().getTimeRemaining());
        assertEquals(0L, bt2.getClock().getTimeRemaining());

        advance(3000L);

        s2.add(Skater.PENALTY, new PenaltyImpl(s2, 2));
        assertEquals(0L, bt1.getClock().getTimeRemaining());
        assertEquals(7000L, bt2.getClock().getTimeRemaining());
    }

    @Test
    public void testJammerSwapLateId() {
        Team t2 = g.getTeam(Team.ID_2);
        t2.execute(Team.ADVANCE_FIELDINGS);
        Skater s2 = t2.getOrCreate(Team.SKATER, "Skater2");
        t2.field(s2, Role.JAMMER);
        t.field(s, Role.JAMMER);
        g.startJam();
        s.add(Skater.PENALTY, new PenaltyImpl(s, 2));
        s2.add(Skater.PENALTY, new PenaltyImpl(s2, 1));

        g.execute(Game.START_BOX_TRIP);
        BoxTrip bt1 = g.getAll(Team.BOX_TRIP).iterator().next();
        bt1.set(BoxTrip.CURRENT_FIELDING, s.getCurrentFielding(), Source.WS);
        advance(7000L);

        assertEquals(23000L, bt1.getClock().getTimeRemaining());
        assertTrue(s.getCurrentFielding().isInBox());

        g.execute(Game.START_BOX_TRIP);
        BoxTrip bt2 = g.getAll(Team.BOX_TRIP).iterator().next();

        advance(4000L);

        bt2.set(BoxTrip.CURRENT_FIELDING, s2.getCurrentFielding(), Source.WS);

        assertNotNull(bt2.getClock());
        assertTrue(s2.getCurrentFielding().isInBox());
        assertEquals(0L, bt1.getClock().getTimeRemaining());
        assertEquals(7000L, bt2.getClock().getTimeRemaining());
    }

    @Test
    public void testJammerSwapABA() {
        Team t2 = g.getTeam(Team.ID_2);
        t2.execute(Team.ADVANCE_FIELDINGS);
        Skater s2 = t2.getOrCreate(Team.SKATER, "Skater2");
        t2.field(s2, Role.JAMMER);
        t.field(s, Role.JAMMER);
        g.startJam();
        s.add(Skater.PENALTY, new PenaltyImpl(s, 2));
        s2.add(Skater.PENALTY, new PenaltyImpl(s2, 1));

        g.execute(Game.START_BOX_TRIP);
        BoxTrip bt1 = g.getAll(Team.BOX_TRIP).iterator().next();
        bt1.set(BoxTrip.CURRENT_FIELDING, s.getCurrentFielding(), Source.WS);
        advance(26000L);

        g.execute(Game.START_JAMMER_BOX_TRIP);
        BoxTrip bt2 = s2.getCurrentFielding().getCurrentBoxTrip();
        assertNotNull(bt2.getClock());
        assertTrue(s2.getCurrentFielding().isInBox());
        assertEquals(0L, bt1.getClock().getTimeRemaining());
        assertEquals(26000L, bt2.getClock().getTimeRemaining());

        bt1.set(BoxTrip.IS_CURRENT, false);
        assertFalse(s.isPenaltyBox());

        advance(11000L);

        s.add(Skater.PENALTY, new PenaltyImpl(s, 3));
        g.execute(Game.START_JAMMER_BOX_TRIP);
        bt1 = s.getCurrentFielding().getCurrentBoxTrip();
        assertEquals(30000L, bt1.getClock().getTimeRemaining());
        assertEquals(15000L, bt2.getClock().getTimeRemaining());
    }
}
