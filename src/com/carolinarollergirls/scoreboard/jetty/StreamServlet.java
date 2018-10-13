package com.carolinarollergirls.scoreboard.jetty;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.xml.AbstractScoreBoardStream;

public class StreamServlet extends DefaultScoreBoardControllerServlet {
    public String getPath() { return "/Stream"; }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
        super.doPost(request, response);

        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
        super.doGet(request, response);

        if (request.getPathInfo().equals("/list")) {
            list(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    protected void list(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
        try {
            File streamDir = new File(streamDirName);

            StringBuffer fileList = new StringBuffer("");
            File[] fileArray = streamDir.listFiles();
            if (null != fileArray) {
                Iterator<File> files = Arrays.asList(fileArray).iterator();
                while (files.hasNext()) {
                    File f = files.next();
                    if (f.isFile()) {
                        fileList.append(f.getName()+"\n");
                    }
                }
            }

            setTextResponse(response, HttpServletResponse.SC_OK, fileList.toString());
        } catch ( FileNotFoundException fnfE ) {
            setTextResponse(response, HttpServletResponse.SC_OK, "");
        } catch ( IllegalArgumentException iaE ) {
            setTextResponse(response, HttpServletResponse.SC_BAD_REQUEST, iaE.getMessage());
        }
    }

    private String streamDirName = ScoreBoardManager.getProperty(AbstractScoreBoardStream.DIRECTORY_KEY);
}
