package com.carolinarollergirls.scoreboard.jetty;

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
