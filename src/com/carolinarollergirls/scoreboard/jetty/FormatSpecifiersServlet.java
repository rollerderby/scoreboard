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

import org.mortbay.jetty.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.viewer.*;
import com.carolinarollergirls.scoreboard.model.*;

public class FormatSpecifiersServlet extends DefaultScoreBoardControllerServlet
{
  public FormatSpecifiersServlet() { }

  public String getPath() { return "/FormatSpecifiers"; }

  public void setScoreBoardModel(ScoreBoardModel model) {
    super.setScoreBoardModel(model);
    formatSpecifierViewer = new FormatSpecifierViewer(model);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
    response.setHeader("Cache-Control", "no-cache");
    response.setHeader("Expires", "-1");
    response.setCharacterEncoding("UTF-8");

    Map<String,String> m = formatSpecifierViewer.getFormatSpecifierDescriptions();
    Iterator<String> keys = m.keySet().iterator();
    response.setContentType("text/plain");
    while (keys.hasNext()) {
      String key = keys.next();
      response.getWriter().println(key+" : "+m.get(key));
    }
    response.setStatus(HttpServletResponse.SC_OK);
  }

  protected FormatSpecifierViewer formatSpecifierViewer;
}
