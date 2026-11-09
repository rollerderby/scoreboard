package com.carolinarollergirls.scoreboard.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.ref.WeakReference;

import org.junit.Before;
import org.junit.Test;

import com.carolinarollergirls.scoreboard.core.game.GameImpl;
import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame.CurrentClock;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame.CurrentTeam;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame.CurrentTimeout;
import com.carolinarollergirls.scoreboard.core.interfaces.FloorPosition;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Period;
import com.carolinarollergirls.scoreboard.core.interfaces.Role;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.Skater;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.core.interfaces.TeamJam;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Source;

public class ScoreBoardImplTests {

    private ScoreBoardImpl sb;

    @Before
    public void setUp() throws Exception {
        GameImpl.setQuickClockThreshold(0L);
        sb = new ScoreBoardImpl(false);
        sb.postAutosaveUpdate();
    }

    @Test
    public void testDeletedGamesStopTheirThreads() throws Exception {
        Thread.sleep(200);
        int before = Thread.activeCount();
        for (int i = 0; i < 20; i++) {
            Game g = sb.getOrCreate(ScoreBoard.GAME, "game" + i);
            sb.remove(ScoreBoard.GAME, g);
        }
        // each game has threads of its own (file writer, trip score timers) that must end with it
        int after = Thread.activeCount();
        for (int i = 0; i < 50 && after > before + 5; i++) {
            Thread.sleep(20);
            after = Thread.activeCount();
        }
        assertTrue("threads before: " + before + ", after deleting 20 games: " + after, after <= before + 5);
    }

    private void play(Game g) {
        Skater s = g.getTeam(Team.ID_1).getOrCreate(Team.SKATER, "skater");
        g.getTeam(Team.ID_1).field(s, Role.JAMMER);
        g.startJam();
        g.stopJamTO();
    }

    private void assertFreed(WeakReference<Game> ref) throws Exception {
        for (int i = 0; i < 100 && ref.get() != null; i++) {
            System.gc();
            Thread.sleep(20);
        }
        assertNull("deleted game is still in memory", ref.get());
    }

    @Test
    public void testDeletedGameIsFreed() throws Exception {
        Game g = sb.getOrCreate(ScoreBoard.GAME, "deleted");
        WeakReference<Game> ref = new WeakReference<>(g);
        sb.remove(ScoreBoard.GAME, g);
        g = null;

        assertFreed(ref);
    }

    @Test
    public void testDeletedRunningGameIsFreed() throws Exception {
        Game g = sb.getOrCreate(ScoreBoard.GAME, "deleted");
        g.getClock(Clock.ID_INTERMISSION).start();
        WeakReference<Game> ref = new WeakReference<>(g);
        sb.remove(ScoreBoard.GAME, g);
        g = null;

        assertFreed(ref);
    }

    @Test
    public void testDeletedCurrentGameIsFreed() throws Exception {
        Game g = sb.getCurrentGame().get(CurrentGame.GAME);
        play(g);
        WeakReference<Game> ref = new WeakReference<>(g);
        sb.remove(ScoreBoard.GAME, g);
        g = null;

        assertFreed(ref);
        assertNotNull(sb.getCurrentGame().get(CurrentGame.GAME));
    }

    @Test
    public void testDeletedFormerCurrentGameIsFreed() throws Exception {
        Game g = sb.getCurrentGame().get(CurrentGame.GAME);
        play(g);
        sb.getCurrentGame().set(CurrentGame.GAME, sb.getOrCreate(ScoreBoard.GAME, "next"));
        WeakReference<Game> ref = new WeakReference<>(g);
        sb.remove(ScoreBoard.GAME, g);
        g = null;

        assertFreed(ref);
    }

    @Test
    public void testPreviousGameKeepsNoMirrors() {
        Game first = sb.getCurrentGame().get(CurrentGame.GAME);
        play(first);
        String clockId = first.getClock(Clock.ID_JAM).getId();
        String teamId = first.getTeam(Team.ID_1).getId();
        assertNotNull(sb.getElement(CurrentClock.class, clockId));
        assertNotNull(sb.getElement(CurrentTeam.class, teamId));

        sb.getCurrentGame().set(CurrentGame.GAME, sb.getOrCreate(ScoreBoard.GAME, "next"));

        assertNull(sb.getElement(CurrentClock.class, clockId));
        assertNull(sb.getElement(CurrentTeam.class, teamId));
    }

