package com.carolinarollergirls.scoreboard.jetty;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.text.*;

import javax.imageio.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.jdom.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.xml.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.file.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public abstract class AbstractXmlServlet extends AbstractRegisterServlet
{
	protected void register(HttpServletRequest request, HttpServletResponse response) throws IOException {
		XmlListener listener = createXmlListener(scoreBoardModel);
		String key = addRegisteredListener(listener);
		response.setContentType("text/xml");
		editor.sendToWriter(editor.createDocument("Key", null, key), response.getWriter());
		response.setStatus(HttpServletResponse.SC_OK);
	}

	protected void getListenerInfo(HttpServletRequest request, HttpServletResponse response) throws IOException,JDOMException {
		Document doc = editor.createDocument();
		synchronized (clientMap) {
			Iterator listeners = clientMap.values().iterator();
			while (listeners.hasNext())
				((XmlListener)listeners.next()).addInfo(doc.getRootElement());
		}

		response.setContentType("text/xml");
		editor.sendToWriter(doc, response.getWriter());
		response.setStatus(HttpServletResponse.SC_OK);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
		super.doPost(request, response);

		try {
			if ("/getListenerInfo".equals(request.getPathInfo()))
				getListenerInfo(request, response);
		} catch ( Exception e ) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	protected XmlListener getXmlListenerForRequest(HttpServletRequest request) {
		return (XmlListener)getRegisteredListenerForRequest(request);
	}

	protected XmlListener createXmlListener(ScoreBoard scoreBoard) {
		return new XmlListener(scoreBoard);
	}

	protected XmlDocumentEditor editor = new XmlDocumentEditor();

	protected class XmlListener extends RegisteredListener
	{
		public XmlListener(ScoreBoard sb) {
			xmlScoreBoardListener = new DefaultXmlScoreBoardListener(sb.getXmlScoreBoard());
		}

		public Document getDocument() {
			return xmlScoreBoardListener.getDocument();
		}

		public Document resetDocument() {
			return xmlScoreBoardListener.resetDocument();
		}

		public boolean isEmpty() { return xmlScoreBoardListener.isEmpty(); }

		public void addInfo(Element node) {
			Element e = editor.addElement(node, "Listener", getKey());
			editor.addElement(e, "LastRequestTime", null, Long.toString(new Date().getTime() - getLastRequestTime()));
		}

		protected DefaultXmlScoreBoardListener xmlScoreBoardListener;
	}
}
