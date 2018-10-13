package com.carolinarollergirls.scoreboard.xml;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import org.jdom.Document;

public class SleepingQueueXmlScoreBoardListener extends QueueXmlScoreBoardListener implements XmlScoreBoardListener {
    public SleepingQueueXmlScoreBoardListener() { super(); }
    public SleepingQueueXmlScoreBoardListener(XmlScoreBoard sb) { super(sb); }

    public void xmlChange(Document d) {
        synchronized (documentsLock) {
            super.xmlChange(d);
            if (!isEmpty() && !isBatchActive()) {
                documentsLock.notifyAll();
            }
        }
    }

    public Document getNextDocument() { return getNextDocument(0); }
    public Document getNextDocument(int timeout) {
        synchronized (documentsLock) {
            try {
                if (isEmpty() || isBatchActive()) {
                    documentsLock.wait(timeout);
                }
            } catch ( InterruptedException iE ) { }
            return super.getNextDocument();
        }
    }
}
