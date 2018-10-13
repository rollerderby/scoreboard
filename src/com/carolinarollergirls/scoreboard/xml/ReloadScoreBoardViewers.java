package com.carolinarollergirls.scoreboard.xml;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import org.jdom.Element;

public class ReloadScoreBoardViewers extends DefaultXmlDocumentManager implements XmlDocumentManager {
    public ReloadScoreBoardViewers() { super("ReloadViewers"); }

    public void reloadViewers() {
        update(editor.setPI(createXPathElement(), "Reload"));
    }

    protected void processElement(Element e) throws Exception {
        if (Boolean.parseBoolean(editor.getText(e))) {
            reloadViewers();
        }
    }
}

