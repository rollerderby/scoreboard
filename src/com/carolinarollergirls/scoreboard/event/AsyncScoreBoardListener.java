package com.carolinarollergirls.scoreboard.event;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AsyncScoreBoardListener extends Thread implements ScoreBoardListener {
    public AsyncScoreBoardListener(ScoreBoardListener l) {
        this.listener = l;
        listeners.add(this);
        start();
    }

    public void scoreBoardChange(ScoreBoardEvent event) {
        synchronized(this) {
            queue.add(event);
            this.notifyAll();
        }
    }

    public void run() {
        while (true) {
            ScoreBoardEvent event = null;
            synchronized (this) {
        	try {
        	    if((event = queue.poll()) == null) {
        		this.wait();
        	    }
        	} catch (InterruptedException e) {}
            }
            if (event != null) {
                listener.scoreBoardChange(event);
            }
        }

    }

    private ScoreBoardListener listener;
    private Queue<ScoreBoardEvent> queue = new LinkedList<ScoreBoardEvent>();

    private static Queue<AsyncScoreBoardListener> listeners = new ConcurrentLinkedQueue<AsyncScoreBoardListener>();
}

