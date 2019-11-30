package com.carolinarollergirls.scoreboard.jetty;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.Set;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import  org.eclipse.jetty.server.session.AbstractSessionManager;
import  org.eclipse.jetty.server.session.AbstractSession;


import com.carolinarollergirls.scoreboard.core.Clients;
import com.carolinarollergirls.scoreboard.core.Clients.Client;
import com.carolinarollergirls.scoreboard.core.Clients.Device;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;

public class ScoreBoardSessionManager extends AbstractSessionManager {

    public ScoreBoardSessionManager(ScoreBoard s) {
        clients = s.getClients();
    }

    @Override
    public AbstractSession getSession(String sessionId) {
      Device d = clients.getDevice(sessionId);
      if (d == null) {
        return null;
      }
      return new Session(this, sessionId, d);
    }
 
    @Override
    protected AbstractSession newSession(HttpServletRequest request) {
      return new Session(this, request);
    }

    @Override
    protected void addSession(AbstractSession session) {
      clients.getOrAddDevice(session.getId());
    }
 
    @Override
    protected void invalidateSessions() {
      throw new RuntimeException("Not implemented");
    }

    @Override
    protected boolean removeSession(String id) {
      throw new RuntimeException("Not implemented");
    }

    private Clients clients;

    protected class Session extends AbstractSession {
        protected Session(ScoreBoardSessionManager manager, HttpServletRequest request) {
            super(manager, request);
            device = clients.getOrAddDevice(getId());
        }
   
        protected Session(ScoreBoardSessionManager manager, String sessionId, Device d) {
            super(manager, d.getCreated(), System.currentTimeMillis(), sessionId);
            device = d;
        }

        Device device;
    }
}
