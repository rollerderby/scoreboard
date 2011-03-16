package com.carolinarollergirls.scoreboard.jetty;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.awt.*;
import java.awt.image.*;

import javax.imageio.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.jdom.*;
import org.jdom.output.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.xml.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.file.*;
import com.carolinarollergirls.scoreboard.policy.*;
import com.carolinarollergirls.scoreboard.defaults.*;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.servlet.*;

public class XmlScoreBoardServlet extends AbstractXmlServlet
{
	public String getPath() { return "/XmlScoreBoard"; }

	protected void getAll(HttpServletRequest request, HttpServletResponse response) throws IOException,JDOMException {
		response.setContentType("text/xml");
		editor.sendToWriter(scoreBoardModel.getXmlScoreBoard().getDocument(), response.getWriter(), Format.getPrettyFormat());
		response.setStatus(HttpServletResponse.SC_OK);
	}

	protected void get(HttpServletRequest request, HttpServletResponse response) throws IOException,JDOMException {
		String key;
		XmlListener listener = null;
		if ((null != (key = request.getParameter("key"))) && (null != (listener = (XmlListener)clientMap.get(key)))) {
			Document d = listener.getDocument();
			if (null == d) {
				response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
			} else {
				response.setContentType("text/xml");
				editor.sendToWriter(d, response.getWriter());
				response.setStatus(HttpServletResponse.SC_OK);
			}
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	protected void reloadListeners(HttpServletRequest request, HttpServletResponse response) {
		Document d = editor.createDocument("Reload");
		d.getRootElement().setAttribute("persistentIgnore", "true");
		scoreBoardModel.getXmlScoreBoard().mergeDocument(d);
		response.setContentType("text/plain");
		response.setStatus(HttpServletResponse.SC_OK);
	}

	protected void set(HttpServletRequest request, HttpServletResponse response) throws IOException,JDOMException {
		Document requestDocument = null;

		try {
			if (ServletFileUpload.isMultipartContent(request)) {
				ServletFileUpload upload = new ServletFileUpload();

				FileItemIterator iter = upload.getItemIterator(request);
				while (iter.hasNext()) {
					FileItemStream item = iter.next();
					if (!item.isFormField()) {
						InputStream stream = item.openStream();
						requestDocument = editor.toDocument(stream);
						stream.close();
						break;
					}
				}
			} else {
				requestDocument = editor.toDocument(request.getReader());
			}
		} catch ( FileUploadException fuE ) {
			response.getWriter().print(fuE.getMessage());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		if (null == requestDocument) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

//FIXME - uncomment this for debugging XML set requests
//System.err.println(editor.toString(requestDocument));

		/* This should clear the scoreboard to prepare for loading a new one */
		/* This does not work with continuous-save-to-file! */
		if (Boolean.parseBoolean(request.getParameter("clearScoreBoard"))) {
			reloadListeners(request, response);

			//Document d = converter.toDocument(scoreBoardModel);
//FIXME - replacing doc is wrong!
			//documentManager.replaceDocument(d);
		}

		scoreBoardModel.getXmlScoreBoard().mergeDocument(requestDocument);

		response.setContentType("text/plain");
		response.setStatus(HttpServletResponse.SC_OK);
	}
 
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
		super.doPost(request, response);
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
		super.doGet(request, response);

		try {
			if ("/get".equals(request.getPathInfo()))
				get(request, response);
			else if ("/set".equals(request.getPathInfo()))
				set(request, response);
			else if ("/reloadViewers".equals(request.getPathInfo()))
				reloadListeners(request, response);
			else if (request.getPathInfo().endsWith(".xml"))
				getAll(request, response);
		} catch ( JDOMException jE ) {
			response.getWriter().print(jE.getMessage());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

}
