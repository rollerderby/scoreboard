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
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import com.carolinarollergirls.scoreboard.xml.XmlDocumentEditor;
import com.carolinarollergirls.scoreboard.xml.XmlScoreBoard;

public class SaveXmlScoreBoard extends DefaultScoreBoardControllerServlet
{
	public String getPath() { return "/SaveXml"; }

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
		super.doPost(request, response);
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
		super.doGet(request, response);

		Document doc = null;
		Element node = getXmlScoreBoard().getDocument().getRootElement();

		boolean viewOnly = (null != request.getParameter("viewOnly"));

		String[] pathArray = request.getParameterValues("path");
		if (pathArray == null) {
			doc = node.getDocument();
		} else {
			Iterator<String> paths = Arrays.asList(pathArray).iterator();
			while (paths.hasNext()) {
				String path = paths.next();
				try {
					doc = getPathDocument(node, doc, path);
				} catch ( JDOMException jE ) {
					/* Ignore invalid path */
				}
			}
		}

		if (doc == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "No elements found.");
		} else {
			response.setContentType("text/xml");
			if (!viewOnly)
				editor.filterNoSavePI(doc);
			prettyXmlOutputter.output(doc, response.getOutputStream());
			response.setStatus(HttpServletResponse.SC_OK);
		}
	}

	/* If any path is specified, this attempts to match the
	 * element(s) for the specified path and return a single
	 * document with all matched elements and all their
	 * descendent elements.	 Associated attributes, text, etc.
	 * for all returned elements are preserved.
	 *
	 * The path format should be either:
	 *	 ScoreBoard Javascript, e.g. ScoreBoard.Team(1).Name
	 *	 xpath, e.g. ScoreBoard/Team[@Id='1']/Name
	 * As an exception, ScoreBoard Javascript format may
	 * be used with forward slashes instead of periods, e.g.
	 * ScoreBoard/Team(1)/Name
	 *
	 * If a Id attribute is omitted for an element with an Id,
	 * all elements with that name will be included in the results.
	 *
	 * examples:
	 * ScoreBoard.Team(1).Name
	 * ScoreBoard/Team(1)/Name
	 * ScoreBoard/Team[@Id='1']/Name
	 * ScoreBoard/Team/Name
	 *
	 * Note the first 3 examples will match only one element, while the
	 * last example will match the Name element under all/both Team elements.
	 */
	protected Document getPathDocument(Element node, Document doc, String path) throws JDOMException {
		if (path.startsWith("/"))
			path = path.substring(1);
		StringBuffer buffer = new StringBuffer();
		Matcher matcher = Pattern.compile("^[^()]+?(\\(.*?\\))?[./]").matcher(path);
		while (matcher.find()) {
			buffer.append(matcher.group().replaceFirst("[.]$", "/"));
			matcher.region(matcher.end(), matcher.regionEnd());
		}
		buffer.append(path.substring(matcher.regionStart()));
		path = buffer.toString().replaceAll("\\((.*?)\\)", "[@Id='$1']");
		if (path.endsWith("/"))
			path = path.substring(0, path.length()-1);
		if (path.length() > 0) {
			Iterator<?> nodes = XPath.selectNodes(node, path).iterator();
			while (nodes.hasNext()) {
				Document d = editor.cloneDocumentToClonedElement((Element)nodes.next()).getDocument();
				if (doc == null)
					doc = d;
				else
					editor.mergeDocuments(doc, d);
			}
			return doc;
		}
		return node.getDocument();
	}

	protected XmlScoreBoard getXmlScoreBoard() { return scoreBoardModel.getXmlScoreBoard(); }

	protected XmlDocumentEditor editor = new XmlDocumentEditor();
	protected XMLOutputter prettyXmlOutputter = XmlDocumentEditor.getPrettyXmlOutputter();
}
