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
import java.util.Iterator;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.json.JSONStateManager;
import com.carolinarollergirls.scoreboard.utils.BasePath;
import com.carolinarollergirls.scoreboard.utils.Logger;


public class JettyServletScoreBoardController {
    public JettyServletScoreBoardController(ScoreBoard sb, JSONStateManager jsm, String host, int port) {
        scoreBoard = sb;
        this.jsm = jsm;

        init(host, port);

        Logger.printMessage("");
        Logger.printMessage("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
        Logger.printMessage("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
        if (port == 8000) {
            Logger.printMessage("Double-click/open the 'start.html' file, or");
        }
        Logger.printMessage("Open a web browser (either Google Chrome or Mozilla Firefox recommended) to:");
        Logger.printMessage("	http://localhost:"+port);
        try {
            Iterator<URL> urls = urlsServlet.getUrls().iterator();
            if (urls.hasNext()) {
                Logger.printMessage("or try one of these URLs:");
            }
            while (urls.hasNext()) {
                Logger.printMessage("	"+urls.next().toString());
            }
        } catch ( MalformedURLException muE ) {
            Logger.printMessage("Internal error: malformed URL from Server Connector: "+muE.getMessage());
        } catch ( SocketException sE ) {
            Logger.printMessage("Internal error: socket exception from Server Connector: "+sE.getMessage());
        }
        Logger.printMessage("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        Logger.printMessage("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        Logger.printMessage("");
    }

    protected void init(String host, int port) {
        SelectChannelConnector sC = new SelectChannelConnector();
        sC.setHost(host);
        sC.setPort(port);
        Server server = new Server();
        server.addConnector(sC);

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

        ws = new WS(scoreBoard, jsm);
        sch.addServlet(new ServletHolder(ws), "/WS/*");

        DefaultExports.initialize();
        metricsServlet = new MetricsServlet();
        sch.addServlet(new ServletHolder(metricsServlet), "/metrics");

        ServletContextHandler c = new ServletContextHandler(contexts, "/");
        ServletHolder sh = new ServletHolder(new DefaultServlet());
        sh.setInitParameter("cacheControl", "no-cache");
        sh.setInitParameter("etags", "true");
        c.addServlet(sh, "/*");
        c.addFilter(mf, "/*", 1);
        c.setResourceBase((new File(BasePath.get(), "html")).getPath());

        HttpServlet sjs = new SaveJsonScoreBoard(jsm);
        c.addServlet(new ServletHolder(sjs), "/SaveJSON/*");

        HttpServlet ljs = new LoadJsonScoreBoard(scoreBoard);
        c.addServlet(new ServletHolder(ljs), "/LoadJSON/*");

        HttpServlet sbvs = new ScoreBoardVersionServlet();
        c.addServlet(new ServletHolder(sbvs), "/version");

        HttpServlet ms = new MediaServlet(scoreBoard, new File(BasePath.get(), "html").getPath());
        c.addServlet(new ServletHolder(ms), "/Media/*");

        try {
            server.start();
        } catch ( Exception e ) {
            throw new RuntimeException("Could not start server : "+e.getMessage());
        }
    }

    protected ScoreBoard scoreBoard;
    protected JSONStateManager jsm;
    protected UrlsServlet urlsServlet;
    protected WS ws;
    protected MetricsServlet metricsServlet;
}
