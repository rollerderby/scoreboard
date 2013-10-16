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
import com.carolinarollergirls.scoreboard.model.*;

public abstract class DefaultScoreBoardControllerServlet extends HttpServlet implements ScoreBoardControllerServlet
{
	public abstract String getPath();

	public void setScoreBoardModel(ScoreBoardModel model) {
		scoreBoardModel = model;
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Expires", "-1");
		response.setCharacterEncoding("UTF-8");
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Expires", "-1");
		response.setCharacterEncoding("UTF-8");
	}

	protected void setTextResponse(HttpServletResponse response, int code, String text) throws IOException {
		response.setContentType("text/plain");
		response.getWriter().print(text);
		response.setStatus(code);
	}

	protected ScoreBoardModel scoreBoardModel;
}
