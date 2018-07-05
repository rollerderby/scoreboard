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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import com.carolinarollergirls.scoreboard.json.ScoreBoardJSONListener;
import com.carolinarollergirls.scoreboard.json.WSUpdate;
import com.carolinarollergirls.scoreboard.model.ScoreBoardModel;

public class WS extends WebSocketServlet {

	public WS(ScoreBoardModel s) {
		sbm = s;
		new ScoreBoardJSONListener(s);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		getServletContext().getNamedDispatcher("default").forward(request, response);
	}

	@Override
	public WebSocket doWebSocketConnect(HttpServletRequest request, String arg1) {
		return new Conn();
	}

	protected static void register(Conn source) {
		synchronized (sourcesLock) {
			if (!sources.contains(source))
				sources.add(source);
		}
	}

	protected static void unregister(Conn source) {
		synchronized (sourcesLock) {
			sources.remove(source);
		}
	}

	private static void updateState(String key, Object value) {
		List<WSUpdate> updates = new ArrayList<WSUpdate>();
		updates.add(new WSUpdate(key, value));
		updateState(updates);
	}

	public static void updateState(List<WSUpdate> updates) {
		Histogram.Timer timer = updateStateDuration.startTimer();
		synchronized (sourcesLock) {
			stateID++;
			for(WSUpdate update : updates) {
				if(update.getValue() == null) {
					for(String stateKey: state.keySet()) {
						if(stateKey.equals(update.getKey()) || stateKey.startsWith(update.getKey()+".")) {
							State s = state.get(stateKey);
							s.stateID = stateID;
							s.value = null;
						}
					}
				} else {
					State s = state.get(update.getKey());
					if(s == null) {
						state.put(update.getKey(), new State(stateID, update.getValue()));
					} else if(!update.getValue().equals(s.value)) {
						s.stateID = stateID;
						s.value = update.getValue();
					}
				}
			}
			
			for (Conn source : sources) {
				source.sendUpdates();
			}
		}
		timer.observeDuration();
		updateStateUpdates.observe(updates.size());
	}

	protected static ScoreBoardModel sbm;
	protected static List<Conn> sources = new LinkedList<Conn>();
	protected static Object sourcesLock = new Object();

	protected static long stateID = 0;
	protected static Map<String, State> state = new LinkedHashMap<String, State>();

	protected static class State {
		public State(long sid, Object v) {
			stateID = sid;
			value = v;
		}

		protected long stateID;
		protected Object value;
	}

	private boolean hasPermission(String action) {
		return true;
	}

	private static final Histogram updateStateDuration = Histogram.build()
		.name("crg_websocket_update_state_duration_seconds").help("Time spent in WS.updateState function").register();
	private static final Histogram updateStateUpdates = Histogram.build()
		.name("crg_websocket_update_state_updates").help("Updates sent to WS.updateState function")
		.exponentialBuckets(1, 2, 10).register();
	private static final Gauge connectionsActive = Gauge.build()
		.name("crg_websocket_active_connections").help("Current WebSocket connections").register();
	private static final Counter messagesReceived = Counter.build()
		.name("crg_websocket_messages_received").help("Number of WebSocket messages received").register();
	private static final Histogram messagesSentDuration = Histogram.build()
		.name("crg_websocket_messages_sent_duration_seconds").help("Time spent sending WebSocket messages").register();
	private static final Counter messagesSentFailures = Counter.build()
		.name("crg_websocket_messages_sent_failed").help("Number of WebSocket messages we failed to send").register();

	public class Conn implements OnTextMessage {
		private Connection connection;
	
