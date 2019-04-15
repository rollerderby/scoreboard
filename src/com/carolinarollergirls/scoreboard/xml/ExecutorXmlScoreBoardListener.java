package com.carolinarollergirls.scoreboard.xml;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jdom.Document;

public class ExecutorXmlScoreBoardListener implements XmlScoreBoardListener {
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

    @Override
    public void xmlChange(Document d) {
        synchronized (listenerLock) {
            Iterator<XmlScoreBoardListener> l = listeners.keySet().iterator();
            while (l.hasNext()) {
                submit(l.next(), d);
            }
        }
    }

    protected void submit(XmlScoreBoardListener l, Document d) {
        ExecutorService eS = listeners.get(l);
        if (eS != null && d != null) {
            eS.submit(new XmlChangeRunnable(l, (Document)d.clone()));
        }
    }

    protected HashMap<XmlScoreBoardListener,ExecutorService> listeners = new LinkedHashMap<>();
    protected Object listenerLock = new Object();

    public class XmlChangeRunnable implements Runnable {
        public XmlChangeRunnable(XmlScoreBoardListener l, Document d) {
            listener = l;
            document = d;
        }
        @Override
        public void run() { listener.xmlChange(document); }
        public XmlScoreBoardListener listener;
        public Document document;
    }
}
