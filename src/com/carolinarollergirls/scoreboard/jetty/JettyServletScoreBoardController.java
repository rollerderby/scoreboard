package com.carolinarollergirls.scoreboard.jetty;

import java.io.File;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServlet;

import org.eclipse.jetty.http.HttpCookie.SameSite;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.json.JSONStateManager;
import com.carolinarollergirls.scoreboard.utils.BasePath;
import com.carolinarollergirls.scoreboard.utils.Logger;

import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.filter.MetricsFilter;
import io.prometheus.client.hotspot.DefaultExports;

public class JettyServletScoreBoardController {
    public JettyServletScoreBoardController(ScoreBoard sb, JSONStateManager jsm, String host, int port) {
        scoreBoard = sb;
        this.jsm = jsm;
        this.host = host;
        this.port = port;

        init();
    }

    protected void init() {
        server = new Server();
        ServerConnector sC = new ServerConnector(server);
        sC.setHost(host);
        sC.setPort(port);
        server.addConnector(sC);

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        server.setHandler(contexts);

        ServletContextHandler sch = new ServletContextHandler(contexts, "/", ServletContextHandler.SESSIONS);

        SessionHandler sessions = new ScoreBoardSessionHandler(scoreBoard);
        sessions.setHttpOnly(true);
        sessions.setSameSite(SameSite.LAX);
        sessions.setSessionCookie("CRG_SCOREBOARD");
        sessions.getSessionCookieConfig().setMaxAge(COOKIE_DURATION_SECONDS);
        sessions.setMaxInactiveInterval(COOKIE_DURATION_SECONDS);
        // Sessions are created per request, so they're actually refreshed on each
        // request which is harmless.
        sessions.setRefreshCookieAge(1);
        sch.setSessionHandler(sessions);

        FilterHolder mf;
        // Only keep the first two path components.
        mf = new FilterHolder(
            new MetricsFilter("jetty_http_request_latency_seconds", "Jetty HTTP request latency", 2, null));
        sch.addFilter(mf, "/*", EnumSet.of(DispatcherType.REQUEST));

        sch.setResourceBase((new File(BasePath.get(), "html")).getPath());
        ServletHolder sh = new ServletHolder(new DefaultServlet());
        sh.setInitParameter("cacheControl", "no-cache");
        sh.setInitParameter("etags", "true");
        sch.addServlet(sh, "/*");

        urlsServlet = new UrlsServlet(server);
        sch.addServlet(new ServletHolder(urlsServlet), "/urls/*");

        ws = new WS(scoreBoard, jsm);
        sch.addServlet(new ServletHolder(ws), "/WS/*");

        DefaultExports.initialize();
        metricsServlet = new MetricsServlet();
        sch.addServlet(new ServletHolder(metricsServlet), "/metrics");

        HttpServlet sjs = new SaveJsonScoreBoard(jsm);
        sch.addServlet(new ServletHolder(sjs), "/SaveJSON/*");

        HttpServlet ljs = new LoadJsonScoreBoard(scoreBoard);
        sch.addServlet(new ServletHolder(ljs), "/Load/*");

        HttpServlet ms = new MediaServlet(scoreBoard, new File(BasePath.get(), "html").getPath());
        sch.addServlet(new ServletHolder(ms), "/Media/*");
    }

    public void start() {
        try {
            server.start();
        } catch (Exception e) { throw new RuntimeException("Could not start server : " + e.getMessage()); }

        Logger.printMessage("");
        Logger.printMessage("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
        Logger.printMessage("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
        if (port == 8000) { Logger.printMessage("Double-click/open the 'start.html' file, or"); }
        Logger.printMessage("Open a web browser (either Google Chrome or Mozilla Firefox recommended) to:");
        Logger.printMessage("	http://" + (host != null ? host : "localhost") + ":" + port);
        try {
            Iterator<String> urls = urlsServlet.getUrls().iterator();
            if (urls.hasNext()) { Logger.printMessage("or try one of these URLs:"); }
            while (urls.hasNext()) { Logger.printMessage("	" + urls.next().toString()); }
        } catch (MalformedURLException muE) {
            Logger.printMessage("Internal error: malformed URL from Server Connector: " + muE.getMessage());
        } catch (SocketException sE) {
            Logger.printMessage("Internal error: socket exception from Server Connector: " + sE.getMessage());
        }
        Logger.printMessage("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        Logger.printMessage("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        Logger.printMessage("");

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                int removed =
                    scoreBoard.getClients().gcOldDevices(System.currentTimeMillis() - COOKIE_DURATION_SECONDS * 1000);
                if (removed > 0) { Logger.printMessage("Garbage collected " + removed + " old device(s)."); }
            }
        }, 0, 3600, TimeUnit.SECONDS);
    }

    protected ScoreBoard scoreBoard;
    protected Server server;
    protected JSONStateManager jsm;
    protected String host;
    protected int port;
    protected UrlsServlet urlsServlet;
    protected WS ws;
    protected MetricsServlet metricsServlet;

    // No tournament lasts more than a week, so this allows plenty of time for
    // a device to be setup in advance and then only used as a backup on the
    // last day of the tournament. WFTDA/MRDA/JRDA allow 2 weeks to submit
    // stats after a sanctioned game, so this is also sufficient time to keep
    // things around in case it happens to help with forensics if something odd
    // is found while preparing the statsbook.
    protected static final int COOKIE_DURATION_SECONDS = 86400 * 15;
}
