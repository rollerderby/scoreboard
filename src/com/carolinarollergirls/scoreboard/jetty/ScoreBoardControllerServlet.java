package com.carolinarollergirls.scoreboard.jetty;

import javax.servlet.*;

import com.carolinarollergirls.scoreboard.*;

public interface ScoreBoardControllerServlet extends ScoreBoardController,Servlet
{
	public String getPath();
}
