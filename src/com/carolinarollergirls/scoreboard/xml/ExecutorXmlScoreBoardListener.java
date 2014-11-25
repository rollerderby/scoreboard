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

import com.carolinarollergirls.scoreboard.*;

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

	// Not a guarantee listeners will batch the changes before processing
	public void requestStartBatchChanges() {
		synchronized (listenerLock) {
			Iterator<XmlScoreBoardListener> i = listeners.keySet().iterator();
			while (i.hasNext()) {
				XmlScoreBoardListener l = i.next();
				if (l instanceof XmlScoreBoardBatchListener) {
					try { ((XmlScoreBoardBatchListener)l).startBatch(); }
					catch ( RuntimeException rE ) { /* Keep processing regardless of Exceptions */ }
				}
			}

			if (batchReleaseTimerTask != null) {
				batchReleaseTimerTask = new BatchReleaseTimerTask();
				timer.schedule(batchReleaseTimerTask, 200);
			}
		}
	}

	public void requestEndBatchChanges() {
		_requestEndBatchChanges(false);
	}

	private void _requestEndBatchChanges(boolean force) {
		synchronized (listenerLock) {
			Iterator<XmlScoreBoardListener> i = listeners.keySet().iterator();
			boolean stillActive = false;
			while (i.hasNext()) {
				XmlScoreBoardListener l = i.next();
				if (l instanceof XmlScoreBoardBatchListener) {
					try {
						if (!((XmlScoreBoardBatchListener)l).endBatch(force))
							stillActive = true;
					}
					catch ( RuntimeException rE ) { /* Keep processing regardless of Exceptions */ }
				}
			}
			if ((!stillActive || force) && batchReleaseTimerTask != null) {
				timer.cancel();
				batchReleaseTimerTask = null;
			}
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

	protected Timer timer = new Timer();
	protected TimerTask batchReleaseTimerTask = null;
	protected HashMap<XmlScoreBoardListener,ExecutorService> listeners = new LinkedHashMap<XmlScoreBoardListener,ExecutorService>();
	protected Object listenerLock = new Object();

	public class BatchReleaseTimerTask extends TimerTask {
		public void run() {
			ExecutorXmlScoreBoardListener.this._requestEndBatchChanges(true);
		}
	}

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