    @Test
    public void testPreviousGameKeepsItsData() {
        Game first = sb.getCurrentGame().get(CurrentGame.GAME);
        play(first);
        TeamJam tj = first.getCurrentPeriod().getJam(1).getTeamJam(Team.ID_1);
        String skater = tj.getFielding(FloorPosition.JAMMER).getSkater().getId();
        int jams = first.getCurrentPeriod().numberOf(Period.JAM);

        sb.getCurrentGame().set(CurrentGame.GAME, sb.getOrCreate(ScoreBoard.GAME, "next"));

        assertEquals(jams, first.getCurrentPeriod().numberOf(Period.JAM));
        assertEquals(skater, tj.getFielding(FloorPosition.JAMMER).getSkater().getId());
    }

    @Test
    public void testSwitchingBackToAGame() {
        Game first = sb.getCurrentGame().get(CurrentGame.GAME);
        play(first);
        CurrentGame cg = sb.getCurrentGame();
        sb.getCurrentGame().set(CurrentGame.GAME, sb.getOrCreate(ScoreBoard.GAME, "next"));
        sb.getCurrentGame().set(CurrentGame.GAME, first);

        assertEquals(first, cg.get(CurrentGame.GAME));
        assertEquals(1, (int) cg.get(Game.CURRENT_PERIOD_NUMBER));
        CurrentClock jc = sb.getElement(CurrentClock.class, first.getClock(Clock.ID_JAM).getId());
        assertNotNull(jc);
        assertEquals(first.getClock(Clock.ID_JAM).getNumber(), (int) jc.get(Clock.NUMBER));

        // the mirrors follow the game again
        first.startJam();
        assertEquals(true, jc.get(Clock.RUNNING));
        assertEquals(first.getClock(Clock.ID_JAM).getNumber(), (int) jc.get(Clock.NUMBER));
        assertTrue(cg.get(Game.IN_JAM));
    }

    @Test
    public void testSettingSameGameKeepsMirrors() {
        Game g = sb.getCurrentGame().get(CurrentGame.GAME);
        CurrentClock jc = sb.getElement(CurrentClock.class, g.getClock(Clock.ID_JAM).getId());
        assertNotNull(jc);

        sb.getCurrentGame().set(CurrentGame.GAME, g, Source.JSON);

        assertEquals(jc, sb.getElement(CurrentClock.class, g.getClock(Clock.ID_JAM).getId()));
        g.startJam();
        assertEquals(true, jc.get(Clock.RUNNING));
    }

    @Test
    public void testTimeoutMirrorFollowsNewGame() {
        sb.getCurrentGame().set(CurrentGame.GAME, sb.getOrCreate(ScoreBoard.GAME, "next"));
        Game next = sb.getCurrentGame().get(CurrentGame.GAME);

        assertEquals(next.getCurrentTimeout(),
                     ((CurrentTimeout) sb.getElement(CurrentTimeout.class, "noTimeout")).getSourceElement());
    }

    @Test
    public void testSwitchingGameAfterAutosaveLoad() {
        // an autosave has ScoreBoard.CurrentGame.Id with an empty value
        sb.getCurrentGame().set(CurrentGame.ID, "", Source.AUTOSAVE);
        Game next = sb.getOrCreate(ScoreBoard.GAME, "next");
        next.getTeam(Team.ID_1).set(Team.TEAM_NAME, "Next");
        CurrentGame cg = sb.getCurrentGame();
        cg.set(CurrentGame.GAME, next);

        assertEquals(next, cg.get(CurrentGame.GAME));
        assertEquals(next.get(Game.NAME), cg.get(Game.NAME));
        assertEquals("Next", cg.getMirror(Game.TEAM, Team.ID_1).get(Team.TEAM_NAME));
        next.startJam();
        assertTrue(cg.get(Game.IN_JAM));
    }
}
