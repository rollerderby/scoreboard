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

import com.carolinarollergirls.scoreboard.ScoreBoard;
import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.json.ScoreBoardJSONListener;
import com.carolinarollergirls.scoreboard.model.ScoreBoardModel;

public class WS extends WebSocketServlet {
	public WS(ScoreBoardModel s) {
    sbm = s;
		ScoreBoardJSONListener listener = new ScoreBoardJSONListener(s);
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
		Map<String, Object> updateMap = new LinkedHashMap<String, Object>();
		updateMap.put(key, value);
		updateState(updateMap);
	}

	public static void updateState(Map<String, Object> updates) {
		Histogram.Timer timer = updateStateDuration.startTimer();
		synchronized (sourcesLock) {
			stateID++;
			List<String> keys = new LinkedList<String>(updates.keySet());
			List<String> deletedKeys = new LinkedList<String>(updates.keySet());
			for (String k : keys) {
				Object v = updates.get(k);
				if (v == null) {
					// Remove this and all children from state
					List<String> stateKeys = new ArrayList<String>(state.keySet());
					for (String key : stateKeys) {
						if (key.equals(k) || key.startsWith(k + ".")) {
							deletedKeys.add(key);
							State s = state.get(key);
							s.stateID = stateID;
							s.value = v;
						}
					}
				} else {
					State s = state.get(k);
					if (s == null) {
						if (v != null)
							state.put(k, new State(stateID, v));
					} else if (!v.equals(s.value)) {
						s.stateID = stateID;
						s.value = v;
					}
				}
			}
			keys.addAll(deletedKeys);

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

		private void sendPendingUpdates() {
			synchronized (this) {
				if (updates.size() == 0)
					return;
				try {
					JSONObject json = new JSONObject();
					json.put("state", new JSONObject(updates));
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
	}
}
