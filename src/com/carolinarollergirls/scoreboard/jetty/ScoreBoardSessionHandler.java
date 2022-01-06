package com.carolinarollergirls.scoreboard.jetty;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.session.SessionHandler;

import com.carolinarollergirls.scoreboard.core.interfaces.Clients;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;

public class ScoreBoardSessionHandler extends SessionHandler {

    public ScoreBoardSessionHandler(ScoreBoard sb) { clients = sb.getClients(); }

    @Override
    public void checkRequestedSessionId(Request baseRequest, HttpServletRequest request) {
        super.checkRequestedSessionId(baseRequest, request);
        baseRequest.setSessionHandler(this);
        baseRequest.getSession(true);
    }

    @Override
    public boolean isIdInUse(String id) throws Exception {
        return super.isIdInUse(id) || clients.getDevice(id) != null;
    }

    private Clients clients;
}
