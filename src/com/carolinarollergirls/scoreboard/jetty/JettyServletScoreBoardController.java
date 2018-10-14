package com.carolinarollergirls.scoreboard.jetty;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.filter.MetricsFilter;
import io.prometheus.client.hotspot.DefaultExports;

import java.io.File;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import javax.servlet.ServletException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.json.JSONStateManager;
import com.carolinarollergirls.scoreboard.model.ScoreBoardModel;
// import org.eclipse.jetty.util.resource.Resource;


public class JettyServletScoreBoardController {
    public JettyServletScoreBoardController(ScoreBoardModel model, JSONStateManager jsm) {
        scoreBoardModel = model;
        this.jsm = jsm;

        init();

        ScoreBoardManager.printMessage("");
        ScoreBoardManager.printMessage("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
        ScoreBoardManager.printMessage("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
        if (port == DEFAULT_PORT) {
            ScoreBoardManager.printMessage("Double-click/open the 'start.html' file, or");
        }
        ScoreBoardManager.printMessage("Open a web browser (either Google Chrome or Mozilla Firefox recommended) to:");
        ScoreBoardManager.printMessage("	http://localhost:"+port);
        try {
            Iterator<URL> urls = urlsServlet.getUrls().iterator();
            if (urls.hasNext()) {
                ScoreBoardManager.printMessage("or try one of these URLs:");
            }
            while (urls.hasNext()) {
                ScoreBoardManager.printMessage("	"+urls.next().toString());
            }
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

        ServletContextHandler sch = new ServletContextHandler(contexts, "/");
        FilterHolder mf;
        try {
            // Only keep the first two path components.
            mf = new FilterHolder(new MetricsFilter("jetty_http_request_latency_seconds", "Jetty HTTP request latency", 2, null));
        } catch ( ServletException e ) {
            // Can't actually throw an exception, so this should never happen.
            throw new RuntimeException("Could not create MetricsFilter : "+e.getMessage());
        }
        sch.addFilter(mf, "/*", 1);

        urlsServlet = new UrlsServlet(server);
        sch.addServlet(new ServletHolder(urlsServlet), "/urls/*");

        jsonServlet = new JSONServlet(server, scoreBoardModel);
        sch.addServlet(new ServletHolder(jsonServlet), "/JSON/*");

        ws = new WS(scoreBoardModel, jsm);
        sch.addServlet(new ServletHolder(ws), "/WS/*");

        DefaultExports.initialize();
        metricsServlet = new MetricsServlet();
        sch.addServlet(new ServletHolder(metricsServlet), "/metrics");

        String staticPath = ScoreBoardManager.getProperty(PROPERTY_HTML_DIR_KEY);
        if (null != staticPath) {
            ServletContextHandler c = new ServletContextHandler(contexts, "/");
            ServletHolder sh = new ServletHolder(new DefaultServlet());
            sh.setInitParameter("cacheControl", "no-cache");
            sh.setInitParameter("etags", "true");
            c.addServlet(sh, "/*");
            c.addFilter(mf, "/*", 1);
            c.setResourceBase((new File(ScoreBoardManager.getDefaultPath(), staticPath)).getPath());
        }

        Enumeration<?> keys = ScoreBoardManager.getProperties().propertyNames();

        while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
            if (!key.startsWith(PROPERTY_SERVLET_KEY)) {
                continue;
            }

            String servlet = ScoreBoardManager.getProperty(key);

            try {
                ScoreBoardControllerServlet sbcS = (ScoreBoardControllerServlet)Class.forName(servlet).newInstance();
                sbcS.setScoreBoardModel(scoreBoardModel);
                ServletContextHandler c = new ServletContextHandler(contexts, sbcS.getPath());
                c.addFilter(mf, "/*", 1);
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
    protected JSONStateManager jsm;
    protected int port;
    protected UrlsServlet urlsServlet;
    protected JSONServlet jsonServlet;
    protected WS ws;
    protected MetricsServlet metricsServlet;

    public static final int DEFAULT_PORT = 8000;

    public static final String PROPERTY_LOCALHOST_KEY = JettyServletScoreBoardController.class.getName() + ".localhost";
    public static final String PROPERTY_PORT_KEY = JettyServletScoreBoardController.class.getName() + ".port";
    public static final String PROPERTY_SERVLET_KEY = JettyServletScoreBoardController.class.getName() + ".servlet";
    public static final String PROPERTY_HTML_DIR_KEY = JettyServletScoreBoardController.class.getName() + ".html.dir";
}
