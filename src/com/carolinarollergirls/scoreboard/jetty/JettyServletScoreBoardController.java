package com.carolinarollergirls.scoreboard.jetty;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

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
    ScoreBoardManager.printMessage("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
    ScoreBoardManager.printMessage("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
    if (port == DEFAULT_PORT)
      ScoreBoardManager.printMessage("Double-click/open the 'start.html' file or");
    ScoreBoardManager.printMessage("Open a web browser (Google Chrome or Mozilla Firefox) to http://localhost:"+port);
    ScoreBoardManager.printMessage("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
    ScoreBoardManager.printMessage("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
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

      /* These are separate Servlets for /images and /videos
       * because we want these to be cached, while the default
       * above has cacheControl set to no-cache
       */
      c = new Context(contexts, "/images", Context.SESSIONS);
      c.addServlet(new ServletHolder(new DefaultServlet()), "/*");
      c.setResourceBase(staticPath+"/images");
      c = new Context(contexts, "/videos", Context.SESSIONS);
      c.addServlet(new ServletHolder(new DefaultServlet()), "/*");
      c.setResourceBase(staticPath+"/videos");
    }

    new Context(contexts, "/urls", Context.SESSIONS).addServlet(new ServletHolder(new UrlsServlet(server)), "/*");

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

  public static final int DEFAULT_PORT = 8000;

  public static final String PROPERTY_LOCALHOST_KEY = JettyServletScoreBoardController.class.getName() + ".localhost";
  public static final String PROPERTY_PORT_KEY = JettyServletScoreBoardController.class.getName() + ".port";
  public static final String PROPERTY_SERVLET_KEY = JettyServletScoreBoardController.class.getName() + ".servlet";
  public static final String PROPERTY_HTML_DIR_KEY = JettyServletScoreBoardController.class.getName() + ".html.dir";
}
