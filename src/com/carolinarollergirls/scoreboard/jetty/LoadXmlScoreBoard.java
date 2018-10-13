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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.jdom.Document;
import org.jdom.JDOMException;

import com.carolinarollergirls.scoreboard.xml.XmlDocumentEditor;
import com.carolinarollergirls.scoreboard.xml.XmlScoreBoard;

public class LoadXmlScoreBoard extends DefaultScoreBoardControllerServlet {
    public String getPath() { return "/LoadXml"; }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
        super.doGet(request, response);
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
        super.doPost(request, response);

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
                    Document doc = editor.toDocument(stream);
                    stream.close();
                    handleDocument(request, response, doc);
                    return;
                }
            }

            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No XML uploaded");
        } catch ( FileUploadException fuE ) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, fuE.getMessage());
        } catch ( JDOMException jE ) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, jE.getMessage());
        }
    }

    protected void handleDocument(HttpServletRequest request, HttpServletResponse response, Document doc) throws IOException {
        if (request.getPathInfo().equalsIgnoreCase("/load")) {
            getXmlScoreBoard().loadDocument(doc);
        } else if (request.getPathInfo().equalsIgnoreCase("/merge")) {
            getXmlScoreBoard().mergeDocument(doc);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Must specify to load or merge document");
        }
        response.setContentType("text/plain");
        response.setStatus(HttpServletResponse.SC_OK);
    }

    protected XmlScoreBoard getXmlScoreBoard() { return scoreBoardModel.getXmlScoreBoard(); }

    protected XmlDocumentEditor editor = new XmlDocumentEditor();
}
