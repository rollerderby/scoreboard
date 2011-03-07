package com.carolinarollergirls.scoreboard.jetty;

import java.io.*;
import java.util.*;

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

		Element node = getXmlScoreBoard().getDocument().getRootElement();

		String path = request.getPathInfo();
		if (path.startsWith("/"))
			path = path.substring(1);
		path = path.replaceAll("\\(", "[@Id='").replaceAll("\\)", "']");
		try {
			if (path.length() > 0) {
				Iterator nodes = XPath.selectNodes(node, path).iterator();
				node = null;
				while (nodes.hasNext()) {
					Element n = editor.cloneDocumentToClonedElement((Element)nodes.next());
					if (node == null)
						node = n;
					else
						editor.mergeDocuments(node.getDocument(), n.getDocument());
				}
			}
		} catch ( Exception e ) {
			node = null;
		}

		if (node == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "No element found for '"+path+"'");
		} else {
			response.setContentType("text/xml");
			editor.sendToWriter(node.getDocument(), response.getWriter());
			response.setStatus(HttpServletResponse.SC_OK);
		}
	}

	protected XmlScoreBoard getXmlScoreBoard() { return scoreBoardModel.getXmlScoreBoard(); }

	protected XmlDocumentEditor editor = new XmlDocumentEditor();
}
