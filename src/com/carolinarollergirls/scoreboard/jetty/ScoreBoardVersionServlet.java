package com.carolinarollergirls.scoreboard.jetty;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.carolinarollergirls.scoreboard.*;

public class ScoreBoardVersionServlet extends DefaultScoreBoardControllerServlet
{
	public String getPath() { return "/version"; }

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
		super.doPost(request, response);
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
		super.doGet(request, response);

		response.setContentType("text/plain");
		response.getWriter().println(ScoreBoardManager.getVersion());
		response.setStatus(HttpServletResponse.SC_OK);
	}
}