		public void onMessage(String message_data) {
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
					String path = json.optString("path", null);
					JSONArray paths = json.optJSONArray("paths");
					if (path != null)
						requestUpdates(path);
					else if (paths != null) {
						for (int i = 0; i < paths.length(); i++)
							requestUpdates(paths.getString(i));
					}
					sendPendingUpdates();
				} else if (action.equals("Penalty")) {
					JSONObject data = json.getJSONObject("data");
					String teamId = data.optString("teamId");
					String skaterId = data.optString("skaterId");
					String penaltyId = data.optString("penaltyId", null);
					boolean fo_exp = data.optBoolean("fo_exp", false);
					int period = data.optInt("period", -1);
					int jam = data.optInt("jam", -1);
					String code = data.optString("code", null);
					if (period == -1 || jam == -1)
						return;
					sbm.penalty(teamId, skaterId, penaltyId, fo_exp, period, jam, code);
				} else if (action.equals("Set")) {
					String key = json.getString("key");
					Object value = json.get("value");
					ScoreBoardManager.printMessage("Setting " + key + " to " + value);
					if (key.startsWith("Custom.")) {
						WS.updateState(key, value);
					}
				} else if (action.equals("Short")) {
					sendShort = true;
				} else if (action.equals("Long")) {
					sendShort = false;
				} else {
					sendError("Unknown Action '" + action + "'");
				}
			} catch (JSONException je) {
				ScoreBoardManager.printMessage("Error parsing JSON message: " + je);
				je.printStackTrace();
			}
		}
		
		public void send(JSONObject json) {
			Histogram.Timer timer = messagesSentDuration.startTimer();
			try {
				json.put("stateID", stateID);
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
		public void onOpen(Connection connection) {
			connectionsActive.inc();
			this.connection = connection;
			id = UUID.randomUUID();
			register(this);

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
			unregister(this);
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

		private void processUpdates(String path, boolean force) {
			for (String k : state.keySet()) {
				State s = state.get(k);
				if (k.startsWith(path) && (stateID < s.stateID || force)) {
					if (s.value == null)
						updates.put(k, JSONObject.NULL);
					else
						updates.put(k, s.value);
				}
			}
		}

		private void setShortChild(JSONObject o, String k, Object v) throws JSONException {
			List<String> path = new ArrayList<String>();
			int s = 0;
			boolean inside = false;
			for (int i = 0; i < k.length(); i++) {
				char c = k.charAt(i);
				if (c == '(')
					inside = true;
				else if (c == ')')
					inside = false;
				else if (c == '.' && !inside) {
					path.add(k.substring(s, i));
					s = i + 1;
				}
			}
			k = k.substring(s);
			// String[] path = k.split("\\.");
			System.out.print("k: " + k + "  path.size(): " + path.size() + "\n");
			for (String p : path) {
				System.out.print("p: " + p + "  has: " + o.has(p) + "\n");
				if (!o.has(p)) {
					o.put(p, new JSONObject());
				}
				Object n = o.getJSONObject(p);
				if (!(n instanceof JSONObject)) {
					JSONObject n2 = new JSONObject();
					n2.put("_", n);
					o = n2;
				} else
					o = (JSONObject)n;
			}
			if (!o.has(k)) {
				o.put(k, v);
			} else {
				Object n = o.getJSONObject(k);
				if (n instanceof JSONObject) {
					((JSONObject)n).put("_", v);
				} else {
					o.put(k, v);
				}
			}
		}

		private void sendPendingUpdates() {
			synchronized (this) {
				if (updates.size() == 0)
					return;
				try {
					JSONObject json = new JSONObject();
					if (sendShort) {
						JSONObject state = new JSONObject();
						for (String k : updates.keySet()) {
							setShortChild(state, k, updates.get(k));
						}
						json.put("state", state);
					} else {
						json.put("state", new JSONObject(updates));
					}
					stateID = WS.stateID;
					send(json);
					updates.clear();
				} catch (JSONException e) {
					ScoreBoardManager.printMessage("Error sending updates to client: " + e);
					e.printStackTrace();
				}
			}
		}

		public void sendUpdates() {
			synchronized (this) {
				for (String p : paths) {
					processUpdates(p, false);
				}
				sendPendingUpdates();
			}
		}

		public void requestUpdates(String path) {
			synchronized (this) {
				if (!paths.contains(path))
					paths.add(path);
				processUpdates(path, true);
			}
		}

		protected UUID id;
		protected List<String> paths = new LinkedList<String>();
		protected long stateID = 0;
		private Map<String, Object> updates = new LinkedHashMap<String, Object>();
		private boolean sendShort = false;
	}
}
