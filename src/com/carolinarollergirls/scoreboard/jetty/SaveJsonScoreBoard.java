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
import java.util.TreeMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.jr.ob.JSON;

import com.carolinarollergirls.scoreboard.json.JSONStateManager;

public class SaveJsonScoreBoard extends HttpServlet {

    public SaveJsonScoreBoard(JSONStateManager jsm) {
      this.jsm = jsm;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
        // Use  a TreeMap to keep output sorted.
        Map<String, Object> state = new TreeMap<>(jsm.getState());

        String path = request.getParameter("path");
        if (path != null) {
            Iterator<String> it = state.keySet().iterator();
            while (it.hasNext())
            {
                if(!it.next().startsWith(path)) {
                    it.remove();
                }
            }
        }
        // Users may use saves rather than the game-data on disk
        // to share with the world, so ellide the secret.
        if (state.containsKey("ScoreBoard.Twitter.AccessTokenSecret")) {
            state.put("ScoreBoard.Twitter.AccessTokenSecret", "<removed>");
        }

        if (state.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No objects found.");
        } else {
            response.setContentType("application/json");
            String json = JSON.std
                    .with(JSON.Feature.PRETTY_PRINT_OUTPUT)
                    .composeString()
                    .startObject()
                    .putObject("state", state)
                    .end()
                    .finish();

            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Expires", "-1");
            response.setCharacterEncoding("utf-8");
            response.getOutputStream().print(json);
            response.getOutputStream().flush();
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }

    protected final JSONStateManager jsm;
}
