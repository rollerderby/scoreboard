package com.carolinarollergirls.scoreboard.jetty;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.session.AbstractSession;
import org.eclipse.jetty.server.session.AbstractSessionManager;

import com.carolinarollergirls.scoreboard.core.Clients;
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
        return new Session(this, sessionId);
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
        // Our session objects are created per request, so nothing to cleanup on
        // shutdown.
    }

    @Override
    protected boolean removeSession(String id) {
        throw new RuntimeException("Not implemented");
    }

    private Clients clients;

    protected class Session extends AbstractSession {
        protected Session(ScoreBoardSessionManager manager, HttpServletRequest request) {
            super(manager, request);
        }

        protected Session(ScoreBoardSessionManager manager, String sessionId) {
            super(manager, 0, System.currentTimeMillis(), sessionId);
        }
    }
}
