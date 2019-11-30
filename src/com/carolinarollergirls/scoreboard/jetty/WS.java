package com.carolinarollergirls.scoreboard.jetty;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.Set;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.jr.ob.JSON;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.OnTextMessage;
import org.eclipse.jetty.websocket.WebSocketServlet;

import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.PreparedTeam;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.Clients.Client;
import com.carolinarollergirls.scoreboard.core.Clients.Device;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Flag;
import com.carolinarollergirls.scoreboard.json.JSONStateManager;
import com.carolinarollergirls.scoreboard.json.JSONStateListener;
import com.carolinarollergirls.scoreboard.json.ScoreBoardJSONSetter;
import com.carolinarollergirls.scoreboard.utils.Logger;

public class WS extends WebSocketServlet {

    public WS(ScoreBoard s, JSONStateManager j) {
        sb = s;
        jsm = j;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        getServletContext().getNamedDispatcher("default").forward(request, response);
    }

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String arg1) {
        return new Conn(jsm, request);
    }


    private ScoreBoard sb;
    private JSONStateManager jsm;

    private boolean hasPermission(String action) {
        return true;
    }

    private static final Gauge connectionsActive = Gauge.build()
            .name("crg_websocket_active_connections").help("Current WebSocket connections").register();
    private static final Counter messagesReceived = Counter.build()
            .name("crg_websocket_messages_received").help("Number of WebSocket messages received").register();
    private static final Histogram messagesSentDuration = Histogram.build()
            .name("crg_websocket_messages_sent_duration_seconds").help("Time spent sending WebSocket messages").register();
    private static final Counter messagesSentFailures = Counter.build()
            .name("crg_websocket_messages_sent_failed").help("Number of WebSocket messages we failed to send").register();

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
                String action = (String)json.get("action");
                if (!hasPermission(action)) {
                    json = new HashMap<>();
                    json.put("authorization", "Not authorized for " + action);
                    send(json);
                    return;
                }
                if (action.equals("Register")) {
                    List<?> jsonPaths = (List<?>)json.get("paths");
                    if (jsonPaths != null) {
                        Set<String> newPaths = new TreeSet<>();
                        for (Object p : jsonPaths) {
                          newPaths.add((String)p);
                        }
                        // Send on updates for the newly registered paths.
                        PathTrie pt = new PathTrie();
                        pt.addAll(newPaths);
                        sendWSUpdatesForPaths(pt, state.keySet());
                        this.paths.addAll(newPaths);
                    }
                } else if (action.equals("Set")) {
                    String key = (String)json.get("key");
                    Object value = json.get("value");
                    String v;
                    if (value == null) {
                        // Null deletes the setting.
                        v = null;
                    } else {
                        v = value.toString();
                    }
                    Flag flag = null;
                    String f = (String)json.get("flag");
                    if ("reset".equals(f)) { flag = Flag.RESET; }
                    if ("change".equals(f)) { flag = Flag.CHANGE; }
                    final ScoreBoardJSONSetter.JSONSet js = new ScoreBoardJSONSetter.JSONSet(key, v, flag);
                    sb.runInBatch(new Runnable() {
                        @Override
                        public void run() {
                            ScoreBoardJSONSetter.set(sb, Collections.singletonList(js));
                        }
                    });
                } else if (action.equals("StartNewGame")) {
                    @SuppressWarnings("unchecked")
                    final Map<String, Object> data = (Map<String, Object>)json.get("data");
                    sb.runInBatch(new Runnable() {
                        @Override
                        public void run() {
                            PreparedTeam t1 = sb.getPreparedTeam((String)data.get("Team1"));
                            PreparedTeam t2 = sb.getPreparedTeam((String)data.get("Team2"));
                            String rs = (String)data.get("Ruleset");
                            sb.reset();
                            sb.getRulesets().setCurrentRuleset(rs);
                            sb.getTeam(Team.ID_1).loadPreparedTeam(t1);
                            sb.getTeam(Team.ID_2).loadPreparedTeam(t2);

                            String intermissionClock = (String)data.get("IntermissionClock");
                            if (intermissionClock != null) {
                                Long ic_time = Long.valueOf(intermissionClock);
                                ic_time = ic_time - (ic_time % 1000);
                                Clock c = sb.getClock(Clock.ID_INTERMISSION);
                                c.setMaximumTime(ic_time);
                                c.restart();
                            }
                        }
                    });
                } else if (action.equals("Ping")) {
                    json = new HashMap<>();
                    json.put("Pong", "");
                    send(json);

                    // This is usually only every 30s, so often enough
                    // to cover us if our process is terminated uncleanly
                    // without having to build something just for it
                    // or risking an update loop.
                    device.access();
                } else {
                    sendError("Unknown Action '" + action + "'");
                }
            } catch (Exception je) {
                Logger.printMessage("Error handling JSON message: " + je);
                je.printStackTrace();
            }
        }

        public void send(Map<String, Object> json) {
            Histogram.Timer timer = messagesSentDuration.startTimer();
            try {
                connection.sendMessage(JSON.std
                    .with(JSON.Feature.WRITE_NULL_PROPERTIES)
                    .composeString()
                    .addObject(json)
                    .finish());
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
            sbClient = sb.getClients().addClient(device.getId(),
                request.getRemoteAddr() + ":" + request.getRemotePort(),
                request.getParameter("source"));
            device.access();

            Map<String, Object> json = new HashMap<>();
            Map<String, Object> state = new HashMap<>();
            // Inject some of our own WS-specific information.
            // Session id is not included, as that's the secret cookie which
            // is meant to be httpOnly.
            state.put("WS.DeviceName", device.getName());
            state.put("WS.DeviceId", device.getId());
            state.put("WS.ClientId", sbClient.getId());
            json.put("state", state);
            send(json);
        }

        @Override
        public void onClose(int closeCode, String message) {
            connectionsActive.dec();
            jsm.unregister(this);
            sbClient.unlink();

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
            for (String k: changed) {
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
            for (String p: c) {
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
                    for (j = i; j < p.length && !p[j].endsWith(")"); j++);
                    if (head.trie.get("*)")._covers(p, j+1)) {
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
