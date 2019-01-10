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

import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.AsyncScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl.BatchEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;

/**
 * Converts a ScoreBoardEvent into a representative XML Document or XML String.
 *
 * This class is not synchronized.	Each event method modifies the same document.
 */
public class ScoreBoardXmlListener implements ScoreBoardListener {
    public ScoreBoardXmlListener() { }
    public ScoreBoardXmlListener(ScoreBoard sb) {
        sb.addScoreBoardListener(new AsyncScoreBoardListener(this));
    }

    public boolean isEmpty() { return empty; }

    public Document getDocument() { return document; }

    public Document resetDocument() {
        Document oldDoc = document;
        empty = true;
        document = editor.createDocument("ScoreBoard");
        return oldDoc;
    }

    private void batchStart() {
        Element root = document.getRootElement();
        String b = root.getAttributeValue("BATCH_START");
        if (b == null) { b = ""; }
        b = b + "X";
        root.setAttribute("BATCH_START", b);
    }

    private void batchEnd() {
        Element root = document.getRootElement();
        String b = root.getAttributeValue("BATCH_END");
        if (b == null) { b = ""; }
        b = b + "X";
        root.setAttribute("BATCH_END", b);
    }

    public void scoreBoardChange(ScoreBoardEvent event) {
        ScoreBoardEventProvider p = event.getProvider();
        Element e = getElement(p);
        Property prop = event.getProperty();
        Object value = event.getValue();
        String v = (value == null ? null : value.toString());
        Boolean rem = event.isRemove();
        if (prop == BatchEvent.START) {
            batchStart();
        } else if (prop == BatchEvent.END) {
            batchEnd();
        } else if (prop instanceof PermanentProperty) {
            editor.setElement(e, prop, null, v);
        } else if (prop instanceof AddRemoveProperty) {
            Element ne;
            if (value instanceof ScoreBoardEventProvider && ((ScoreBoardEventProvider)value).getParent() == p) {
        	ne = converter.toElement(e, (ScoreBoardEventProvider)value);
            } else {
                ne = editor.setElement(e, prop, ((ValueWithId)value).getId(), ((ValueWithId)value).getValue());
            }
            if (rem) {
                editor.setRemovePI(ne);
            }
        } else {
            return;
        }
        empty = false;
    }
    
    protected Element getElement(ScoreBoardEventProvider p) {
	if (p.getParent() == null) {
	    return editor.getElement(document.getRootElement(), p.getProviderName());
	} else {
	    return editor.getElement(getElement(p.getParent()), p.getProviderName(), p.getProviderId());
	}
    }

    protected XmlDocumentEditor editor = new XmlDocumentEditor();
    protected ScoreBoardXmlConverter converter = new ScoreBoardXmlConverter();

    protected Document document = editor.createDocument("ScoreBoard");
    protected boolean empty = true;
}
