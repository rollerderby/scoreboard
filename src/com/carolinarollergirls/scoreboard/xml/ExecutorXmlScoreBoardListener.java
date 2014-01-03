package com.carolinarollergirls.scoreboard.xml;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import org.jdom.*;

import java.util.*;
import java.util.concurrent.*;

public class ExecutorXmlScoreBoardListener implements XmlScoreBoardListener
{
	public void addXmlScoreBoardListener(XmlScoreBoardListener l) {
		addXmlScoreBoardListener(l, null);
	}

	public void addXmlScoreBoardListener(XmlScoreBoardListener l, Document d) {
		synchronized (listenerLock) {
			if (!listeners.containsKey(l)) {
				listeners.put(l, Executors.newSingleThreadExecutor());
				submit(l, d);
			}
		}
	}

	public void removeXmlScoreBoardListener(XmlScoreBoardListener l) {
		synchronized (listenerLock) {
			listeners.remove(l);
		}
	}

	public void xmlChange(Document d) {
		synchronized (listenerLock) {
			Iterator<XmlScoreBoardListener> l = listeners.keySet().iterator();
			while (l.hasNext())
				submit(l.next(), d);
		}
	}

	protected void submit(XmlScoreBoardListener l, Document d) {
		ExecutorService eS = listeners.get(l);
		if (eS != null && d != null)
			eS.submit(new XmlChangeRunnable(l, (Document)d.clone()));
	}

	protected HashMap<XmlScoreBoardListener,ExecutorService> listeners = new LinkedHashMap<XmlScoreBoardListener,ExecutorService>();
	protected Object listenerLock = new Object();

	public class XmlChangeRunnable implements Runnable
	{
		public XmlChangeRunnable(XmlScoreBoardListener l, Document d) {
			listener = l;
			document = d;
		}
		public void run() { listener.xmlChange(document); }
		public XmlScoreBoardListener listener;
		public Document document;
	}
}
