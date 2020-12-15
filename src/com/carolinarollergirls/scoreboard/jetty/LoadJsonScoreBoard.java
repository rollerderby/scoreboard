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
import java.io.InputStream;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Source;
import com.carolinarollergirls.scoreboard.json.ScoreBoardJSONSetter;
import com.fasterxml.jackson.jr.ob.JSON;

public class LoadJsonScoreBoard extends HttpServlet {
    public LoadJsonScoreBoard(ScoreBoard sb) {
        this.scoreBoard = sb;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (scoreBoard.getClients().getDevice(request.getSession().getId()).mayWrite()) {
            scoreBoard.getClients().getDevice(request.getSession().getId()).write();
            try {
                if (!ServletFileUpload.isMultipartContent(request)) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }

                ServletFileUpload sfU = new ServletFileUpload();
                FileItemIterator items = sfU.getItemIterator(request);
                while (items.hasNext()) {
                    FileItemStream item = items.next();
                    if (!item.isFormField()) {
                        InputStream stream = item.openStream();
                        Map<String, Object> map = JSON.std.mapFrom(stream);
                        stream.close();
                        @SuppressWarnings("unchecked")
                        Map<String, Object> state = (Map<String, Object>) map.get("state");
                        handleJSON(request, response, state);
                        return;
                    }
                }

                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No JSON uploaded");
            } catch (FileUploadException fuE) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, fuE.getMessage());
            }
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "No write access");
        }
    }

    protected void handleJSON(HttpServletRequest request, HttpServletResponse response, final Map<String, Object> state)
            throws IOException {
        if (request.getPathInfo().equalsIgnoreCase("/load")) {
            scoreBoard.runInBatch(new Runnable() {
                @Override
                public void run() {
                    scoreBoard.reset();
                    ScoreBoardJSONSetter.set(scoreBoard, state, Source.JSON);
                }
            });
        } else if (request.getPathInfo().equalsIgnoreCase("/merge")) {
            scoreBoard.runInBatch(new Runnable() {
                @Override
                public void run() {
                    ScoreBoardJSONSetter.set(scoreBoard, state, Source.JSON);
                }
            });
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Must specify to load or merge");
        }
        response.setContentType("text/plain");
        response.setStatus(HttpServletResponse.SC_OK);
    }

    protected final ScoreBoard scoreBoard;

}
