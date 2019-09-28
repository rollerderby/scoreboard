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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.json.JSONException;

import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.json.ScoreBoardJSONSetter;

public class LoadJsonScoreBoard extends HttpServlet {
    public LoadJsonScoreBoard(ScoreBoard sb) {
        this.scoreBoard = sb;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
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
                    String data = IOUtils.toString(stream, "utf-8");
                    JSONObject json = new JSONObject(data);
                    stream.close();
                    handleJSON(request, response, json);
                    return;
                }
            }

            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No JSON uploaded");
        } catch ( FileUploadException fuE ) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, fuE.getMessage());
        } catch ( JSONException jE ) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, jE.getMessage());
        }
    }

    protected void handleJSON(HttpServletRequest request, HttpServletResponse response, final JSONObject json) throws IOException {
        if (request.getPathInfo().equalsIgnoreCase("/load")) {
            scoreBoard.runInBatch(new Runnable() {
                @Override
                public void run() {
                    scoreBoard.reset();
                    ScoreBoardJSONSetter.set(scoreBoard, json);
                }
            });
        } else if (request.getPathInfo().equalsIgnoreCase("/merge")) {
            scoreBoard.runInBatch(new Runnable() {
                @Override
                public void run() {
                    ScoreBoardJSONSetter.set(scoreBoard, json);
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
