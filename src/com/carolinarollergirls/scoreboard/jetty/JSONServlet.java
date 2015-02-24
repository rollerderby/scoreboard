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

import org.json.JSONException;

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
			if ("/Ruleset/List".equals(request.getPathInfo())) {
				response.getWriter().print(Ruleset.RequestType.LIST_ALL_RULESETS.toJSON());
			} else if ("/Ruleset/ListDefinitions".equals(request.getPathInfo())) {
				response.getWriter().print(Ruleset.RequestType.LIST_DEFINITIONS.toJSON());
			} else
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
		} catch ( SocketException sE ) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Socket Exception : "+sE.getMessage());
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Expires", "-1");
		response.setCharacterEncoding("UTF-8");

		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_OK);
		try {
			if ("/Ruleset/Update".equals(request.getPathInfo())) {
				Ruleset rs = Ruleset.Update(getPostDataAsString(request));
				if (rs == null)
					error(response, "Error saving ruleset");
				else
					response.getWriter().print(rs.toJSON());
			} else if ("/Ruleset/New".equals(request.getPathInfo())) {
				Ruleset rs = Ruleset.New(getPostDataAsString(request));
				if (rs == null)
					error(response, "Error creating ruleset");
				else
					response.getWriter().print(rs.toJSON());
			} else
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
		} catch ( SocketException sE ) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Socket Exception : "+sE.getMessage());
		} catch ( JSONException je ) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "JSON Exception : "+je.getMessage());
		}
	}

	private void error(HttpServletResponse response, String errorMessage) throws IOException {
		// TODO: Return error message as JSON
		response.getWriter().print(errorMessage);
	}

	private String getPostDataAsString(HttpServletRequest request) throws IOException {
		BufferedReader bufferedReader = request.getReader();

		StringBuffer sb = new StringBuffer();
		String line = null;

		while (null != (line = bufferedReader.readLine())) {
			sb.append(line).append("\n");
		}

		return sb.toString();
	}

	protected Server server;
}
