package com.carolinarollergirls.scoreboard.defaults;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;

import com.carolinarollergirls.scoreboard.event.*;

public abstract class DefaultScoreBoardEventProvider implements ScoreBoardEventProvider,ScoreBoardListener
{
  public abstract String getProviderName();
  public abstract Class getProviderClass();

  public void scoreBoardChange(ScoreBoardEvent event) {
    synchronized (listenersLock) {
      Iterator<ManagerRunnable> i = listeners.values().iterator();
      while (i.hasNext())
        i.next().addScoreBoardEvent(new ScoreBoardEvent(event.getProvider(), event.getProperty(), event.getValue()));
    }
  }

  public void addScoreBoardListener(ScoreBoardListener listener) {
    ManagerRunnable mR = new ManagerRunnable(listener);
    synchronized (listenersLock) {
      if (!listeners.containsKey(listener)) {
        Thread t = new Thread(mR);
        t.setDaemon(false);
        t.start();
        listeners.put(listener, mR);
      }
    }
  }
  public void removeScoreBoardListener(ScoreBoardListener listener) {
    ManagerRunnable mR;
    synchronized (listenersLock) {
      mR = listeners.remove(listener);
    }
    if (null != mR)
      mR.stop();
  }

  protected Object listenersLock = new Object();
  protected Map<ScoreBoardListener,ManagerRunnable> listeners = new Hashtable<ScoreBoardListener,ManagerRunnable>();

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

    public void stop() {
      synchronized (eventLock) {
        running = false;
        eventLock.notifyAll();
      }
    }

    public void run() {
      while (running) {
        ScoreBoardEvent event;

        synchronized (eventLock) {
          if (null == (event = eventQueue.poll()))
            try { eventLock.wait(); }
            catch ( Exception e ) { }
        }

        if (null != event) {
          try { listener.scoreBoardChange(event); }
          catch ( RuntimeException rE ) { /* Keep delivering events regardless of Exceptions in a listener's handler */ }
        }
      }
    }

    protected boolean running = true;

    protected Object eventLock = new Object();
    protected Queue<ScoreBoardEvent> eventQueue = new LinkedList<ScoreBoardEvent>();

    protected ScoreBoardListener listener;
  }
}
