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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;

public class ScoreBoardVersionServlet extends DefaultScoreBoardControllerServlet {
    @Override
    public String getPath() { return "/version"; }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
        super.doPost(request, response);
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
        super.doGet(request, response);

        response.setContentType("text/plain");
        response.getWriter().println(ScoreBoardManager.getVersion());
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
