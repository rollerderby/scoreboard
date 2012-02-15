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
import com.carolinarollergirls.scoreboard.policy.*;
import com.carolinarollergirls.scoreboard.defaults.*;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.servlet.*;

public class XmlScoreBoardServlet extends AbstractXmlServlet
{
  public String getPath() { return "/XmlScoreBoard"; }

  protected void getAll(HttpServletRequest request, HttpServletResponse response) throws IOException,JDOMException {
    response.setContentType("text/xml");
    prettyXmlOutputter.output(scoreBoardModel.getXmlScoreBoard().getDocument(), response.getOutputStream());
    response.setStatus(HttpServletResponse.SC_OK);
  }

  protected void get(HttpServletRequest request, HttpServletResponse response) throws IOException,JDOMException {
    XmlListener listener = getXmlListenerForRequest(request);
    if (null == listener) {
      registrationKeyNotFound(request, response);
      return;
    }

    Document d = listener.getDocument();
    if (null == d) {
      response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
    } else {
      if (debugGet)
        ScoreBoardManager.printMessage("GET to "+listener.getKey()+"\n"+prettyXmlOutputter.outputString(d));
      response.setContentType("text/xml");
      rawXmlOutputter.output(d, response.getOutputStream());
      response.setStatus(HttpServletResponse.SC_OK);
    }
  }

  protected void set(HttpServletRequest request, HttpServletResponse response) throws IOException,JDOMException {
    XmlListener listener = getXmlListenerForRequest(request);
    Document requestDocument = editor.toDocument(request.getInputStream());

    if (null == listener) {
      registrationKeyNotFound(request, response);
      return;
    }

    if (null == requestDocument) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    if (debugSet)
      ScoreBoardManager.printMessage("SET from "+listener.getKey()+"\n"+rawXmlOutputter.outputString(requestDocument));

    scoreBoardModel.getXmlScoreBoard().mergeDocument(requestDocument);

    response.setContentType("text/plain");
    response.setStatus(HttpServletResponse.SC_OK);
  }
 
  protected void setDebug(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
    String get = request.getParameter("get");
    String set = request.getParameter("set");

    if (null != get) {
      if (get.equals("1") || get.equalsIgnoreCase("true"))
        debugGet = true;
      else if (get.equals("0") || get.equalsIgnoreCase("false"))
        debugGet = false;
    }
    if (null != set) {
      if (set.equals("1") || set.equalsIgnoreCase("true"))
        debugSet = true;
      else if (set.equals("0") || set.equalsIgnoreCase("false"))
        debugSet = false;
    }

    response.setContentType("text/plain");
    response.getWriter().println("Debug /get : "+debugGet);
    response.getWriter().println("Debug /set : "+debugSet);
    response.setStatus(HttpServletResponse.SC_OK);
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
    super.doPost(request, response);

    try {
      if ("/set".equals(request.getPathInfo()))
        set(request, response);
      else if (!response.isCommitted())
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    } catch ( JDOMException jE ) {
      ScoreBoardManager.printMessage("XmlScoreBoardServlet ERROR: "+jE.getMessage());
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
    super.doGet(request, response);

    try {
      if ("/get".equals(request.getPathInfo()))
        get(request, response);
      else if ("/debug".equals(request.getPathInfo()))
        setDebug(request, response);
      else if (request.getPathInfo().endsWith(".xml"))
        getAll(request, response);
      else if (!response.isCommitted())
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    } catch ( JDOMException jE ) {
      ScoreBoardManager.printMessage("XmlScoreBoardServlet ERROR: "+jE.getMessage());
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  protected boolean debugGet = false;
  protected boolean debugSet = false;
}
