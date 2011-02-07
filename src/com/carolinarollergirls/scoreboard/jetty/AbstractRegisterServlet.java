package com.carolinarollergirls.scoreboard.jetty;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.awt.image.*;

import javax.imageio.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public abstract class AbstractRegisterServlet extends DefaultScoreBoardControllerServlet
{
	public AbstractRegisterServlet() {
		Runnable r = new RegisteredListenerWatchdog();
		Thread t = new Thread(r);
		t.start();
	}

	protected abstract void register(HttpServletRequest request, HttpServletResponse response) throws IOException;

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
		super.doPost(request, response);

		if ("/register".equals(request.getPathInfo()))
			register(request, response);

		RegisteredListener listener = getRegisteredListenerForRequest(request);
		if (null != listener)
			listener.setLastRequestTime(new Date().getTime());
	}

	protected RegisteredListener getRegisteredListenerForRequest(HttpServletRequest request) {
		String key = null;
		if (null != (key = request.getParameter("key")))
			return clientMap.get(key);
		else
			return null;
	}

	protected String getRandomString() {
		return Long.toHexString((long)(Math.random() * (double)Long.MAX_VALUE));
	}

	protected String addRegisteredListener(RegisteredListener listener) {
		String oldKey = listener.getKey();
		synchronized (clientMap) {
			if (null == oldKey || "".equals(oldKey) || !clientMap.containsKey(oldKey)) {
				String newKey = getRandomString();
				while (clientMap.containsKey(newKey))
					newKey = getRandomString();
				listener.setKey(newKey);
				clientMap.put(listener.getKey(), listener);
			}
		}

		return listener.getKey();
	}

	protected void removeRegisteredListener(RegisteredListener listener) {
		synchronized (clientMap) {
			clientMap.remove(listener.getKey());
		}
	}

	protected Map<String,RegisteredListener> clientMap = new HashMap<String,RegisteredListener>();

	protected abstract class RegisteredListener
	{
		public void setKey(String k) { key = k; }
		public String getKey() { return key; }

		public void setLastRequestTime(long t) { lastRequestTime = t; }
		public long getLastRequestTime() { return lastRequestTime; }

		protected String key = "";

		protected long lastRequestTime = new Date().getTime();
	}

	protected class RegisteredListenerWatchdog implements Runnable
	{
		public void run() {
			while (true) {
				Map<String,RegisteredListener> map = AbstractRegisterServlet.this.clientMap;
				List<RegisteredListener> killList = new ArrayList<RegisteredListener>();

				synchronized (map) {
					Iterator<RegisteredListener> listeners = map.values().iterator();
					while (listeners.hasNext()) {
						RegisteredListener listener = listeners.next();
						if ((new Date().getTime() - listener.getLastRequestTime()) > MAX_LAST_REQUEST_TIME)
							killList.add(listener);
					}
					for (int i=0; i<killList.size(); i++) {
						RegisteredListener listener = killList.get(i);
						map.remove(listener.getKey());
System.err.println("Removed listener " + listener.getKey());
					}
				}

				try { Thread.sleep(WATCHDOG_TIMER); } catch ( InterruptedException iE ) { }
			}
		}

		public static final long MAX_LAST_REQUEST_TIME = 600000;
		public static final long WATCHDOG_TIMER = 10000;
	}

	public static final int MAX_CLIENTS = 250;
}
