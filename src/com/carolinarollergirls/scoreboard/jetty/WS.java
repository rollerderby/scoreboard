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
import java.util.Hashtable;
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

	public static void update(Map<String, Object> updates) {
		synchronized (sourcesLock) {
			stateID++;
			List<String> keys = new ArrayList<String>(updates.keySet());
			for (String k : keys) {
				State s = state.get(k);
				Object v = updates.get(k);
				if (s == null)
					state.put(k, new State(stateID, v));
				else {
					s.stateID = stateID;
					s.value = v;
				}
			}

			for (Conn source : sources) {
				source.sendUpdates(keys);
			}
		}
	}

	protected static List<Conn> sources = new LinkedList<Conn>();
	protected static Object sourcesLock = new Object();

	protected static long stateID = 0;
	protected static Map<String, State> state = new Hashtable<String, State>();

	protected static class State {
		public State(long sid, Object v) {
			stateID = sid;
			value = v;
		}

		protected long stateID;
		protected Object value;
	}

	public class Conn implements OnTextMessage {
		private Connection connection;
	
		public void onMessage(String data) {
			try {
				JSONObject json = new JSONObject(data);
				String action = json.getString("action");
				if (action.equals("Register")) {
					String path = json.getString("path");
					requestUpdates(path);
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
				ScoreBoardManager.printMessage("send: " + json.toString());
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
				if (k.equals(path) && (stateID < s.stateID || force))
					updates.put(k, s.value);
			}
		}

		private void sendUpdates() {
			synchronized (this) {
				if (updates.size() == 0)
					return;
				JSONObject json = new JSONObject(updates);
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
				sendUpdates();
			}
		}

		protected UUID id;
		protected List<String> paths = new LinkedList<String>();
		protected long stateID = 0;
		private Map<String, Object> updates = new Hashtable<String, Object>();
	}
}
