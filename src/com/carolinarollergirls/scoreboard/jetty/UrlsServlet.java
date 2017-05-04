package com.carolinarollergirls.scoreboard.jetty;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.io.*;
import java.net.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.eclipse.jetty.server.*;
// import org.mortbay.jetty.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;

public class UrlsServlet extends HttpServlet
{
	public UrlsServlet(Server s) { server = s; }

	public List<URL> getUrls() throws MalformedURLException,SocketException {
		List<URL> urls = new ArrayList<URL>();
		Iterator<Connector> connectors = Arrays.asList(server.getConnectors()).iterator();
		while (connectors.hasNext()) {
			Connector c = connectors.next();
			addURLs(urls, c.getHost(), c.getLocalPort());
		}
		return urls;
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Expires", "-1");
		response.setCharacterEncoding("UTF-8");

		try {
			response.setContentType("text/plain");
			Iterator<URL> urls = getUrls().iterator();
			while (urls.hasNext())
				response.getWriter().println(urls.next().toString());
			response.setStatus(HttpServletResponse.SC_OK);
		} catch ( MalformedURLException muE ) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not parse internal URL : "+muE.getMessage());
		} catch ( SocketException sE ) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Socket Exception : "+sE.getMessage());
		}
	}

	protected void addURLs(List<URL> urls, String host, int port) throws MalformedURLException,SocketException {
		if (null == host)
			addInterfaces(urls, port);
		else
			addHost(urls, host, port);
	}

	protected void addInterfaces(List<URL> urls, int port) throws MalformedURLException,SocketException {
		Iterator<NetworkInterface> ifaces = Collections.list(NetworkInterface.getNetworkInterfaces()).iterator();
		while (ifaces.hasNext()) {
			Iterator<InetAddress> addrs = Collections.list(ifaces.next().getInetAddresses()).iterator();
			while (addrs.hasNext()) {
				InetAddress addr = addrs.next();
				if (addr instanceof Inet4Address)
					addHost(urls, addr.getHostAddress(), port);
			}
		}
	}

	protected void addHost(List<URL> urls, String host, int port) throws MalformedURLException {
		try {
			InetAddress addr = InetAddress.getByName(host);
			if (addr.isLoopbackAddress())
				return;
			String hostname = addr.getHostName();
			String hostaddr = addr.getHostAddress();
			if (!hostaddr.equals(hostname)) {
				String shortname = hostname.replaceAll("[.].*$", "");
				if (!hostname.equals(shortname))
					urls.add(new URL("http", shortname, port, "/"));
				urls.add(new URL("http", hostname, port, "/"));
			}
			urls.add(new URL("http", hostaddr, port, "/"));
		} catch ( UnknownHostException uhE ) {
			urls.add(new URL("http", host, port, "/"));
		}
	}

	protected Server server;
}
