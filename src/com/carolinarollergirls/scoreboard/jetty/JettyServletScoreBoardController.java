package com.carolinarollergirls.scoreboard.jetty;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.net.*;
import java.util.*;

import org.mortbay.jetty.*;
import org.mortbay.jetty.bio.*;
import org.mortbay.jetty.handler.*;
import org.mortbay.jetty.servlet.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;

public class JettyServletScoreBoardController implements ScoreBoardController
{
  public void setScoreBoardModel(ScoreBoardModel model) {
    scoreBoardModel = model;

    init();

    ScoreBoardManager.printMessage("");
    ScoreBoardManager.printMessage("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
    ScoreBoardManager.printMessage("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
    if (port == DEFAULT_PORT)
      ScoreBoardManager.printMessage("Double-click/open the 'start.html' file, or");
    ScoreBoardManager.printMessage("Open a web browser (either Google Chrome or Mozilla Firefox recommended) to:");
    ScoreBoardManager.printMessage("  http://localhost:"+port);
    try {
      Iterator<URL> urls = urlsServlet.getUrls().iterator();
      if (urls.hasNext())
        ScoreBoardManager.printMessage("or try one of these URLs:");
      while (urls.hasNext())
        ScoreBoardManager.printMessage("  "+urls.next().toString());
    } catch ( MalformedURLException muE ) {
      ScoreBoardManager.printMessage("Internal error: malformed URL from Server Connector: "+muE.getMessage());
    } catch ( SocketException sE ) {
      ScoreBoardManager.printMessage("Internal error: socket exception from Server Connector: "+sE.getMessage());
    }
    ScoreBoardManager.printMessage("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
    ScoreBoardManager.printMessage("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
    ScoreBoardManager.printMessage("");
  }

  protected void init() {
    port = DEFAULT_PORT;
    try {
      port = Integer.parseInt(ScoreBoardManager.getProperty(PROPERTY_PORT_KEY));
    } catch ( Exception e ) {
      ScoreBoardManager.printMessage("No server port defined, using default " + DEFAULT_PORT);
    }

    Server server;
    SessionIdManager sessionIdManager;

    // See http://docs.codehaus.org/display/JETTY/Connectors+slow+to+startup
    if (Boolean.parseBoolean(ScoreBoardManager.getProperty(PROPERTY_SECURE_SESSION_ID_KEY, DEFAULT_SECURE_SESSION_ID))) {
      ScoreBoardManager.printMessage("Using secure session IDs (this may cause a delay starting up)");
      // By default HashSessionIdManager uses SecureRandom, which can block
      // on systems with low entropy, which can delay the jetty server
      // startup, sometimes for a long time.
      sessionIdManager = new HashSessionIdManager();
    } else {
      ScoreBoardManager.printMessage("Using less-secure session IDs (this is ok for now, and speeds up startup)");
      // This uses Random, which doesn't block, but is not as "secure"
      // (i.e. session id's may be predictable).  Since we don't really
      // use security at all (currently), that shouldn't matter.
      sessionIdManager = new HashSessionIdManager(new Random());
    }

    String localhost = ScoreBoardManager.getProperty(PROPERTY_LOCALHOST_KEY);
    if (null != localhost && Boolean.parseBoolean(localhost)) {
      ScoreBoardManager.printMessage("ScoreBoard configured to listen ONLY on localhost interface.");
      SocketConnector sC = new SocketConnector();
      sC.setHost("localhost");
      sC.setPort(port);
      server = new Server();
      server.addConnector(sC);
    } else {
      server = new Server(port);
    }

    server.setSessionIdManager(sessionIdManager);
    server.setSendDateHeader(true);
    ContextHandlerCollection contexts = new ContextHandlerCollection();
    server.setHandler(contexts);

    String staticPath = ScoreBoardManager.getProperty(PROPERTY_HTML_DIR_KEY);
    if (null != staticPath) {
      Context c = new Context(contexts, "/", Context.SESSIONS);
      Map<String,String> initParams = new Hashtable<String,String>();
      initParams.put("org.mortbay.jetty.servlet.Default.cacheControl", "no-cache");
      c.setInitParams(initParams);
      c.addServlet(new ServletHolder(new DefaultServlet()), "/*");
      c.setResourceBase(staticPath);
    }

    urlsServlet = new UrlsServlet(server);
    new Context(contexts, "/urls", Context.SESSIONS).addServlet(new ServletHolder(urlsServlet), "/*");

    Enumeration keys = ScoreBoardManager.getProperties().propertyNames();

    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      if (!key.startsWith(PROPERTY_SERVLET_KEY))
        continue;

      String servlet = ScoreBoardManager.getProperty(key);

      try {
        ScoreBoardControllerServlet sbcS = (ScoreBoardControllerServlet)Class.forName(servlet).newInstance();
        sbcS.setScoreBoardModel(scoreBoardModel);
        Context c = new Context(contexts, sbcS.getPath(), Context.SESSIONS);
        c.addServlet(new ServletHolder(sbcS), "/*");
      } catch ( Exception e ) {
        ScoreBoardManager.printMessage("Could not create Servlet " + servlet + " : " + e.getMessage());
        e.printStackTrace();
      }
    }

    try {
      server.start();
    } catch ( Exception e ) {
      throw new RuntimeException("Could not start server : "+e.getMessage());
    }
  }

  protected ScoreBoardModel scoreBoardModel;
  protected int port;
  protected UrlsServlet urlsServlet;

  public static final int DEFAULT_PORT = 8000;
  public static final String DEFAULT_SECURE_SESSION_ID = "false";

  public static final String PROPERTY_LOCALHOST_KEY = JettyServletScoreBoardController.class.getName() + ".localhost";
  public static final String PROPERTY_PORT_KEY = JettyServletScoreBoardController.class.getName() + ".port";
  public static final String PROPERTY_SERVLET_KEY = JettyServletScoreBoardController.class.getName() + ".servlet";
  public static final String PROPERTY_HTML_DIR_KEY = JettyServletScoreBoardController.class.getName() + ".html.dir";
  public static final String PROPERTY_SECURE_SESSION_ID_KEY = JettyServletScoreBoardController.class.getName() + ".secure.session.ids";
}
