package com.carolinarollergirls.scoreboard.jetty;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.json.JSONException;
import org.json.JSONObject;

import com.carolinarollergirls.scoreboard.Ruleset;
import com.carolinarollergirls.scoreboard.model.ClockModel;
import com.carolinarollergirls.scoreboard.model.ScoreBoardModel;
import com.carolinarollergirls.scoreboard.view.Clock;
import com.carolinarollergirls.scoreboard.view.Team;
import com.carolinarollergirls.scoreboard.xml.TeamsXmlDocumentManager;
import com.carolinarollergirls.scoreboard.xml.XmlDocumentManager;

public class JSONServlet extends HttpServlet {
    public JSONServlet(Server s, ScoreBoardModel m) {
        server = s;
        scoreBoardModel = m;
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Expires", "-1");
        response.setCharacterEncoding("UTF-8");

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        try {
            if ("/Game/Adhoc".equals(request.getPathInfo())) {
                JSONObject json = new JSONObject(getPostDataAsString(request));

                String t1 = json.optString("Team1", null);
                String t2 = json.optString("Team2", null);
                String rs = json.optString("Ruleset", null);
                String intermissionClock = json.optString("IntermissionClock", null);
                if (t1 == null || t2 == null || rs == null) {
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

                response.getWriter().print("{}");
                if (intermissionClock != null) {
                    Long ic_time = new Long(intermissionClock);
                    ic_time = ic_time - (ic_time % 1000);
                    ClockModel c = scoreBoardModel.getClockModel(Clock.ID_INTERMISSION);
                    c.reset();
                    c.setNumber(0);
                    if (c.getMaximumTime() < ic_time) {
                        c.setMaximumTime(ic_time);
                    }
                    c.setTime(ic_time);
                    c.start();
                }

            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
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
