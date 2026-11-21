package com.carolinarollergirls.scoreboard.core.admin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import com.fasterxml.jackson.jr.ob.JSON;

import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Period;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.core.interfaces.TimeoutOwner;
import com.carolinarollergirls.scoreboard.core.interfaces.Tracks;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.Command;
import com.carolinarollergirls.scoreboard.event.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.Value;
import com.carolinarollergirls.scoreboard.json.ScoreBoardJSONSetter;
import com.carolinarollergirls.scoreboard.utils.Logger;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

public final class TracksImpl extends ScoreBoardEventProviderImpl<Tracks> implements Tracks {
    public TracksImpl(ScoreBoard parent) {
        super(parent, "", ScoreBoard.TRACKS);
        addProperties(props);
        addWriteProtectionOverride(SERVER_ERROR, Source.NON_WS);
        addWriteProtectionOverride(SERVER_CONTACT, Source.NON_WS);
        addWriteProtectionOverride(SERVER_GAME, Source.NON_WS);
        addWriteProtectionOverride(PULLED_GAME, Source.NON_WS);
        addWriteProtectionOverride(TRACK_GAME, Source.NON_WS);
        addWriteProtectionOverride(TRACK, Source.NON_WS);
        addWriteProtectionOverride(SERVER_CONNECTED, Source.NON_WS);
    }

    @Override
    public ScoreBoardEventProvider create(Child<? extends ScoreBoardEventProvider> prop, String id, Source source) {
        synchronized (coreLock) {
            if (prop == SERVER_GAME) { return new ServerGameImpl(this, id); }
            if (prop == PULLED_GAME) { return new PulledGameImpl(this, id); }
            if (prop == TRACK_GAME) { return new TrackGameImpl(this, id); }
            if (prop == TRACK) { return new TrackImpl(this, id); }
            return null;
        }
    }

