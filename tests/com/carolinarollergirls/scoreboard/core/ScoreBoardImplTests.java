package com.carolinarollergirls.scoreboard.core;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;

public class ScoreBoardImplTests {

    private ScoreBoardImpl sb;

    @Before
    public void setUp() throws Exception {
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
}
