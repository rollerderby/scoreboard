package com.carolinarollergirls.scoreboard.jetty;

import java.util.*;

import org.mortbay.jetty.*;
import org.mortbay.jetty.handler.*;
import org.mortbay.jetty.servlet.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;

public class JettyServletScoreBoardController implements ScoreBoardController
{
	public void setScoreBoardModel(ScoreBoardModel model) {
		scoreBoardModel = model;

		init();
	}

	protected void init() {
		int port = DEFAULT_PORT;
		try {
			port = Integer.parseInt(ScoreBoardManager.getProperties().getProperty(PROPERTY_PORT_KEY));
		} catch ( Exception e ) {
			ScoreBoardManager.printMessage("No server port defined, using default " + DEFAULT_PORT);
		}

		Server server = new Server(port);
		server.setSendDateHeader(true);
		ContextHandlerCollection contexts = new ContextHandlerCollection();
		server.setHandler(contexts);

		String staticPath = ScoreBoardManager.getProperties().getProperty(PROPERTY_HTML_DIR_KEY);
		if (null != staticPath) {
			Context c = new Context(contexts, "/", Context.SESSIONS);
			Map<String,String> initParams = new Hashtable<String,String>();
			initParams.put("org.mortbay.jetty.servlet.Default.cacheControl", "no-cache");
			c.setInitParams(initParams);
			c.addServlet(new ServletHolder(new DefaultServlet()), "/*");
			c.setResourceBase(staticPath);

			c = new Context(contexts, "/images", Context.SESSIONS);
			c.addServlet(new ServletHolder(new DefaultServlet()), "/*");
			c.setResourceBase(staticPath+"/images");

			c = new Context(contexts, "/video", Context.SESSIONS);
			c.addServlet(new ServletHolder(new DefaultServlet()), "/*");
			c.setResourceBase(staticPath+"/video");
		}

		Enumeration keys = ScoreBoardManager.getProperties().propertyNames();

		while (keys.hasMoreElements()) {
			String key = keys.nextElement().toString();
			if (!key.startsWith(PROPERTY_SERVLET_KEY))
				continue;

			String servlet = ScoreBoardManager.getProperties().getProperty(key);

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

	public static final int DEFAULT_PORT = 8080;

	public static final String PROPERTY_PORT_KEY = JettyServletScoreBoardController.class.getName() + ".port";
	public static final String PROPERTY_SERVLET_KEY = JettyServletScoreBoardController.class.getName() + ".servlet";
	public static final String PROPERTY_HTML_DIR_KEY = JettyServletScoreBoardController.class.getName() + ".html.dir";
}
