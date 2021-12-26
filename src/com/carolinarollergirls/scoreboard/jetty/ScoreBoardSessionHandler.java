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
