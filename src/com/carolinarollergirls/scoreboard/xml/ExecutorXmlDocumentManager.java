package com.carolinarollergirls.scoreboard.xml;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jdom.Document;

public class ExecutorXmlDocumentManager implements XmlDocumentManager
{
	public void addXmlDocumentManager(XmlDocumentManager m) {
		synchronized (managerLock) {
			if (!managers.containsKey(m))
				managers.put(m, Executors.newSingleThreadExecutor());
		}
	}

	public void removeXmlDocumentManager(XmlDocumentManager m) {
		synchronized (managerLock) {
			managers.remove(m);
		}
	}

	public List<XmlDocumentManager> findXmlDocumentManagers(Class<? extends XmlDocumentManager> c) {
		synchronized (managerLock) {
			List<XmlDocumentManager> l = new ArrayList<XmlDocumentManager>();
			Iterator<XmlDocumentManager> m = managers.keySet().iterator();
			while (m.hasNext()) {
				XmlDocumentManager m2 = m.next();
				if (c.isInstance(m2))
					l.add(m2);
			}
			return l;
		}
	}

	public void processDocument(Document d) {
		synchronized (managerLock) {
			Iterator<XmlDocumentManager> m = managers.keySet().iterator();
			while (m.hasNext())
				submitProcessDocument(m.next(), d);
		}
	}

	public void reset() {
		synchronized (managerLock) {
			Iterator<XmlDocumentManager> m = managers.keySet().iterator();
			while (m.hasNext())
				submitReset(m.next());
		}
	}

	protected void submitProcessDocument(XmlDocumentManager m, Document d) {
		ExecutorService eS = managers.get(m);
		if (eS != null && d != null)
			eS.submit(new ProcessDocumentRunnable(m, (Document)d.clone()));
	}

	protected void submitReset(XmlDocumentManager m) {
		ExecutorService eS = managers.get(m);
		if (eS != null)
			eS.submit(new ResetRunnable(m));
	}

	protected HashMap<XmlDocumentManager,ExecutorService> managers = new LinkedHashMap<XmlDocumentManager,ExecutorService>();
	protected Object managerLock = new Object();

	public class ProcessDocumentRunnable implements Runnable
	{
		public ProcessDocumentRunnable(XmlDocumentManager m, Document d) {
			manager = m;
			document = d;
		}
		public void run() { manager.processDocument(document); }
		public XmlDocumentManager manager;
		public Document document;
	}

	public class ResetRunnable implements Runnable
	{
		public ResetRunnable(XmlDocumentManager m) {
			manager = m;
		}
		public void run() { manager.reset(); }
		public XmlDocumentManager manager;
	}
}

