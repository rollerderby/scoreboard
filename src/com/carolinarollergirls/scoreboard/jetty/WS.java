package com.carolinarollergirls.scoreboard.jetty;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.OnTextMessage;
import org.eclipse.jetty.websocket.WebSocketServlet;

import com.carolinarollergirls.scoreboard.core.interfaces.Clock;
import com.carolinarollergirls.scoreboard.core.interfaces.Jam;
import com.carolinarollergirls.scoreboard.core.interfaces.Period;
import com.carolinarollergirls.scoreboard.core.interfaces.PreparedTeam;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.core.interfaces.Timeout;
import com.carolinarollergirls.scoreboard.core.interfaces.Clients.Client;
import com.carolinarollergirls.scoreboard.core.interfaces.Clients.Device;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Flag;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Source;
import com.carolinarollergirls.scoreboard.json.JSONStateListener;
import com.carolinarollergirls.scoreboard.json.JSONStateManager;
import com.carolinarollergirls.scoreboard.json.ScoreBoardJSONSetter;
import com.carolinarollergirls.scoreboard.utils.Logger;
import com.fasterxml.jackson.jr.ob.JSON;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;

public class WS extends WebSocketServlet {

    public WS(ScoreBoard s, JSONStateManager j) {
        sb = s;
        jsm = j;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        getServletContext().getNamedDispatcher("default").forward(request, response);
    }

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String arg1) {
        return new Conn(jsm, request);
    }

    private ScoreBoard sb;
    private JSONStateManager jsm;

    private boolean hasPermission(Device device, String action) {
        switch (action) {
        case "Register":
        case "Ping":
            return true;
        case "Set":
        case "StartNewGame":
        default:
            return device.mayWrite();
        }
    }

    private static final Gauge connectionsActive = Gauge.build().name("crg_websocket_active_connections")
            .help("Current WebSocket connections").register();
    private static final Counter messagesReceived = Counter.build().name("crg_websocket_messages_received")
            .help("Number of WebSocket messages received").register();
    private static final Histogram messagesSentDuration = Histogram.build()
            .name("crg_websocket_messages_sent_duration_seconds").help("Time spent sending WebSocket messages")
            .register();
    private static final Counter messagesSentFailures = Counter.build().name("crg_websocket_messages_sent_failed")
            .help("Number of WebSocket messages we failed to send").register();

    public class Conn implements OnTextMessage, JSONStateListener {
        private Connection connection;
        @SuppressWarnings("hiding")
        private JSONStateManager jsm;

        public Conn(JSONStateManager jsm, HttpServletRequest request) {
            this.jsm = jsm;
            this.request = request;
            device = sb.getClients().getDevice(request.getSession().getId());
        }

        @Override
        public synchronized void onMessage(String message_data) {
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
                        for (Object p : jsonPaths) {
                            newPaths.add((String) p);
                        }
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
                            String rs = (String) data.get("Ruleset");
                            sb.reset();
                            sb.getRulesets().setCurrentRuleset(rs);
                            sb.getTeam(Team.ID_1).loadPreparedTeam(t1);
                            sb.getTeam(Team.ID_2).loadPreparedTeam(t2);

                            if ((Boolean) data.get("Advance")) {
                                sb.startJam();
                                sb.timeout();
                                for (int i = 0; i < (Integer) data.get("TO1"); i++) {
                                    sb.setTimeoutType(sb.getTeam(Team.ID_1), false);
                                    sb.getClock(Clock.ID_TIMEOUT).elapseTime(1000); // avoid double click detection
                                    sb.timeout();
                                }
                                for (int i = 0; i < (Integer) data.get("TO2"); i++) {
                                    sb.setTimeoutType(sb.getTeam(Team.ID_2), false);
                                    sb.getClock(Clock.ID_TIMEOUT).elapseTime(1000); // avoid double click detection
                                    sb.timeout();
                                }
                                for (int i = 0; i < (Integer) data.get("OR1"); i++) {
                                    sb.setTimeoutType(sb.getTeam(Team.ID_1), true);
                                    sb.getClock(Clock.ID_TIMEOUT).elapseTime(1000); // avoid double click detection
                                    sb.timeout();
                                }
                                for (int i = 0; i < (Integer) data.get("OR2"); i++) {
                                    sb.setTimeoutType(sb.getTeam(Team.ID_2), true);
                                    sb.getClock(Clock.ID_TIMEOUT).elapseTime(1000); // avoid double click detection
                                    sb.timeout();
                                }
                                sb.setTimeoutType(Timeout.Owners.OTO, false);
                                sb.getTeam(Team.ID_1).set(Team.TRIP_SCORE, (Integer) data.get("Points1"));
                                sb.getTeam(Team.ID_2).set(Team.TRIP_SCORE, (Integer) data.get("Points2"));
                                int period = (Integer) data.get("Period");
                                int jam = (Integer) data.get("Jam");
                                if (jam == 0 && period > 1) {
                                    sb.getClock(Clock.ID_PERIOD)
                                            .elapseTime(sb.getClock(Clock.ID_PERIOD).getMaximumTime());
                                    sb.stopJamTO();
                                    sb.getClock(Clock.ID_INTERMISSION)
                                            .elapseTime(sb.getClock(Clock.ID_INTERMISSION).getMaximumTime());
                                    for (int i = 2; i < period; i++) {
                                        sb.getCurrentPeriod().execute(Period.INSERT_BEFORE);
                                    }
                                } else {
                                    for (int i = 1; i < period; i++) {
                                        sb.getCurrentPeriod().execute(Period.INSERT_BEFORE);
                                    }
                                    for (int i = 1; i < jam; i++) {
                                        sb.getCurrentPeriod().getCurrentJam().execute(Jam.INSERT_BEFORE);
                                    }
                                    sb.getClock(Clock.ID_PERIOD)
                                            .setTime(Long.valueOf((String) data.get("PeriodClock")));
                                }
                            } else {
                                String intermissionClock = (String) data.get("IntermissionClock");
                                if (intermissionClock != null) {
                                    Long ic_time = Long.valueOf(intermissionClock);
                                    ic_time = ic_time - (ic_time % 1000);
                                    Clock c = sb.getClock(Clock.ID_INTERMISSION);
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
                default:
                    sendError("Unknown Action '" + action + "'");
                    break;
                }
            } catch (Exception je) {
                Logger.printMessage("Error handling JSON message: " + je);
                je.printStackTrace();
            }
        }

        public void send(Map<String, Object> json) {
            Histogram.Timer timer = messagesSentDuration.startTimer();
            try {
                connection.sendMessage(
                        JSON.std.with(JSON.Feature.WRITE_NULL_PROPERTIES).composeString().addObject(json).finish());
            } catch (Exception e) {
                Logger.printMessage("Error sending JSON update: " + e);
                e.printStackTrace();
                messagesSentFailures.inc();
            } finally {
                timer.observeDuration();
            }
        }

        @Override
        public void onOpen(Connection conn) {
            connectionsActive.inc();
            // Some messages can be bigger than the 16k default
            // when there is broad registration.
            conn.setMaxTextMessageSize(1024 * 1024);
            connection = conn;
            jsm.register(this);
            String source = request.getParameter("source");
            if (source == null) {
                source = "CUSTOM CLIENT";
            }
            String platform = request.getParameter("platform");
            if (platform == null) {
                platform = request.getHeader("User-Agent");
            }
            sbClient = sb.getClients().addClient(device.getId(), request.getRemoteAddr(), source, platform);
            device.access();

            Map<String, Object> json = new HashMap<>();
            Map<String, Object> initialState = new HashMap<>();
            // Inject some of our own WS-specific information.
            // Session id is not included, as that's the secret cookie which
            // is meant to be httpOnly.
            initialState.put("WS.Device.Id", device.getId());
            initialState.put("WS.Device.Name", device.getName());
            initialState.put("WS.Client.Id", sbClient.getId());
            initialState.put("WS.Client.RemoteAddress", request.getRemoteAddr());
            json.put("state", initialState);
            send(json);
        }

        @Override
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
                if (watchedPaths.covers(k) && !k.endsWith("Secret")) {
                    updates.put(k, state.get(k));
                }
            }
            if (updates.size() == 0) {
                return;
            }
            Map<String, Object> json = new HashMap<>();
            json.put("state", updates);
            send(json);
            updates.clear();
        }

        protected Client sbClient;
        protected Device device;
        protected HttpServletRequest request;
        protected PathTrie paths = new PathTrie();
        private Map<String, Object> state;
    }

    protected static class PathTrie {
        boolean exists = false;
        Map<String, PathTrie> trie = new HashMap<>();

        public void addAll(Set<String> c) {
            for (String p : c) {
                add(p);
            }
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
        public boolean covers(String p) {
            return _covers(p.split("[.(]"), 0);
        }
        private boolean _covers(String[] p, int i) {
            PathTrie head = this;
            for (;; i++) {
                if (head.exists) {
                    return true;
                }
                if (i >= p.length) {
                    return false;
                }
                // Allow Blah(*).
                if (head.trie.containsKey("*)")) {
                    int j;
                    // id captured by * might contain . and thus be split - find the end
                    for (j = i; j < p.length && !p[j].endsWith(")"); j++)
                        ;
                    if (head.trie.get("*)")._covers(p, j + 1)) {
                        return true;
                    }
                }
                head = head.trie.get(p[i]);
                if (head == null) {
                    return false;
                }
            }
        }
    }
}
