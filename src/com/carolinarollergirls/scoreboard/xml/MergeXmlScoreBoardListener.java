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

public class MergeXmlScoreBoardListener implements XmlScoreBoardListener {
    public MergeXmlScoreBoardListener() { super(); }
    public MergeXmlScoreBoardListener(XmlScoreBoard sb) {
        super();
        sb.addXmlScoreBoardListener(this);
    }

    public void xmlChange(Document d) {
        synchronized (documentLock) {
            editor.mergeDocuments(document, d);
            empty = false;
        }
    }

    public Document getDocument() {
        synchronized (documentLock) {
            return (Document)document.clone();
        }
    }

    public Document resetDocument() {
        synchronized (documentLock) {
            Document oldDoc = document;
            document = editor.createDocument();
            empty = true;
            return oldDoc;
        }
    }

    public boolean isEmpty() { return empty; }

    protected XmlDocumentEditor editor = new XmlDocumentEditor();

    protected Document document = editor.createDocument();
    protected Object documentLock = new Object();
    protected boolean empty = true;
}
