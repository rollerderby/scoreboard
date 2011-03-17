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

	protected XmlListener getXmlListenerForRequest(HttpServletRequest request) {
		return (XmlListener)getRegisteredListenerForRequest(request);
	}

	protected XmlListener createXmlListener(ScoreBoard scoreBoard) {
		return new XmlListener(scoreBoard);
	}

	protected XmlDocumentEditor editor = new XmlDocumentEditor();

	protected class XmlListener extends RegisteredListener
	{
		public XmlListener(ScoreBoard sB) {
			queueListener = new QueueXmlScoreBoardListener(sB.getXmlScoreBoard());
		}

		public Document getDocument() { return queueListener.getNextDocument(); }

		public boolean isEmpty() { return queueListener.isEmpty(); }

		protected QueueXmlScoreBoardListener queueListener;
	}
}
