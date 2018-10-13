package com.carolinarollergirls.scoreboard.xml.stream;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Date;

import org.jdom.Document;

import com.carolinarollergirls.scoreboard.xml.XmlDocumentEditor;

public class RealtimeStreamListenerFilter extends StreamListenerFilter implements StreamListener {
    public RealtimeStreamListenerFilter(StreamListener l) { super(l); }

    public void xmlChange(Document d) {
        if (!lastTimeSet) {
            setLastTime(d);
        }

        try {
            waitUntilTime(d);
        } catch ( InterruptedException iE ) {
            /* Indicate stop processing */
            return;
        }
        super.xmlChange(d);
    }

    public void setPaused(boolean p) { paused = p; }
    public boolean isPaused() { return paused; }

    public void setSpeed(double s) { speed = s; }
    public double getSpeed() { return speed; }

    protected void setLastTime(Document d) {
        lastDocTime = editor.getSystemTime(d);
        lastRealTime = new Date().getTime();
        lastTimeSet = true;
    }

    protected void waitUntilTime(Document d) throws InterruptedException {
        long docTime = editor.getSystemTime(d);
        long elapsedDocTime = docTime - lastDocTime;
        lastDocTime = docTime;

        do {
            long sleepTime = Math.min(POLL_TIME, (elapsedDocTime - getElapsedRealTime()));
            if (sleepTime > 0) {
                Thread.sleep(sleepTime);
            }
        } while (getElapsedRealTime() < elapsedDocTime);

        while (isPaused()) {
            Thread.sleep(POLL_TIME);
        }

        lastRealTime = new Date().getTime();
    }

    protected long getElapsedRealTime() {
        return (long)(((new Date().getTime()) - lastRealTime) * getSpeed());
    }

    protected XmlDocumentEditor editor = new XmlDocumentEditor();
    protected long lastDocTime;
    protected long lastRealTime;
    protected boolean lastTimeSet = false;
    protected boolean paused = false;
    protected double speed = 1.0;

    public static final long POLL_TIME = 250; /* 250 ms */
}
