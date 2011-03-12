package com.carolinarollergirls.scoreboard.jetty;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.jdom.*;
import org.jdom.xpath.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.xml.*;
import com.carolinarollergirls.scoreboard.model.*;

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
			editor.sendToWriter(doc, response.getWriter());
			response.setStatus(HttpServletResponse.SC_OK);
		}
	}

	/* If any path is specified, this attempts to match the
	 * element(s) for the specified path and return a single
	 * document with all matched elements and all their
	 * descendent elements.  Associated attributes, text, etc.
	 * for all returned elements are preserved.
	 *
	 * The path format should be either:
	 *   ScoreBoard Javascript, e.g. ScoreBoard.Team(1).Name
	 *   xpath, e.g. ScoreBoard/Team[@Id='1']/Name
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
			Iterator nodes = XPath.selectNodes(node, path).iterator();
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
}
