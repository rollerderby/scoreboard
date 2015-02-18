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
import java.net.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.eclipse.jetty.server.*;

import com.carolinarollergirls.scoreboard.*;

public class JSONServlet extends HttpServlet
{
	public JSONServlet(Server s) { server = s; }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Expires", "-1");
		response.setCharacterEncoding("UTF-8");

		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_OK);
		try {
			if ("/RuleSet/ListAll".equals(request.getPathInfo())) {
				response.getWriter().print(RuleSet.RequestType.LIST_ALL_RULESETS.toJSON());
			} else if ("/RuleSet/ListDefinitions".equals(request.getPathInfo())) {
				response.getWriter().print(RuleSet.RequestType.LIST_DEFINITIONS.toJSON());
			} else
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
		} catch ( SocketException sE ) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Socket Exception : "+sE.getMessage());
		}
	}

	protected Server server;
}
