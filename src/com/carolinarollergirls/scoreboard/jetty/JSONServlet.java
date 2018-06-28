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
import java.text.DateFormat;

import javax.servlet.*;
import javax.servlet.http.*;

import org.json.JSONException;
import org.json.JSONObject;

import org.eclipse.jetty.server.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.ScoreBoardModel;
import com.carolinarollergirls.scoreboard.model.ClockModel;
import com.carolinarollergirls.scoreboard.xml.TeamsXmlDocumentManager;
import com.carolinarollergirls.scoreboard.xml.XmlDocumentManager;

public class JSONServlet extends HttpServlet
{
	public JSONServlet(Server s, ScoreBoardModel m) { 
		server = s; 
		scoreBoardModel = m;
	}

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
			if ("/Ruleset/New".equals(request.getPathInfo())) {
				Ruleset rs = Ruleset.New(getPostDataAsString(request));
				if (rs == null)
					error(response, "Error creating ruleset");
				response.getWriter().print(rs.toJSON());

			} else if ("/Ruleset/Update".equals(request.getPathInfo())) {
				Ruleset rs = Ruleset.Update(getPostDataAsString(request));
				if (rs == null)
					error(response, "Error saving ruleset");
				response.getWriter().print(rs.toJSON());

			} else if ("/Ruleset/Delete".equals(request.getPathInfo())) {
				boolean success = Ruleset.Delete(getPostDataAsString(request));
				if (!success)
					error(response, "Error deleting ruleset");
				response.getWriter().print("{ \"success\": \"" + success + "\" }");

			} else if ("/Game/Adhoc".equals(request.getPathInfo())) {
				JSONObject json = new JSONObject(getPostDataAsString(request));

				String t1 = json.optString("Team1", null);
				String t2 = json.optString("Team2", null);
				String rs = json.optString("Ruleset", null);
				String name = json.optString("Name", null);
				String intermissionClock = json.optString("IntermissionClock", null);
				if (t1 == null || t2 == null || rs == null || name == null) {
					error(response, "Error creating game");
				}
				scoreBoardModel.setRuleset(rs);
				scoreBoardModel.reset();
				List<XmlDocumentManager> l = scoreBoardModel.getXmlScoreBoard().findXmlDocumentManagers(TeamsXmlDocumentManager.class);
				for (XmlDocumentManager xdM : l) {
					TeamsXmlDocumentManager txdM = (TeamsXmlDocumentManager)xdM;
					txdM.toScoreBoard(Team.ID_1, t1, false);
					txdM.toScoreBoard(Team.ID_2, t2, false);
				}

				Game g = ScoreBoardManager.gameStart(name);
				response.getWriter().print(g.toJSON());

				if (intermissionClock != null) {
					Long ic_time = new Long(intermissionClock);
					ic_time = ic_time - (ic_time % 1000);
					ClockModel c = scoreBoardModel.getClockModel(Clock.ID_INTERMISSION);
					c.reset();
					c.setNumber(0);
					if (c.getMaximumTime() < ic_time)
						c.setMaximumTime(ic_time);
					c.setTime(ic_time);
					c.start();
				}

			} else
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
		} catch ( SocketException sE ) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Socket Exception : "+sE.getMessage());
			sE.printStackTrace();
		} catch ( JSONException je ) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "JSON Exception : "+je.getMessage());
			je.printStackTrace();
		} catch ( Exception e ) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Exception : "+e.getMessage());
			e.printStackTrace();
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
	protected ScoreBoardModel scoreBoardModel;
}