    @Override
    public void start() {
        synchronized (coreLock) {
            if (executor != null) { return; }
            executor = Executors.newScheduledThreadPool(2, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "tracks");
                    t.setDaemon(true);
                    return t;
                }
            });
        }
        executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                tick();
            }
        }, 2, 5, TimeUnit.SECONDS);
        executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    sendStatus();
                } catch (Throwable t) { Logger.printStackTrace("tracks status", t); }
            }
        }, 2, 1, TimeUnit.SECONDS);
    }

    // network requests must not hold up the scoreboard: run them on the tracks thread (directly in unit tests)
    private void later(Runnable r) {
        Runnable guarded = new Runnable() {
            @Override
            public void run() {
                try {
                    r.run();
                } catch (Throwable t) { Logger.printStackTrace("tracks", t); }
            }
        };
        if (executor == null) {
            guarded.run();
        } else {
            executor.execute(guarded);
        }
    }

    private String setting(String name) {
        String v = scoreBoard.getSettings().get(name);
        return v == null ? "" : v.trim();
    }

    @Override
    public void tick() {
        // a track reports at least every 10 seconds - one that doesn't is gone, even if its WebSocket didn't close
        long gone = System.currentTimeMillis() - 30000;
        synchronized (coreLock) {
            for (Track t : getAll(TRACK)) {
                if (t.get(Track.CONNECTED) && t.get(Track.LAST_SEEN) < gone) {
                    connections.remove(t.getId());
                    t.set(Track.CONNECTED, false);
                }
            }
        }
        String server = setting(SETTING_SERVER);
        if (!server.isEmpty()) { refresh(server); }
        for (PulledGame pg : getAll(PULLED_GAME)) { push(pg, false); }
    }

    /**
     * On a track: keep a WebSocket open to the tournament server and send what's happening on this track, whenever it
     * changes and at least every 10 seconds.
     */
    public void sendStatus() {
        String server = setting(SETTING_SERVER);
        String track = setting(SETTING_TRACK);
        boolean connected = false;
        if (server.isEmpty() || track.isEmpty()) {
            remote.disconnect();
        } else {
            Map<String, Object> status = status();
            long now = System.currentTimeMillis();
            try {
                if (!remote.isConnected(server)) { lastStatus = null; }
                if (!status.equals(lastStatus) || now - lastStatusSent > 10000) {
                    remote.sendStatus(server, track, status);
                    lastStatus = status;
                    lastStatusSent = now;
                }
                connected = true;
            } catch (Exception e) { lastStatus = null; }
        }
        synchronized (coreLock) { set(SERVER_CONNECTED, connected, Source.OTHER); }
    }

    private Map<String, Object> status() {
        Map<String, Object> s = new HashMap<>();
        synchronized (coreLock) {
            Game g = scoreBoard.getCurrentGame().getSourceElement();
            if (g == null) { return s; }
            s.put(Track.GAME.getJsonName(), g.getId());
            s.put(Track.GAME_NAME.getJsonName(), g.get(Game.NAME));
            s.put(Track.GAME_STATE.getJsonName(), g.get(Game.STATE).toString());
            s.put(Track.PERIOD.getJsonName(), g.getCurrentPeriodNumber());
            s.put(Track.JAM.getJsonName(), g.getCurrentPeriod().get(Period.CURRENT_JAM_NUMBER));
            s.put(Track.IN_JAM.getJsonName(), g.isInJam());
            String clock = "";
            if (g.isInJam()) {
                clock = Clock.ID_JAM;
            } else if (g.getClock(Clock.ID_TIMEOUT).isRunning()) {
                clock = Clock.ID_TIMEOUT;
            } else if (g.getClock(Clock.ID_LINEUP).isRunning()) {
                clock = Clock.ID_LINEUP;
            } else if (g.getClock(Clock.ID_INTERMISSION).isRunning()) {
                clock = Clock.ID_INTERMISSION;
            }
            s.put(Track.CLOCK.getJsonName(), clock);
            // whole seconds: that's what a display shows, and it keeps the messages down to one a second
            s.put(Track.CLOCK_TIME.getJsonName(), "".equals(clock) ? 0L : g.getClock(clock).getTime() / 1000 * 1000);
            s.put(Track.PERIOD_CLOCK.getJsonName(), g.getClock(Clock.ID_PERIOD).getTime() / 1000 * 1000);
            s.put(Track.TEAM_1.getJsonName(), g.getTeam(Team.ID_1).get(Team.DISPLAY_NAME));
            s.put(Track.TEAM_2.getJsonName(), g.getTeam(Team.ID_2).get(Team.DISPLAY_NAME));
            s.put(Track.SCORE_1.getJsonName(), g.getTeam(Team.ID_1).getScore());
            s.put(Track.SCORE_2.getJsonName(), g.getTeam(Team.ID_2).getScore());
            Team t1 = g.getTeam(Team.ID_1);
            Team t2 = g.getTeam(Team.ID_2);
            s.put(Track.TIMEOUTS_1.getJsonName(), t1.get(Team.TIMEOUTS));
            s.put(Track.TIMEOUTS_2.getJsonName(), t2.get(Team.TIMEOUTS));
            s.put(Track.OFFICIAL_REVIEWS_1.getJsonName(), t1.get(Team.OFFICIAL_REVIEWS));
            s.put(Track.OFFICIAL_REVIEWS_2.getJsonName(), t2.get(Team.OFFICIAL_REVIEWS));
            s.put(Track.RETAINED_OFFICIAL_REVIEW_1.getJsonName(), t1.get(Team.RETAINED_OFFICIAL_REVIEW));
            s.put(Track.RETAINED_OFFICIAL_REVIEW_2.getJsonName(), t2.get(Team.RETAINED_OFFICIAL_REVIEW));
            s.put(Track.LEAD_1.getJsonName(), t1.get(Team.DISPLAY_LEAD));
            s.put(Track.LEAD_2.getJsonName(), t2.get(Team.DISPLAY_LEAD));
            TimeoutOwner owner = g.get(Game.TIMEOUT_OWNER);
            String ownerId = "";
            if (owner == t1) {
                ownerId = Team.ID_1;
            } else if (owner == t2) {
                ownerId = Team.ID_2;
            } else if (owner != null) {
                ownerId = owner.getId(); // officials or nobody
            }
            s.put(Track.TIMEOUT_OWNER.getJsonName(), ownerId);
            s.put(Track.OFFICIAL_REVIEW.getJsonName(), g.get(Game.OFFICIAL_REVIEW));
        }
        return s;
    }

    @Override
    public String trackStatus(String track, Map<String, Object> status, Object connection, String remoteAddr) {
        if (!Boolean.parseBoolean(setting(SETTING_TOURNAMENT_SERVER))) {
            return "This scoreboard is not set up as tournament server (see its Tracks page).";
        }
        if (track == null || track.isEmpty() || status == null) { return "track and status needed"; }
        synchronized (coreLock) {
            requestBatchStart();
            Track t = get(TRACK, track);
            if (t == null) {
                t = new TrackImpl(this, track);
                add(TRACK, t, Source.OTHER);
            }
            connections.put(track, connection);
            t.set(Track.CONNECTED, true);
            t.set(Track.REMOTE_ADDR, remoteAddr == null ? "" : remoteAddr);
            t.set(Track.LAST_SEEN, System.currentTimeMillis());
            for (Property<?> prop : Track.props) {
                if (prop instanceof Value && prop != Track.CONNECTED && prop != Track.REMOTE_ADDR &&
                    prop != Track.LAST_SEEN) {
                    @SuppressWarnings("unchecked")
                    Value<Object> v = (Value<Object>) prop;
                    Object value = status.get(prop.getJsonName());
                    t.set(v, value == null ? v.getDefaultValue() : t.valueFromString(v, String.valueOf(value)));
                }
            }
            requestBatchEnd();
        }
        return null;
    }

    @Override
    public void trackDisconnected(String track, Object connection) {
        synchronized (coreLock) {
            if (connections.get(track) != connection) { return; } // another connection took over
            connections.remove(track);
            Track t = get(TRACK, track);
            if (t != null) { t.set(Track.CONNECTED, false); }
        }
    }

    private void refresh(String server) {
        try {
            Map<String, Object> r = remote.call(server, "games", new HashMap<String, String>(), null);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> games = (List<Map<String, Object>>) r.get("games");
            synchronized (coreLock) {
                requestBatchStart();
                Set<String> seen = new HashSet<>();
                for (Map<String, Object> g : games) {
                    String id = String.valueOf(g.get("Id"));
                    seen.add(id);
                    ServerGame sg = get(SERVER_GAME, id);
                    if (sg == null) {
                        sg = new ServerGameImpl(this, id);
                        add(SERVER_GAME, sg, Source.OTHER);
                    }
                    sg.set(ServerGame.NAME, str(g.get("Name")));
                    sg.set(ServerGame.GAME_NO, str(g.get("GameNo")));
                    sg.set(ServerGame.DATE, str(g.get("Date")));
                    sg.set(ServerGame.START_TIME, str(g.get("StartTime")));
                    sg.set(ServerGame.TRACK, str(g.get("Track")));
                    sg.set(ServerGame.STATE, str(g.get("State")));
                    sg.set(ServerGame.RUNS_ON, str(g.get("RunsOn")));
                    sg.set(ServerGame.HANDED_OVER, Boolean.TRUE.equals(g.get("HandedOver")));
                }
                for (ServerGame sg : getAll(SERVER_GAME)) {
                    if (!seen.contains(sg.getId())) { remove(SERVER_GAME, sg, Source.OTHER); }
                }
                set(SERVER_ERROR, "", Source.OTHER);
                set(SERVER_CONTACT, System.currentTimeMillis(), Source.OTHER);
                requestBatchEnd();
            }
        } catch (Exception e) {
            synchronized (coreLock) { set(SERVER_ERROR, message(e), Source.OTHER); }
        }
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }

    private static String message(Exception e) {
        return e.getMessage() == null || e.getMessage().isEmpty() ? e.getClass().getSimpleName() : e.getMessage();
    }

    private Map<String, String> params(String gameId, String track) {
        Map<String, String> p = new HashMap<>();
        p.put("game", gameId);
        p.put("track", track);
        return p;
    }

    /** On a track: pull a game from the tournament server and put it on the scoreboard. */
    private void pull(String gameId) {
        String server = setting(SETTING_SERVER);
        String track = setting(SETTING_TRACK);
        PulledGame pg = get(PULLED_GAME, gameId);
        boolean wasHandedOver = pg != null && pg.get(PulledGame.HANDED_OVER) != 0L;
        if (pg != null && !wasHandedOver && scoreBoard.getGame(gameId) != null) {
            // this track has the latest version of the game
            putOnScoreBoard(gameId);
            return;
        }
        if (track.isEmpty()) {
            synchronized (coreLock) { set(SERVER_ERROR, "Enter this track's number first.", Source.OTHER); }
            return;
        }
        if (pg != null) { server = pg.get(PulledGame.SERVER); }
        try {
            Map<String, Object> r = remote.call(server, "pull", params(gameId, track), new HashMap<String, Object>());
            @SuppressWarnings("unchecked")
            Map<String, Object> state = (Map<String, Object>) r.get("state");
            replaceGame(gameId, state);
            synchronized (coreLock) {
                requestBatchStart();
                if (pg == null) {
                    pg = new PulledGameImpl(this, gameId);
                    add(PULLED_GAME, pg, Source.OTHER);
                }
                pg.set(PulledGame.SERVER, server);
                pg.set(PulledGame.TRACK, track);
                pg.set(PulledGame.PULLED, System.currentTimeMillis());
                pg.set(PulledGame.HANDED_OVER, 0L);
                pg.set(PulledGame.ERROR, "");
                pg.set(PulledGame.NAME, scoreBoard.getGame(gameId).get(Game.NAME));
                set(SERVER_ERROR, "", Source.OTHER);
                requestBatchEnd();
            }
            pushed.remove(gameId);
            putOnScoreBoard(gameId);
        } catch (Exception e) {
            synchronized (coreLock) {
                if (pg != null) {
                    pg.set(PulledGame.ERROR, message(e));
                } else {
                    set(SERVER_ERROR, message(e), Source.OTHER);
                }
            }
        }
    }

    private void putOnScoreBoard(String gameId) {
        scoreBoard.runInBatch(new Runnable() {
            @Override
            public void run() {
                Game g = scoreBoard.getGame(gameId);
                if (g != null && g != scoreBoard.getCurrentGame().getSourceElement()) {
                    scoreBoard.getCurrentGame().load(g);
                }
            }
        });
    }

    /** On a track: send the game to the tournament server if it changed since the last push. */
    private void push(PulledGame pg, boolean always) {
        if (pg.get(PulledGame.HANDED_OVER) != 0L) { return; }
        String gameId = pg.getId();
        Map<String, Object> state = gameState(gameId, false);
        if (!state.containsKey("ScoreBoard.Game(" + gameId + ").Id")) {
            synchronized (coreLock) { pg.set(PulledGame.ERROR, "The game has been deleted on this scoreboard."); }
            return;
        }
        // a game loaded from a file in the middle of a jam can have the current trip's points twice, so it is sent
        // between jams; the tournament server gets the live score from the track's status meanwhile
        if (!always && Boolean.TRUE.equals(state.get("ScoreBoard.Game(" + gameId + ").InJam"))) { return; }
        // the clocks alone change all the time - they are sent along with the next other change
        Map<String, Object> withoutClocks = new HashMap<>();
        for (Map.Entry<String, Object> e : state.entrySet()) {
            if (!e.getKey().contains(".Clock(")) { withoutClocks.put(e.getKey(), e.getValue()); }
        }
        boolean changed = !withoutClocks.equals(pushed.get(gameId)) || !"".equals(pg.get(PulledGame.ERROR));
        if (changed || always) {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("state", state);
                remote.call(pg.get(PulledGame.SERVER), "push", params(gameId, pg.get(PulledGame.TRACK)), body);
                pushed.put(gameId, withoutClocks);
                synchronized (coreLock) {
                    requestBatchStart();
                    pg.set(PulledGame.PUSHED, System.currentTimeMillis());
                    pg.set(PulledGame.ERROR, "");
                    pg.set(PulledGame.NAME, scoreBoard.getGame(gameId).get(Game.NAME));
                    requestBatchEnd();
                }
            } catch (Exception e) {
                synchronized (coreLock) { pg.set(PulledGame.ERROR, message(e)); }
                return;
            }
        }
    }

    private void handOver(PulledGame pg) {
        try {
            remote.call(pg.get(PulledGame.SERVER), "handover", params(pg.getId(), pg.get(PulledGame.TRACK)),
                        new HashMap<String, Object>());
            synchronized (coreLock) { pg.set(PulledGame.HANDED_OVER, System.currentTimeMillis()); }
        } catch (Exception e) {
            synchronized (coreLock) { pg.set(PulledGame.ERROR, message(e)); }
        }
    }

    private Map<String, Object> gameState(String gameId, boolean withTeams) {
        String prefix = "ScoreBoard.Game(" + gameId + ")";
        Map<String, Object> all = scoreBoard.getJsm().getState(true);
        Set<String> prefixes = new HashSet<>();
        prefixes.add(prefix);
        if (withTeams) {
            for (String t : new String[] {Team.ID_1, Team.ID_2}) {
                Object pt = all.get(prefix + ".Team(" + t + ").PreparedTeam");
                if (pt != null && !"".equals(pt)) { prefixes.add("ScoreBoard.PreparedTeam(" + pt + ")"); }
            }
        }
        Map<String, Object> state = new HashMap<>();
        for (Map.Entry<String, Object> e : all.entrySet()) {
            String key = e.getKey();
            boolean keep = key.startsWith("ScoreBoard.Version");
            for (String p : prefixes) {
                if (key.startsWith(p + ".")) { keep = true; }
            }
            if (keep) { state.put(key, e.getValue()); }
        }
        return state;
    }

    /** Puts the game in place of this scoreboard's version of it (loading only adds to a game). */
    private void replaceGame(String gameId, Map<String, Object> state) {
        ScoreBoardJSONSetter.updateToCurrentVersion(state);
        // put back if the new version can't be loaded, rather than keep half a game
        Map<String, Object> before = scoreBoard.getGame(gameId) == null ? null : gameState(gameId, false);
        try {
            loadGame(gameId, state);
        } catch (RuntimeException e) {
            Logger.printStackTrace("loading game " + gameId, e);
            if (before != null) {
                loadGame(gameId, before);
            } else if (scoreBoard.getGame(gameId) != null) {
                scoreBoard.remove(ScoreBoard.GAME, scoreBoard.getGame(gameId));
            }
            throw new TracksException(500, "The game could not be loaded: " + e);
        } finally { scoreBoard.cleanupAliases(); }
    }

    private void loadGame(String gameId, Map<String, Object> state) {
        scoreBoard.runInBatch(new Runnable() {
            @Override
            public void run() {
                Game old = scoreBoard.getGame(gameId);
                boolean current = old != null && old == scoreBoard.getCurrentGame().getSourceElement();
                if (old != null) { scoreBoard.remove(ScoreBoard.GAME, old); }
                // in the order of a saved file: loading depends on it (a box trip before the fielding's skater)
                ScoreBoardJSONSetter.set(scoreBoard, new TreeMap<>(state), Source.JSON);
                if (current && scoreBoard.getGame(gameId) != null) {
                    scoreBoard.getCurrentGame().load(scoreBoard.getGame(gameId));
                }
            }
        });
    }

    @Override
    public Map<String, Object> handle(String action, Map<String, String> params, Map<String, Object> body) {
        if (!Boolean.parseBoolean(setting(SETTING_TOURNAMENT_SERVER))) {
            throw new TracksException(403, "This scoreboard is not set up as tournament server (see its Tracks page).");
        }
        Map<String, Object> result = new HashMap<>();
        if ("games".equals(action)) {
            List<Map<String, Object>> games = new ArrayList<>();
            synchronized (coreLock) {
                for (Game g : scoreBoard.getAll(ScoreBoard.GAME)) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("Id", g.getId());
                    m.put("Name", g.get(Game.NAME));
                    m.put("GameNo", info(g, Game.INFO_GAME_NUMBER));
                    m.put("Date", info(g, Game.INFO_DATE));
                    m.put("StartTime", info(g, Game.INFO_START_TIME));
                    m.put("Track", info(g, INFO_TRACK));
                    m.put("State", g.get(Game.STATE).toString());
                    TrackGame tg = get(TRACK_GAME, g.getId());
                    m.put("RunsOn", tg == null ? "" : tg.get(TrackGame.TRACK));
                    m.put("HandedOver", tg != null && tg.get(TrackGame.HANDED_OVER) != 0L);
                    games.add(m);
                }
            }
            result.put("games", games);
            return result;
        }

        String gameId = params.get("game");
        String track = params.get("track");
        if (gameId == null || track == null || track.isEmpty()) {
            throw new TracksException(400, "game and track needed");
        }
        Game g = scoreBoard.getGame(gameId);
        TrackGame tg = get(TRACK_GAME, gameId);
        if ("pull".equals(action)) {
            synchronized (coreLock) {
                if (g == null) { throw new TracksException(404, "There is no such game on the tournament server."); }
                if (g == scoreBoard.getCurrentGame().getSourceElement()) {
                    throw new TracksException(409, "This game is on the tournament server's own scoreboard.");
                }
                if (tg != null && !track.equals(tg.get(TrackGame.TRACK))) {
                    throw new TracksException(409, "This game runs on track " + tg.get(TrackGame.TRACK) + ".");
                }
                requestBatchStart();
                if (tg == null) {
                    tg = new TrackGameImpl(this, gameId);
                    add(TRACK_GAME, tg, Source.OTHER);
                }
                tg.set(TrackGame.TRACK, track);
                tg.set(TrackGame.NAME, g.get(Game.NAME));
                tg.set(TrackGame.PULLED, System.currentTimeMillis());
                tg.set(TrackGame.HANDED_OVER, 0L);
                requestBatchEnd();
            }
            result.put("state", gameState(gameId, true));
            return result;
        }
        if (tg == null) {
            throw new TracksException(409, "This game hasn't been pulled by a track (or was released on the "
                                               + "tournament server). Pull it again.");
        }
        if (!track.equals(tg.get(TrackGame.TRACK))) {
            throw new TracksException(409, "This game runs on track " + tg.get(TrackGame.TRACK) + " now.");
        }
        if (tg.get(TrackGame.HANDED_OVER) != 0L) {
            if ("handover".equals(action)) { return result; }
            throw new TracksException(409, "This game has been handed over for review. Take it back to change it.");
        }
        if ("push".equals(action)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> state = body == null ? null : (Map<String, Object>) body.get("state");
            if (state == null || !state.containsKey("ScoreBoard.Game(" + gameId + ").Id")) {
                throw new TracksException(400, "No game in the push.");
            }
            for (String key : state.keySet()) {
                if (!key.startsWith("ScoreBoard.Game(" + gameId + ").") && !key.startsWith("ScoreBoard.Version")) {
                    throw new TracksException(400, "A push may only contain its game.");
                }
            }
            replaceGame(gameId, state);
            synchronized (coreLock) {
                tg.set(TrackGame.PUSHED, System.currentTimeMillis());
                Game pushedGame = scoreBoard.getGame(gameId);
                if (pushedGame != null) { tg.set(TrackGame.NAME, pushedGame.get(Game.NAME)); }
            }
            return result;
        }
        if ("handover".equals(action)) {
            synchronized (coreLock) { tg.set(TrackGame.HANDED_OVER, System.currentTimeMillis()); }
            return result;
        }
        throw new TracksException(404, "Unknown action " + action);
    }

    private static String info(Game g, String id) {
        ValWithId v = g.get(Game.EVENT_INFO, id);
        return v == null ? "" : v.getValue();
    }

    @Override
    public String readOnlyReason(String key) {
        String gameId = null;
        if (key.startsWith("ScoreBoard.Game(")) {
            int end = key.indexOf(')');
            if (end > 0) { gameId = key.substring("ScoreBoard.Game(".length(), end); }
        } else if (key.startsWith("ScoreBoard.CurrentGame.")) {
            Game g = scoreBoard.getCurrentGame().getSourceElement();
            if (g != null) { gameId = g.getId(); }
        }
        if (gameId == null) { return null; }
        TrackGame tg = get(TRACK_GAME, gameId);
        if (tg != null && tg.get(TrackGame.HANDED_OVER) == 0L) {
            return "This game runs on track " + tg.get(TrackGame.TRACK) + ". Change it on that track's scoreboard.";
        }
        PulledGame pg = get(PULLED_GAME, gameId);
        if (pg != null && pg.get(PulledGame.HANDED_OVER) != 0L) {
            return "This game has been handed over to the tournament server for review. Change it there, or take it "
                + "back on the Tracks page.";
        }
        return null;
    }

    public class ServerGameImpl extends ScoreBoardEventProviderImpl<ServerGame> implements ServerGame {
        ServerGameImpl(Tracks parent, String id) {
            super(parent, id, Tracks.SERVER_GAME);
            addProperties(props);
        }

        @Override
        public void execute(Command prop, Source source) {
            if (prop == PULL) {
                final String id = getId();
                later(new Runnable() {
                    @Override
                    public void run() {
                        pull(id);
                    }
                });
            }
        }
    }

    public class PulledGameImpl extends ScoreBoardEventProviderImpl<PulledGame> implements PulledGame {
        PulledGameImpl(Tracks parent, String id) {
            super(parent, id, Tracks.PULLED_GAME);
            addProperties(props);
        }

        @Override
        public void execute(Command prop, Source source) {
            final PulledGame pg = this;
            if (prop == PUSH) {
                later(new Runnable() {
                    @Override
                    public void run() {
                        push(pg, true);
                    }
                });
            } else if (prop == HAND_OVER) {
                later(new Runnable() {
                    @Override
                    public void run() {
                        push(pg, true);
                        if (pg.get(HANDED_OVER) == 0L && "".equals(pg.get(ERROR))) { handOver(pg); }
                    }
                });
            } else if (prop == TAKE_BACK) {
                later(new Runnable() {
                    @Override
                    public void run() {
                        pull(pg.getId());
                    }
                });
            } else if (prop == FORGET) {
                delete();
            }
        }
    }

    public class TrackGameImpl extends ScoreBoardEventProviderImpl<TrackGame> implements TrackGame {
        TrackGameImpl(Tracks parent, String id) {
            super(parent, id, Tracks.TRACK_GAME);
            addProperties(props);
        }

        @Override
        public void execute(Command prop, Source source) {
            if (prop == RELEASE) { delete(); }
        }
    }

    public class TrackImpl extends ScoreBoardEventProviderImpl<Track> implements Track {
        TrackImpl(Tracks parent, String id) {
            super(parent, id, Tracks.TRACK);
            addProperties(props);
        }

        @Override
        public void execute(Command prop, Source source) {
            if (prop == REMOVE && !get(CONNECTED)) { delete(); }
        }
    }

    /** How a track reaches the tournament server. */
    public static interface Remote {
        public Map<String, Object> call(String server, String action, Map<String, String> params,
                                        Map<String, Object> body) throws IOException;

        /** Send the track's status over the WebSocket, opening it first when needed. */
        public void sendStatus(String server, String track, Map<String, Object> status) throws IOException;

        public boolean isConnected(String server);

        public void disconnect();
    }

    /** Over HTTP, keeping the session cookie, so the tournament server sees the track as one device. */
    public static final class HttpRemote implements Remote {
        @Override
        public Map<String, Object> call(String server, String action, Map<String, String> params,
                                        Map<String, Object> body) throws IOException {
            String base = baseUrl(server);
            StringBuilder url = new StringBuilder(base).append("/Tracks/").append(action);
            String sep = "?";
            for (Map.Entry<String, String> p : params.entrySet()) {
                url.append(sep)
                    .append(URLEncoder.encode(p.getKey(), "UTF-8"))
                    .append('=')
                    .append(URLEncoder.encode(p.getValue(), "UTF-8"));
                sep = "&";
            }
            URI uri;
            try {
                uri = new URI(url.toString());
            } catch (Exception e) { throw new IOException("Not a valid address: " + server); }
            HttpURLConnection c = (HttpURLConnection) uri.toURL().openConnection();
            c.setConnectTimeout(5000);
            c.setReadTimeout(30000);
            c.setRequestProperty("User-Agent", USER_AGENT);
            for (HttpCookie hc : cookies.getCookieStore().get(uri)) { c.addRequestProperty("Cookie", hc.toString()); }
            if (body != null) {
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                OutputStream out = c.getOutputStream();
                out.write(JSON.std.asString(body).getBytes(StandardCharsets.UTF_8));
                out.close();
            }
            int status = c.getResponseCode();
            cookies.put(uri, c.getHeaderFields());
            InputStream in = status < 400 ? c.getInputStream() : c.getErrorStream();
            String text = in == null ? "" : IOUtils.toString(in, StandardCharsets.UTF_8);
            if (in != null) { in.close(); }
            Map<String, Object> result;
            try {
                result = text.isEmpty() ? new HashMap<String, Object>() : JSON.std.mapFrom(text);
            } catch (IOException e) {
                throw new IOException("HTTP " + status + ": " + base + " doesn't answer as a tournament server");
            }
            if (status != 200) {
                throw new IOException(result.get("error") != null ? String.valueOf(result.get("error"))
                                                                  : "HTTP " + status);
            }
            return result;
        }

        @Override
        public synchronized void sendStatus(String server, String track, Map<String, Object> status)
            throws IOException {
            if (!isConnected(server)) { connect(server, track); }
            Map<String, Object> message = new HashMap<>();
            message.put("action", "TrackStatus");
            message.put("track", track);
            message.put("status", status);
            try {
                session.getRemote().sendString(JSON.std.asString(message));
            } catch (IOException e) {
                disconnect();
                throw e;
            }
        }

        @Override
        public synchronized boolean isConnected(String server) {
            return session != null && session.isOpen() && server.equals(sessionServer);
        }

        @Override
        public synchronized void disconnect() {
            if (session != null) { session.close(); }
            session = null;
            sessionServer = null;
        }

        private void connect(String server, String track) throws IOException {
            disconnect();
            try {
                if (wsClient == null) {
                    wsClient = new WebSocketClient();
                    // the same session cookie as the requests, so the tournament server sees one device per track
                    wsClient.setCookieStore(cookies.getCookieStore());
                    wsClient.start();
                }
                URI uri = new URI(baseUrl(server).replaceFirst("^http", "ws") +
                                  "/WS/?source=" + URLEncoder.encode("Track " + track, "UTF-8") +
                                  "&platform=" + URLEncoder.encode(USER_AGENT, "UTF-8"));
                session =
                    wsClient.connect(new WebSocketAdapter(), uri, new ClientUpgradeRequest()).get(5, TimeUnit.SECONDS);
                sessionServer = server;
            } catch (Exception e) { throw new IOException("No WebSocket to " + server + ": " + e.getMessage()); }
        }

        private static String baseUrl(String server) {
            String base = server.trim().replaceAll("/+$", "");
            return base.contains("://") ? base : "http://" + base;
        }

        private static final String USER_AGENT = "CRG ScoreBoard track";

        private CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        private WebSocketClient wsClient;
        private Session session;
        private String sessionServer;
    }

    public void setRemote(Remote r) { remote = r; } // for unit tests

    private Remote remote = new HttpRemote();
    private ScheduledExecutorService executor;
    // what the tournament server has of each game, without the clocks
    private Map<String, Map<String, Object>> pushed = new HashMap<>();
    private Map<String, Object> lastStatus;
    private long lastStatusSent;
    // on the tournament server: the WebSocket each track reports over
    private Map<String, Object> connections = new HashMap<>();
}
