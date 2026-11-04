package com.carolinarollergirls.scoreboard.core.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.jr.ob.JSON;

import com.carolinarollergirls.scoreboard.core.ScoreBoardImpl;
import com.carolinarollergirls.scoreboard.core.game.GameImpl;
import com.carolinarollergirls.scoreboard.core.interfaces.CurrentGame;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Period;
import com.carolinarollergirls.scoreboard.core.interfaces.Role;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.Skater;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.core.interfaces.Tracks;
import com.carolinarollergirls.scoreboard.core.interfaces.Tracks.PulledGame;
import com.carolinarollergirls.scoreboard.core.interfaces.Tracks.ServerGame;
import com.carolinarollergirls.scoreboard.core.interfaces.Tracks.Track;
import com.carolinarollergirls.scoreboard.core.interfaces.Tracks.TrackGame;
import com.carolinarollergirls.scoreboard.core.interfaces.Tracks.TracksException;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Source;
import com.carolinarollergirls.scoreboard.json.ScoreBoardJSONListener;
import com.carolinarollergirls.scoreboard.utils.BasePath;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public class TracksImplTests {

    @Rule
    public TemporaryFolder dir = new TemporaryFolder();
    private File oldDir;

    private ScoreBoardImpl server;
    private ScoreBoardImpl track2;
    private ScoreBoardImpl track3;
    private Game game;
    private boolean serverReachable = true;

    // the tracks reach the tournament server directly, with the request and answer going through JSON as over HTTP
    private TracksImpl.Remote remote = new TracksImpl.Remote() {
        @Override
        public Map<String, Object> call(String address, String action, Map<String, String> params,
                                        Map<String, Object> body) throws IOException {
            if (!serverReachable) { throw new IOException("Connection refused"); }
            assertEquals("tournament", address);
            Map<String, Object> b = body == null ? null : JSON.std.mapFrom(JSON.std.asString(body));
            if ("push".equals(action)) { lastPush = b; }
            try {
                return JSON.std.mapFrom(JSON.std.asString(server.getTracks().handle(action, params, b)));
            } catch (TracksException e) { throw new IOException(e.getMessage()); }
        }

        @Override
        public void sendStatus(String address, String track, Map<String, Object> status) throws IOException {
            if (!serverReachable) { throw new IOException("Connection refused"); }
            String error = server.getTracks().trackStatus(track, JSON.std.mapFrom(JSON.std.asString(status)), this,
                                                          "192.168.1.12");
            if (error != null) { throw new IOException(error); }
            statusMessages++;
        }

        @Override
        public boolean isConnected(String address) {
            return serverReachable;
        }

        @Override
        public void disconnect() {}
    };
    private int statusMessages = 0;
    private Map<String, Object> lastPush;

    @Before
    public void setUp() throws Exception {
        oldDir = BasePath.get();
        BasePath.set(dir.getRoot());
        dir.newFolder("config", "penalties");
        Files.copy(oldDir.toPath().resolve("config/penalties/wftda2018.json"),
                   dir.getRoot().toPath().resolve("config/penalties/wftda2018.json"));
        ScoreBoardClock.getInstance().stop();
        GameImpl.setQuickClockThreshold(0L);

        server = newScoreBoard();
        server.getSettings().set(Tracks.SETTING_TOURNAMENT_SERVER, "true");
        track2 = newScoreBoard();
        track2.getSettings().set(Tracks.SETTING_SERVER, "tournament");
        track2.getSettings().set(Tracks.SETTING_TRACK, "2");
        track3 = newScoreBoard();
        track3.getSettings().set(Tracks.SETTING_SERVER, "tournament");
        track3.getSettings().set(Tracks.SETTING_TRACK, "3");

        // a game prepared on the tournament server for track 2
        game = server.getOrCreate(ScoreBoard.GAME, "g1");
        game.getTeam(Team.ID_1).set(Team.TEAM_NAME, "Home");
        game.getTeam(Team.ID_2).set(Team.TEAM_NAME, "Away");
        game.add(Game.EVENT_INFO, new ValWithId(Game.INFO_GAME_NUMBER, "7"));
        game.add(Game.EVENT_INFO, new ValWithId(Tracks.INFO_TRACK, "2"));
    }

    private ScoreBoardImpl newScoreBoard() {
        ScoreBoardImpl sb = new ScoreBoardImpl(false);
        ((TracksImpl) sb.getTracks()).setRemote(remote);
        sb.postAutosaveUpdate();
        new ScoreBoardJSONListener(sb, sb.getJsm());
        return sb;
    }

    @After
    public void tearDown() throws Exception {
        BasePath.set(oldDir);
        ScoreBoardClock.getInstance().start(false);
        GameImpl.setQuickClockThreshold(1000L);
    }

    private void advance(long time_ms) { ScoreBoardClock.getInstance().advance(time_ms); }

    private Game pullOn(ScoreBoardImpl track) {
        track.getTracks().tick();
        ServerGame sg = track.getTracks().get(Tracks.SERVER_GAME, "g1");
        assertNotNull(sg);
        sg.execute(ServerGame.PULL);
        return track.getGame("g1");
    }

    @Test
    public void testOnlyWhenSetUp() {
        server.getSettings().set(Tracks.SETTING_TOURNAMENT_SERVER, "false");
        try {
            server.getTracks().handle("games", new HashMap<String, String>(), null);
            fail();
        } catch (TracksException e) { assertEquals(403, e.getStatus()); }
        track2.getTracks().tick();
        assertTrue(track2.getTracks().get(Tracks.SERVER_ERROR).contains("not set up as tournament server"));
        assertEquals(0, track2.getTracks().numberOf(Tracks.SERVER_GAME));
    }

    @Test
    public void testGameList() {
        track2.getTracks().tick();
        assertEquals("", track2.getTracks().get(Tracks.SERVER_ERROR));
        ServerGame sg = track2.getTracks().get(Tracks.SERVER_GAME, "g1");
        assertEquals("7", sg.get(ServerGame.GAME_NO));
        assertEquals("2", sg.get(ServerGame.TRACK));
        assertEquals("Prepared", sg.get(ServerGame.STATE));
        assertEquals("", sg.get(ServerGame.RUNS_ON));
        assertTrue(sg.get(ServerGame.NAME).contains("Home"));
        // the tournament server's own game is listed too
        assertEquals(2, track2.getTracks().numberOf(Tracks.SERVER_GAME));

        server.remove(ScoreBoard.GAME, game);
        track2.getTracks().tick();
        assertNull(track2.getTracks().get(Tracks.SERVER_GAME, "g1"));
    }

    @Test
    public void testPullAndPush() {
        Game g = pullOn(track2);
        assertNotNull(g);
        assertEquals("Home", g.getTeam(Team.ID_1).get(Team.TEAM_NAME));
        assertEquals(g, track2.getCurrentGame().getSourceElement());
        PulledGame pg = track2.getTracks().get(Tracks.PULLED_GAME, "g1");
        assertEquals("2", pg.get(PulledGame.TRACK));
        TrackGame tg = server.getTracks().get(Tracks.TRACK_GAME, "g1");
        assertEquals("2", tg.get(TrackGame.TRACK));

        // the copy on the tournament server can't be changed, the track's can
        assertNotNull(server.getTracks().readOnlyReason("ScoreBoard.Game(g1).Team(1).TeamName"));
        String ownGame = server.getCurrentGame().get(CurrentGame.GAME).getId();
        assertNull(server.getTracks().readOnlyReason("ScoreBoard.Game(" + ownGame + ").Team(1).TeamName"));
        assertNull(track2.getTracks().readOnlyReason("ScoreBoard.Game(g1).Team(1).TeamName"));
        assertNull(track2.getTracks().readOnlyReason("ScoreBoard.CurrentGame.Team(1).TeamName"));

        // play a jam on the track
        g.startJam();
        advance(30000);
        g.getTeam(Team.ID_1).set(Team.TRIP_SCORE, 4);
        track2.getTracks().tick(); // not sent during a jam
        assertEquals(Game.State.PREPARED, server.getGame("g1").get(Game.STATE));
        g.stopJamTO();
        advance(1000);
        track2.getTracks().tick();
        assertEquals("", pg.get(PulledGame.ERROR));
        assertNotEquals(0L, (long) pg.get(PulledGame.PUSHED));
        Game copy = server.getGame("g1");
        assertNotEquals(game, copy); // replaced
        assertEquals(4, copy.getTeam(Team.ID_1).getScore());
        assertEquals(Game.State.RUNNING, copy.get(Game.STATE));
        assertEquals(1, copy.getCurrentPeriodNumber());
        assertEquals(1, copy.getCurrentPeriod().numberOf(Period.JAM));

        // nothing changed: no push
        long pushed = pg.get(PulledGame.PUSHED);
        advance(1000);
        track2.getTracks().tick();
        assertEquals(pushed, (long) pg.get(PulledGame.PUSHED));

        // another jam, pushed again - replacing, not adding to, the copy
        g.startJam();
        advance(20000);
        g.getTeam(Team.ID_2).set(Team.TRIP_SCORE, 3);
        track2.getTracks().tick(); // not sent during a jam
        assertEquals(0, server.getGame("g1").getTeam(Team.ID_2).getScore());
        assertFalse(server.getGame("g1").isInJam());
        g.stopJamTO();
        track2.getTracks().tick();
        copy = server.getGame("g1");
        assertEquals(4, copy.getTeam(Team.ID_1).getScore());
        assertEquals(3, copy.getTeam(Team.ID_2).getScore());
        assertEquals(2, copy.getCurrentPeriod().numberOf(Period.JAM));

        // the list shows where it runs
        track3.getTracks().tick();
        assertEquals("2", track3.getTracks().get(Tracks.SERVER_GAME, "g1").get(ServerGame.RUNS_ON));
    }

    @Test
    public void testOtherTrackCantPull() {
        pullOn(track2);
        assertNull(pullOn(track3));
        assertTrue(track3.getTracks().get(Tracks.SERVER_ERROR).contains("runs on track 2"));

        // released on the tournament server (say track 2's laptop broke down): track 3 can take over
        server.getTracks().get(Tracks.TRACK_GAME, "g1").execute(TrackGame.RELEASE);
        assertNotNull(pullOn(track3));
        assertEquals("3", server.getTracks().get(Tracks.TRACK_GAME, "g1").get(TrackGame.TRACK));
        track2.getTracks().tick();
        assertTrue(track2.getTracks().get(Tracks.PULLED_GAME, "g1").get(PulledGame.ERROR).contains("track 3"));
    }

    @Test
    public void testServerUnreachable() {
        Game g = pullOn(track2);
        PulledGame pg = track2.getTracks().get(Tracks.PULLED_GAME, "g1");
        serverReachable = false;
        g.startJam();
        advance(10000);
        g.getTeam(Team.ID_1).set(Team.TRIP_SCORE, 2);
        g.stopJamTO();
        track2.getTracks().tick();
        assertEquals("Connection refused", pg.get(PulledGame.ERROR));
        assertEquals("Connection refused", track2.getTracks().get(Tracks.SERVER_ERROR));
        assertEquals(0, server.getGame("g1").getTeam(Team.ID_1).getScore());

        serverReachable = true;
        track2.getTracks().tick();
        assertEquals("", pg.get(PulledGame.ERROR));
        assertEquals(2, server.getGame("g1").getTeam(Team.ID_1).getScore());
    }

    @Test
    public void testHandOverAndTakeBack() {
        Game g = pullOn(track2);
        PulledGame pg = track2.getTracks().get(Tracks.PULLED_GAME, "g1");
        g.startJam();
        advance(10000);
        g.getTeam(Team.ID_1).set(Team.TRIP_SCORE, 2);
        g.stopJamTO();
        // ended early, during a timeout, as with End Game on the IGRF tab
        advance(31000); // after the time the teams have to call an official review
        g.timeout();
        g.set(Game.OFFICIAL_SCORE, true, Source.WS);
        assertEquals(Game.State.FINISHED, g.get(Game.STATE));

        // a finished game stays on the track while the NSOs check their paperwork
        track2.getTracks().tick();
        assertEquals(0L, (long) pg.get(PulledGame.HANDED_OVER));
        assertNull(track2.getTracks().readOnlyReason("ScoreBoard.Game(g1).Team(1).TeamName"));

        // handed over from the IGRF tab
        pg.execute(PulledGame.HAND_OVER);
        assertNotEquals(0L, (long) pg.get(PulledGame.HANDED_OVER));
        assertNotEquals(0L, (long) server.getTracks().get(Tracks.TRACK_GAME, "g1").get(TrackGame.HANDED_OVER));
        assertEquals(Game.State.FINISHED, server.getGame("g1").get(Game.STATE));
        assertNotNull(track2.getTracks().readOnlyReason("ScoreBoard.Game(g1).Team(1).TeamName"));
        assertNotNull(track2.getTracks().readOnlyReason("ScoreBoard.CurrentGame.Team(1).TeamName"));
        assertNull(server.getTracks().readOnlyReason("ScoreBoard.Game(g1).Team(1).TeamName"));

        // reviewed on the tournament server
        server.getGame("g1").getTeam(Team.ID_2).set(Team.TEAM_NAME, "Away (corrected)");

        // taken back with the review's changes
        pg.execute(PulledGame.TAKE_BACK);
        g = track2.getGame("g1");
        assertEquals("Away (corrected)", g.getTeam(Team.ID_2).get(Team.TEAM_NAME));
        assertEquals(Game.State.FINISHED, g.get(Game.STATE));
        assertEquals(1, g.getCurrentPeriodNumber());
        assertEquals(2, g.getTeam(Team.ID_1).getScore());
        assertEquals(g, track2.getCurrentGame().getSourceElement());
        assertEquals(0L, (long) pg.get(PulledGame.HANDED_OVER));
        assertNull(track2.getTracks().readOnlyReason("ScoreBoard.Game(g1).Team(1).TeamName"));
        assertNotNull(server.getTracks().readOnlyReason("ScoreBoard.Game(g1).Team(1).TeamName"));
        track2.getTracks().tick();
        assertEquals(0L, (long) pg.get(PulledGame.HANDED_OVER));

        // and handed over again
        pg.execute(PulledGame.HAND_OVER);
        assertNotEquals(0L, (long) pg.get(PulledGame.HANDED_OVER));
    }

    @Test
    public void testPullAgainKeepsTracksVersion() {
        Game g = pullOn(track2);
        g.getTeam(Team.ID_1).set(Team.TEAM_NAME, "Home on track");
        track2.getTracks().get(Tracks.SERVER_GAME, "g1").execute(ServerGame.PULL);
        assertEquals("Home on track", track2.getGame("g1").getTeam(Team.ID_1).get(Team.TEAM_NAME));
    }

    @Test
    public void testPushOnlyItsGame() {
        pullOn(track2);
        Map<String, String> params = new HashMap<>();
        params.put("game", "g1");
        params.put("track", "2");
        Map<String, Object> state = new HashMap<>();
        state.put("ScoreBoard.Game(g1).Id", "g1");
        state.put("ScoreBoard.Settings.Setting(ScoreBoard.Tracks.TournamentServer)", "false");
        Map<String, Object> body = new HashMap<>();
        body.put("state", state);
        try {
            server.getTracks().handle("push", params, body);
            fail();
        } catch (TracksException e) { assertEquals(400, e.getStatus()); }
        assertEquals("true", server.getSettings().get(Tracks.SETTING_TOURNAMENT_SERVER));
    }

    @Test
    public void testTrackStatus() {
        Game g = pullOn(track2);
        g.getTeam(Team.ID_1).set(Team.TEAM_NAME, "Home");
        g.startJam();
        advance(12300);
        g.getTeam(Team.ID_1).set(Team.TRIP_SCORE, 4);
        g.getTeam(Team.ID_2).set(Team.LEAD, true);
        ((TracksImpl) track2.getTracks()).sendStatus();
        assertTrue(track2.getTracks().get(Tracks.SERVER_CONNECTED));

        Track t = server.getTracks().get(Tracks.TRACK, "2");
        assertTrue(t.get(Track.CONNECTED));
        assertEquals("192.168.1.12", t.get(Track.REMOTE_ADDR));
        assertEquals("g1", t.get(Track.GAME));
        assertEquals("Running", t.get(Track.GAME_STATE));
        assertEquals(1, (int) t.get(Track.PERIOD));
        assertEquals(1, (int) t.get(Track.JAM));
        assertTrue(t.get(Track.IN_JAM));
        assertEquals("Jam", t.get(Track.CLOCK));
        assertEquals(120000L - 13000L, (long) t.get(Track.CLOCK_TIME)); // whole seconds, as a display shows them
        assertEquals("Home", t.get(Track.TEAM_1));
        assertEquals(4, (int) t.get(Track.SCORE_1));
        assertEquals(0, (int) t.get(Track.SCORE_2));
        assertFalse(t.get(Track.LEAD_1));
        assertTrue(t.get(Track.LEAD_2));

        // nothing new: nothing sent
        int sent = statusMessages;
        ((TracksImpl) track2.getTracks()).sendStatus();
        assertEquals(sent, statusMessages);
        g.stopJamTO();
        ((TracksImpl) track2.getTracks()).sendStatus();
        assertEquals(sent + 1, statusMessages);
        assertEquals("Lineup", t.get(Track.CLOCK));
        assertFalse(t.get(Track.IN_JAM));
        assertEquals(3, (int) t.get(Track.TIMEOUTS_1));
        assertEquals(1, (int) t.get(Track.OFFICIAL_REVIEWS_2));
        assertEquals("", t.get(Track.TIMEOUT_OWNER));
        g.getTeam(Team.ID_1).execute(Team.TIMEOUT);
        ((TracksImpl) track2.getTracks()).sendStatus();
        assertEquals("Timeout", t.get(Track.CLOCK));
        assertEquals(2, (int) t.get(Track.TIMEOUTS_1));
        assertEquals("1", t.get(Track.TIMEOUT_OWNER));
        assertFalse(t.get(Track.OFFICIAL_REVIEW));
        advance(30000);
        g.stopJamTO();
        advance(1000);
        g.startJam();
        ((TracksImpl) track2.getTracks()).sendStatus();
        assertEquals("", t.get(Track.TIMEOUT_OWNER));
        g.stopJamTO();
        ((TracksImpl) track2.getTracks()).sendStatus();

        // no word from the track for a while
        t.set(Track.LAST_SEEN, System.currentTimeMillis() - 31000);
        server.getTracks().tick();
        assertFalse(t.get(Track.CONNECTED));
        ((TracksImpl) track2.getTracks()).sendStatus();
        assertFalse(t.get(Track.CONNECTED)); // nothing new to send yet
        advance(1000);
        ((TracksImpl) track2.getTracks()).sendStatus();
        assertTrue(t.get(Track.CONNECTED));

        // the track's WebSocket closes
        t.execute(Track.REMOVE);
        assertNotNull(server.getTracks().get(Tracks.TRACK, "2")); // not while it's connected
        server.getTracks().trackDisconnected("2", new Object());  // some other connection
        assertTrue(t.get(Track.CONNECTED));
        server.getTracks().trackDisconnected("2", remote);
        assertFalse(t.get(Track.CONNECTED));
        assertEquals(4, (int) t.get(Track.SCORE_1));
        t.execute(Track.REMOVE);
        assertNull(server.getTracks().get(Tracks.TRACK, "2"));

        serverReachable = false;
        ((TracksImpl) track2.getTracks()).sendStatus();
        assertFalse(track2.getTracks().get(Tracks.SERVER_CONNECTED));

        // not a tournament server
        serverReachable = true;
        server.getSettings().set(Tracks.SETTING_TOURNAMENT_SERVER, "false");
        ((TracksImpl) track2.getTracks()).sendStatus();
        assertFalse(track2.getTracks().get(Tracks.SERVER_CONNECTED));
        assertNull(server.getTracks().get(Tracks.TRACK, "2"));
    }

    @Test
    public void testPushWithSkaterInBox() {
        Game g = pullOn(track2);
        Team t = g.getTeam(Team.ID_1);
        Skater s = t.getOrCreate(Team.SKATER, "s1");
        s.set(Skater.ROSTER_NUMBER, "12");
        t.field(s, Role.BLOCKER);
        g.startJam();
        advance(20000);
        s.setPenaltyBox(true);
        advance(10000);
        g.stopJamTO();
        // still in the box in the next jam
        track2.getTracks().tick();
        assertEquals("", track2.getTracks().get(Tracks.PULLED_GAME, "g1").get(PulledGame.ERROR));

        // the keys of a push come in any order
        @SuppressWarnings("unchecked")
        Map<String, Object> state = (Map<String, Object>) lastPush.get("state");
        Map<String, String> params = new HashMap<>();
        params.put("game", "g1");
        params.put("track", "2");
        for (int seed = 0; seed < 30; seed++) {
            List<String> keys = new ArrayList<>(state.keySet());
            Collections.shuffle(keys, new Random(seed));
            Map<String, Object> shuffled = new LinkedHashMap<>();
            for (String k : keys) { shuffled.put(k, state.get(k)); }
            Map<String, Object> body = new HashMap<>();
            body.put("state", shuffled);
            server.getTracks().handle("push", params, body);
            assertTrue(server.getGame("g1").getTeam(Team.ID_1).getSkater("s1").isPenaltyBox());
        }
    }
}
