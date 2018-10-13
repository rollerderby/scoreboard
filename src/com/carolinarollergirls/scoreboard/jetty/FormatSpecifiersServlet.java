package com.carolinarollergirls.scoreboard.jetty;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.carolinarollergirls.scoreboard.model.ScoreBoardModel;
import com.carolinarollergirls.scoreboard.viewer.FormatSpecifierViewer;

public class FormatSpecifiersServlet extends DefaultScoreBoardControllerServlet {
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

        if ("/descriptions".equals(request.getPathInfo())) {
            Map<String,String> m = formatSpecifierViewer.getFormatSpecifierDescriptions();
            Iterator<String> keys = m.keySet().iterator();
            response.setContentType("text/plain");
            while (keys.hasNext()) {
                String key = keys.next();
                response.getWriter().println(key+" : "+m.get(key));
            }
            response.setStatus(HttpServletResponse.SC_OK);
        } else if ("/description".equals(request.getPathInfo())) {
            String format = request.getParameter("format");
            String description = formatSpecifierViewer.getFormatSpecifierDescription(format);
            response.setContentType("text/plain");
            response.getWriter().print(description);
            response.setStatus(HttpServletResponse.SC_OK);
        } else if ("/keys".equals(request.getPathInfo())) {
            Map<String,String> m = formatSpecifierViewer.getFormatSpecifierDescriptions();
            Iterator<String> keys = m.keySet().iterator();
            response.setContentType("text/plain");
            while (keys.hasNext()) {
                response.getWriter().println(keys.next());
            }
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Expires", "-1");
        response.setCharacterEncoding("UTF-8");

        if ("/parse".equals(request.getPathInfo())) {
            response.setContentType("text/plain");
            response.getWriter().print(formatSpecifierViewer.parse(request.getParameter("format")));
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }

    protected FormatSpecifierViewer formatSpecifierViewer;
}
