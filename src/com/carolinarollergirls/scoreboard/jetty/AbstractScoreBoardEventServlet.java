package com.carolinarollergirls.scoreboard.jetty;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.awt.image.*;

import javax.imageio.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public abstract class AbstractScoreBoardEventServlet extends AbstractRegisterServlet
{
	protected abstract void events(HttpServletRequest request, HttpServletResponse response) throws IOException;

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
		super.doPost(request, response);

		if ("/events".equals(request.getPathInfo()))
			events(request, response);
	}

	protected class QueueingListener extends RegisteredListener implements ScoreBoardListener
	{
		public void scoreBoardChange(ScoreBoardEvent event) {
			if (MAX_QUEUE_SIZE <= q.size()) {
				AbstractScoreBoardEventServlet.this.removeRegisteredListener(this);
				return;
			}

			q.add(event);
		}

		public Queue<ScoreBoardEvent> q = new ConcurrentLinkedQueue<ScoreBoardEvent>();

		public static final int MAX_QUEUE_SIZE = 250;
	}

	public static final int MAX_CLIENTS = 250;
}
