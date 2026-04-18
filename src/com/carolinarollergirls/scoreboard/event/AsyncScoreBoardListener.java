package com.carolinarollergirls.scoreboard.event;

import java.util.LinkedList;
import java.util.Queue;

import com.carolinarollergirls.scoreboard.utils.Logger;

public final class AsyncScoreBoardListener extends Thread implements ScoreBoardListener {
    public AsyncScoreBoardListener(ScoreBoardListener l) {
        this.listener = l;
        start();
    }

    @Override
    public void scoreBoardChange(ScoreBoardEvent<?> event) {
        synchronized (queue) {
            queue.add(event);
            queue.notifyAll();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                ScoreBoardEvent<?> event = null;
                synchronized (queue) {
                    try {
                        while ((event = queue.poll()) == null) { queue.wait(); }
                    } catch (InterruptedException e) {}
                }
                if (event != null) { listener.scoreBoardChange(event); }
            } catch (Throwable t) { Logger.printStackTrace("async event listener", t); }
        }
    }

    private ScoreBoardListener listener;
    private Queue<ScoreBoardEvent<?>> queue = new LinkedList<>();
}
