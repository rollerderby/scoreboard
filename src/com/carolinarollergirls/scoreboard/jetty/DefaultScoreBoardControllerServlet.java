package com.carolinarollergirls.scoreboard.jetty;

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
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
		doPost(request, response);
	}

	protected ScoreBoardModel scoreBoardModel;
}
