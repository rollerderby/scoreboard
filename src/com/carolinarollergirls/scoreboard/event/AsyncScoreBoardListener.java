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
            synchronized(this) {
                try {
                    if((event = queue.poll()) == null) {
                        this.wait();
                    }
                } catch (InterruptedException e) {}
                inProgress = true;
            }
            if (event != null) {
                listener.scoreBoardChange(event);
            }
            synchronized(this) {
                inProgress = false;
            }
        }

    }

    private boolean isEmpty() {
        synchronized(this) {
            return queue.size() == 0 && !inProgress;
        }
    }

    private ScoreBoardListener listener;
    private boolean inProgress;
    private Queue<ScoreBoardEvent> queue = new LinkedList<ScoreBoardEvent>();

    // For unittests.
    public static void waitForEvents() {
        while (true) {
            boolean empty = true;
            for (AsyncScoreBoardListener asbl : listeners) {
                if (!asbl.isEmpty()) {
                    empty = false;
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
    private static Queue<AsyncScoreBoardListener> listeners = new ConcurrentLinkedQueue<AsyncScoreBoardListener>();
}

