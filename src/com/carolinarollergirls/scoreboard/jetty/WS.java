package com.carolinarollergirls.scoreboard.jetty;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import com.fasterxml.jackson.jr.ob.JSON;

import com.carolinarollergirls.scoreboard.core.game.GameImpl;
import com.carolinarollergirls.scoreboard.core.interfaces.Clients.Client;
import com.carolinarollergirls.scoreboard.core.interfaces.Clients.Device;
import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Jam;
import com.carolinarollergirls.scoreboard.core.interfaces.Period;
import com.carolinarollergirls.scoreboard.core.interfaces.PreparedTeam;
import com.carolinarollergirls.scoreboard.core.interfaces.Rulesets.Ruleset;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.core.interfaces.Timeout;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Flag;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Source;
import com.carolinarollergirls.scoreboard.json.JSONStateListener;
import com.carolinarollergirls.scoreboard.json.JSONStateManager;
import com.carolinarollergirls.scoreboard.json.ScoreBoardJSONSetter;
import com.carolinarollergirls.scoreboard.utils.Logger;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;

public class WS extends WebSocketServlet {

    public WS(ScoreBoard s, JSONStateManager j) {
        sb = s;
        jsm = j;
    }

    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.setCreator(new ScoreBoardWebSocketCreator());
    }

    private boolean hasPermission(Device device, String action) {
        switch (action) {
        case "Register":
        case "Ping": return true;
        case "Set":
        case "StartNewGame":
        default: return device.mayWrite();
        }
    }

    private ScoreBoard sb;
    private JSONStateManager jsm;

    private static final Gauge connectionsActive =
        Gauge.build().name("crg_websocket_active_connections").help("Current WebSocket connections").register();
    private static final Counter messagesReceived = Counter.build()
                                                        .name("crg_websocket_messages_received")
                                                        .help("Number of WebSocket messages received")
                                                        .register();
    private static final Histogram messagesSentDuration = Histogram.build()
                                                              .name("crg_websocket_messages_sent_duration_seconds")
                                                              .help("Time spent sending WebSocket messages")
                                                              .register();
    private static final Counter messagesSentFailures = Counter.build()
                                                            .name("crg_websocket_messages_sent_failed")
                                                            .help("Number of WebSocket messages we failed to send")
                                                            .register();

    public class ScoreBoardWebSocketCreator implements WebSocketCreator {
        @Override
        public Object createWebSocket(ServletUpgradeRequest request, ServletUpgradeResponse response) {
            HttpServletRequest baseRequest = request.getHttpServletRequest();
            String httpSessionId = baseRequest.getSession().getId();
            String remoteAddress = baseRequest.getRemoteAddr();
            String source = baseRequest.getParameter("source");
            if (source == null) { source = "CUSTOM CLIENT"; }
            String platform = baseRequest.getParameter("platform");
            if (platform == null) { platform = baseRequest.getHeader("User-Agent"); }
            return new ScoreBoardWebSocket(httpSessionId, remoteAddress, source, platform);
        }
    }

    @WebSocket(maxTextMessageSize = 1024 * 1024)
    public class ScoreBoardWebSocket implements JSONStateListener {

        public ScoreBoardWebSocket(String httpSessionId, String remoteAddress, String source, String platform) {
            device = sb.getClients().getOrAddDevice(httpSessionId);
            sbClient = sb.getClients().addClient(device.getId(), remoteAddress, source, platform);
        }

        @OnWebSocketMessage
        public synchronized void onMessage(Session session, String message_data) {
            messagesReceived.inc();
            try {
                Map<String, Object> json = JSON.std.mapFrom(message_data);
                String action = (String) json.get("action");
                if (!hasPermission(device, action)) {
                    json = new HashMap<>();
                    json.put("authorization", "Not authorized for " + action);
                    send(json);
                    return;
                }
                switch (action) {
                case "Register":
                    List<?> jsonPaths = (List<?>) json.get("paths");
                    if (jsonPaths != null) {
                        Set<String> newPaths = new TreeSet<>();
                        for (Object p : jsonPaths) { newPaths.add((String) p); }
                        // Send on updates for the newly registered paths.
                        PathTrie pt = new PathTrie();
                        pt.addAll(newPaths);
                        sendWSUpdatesForPaths(pt, state.keySet());
                        this.paths.addAll(newPaths);
                    }
                    break;
                case "Set":
                    sbClient.write();
                    String key = (String) json.get("key");
                    Object value = json.get("value");
                    String v;
                    if (value == null) {
                        // Null deletes the setting.
                        v = null;
                    } else {
                        v = value.toString();
                    }
                    Flag flag = null;
                    String f = (String) json.get("flag");
                    if ("reset".equals(f)) { flag = Flag.RESET; }
                    if ("change".equals(f)) { flag = Flag.CHANGE; }
                    final ScoreBoardJSONSetter.JSONSet js = new ScoreBoardJSONSetter.JSONSet(key, v, flag);
                    sb.runInBatch(new Runnable() {
                        @Override
                        public void run() {
                            ScoreBoardJSONSetter.set(sb, Collections.singletonList(js), Source.WS);
                        }
                    });
                    break;
                case "StartNewGame":
                    sbClient.write();
                    @SuppressWarnings("unchecked")
                    final Map<String, Object> data = (Map<String, Object>) json.get("data");
                    sb.runInBatch(new Runnable() {
                        @Override
                        public void run() {
                            PreparedTeam t1 = sb.getPreparedTeam((String) data.get("Team1"));
                            PreparedTeam t2 = sb.getPreparedTeam((String) data.get("Team2"));
                            Ruleset rs = sb.getRulesets().getRuleset((String) data.get("Ruleset"));
                            Game g = new GameImpl(sb, t1, t2, rs);
                            sb.add(ScoreBoard.GAME, g);
                            sb.getCurrentGame().load(g);

                            if ((Boolean) data.get("Advance")) {
                                g.startJam();
                                g.timeout();
                                for (int i = 0; i < (Integer) data.get("TO1"); i++) {
                                    g.setTimeoutType(g.getTeam(Team.ID_1), false);
                                    g.getClock(Clock.ID_TIMEOUT).elapseTime(1000); // avoid double click detection
                                    g.timeout();
                                }
                                for (int i = 0; i < (Integer) data.get("TO2"); i++) {
                                    g.setTimeoutType(g.getTeam(Team.ID_2), false);
                                    g.getClock(Clock.ID_TIMEOUT).elapseTime(1000); // avoid double click detection
                                    g.timeout();
                                }
                                for (int i = 0; i < (Integer) data.get("OR1"); i++) {
                                    g.setTimeoutType(g.getTeam(Team.ID_1), true);
                                    g.getClock(Clock.ID_TIMEOUT).elapseTime(1000); // avoid double click detection
                                    g.timeout();
                                }
                                for (int i = 0; i < (Integer) data.get("OR2"); i++) {
                                    g.setTimeoutType(g.getTeam(Team.ID_2), true);
                                    g.getClock(Clock.ID_TIMEOUT).elapseTime(1000); // avoid double click detection
                                    g.timeout();
                                }
                                g.setTimeoutType(Timeout.Owners.OTO, false);
                                g.getTeam(Team.ID_1).set(Team.TRIP_SCORE, (Integer) data.get("Points1"));
                                g.getTeam(Team.ID_2).set(Team.TRIP_SCORE, (Integer) data.get("Points2"));
                                int period = (Integer) data.get("Period");
                                int jam = (Integer) data.get("Jam");
                                if (jam == 0 && period > 1) {
                                    g.getClock(Clock.ID_PERIOD)
                                        .elapseTime(g.getClock(Clock.ID_PERIOD).getMaximumTime() + 1000);
                                    g.stopJamTO();
                                    g.getClock(Clock.ID_INTERMISSION)
                                        .elapseTime(g.getClock(Clock.ID_INTERMISSION).getMaximumTime() + 1000);
                                    for (int i = 2; i < period; i++) {
                                        g.getCurrentPeriod().execute(Period.INSERT_BEFORE);
                                    }
                                } else {
                                    for (int i = 1; i < period; i++) {
                                        g.getCurrentPeriod().execute(Period.INSERT_BEFORE);
                                    }
                                    for (int i = 1; i < jam; i++) {
                                        g.getCurrentPeriod().getCurrentJam().execute(Jam.INSERT_BEFORE);
                                    }
                                }
                                long periodClock = Long.valueOf((String) data.get("PeriodClock"));
                                if (periodClock > 0) { g.getClock(Clock.ID_PERIOD).setTime(periodClock); }
                            } else {
                                String intermissionClock = (String) data.get("IntermissionClock");
                                if (intermissionClock != null) {
                                    Long ic_time = Long.valueOf(intermissionClock);
                                    ic_time = ic_time - (ic_time % 1000);
                                    Clock c = g.getClock(Clock.ID_INTERMISSION);
                                    c.setMaximumTime(ic_time);
                                    c.restart();
                                }
                            }
                        }
                    });
                    break;
                case "Ping":
                    json = new HashMap<>();
                    json.put("Pong", "");
                    send(json);

                    // This is usually only every 30s, so often enough
                    // to cover us if our process is terminated uncleanly
                    // without having to build something just for it
                    // or risking an update loop.
                    device.access();
                    break;
                default: sendError("Unknown Action '" + action + "'"); break;
                }
            } catch (Exception je) {
                Logger.printMessage("Error handling JSON message: " + je);
                Logger.printStackTrace(je);
            }
        }

        public void send(Map<String, Object> json) {
            Histogram.Timer timer = messagesSentDuration.startTimer();
            try {
                wsSession.getRemote().sendStringByFuture(
                    JSON.std.with(JSON.Feature.WRITE_NULL_PROPERTIES).composeString().addObject(json).finish());
            } catch (Exception e) {
                Logger.printMessage("Error sending JSON update: " + e);
                Logger.printStackTrace(e);
                messagesSentFailures.inc();
            } finally { timer.observeDuration(); }
        }

        @OnWebSocketConnect
        public void onOpen(Session session) {
            wsSession = session;
            connectionsActive.inc();
            jsm.register(this);
            device.access();

            Map<String, Object> json = new HashMap<>();
            Map<String, Object> initialState = new HashMap<>();
            // Inject some of our own WS-specific information.
            // Session id is not included, as that's the secret cookie which
            // is meant to be httpOnly.
            initialState.put("WS.Device.Id", device.getId());
            initialState.put("WS.Device.Name", device.getName());
            initialState.put("WS.Client.Id", sbClient.getId());
            initialState.put("WS.Client.RemoteAddress", session.getRemoteAddress().getAddress().getHostAddress());
            json.put("state", initialState);
            send(json);
        }

        @OnWebSocketClose
        public void onClose(int closeCode, String message) {
            connectionsActive.dec();
            jsm.unregister(this);
            sb.getClients().removeClient(sbClient);

            device.access();
        }

        public void sendError(String message) {
            Map<String, Object> json = new HashMap<>();
            json.put("error", message);
            send(json);
        }

        // State changes from JSONStateManager.
        @SuppressWarnings("hiding")
        @Override
        public synchronized void sendUpdates(Map<String, Object> state, Set<String> changed) {
            this.state = state;
            sendWSUpdatesForPaths(paths, changed);
        }

        private void sendWSUpdatesForPaths(PathTrie watchedPaths, Set<String> changed) {
            Map<String, Object> updates = new HashMap<>();
            for (String k : changed) {
                if (watchedPaths.covers(k) && !k.endsWith("Secret")) { updates.put(k, state.get(k)); }
            }
            if (updates.size() == 0) { return; }
            Map<String, Object> json = new HashMap<>();
            json.put("state", updates);
            send(json);
            updates.clear();
        }

        protected Client sbClient;
        protected Device device;
        protected PathTrie paths = new PathTrie();
        private Map<String, Object> state = new HashMap<>();
        private Session wsSession;
    }

    protected static class PathTrie {
        boolean exists = false;
        Map<String, PathTrie> trie = new HashMap<>();

        public void addAll(Set<String> c) {
            for (String p : c) { add(p); }
        }
        public void add(String path) {
            String[] p = path.split("[.(]");
            PathTrie head = this;
            for (int i = 0; !head.exists && i < p.length; i++) {
                if (head.trie.containsKey(p[i])) {
                    head = head.trie.get(p[i]);
                } else {
                    PathTrie child = new PathTrie();
                    head.trie.put(p[i], child);
                    head = child;
                }
            }
            head.exists = true;
        }
        public boolean covers(String p) { return _covers(p.split("[.(]"), 0); }
        private boolean _covers(String[] p, int i) {
            PathTrie head = this;
            for (;; i++) {
                if (head.exists) { return true; }
                if (i >= p.length) { return false; }
                // Allow Blah(*).
                if (head.trie.containsKey("*)")) {
                    int j;
                    // id captured by * might contain . and thus be split - find the end
                    for (j = i; j < p.length && !p[j].endsWith(")"); j++)
                        ;
                    if (head.trie.get("*)")._covers(p, j + 1)) { return true; }
                }
                head = head.trie.get(p[i]);
                if (head == null) { return false; }
            }
        }
    }
}
