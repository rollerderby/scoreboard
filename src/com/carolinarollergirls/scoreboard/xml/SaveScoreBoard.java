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
import java.io.FileOutputStream;

import org.jdom.Element;
import org.jdom.output.XMLOutputter;

public class SaveScoreBoard extends SegmentedXmlDocumentManager {
    public SaveScoreBoard() { super("SaveLoad", "Save"); }

    @Override
    public void setXmlScoreBoard(XmlScoreBoard xsB) {
        super.setXmlScoreBoard(xsB);

        Element e = createXPathElement();
        e.addContent(new Element("Filename"));
        e.addContent(new Element("Save"));
        e.addContent(editor.setText(new Element("Error"), "false"));
        e.addContent(new Element("Message"));
        update(e);
    }

    @Override
    public void reset() {
        /* Don't reset anything, as these controls should not be saved. */
    }

    @Override
    protected void processChildElement(Element e) {
        if (e.getName().equals("Filename")) {
            update(editor.cloneDocumentToElement(e, true));
        } else if (e.getName().equals("Save")) {
            save();
        }
    }

    protected void save() {
        Element msg = new Element("Message");
        Element error = editor.setText(new Element("Error"), "false");
        Element updateE = createXPathElement().addContent(msg).addContent(error);
        String filename = "";
        try {
            filename = editor.getText(getXPathElement().getChild("Filename"));
            FileOutputStream fos = new FileOutputStream(new File(DIRECTORY_NAME, filename));
            xmlOutputter.output(editor.filterNoSavePI(getXmlScoreBoard().getDocument()), fos);
            fos.close();
            editor.setText(msg, "Saved ScoreBoard to file '"+filename+"'");
        } catch ( Exception e ) {
            editor.setText(msg, "Could not save to file '"+filename+"' : "+e.getMessage());
            editor.setText(error, "true");
        } finally {
            update(updateE);
        }
    }

    @Override
    protected Element createXPathElement() {
        return editor.setNoSavePI(super.createXPathElement());
    }

    protected XMLOutputter xmlOutputter = XmlDocumentEditor.getPrettyXmlOutputter();

    public static final String DIRECTORY_NAME = "html/save";

}
