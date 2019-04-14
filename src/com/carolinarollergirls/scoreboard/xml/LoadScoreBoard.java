package com.carolinarollergirls.scoreboard.xml;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.io.File;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class LoadScoreBoard extends SegmentedXmlDocumentManager {
    public LoadScoreBoard() { super("SaveLoad", "Load"); }

    @Override
    public void setXmlScoreBoard(XmlScoreBoard xsB) {
        super.setXmlScoreBoard(xsB);

        Element e = createXPathElement();
        editor.addElement(e, "LoadFile");
        editor.addElement(e, "MergeFile");
        update(e);
    }

    @Override
    public void reset() {
        /* Don't reset anything, as this controls loading. */
    }

    @Override
    protected void processChildElement(Element e) throws Exception {
        super.processChildElement(e);
        Document d = saxBuilder.build(new File(DIRECTORY_NAME, editor.getText(e)));
        if (e.getName().equals("LoadFile")) {
            xmlScoreBoard.loadDocument(d);
        } else if (e.getName().equals("MergeFile")) {
            xmlScoreBoard.mergeDocument(d, false);
        }
    }

    @Override
    protected Element createXPathElement() {
        return editor.setNoSavePI(super.createXPathElement());
    }

    protected SAXBuilder saxBuilder = new SAXBuilder();

    public static final String DIRECTORY_NAME = "html/save";
}
