package com.carolinarollergirls.scoreboard.jetty;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
// import org.mortbay.jetty.*;

public class UrlsServlet extends HttpServlet {
    public UrlsServlet(Server s) { server = s; }

    public Set<String> getUrls() throws MalformedURLException, SocketException {
        Set<String> urls = new TreeSet<>();
        for (Connector c : server.getConnectors()) {
            if (c instanceof NetworkConnector) {
                addURLs(urls, ((NetworkConnector) c).getHost(), ((NetworkConnector) c).getLocalPort());
            }
        }
        return urls;
    }

    protected void addURLs(Set<String> urls, String host, int port) throws MalformedURLException, SocketException {
        if (null == host) {
            for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress addr : Collections.list(iface.getInetAddresses())) {
                    if (!addr.isLoopbackAddress()) {
                        urls.add(new URL("http", addr.getHostAddress(), port, "/").toString());
                    }
                }
            }
        } else {
            urls.add(new URL("http", host, port, "/").toString());
            try {
                // Get the IP address of the given host.
                urls.add(new URL("http", InetAddress.getByName(host).getHostAddress(), port, "/").toString());
            } catch (UnknownHostException uhE) {}
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Expires", "-1");
        response.setCharacterEncoding("UTF-8");

        try {
            response.setContentType("text/plain");
            for (String u : getUrls()) { response.getWriter().println(u); }
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (MalformedURLException muE) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Could not parse internal URL : " + muE.getMessage());
        } catch (SocketException sE) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Socket Exception : " + sE.getMessage());
        }
    }

    protected Server server;
}
