package com.carolinarollergirls.scoreboard.xml;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.LinkedList;

import org.jdom.Document;

public class QueueXmlScoreBoardListener extends FilterXmlScoreBoardListener implements XmlScoreBoardListener {
    public QueueXmlScoreBoardListener() { }
    public QueueXmlScoreBoardListener(XmlScoreBoard sb) {
        sb.addXmlScoreBoardListener(this);
    }

    public void xmlChange(Document d) {
        super.xmlChange(d);
        if (editor.isEmptyDocument(d)) {
            return;
        }
        synchronized (documentsLock) {
            if (queueNextDocument || documents.isEmpty()) {
                documents.addLast(d);
            } else {
                editor.mergeDocuments(documents.getLast(), d);
            }

            queueNextDocument = editor.hasRemovePI(d);
        }
    }

    public Document getNextDocument() {
        synchronized (documentsLock) {
            if (isBatchActive()) {
                return null;
            }
            return documents.poll();
        }
    }

    public boolean isEmpty() { return (null == documents.peek()); }

    protected boolean queueNextDocument = false;
    protected LinkedList<Document> documents = new LinkedList<Document>();
    protected Object documentsLock = new Object();
}
