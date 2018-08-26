package com.carolinarollergirls.scoreboard.defaults;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;

public class ScoreBoardEventProviderManager {
	private static final ScoreBoardEventProviderManager singleton = new ScoreBoardEventProviderManager();
	public static ScoreBoardEventProviderManager getSingleton() {
		return singleton;
	}

	public void addProviderListener(ScoreBoardEventProvider p, ScoreBoardListener l) {
		synchronized (mapLock) {
			ManagerRunnable manager = listenerMap.get(l);
			if (manager == null) {
				manager = new ManagerRunnable(l);
				listenerMap.put(l, manager);
			}

			Queue<ManagerRunnable> q = providerMap.get(p);
			if (q == null) {
				q = new LinkedList<ManagerRunnable>();
				providerMap.put(p, q);
			} else {
				Iterator<ManagerRunnable> i = q.iterator();
				while (i.hasNext()) {
					if (i.next().listener == l)
						return;
				}
			}
			q.add(manager);
			manager.providerCountInc();
		}
	}

	public void removeProviderListener(ScoreBoardEventProvider p, ScoreBoardListener l) {
		synchronized (mapLock) {
			Queue<ManagerRunnable> q = providerMap.get(p);
			if (q == null)
				return;
			Iterator<ManagerRunnable> i = q.iterator();
			while (i.hasNext()) {
				ManagerRunnable manager = i.next();
				if (manager.listener == l) {
					q.remove(manager);
					manager.providerCountDec();
					return;
				}
			}
		}
	}

	public void addScoreBoardEvent(ScoreBoardEventProvider p, ScoreBoardEvent e) {
		synchronized (mapLock) {
			Queue<ManagerRunnable> q = providerMap.get(p);
			if (q == null)
				return;
			Iterator<ManagerRunnable> i = q.iterator();
			while (i.hasNext())
				i.next().addScoreBoardEvent((ScoreBoardEvent)e.clone());
		}
	}

	// For unitests.
	public void waitForEvents() {
		while (true) {
			boolean empty = true;
			synchronized (mapLock) {
				for(Queue<ManagerRunnable> q: getSingleton().providerMap.values()) {
					for(ManagerRunnable mr : q) {
						if (!mr.isEmpty()) {
							empty = false;
						}
					}
				}
			}
			if (empty) {
				break;
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {};
		}
	}

	protected Object mapLock = new Object();
	protected Hashtable<ScoreBoardEventProvider, Queue<ManagerRunnable>> providerMap = new Hashtable<ScoreBoardEventProvider, Queue<ManagerRunnable>>();
	protected Hashtable<ScoreBoardListener, ManagerRunnable> listenerMap = new Hashtable<ScoreBoardListener, ManagerRunnable>();

	protected class ManagerRunnable implements Runnable
	{
		public ManagerRunnable(ScoreBoardListener l) {
			listener = l;
		}

		public void addScoreBoardEvent(ScoreBoardEvent event) {
			synchronized (eventLock) {
				eventQueue.add(event);
				eventLock.notifyAll();
			}
		}

		public void run() {
			while (providerCount > 0) {
				ScoreBoardEvent event;

				synchronized (eventLock) {
					if (null == (event = eventQueue.poll())) {
						try { eventLock.wait(); }
						catch ( Exception e ) { }
					}
					inProgress = true;
				}

				if (null != event) {
					try { listener.scoreBoardChange(event); }
					catch ( RuntimeException rE ) { /* Keep delivering events regardless of Exceptions in a listener's handler */ }
				}
				synchronized (eventLock) {
					inProgress = false;
				}
			}
		}

		private void providerCountInc() {
			synchronized (eventLock) {
				providerCount++;
				if (providerCount == 1) {
					Thread t = new Thread(this);
					t.setDaemon(false);
					t.start();
				}
			}
		}

		private void providerCountDec() {
			synchronized (eventLock) {
				providerCount--;
				if (providerCount == 0) {
					eventLock.notifyAll();
				}
			}
		}

		protected boolean isEmpty() {
			synchronized (eventLock) {
				return eventQueue.size() == 0 && !inProgress;
			}
		}

		protected Object eventLock = new Object();
		protected Queue<ScoreBoardEvent> eventQueue = new LinkedList<ScoreBoardEvent>();

		protected ScoreBoardListener listener;
		protected int providerCount = 0;
		protected boolean inProgress = false;
	}
}
