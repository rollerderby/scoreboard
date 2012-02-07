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
