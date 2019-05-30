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
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.OnTextMessage;
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Flag;
import com.carolinarollergirls.scoreboard.json.JSONStateManager;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;
import com.carolinarollergirls.scoreboard.json.JSONStateListener;

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
        return new Conn(jsm);
    }


    private ScoreBoard sb;
    private JSONStateManager jsm;
    private static final Pattern pathElementPattern = Pattern.compile("^(?<name>\\w+)(\\((?<id>[^\\)]*)\\))?(\\.(?<remainder>.*))?$");

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

        public Conn(JSONStateManager jsm) {
            this.jsm = jsm;
        }

        @Override
        public synchronized void onMessage(String message_data) {
            messagesReceived.inc();
            try {
                JSONObject json = new JSONObject(message_data);
                String action = json.getString("action");
                if (!hasPermission(action)) {
                    json = new JSONObject();
                    json.put("authorization", "Not authorized for " + action);
                    send(json);
                    return;
                }
                if (action.equals("Register")) {
                    JSONArray jsonPaths = json.optJSONArray("paths");
                    if (jsonPaths != null) {
                        Set<String> newPaths = new TreeSet<>();
                        for (int i = 0; i < jsonPaths.length(); i++) {
                            newPaths.add(jsonPaths.getString(i));
                        }
                        // Send on updates for the newly registered paths.
                        PathTrie pt = new PathTrie();
                        pt.addAll(newPaths);
                        sendWSUpdatesForPaths(pt, state.keySet());
                        this.paths.addAll(newPaths);
                    }
                } else if (action.equals("Set")) {
                    String key = json.getString("key");
                    Object value = json.get("value");
                    String v;
                    if (value == JSONObject.NULL) {
                        // Null deletes the setting.
                        v = null;
                    } else {
                        v = value.toString();
                    }
                    Flag flag = null;
                    String f = json.getString("flag");
                    if ("reset".equals(f)) { flag = Flag.RESET; }
                    if ("change".equals(f)) { flag = Flag.CHANGE; }
                    Matcher m = pathElementPattern.matcher(key);
                    if (m.matches() && m.group("name").equals("ScoreBoard") &&
                            m.group("id") == null && m.group("remainder") != null) {
                        set(sb, m.group("remainder"), v, flag);
                    } else {
                        ScoreBoardManager.printMessage("Illegal path: " + key);
                    }
                } else if (action.equals("Ping")) {
                    send(new JSONObject().put("Pong", ""));
                } else {
                    sendError("Unknown Action '" + action + "'");
                }
            } catch (JSONException je) {
                ScoreBoardManager.printMessage("Error parsing JSON message: " + je);
                je.printStackTrace();
            }
        }

        private void set(ScoreBoardEventProvider p, String path, String value, Flag flag) {
            Matcher m = pathElementPattern.matcher(path);
            if (m.matches()) {
                String name = m.group("name");
                String elementId = m.group("id");
                String remainder = m.group("remainder");
                if (elementId == null) { elementId = ""; }
                try {
                    Property prop = PropertyConversion.fromFrontend(name, p.getProperties());
                    if (prop == null) { throw new IllegalArgumentException("Unknown property"); }

                    if (prop instanceof PermanentProperty) {
                        p.set((PermanentProperty)prop, p.valueFromString((PermanentProperty)prop, value, flag), flag);
                    } else if (prop instanceof CommandProperty) { 
                        if (Boolean.parseBoolean(value)) {
                            p.execute((CommandProperty)prop);
                        }
                    } else if (remainder != null) {
                        set((ScoreBoardEventProvider)p.getOrCreate((AddRemoveProperty)prop, elementId), remainder, value, flag);
                    } else if (value == null) {
                        p.remove((AddRemoveProperty)prop, elementId);
                    } else {
                        p.add((AddRemoveProperty)prop, p.childFromString((AddRemoveProperty)prop, elementId, value));
                    }
                } catch (Exception e) {
                    ScoreBoardManager.printMessage("Exception parsing JSON for " + p.getProviderName() +
                            "(" + p.getProviderId() + ")." + name + "(" + elementId + ") - " + value + ": " + e.toString());
                    e.printStackTrace();
                }
            } else {
                ScoreBoardManager.printMessage("Illegal path element: " + path);
            }
        }

        public void send(JSONObject json) {
            Histogram.Timer timer = messagesSentDuration.startTimer();
            try {
                connection.sendMessage(json.toString());
            } catch (Exception e) {
                ScoreBoardManager.printMessage("Error sending JSON update: " + e);
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
            this.connection = conn;
            id = UUID.randomUUID();
            jsm.register(this);

            try {
                JSONObject json = new JSONObject();
                json.put("id", id);
                send(json);
            } catch (JSONException je) {
                ScoreBoardManager.printMessage("Error sending ID to client: " + je);
                je.printStackTrace();
            }
        }

        @Override
        public void onClose(int closeCode, String message) {
            connectionsActive.dec();
            jsm.unregister(this);
        }

        public void sendError(String message) {
            try {
                JSONObject json = new JSONObject();
                json.put("error", message);
                send(json);
            } catch (JSONException je) {
                ScoreBoardManager.printMessage("Error sending error to client: " + je);
                je.printStackTrace();
            }
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
              if (watchedPaths.covers(k)) {
                if (state.get(k) == null) {
                  updates.put(k, JSONObject.NULL);
                } else {
                  updates.put(k, state.get(k));
                }
              }
            }
            if (updates.size() == 0) {
                return;
            }
            try {
                JSONObject json = new JSONObject();
                json.put("state", new JSONObject(updates));
                send(json);
                updates.clear();
            } catch (JSONException e) {
                ScoreBoardManager.printMessage("Error sending updates to client: " + e);
                e.printStackTrace();
            }
        }

        protected UUID id;
        protected PathTrie paths = new PathTrie();
        private Map<String, Object> state;

        private class PathTrie {
          boolean exists = false;
          Map<Character, PathTrie> trie = new HashMap<>();

          public void addAll(Set<String> c) {
            for (String p: c) {
              add(p);
            }
          }
          public void add(String p) {
            PathTrie head = this;
            for (int i = 0;; i++) {
              if (head.exists) {
                // Already covered.
                return;
              }
              if (i >= p.length()) {
                break;
              }
              if (head.trie.containsKey(p.charAt(i))) {
                 head = head.trie.get(p.charAt(i));
              } else {
                 PathTrie child = new PathTrie();
                 head.trie.put(p.charAt(i), child);
                 head = child;
              }
            }
            head.exists = true;
          }
          public boolean covers(String p) {
            PathTrie head = this;
            for (int i = 0;; i++) {
              if (head.exists) {
                return true;
              }
              if (i >= p.length()) {
                return false;
              }
              Character c = p.charAt(i);
              head = head.trie.get(c);
              if (head == null) {
                return false;
              }
              // Allow Blah(*).
              if (c == '(' && head.trie.containsKey('*')) {
                int closeIndex = p.indexOf(')', i);
                if (closeIndex != -1 && head.trie.get('*').covers(p.substring(closeIndex))) {
                  return true;
                }
              }
            }
          }
        }
    }
}
