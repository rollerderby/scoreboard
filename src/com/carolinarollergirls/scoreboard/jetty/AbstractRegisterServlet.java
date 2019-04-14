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
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractRegisterServlet extends DefaultScoreBoardControllerServlet {
    public AbstractRegisterServlet() {
        Runnable r = new RegisteredListenerWatchdog();
        Thread t = new Thread(r);
        t.start();
    }

    protected abstract void register(HttpServletRequest request, HttpServletResponse response) throws IOException;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
        super.doGet(request, response);

        if ("/register".equals(request.getPathInfo())) {
            register(request, response);
        }

        RegisteredListener listener = getRegisteredListenerForRequest(request);
        if (null != listener) {
            listener.setLastRequestTime(new Date().getTime());
        }
    }

    protected void registrationKeyNotFound(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String key = request.getParameter("key");
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Registration key '"+key+"' not found");
    }

    protected RegisteredListener getRegisteredListenerForRequest(HttpServletRequest request) {
        String key = null;
        if (null != (key = request.getParameter("key"))) {
            return clientMap.get(key);
        } else {
            return null;
        }
    }

    protected String getRandomString() {
        return Long.toHexString((long)(Math.random() * (double)Long.MAX_VALUE));
    }

    protected String addRegisteredListener(RegisteredListener listener) {
        String oldKey = listener.getKey();
        if (null == oldKey || "".equals(oldKey) || !clientMap.containsKey(oldKey)) {
//FIXME - use client IP address, port, etc. unique info instead of or in addition to UUID
            String newKey = UUID.randomUUID().toString();
            listener.setKey(newKey);
            clientMap.put(newKey, listener);
        }

        return listener.getKey();
    }

    protected void removeRegisteredListener(RegisteredListener listener) {
        clientMap.remove(listener.getKey());
    }

    protected Map<String,RegisteredListener> clientMap = new ConcurrentHashMap<>();

    protected class RegisteredListener {
        public void setKey(String k) { key = k; }
        public String getKey() { return key; }

        public void setLastRequestTime(long t) { lastRequestTime = t; }
        public long getLastRequestTime() { return lastRequestTime; }

        protected String key = "";

        protected long lastRequestTime = new Date().getTime();
    }

    protected class RegisteredListenerWatchdog implements Runnable {
        @Override
        public void run() {
            while (true) {
                Iterator<RegisteredListener> listeners = clientMap.values().iterator();
                while (listeners.hasNext()) {
                    RegisteredListener listener = listeners.next();
                    if ((new Date().getTime() - listener.getLastRequestTime()) > MAX_LAST_REQUEST_TIME) {
                        listeners.remove();
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
