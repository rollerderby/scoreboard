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

public class WS extends WebSocketServlet {
	public WS(ScoreBoard sb) {
		ScoreBoardJSONListener listener = new ScoreBoardJSONListener(sb);
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

	public static void requestUpdates(UUID id, String path) {
		synchronized (sourcesLock) {
			for (Conn source : sources) {
				if (source.id.equals(id)) {
					source.requestUpdates(path);
				}
			}
		}
	}

	public static void updateState(Map<String, Object> updates) {
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
				source.sendUpdates(keys);
			}
		}
	}

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

	public class Conn implements OnTextMessage {
		private Connection connection;
	
		public void onMessage(String data) {
			try {
				JSONObject json = new JSONObject(data);
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
					sendUpdates();
				} else if (action.equals("Short")) {
					sendShort = true;
				} else if (action.equals("Long")) {
					sendShort = false;
				} else if (action.equals("Penalty")) {
				} else {
					sendError("Unknown Action '" + action + "'");
				}
			} catch (JSONException je) {
				ScoreBoardManager.printMessage("Error parsing JSON message: " + je);
				je.printStackTrace();
			}
		}
		
		public void send(JSONObject json) {
			try {
				json.put("stateID", stateID);
				connection.sendMessage(json.toString());
			} catch (Exception e) {
				ScoreBoardManager.printMessage("Error sending JSON update: " + e);
				e.printStackTrace();
			}
		}
	
		@Override
		public void onOpen(Connection connection) {
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

		private void setShortChild(JSONObject o, String k, Object v) {
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

		private void sendUpdates() {
			synchronized (this) {
				if (updates.size() == 0)
					return;
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
			}
		}

		public void sendUpdates(List<String> paths) {
			synchronized (this) {
				for (String p : paths) {
					processUpdates(p, false);
				}
				sendUpdates();
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
