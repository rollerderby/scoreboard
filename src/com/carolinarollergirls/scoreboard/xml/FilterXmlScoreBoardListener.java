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
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;

public class FilterXmlScoreBoardListener implements XmlScoreBoardListener {
    public void xmlChange(Document d) {
        try {
            editor.filterOutDocumentXPath(d, getFilter());
            batchCnt += batchAdjustment(d, "BATCH_START");
            batchCnt -= batchAdjustment(d, "BATCH_END");
        } catch ( JDOMException jE ) {
            ScoreBoardManager.printMessage("Error filtering XML event : "+jE.getMessage());
        }
    }

    private int batchAdjustment(Document d, String which) {
        Element root = d.getRootElement();
        String b = root.getAttributeValue(which);
        return (b == null ? 0 : b.length());
    }

    public void clearFilter() { filterXPath = null; }
    public void setFilter(XPath f) { filterXPath = f; }
    public XPath getFilter() { return filterXPath; }

    public boolean isBatchActive() { return batchCnt > 0; }
    public int batchCnt = 0;

    protected XmlDocumentEditor editor = new XmlDocumentEditor();

    protected XPath filterXPath = null;
}
