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
import org.jdom.xpath.XPath;

/**
 * This class simply passes any Element under its management back to the XmlScoreBoard,
 * So the subtree managed by this is open for anyone to edit.
 */
public abstract class PartialOpenXmlDocumentManager extends DefaultXmlDocumentManager implements XmlDocumentManager
{
	public PartialOpenXmlDocumentManager(String n) { super(n); }

	public void setXmlScoreBoard(XmlScoreBoard xsB) {
		myPartialXPath = editor.createXPath(getPartialXPathString());
		super.setXmlScoreBoard(xsB);
	}

	/* Write back all child elements after pruning (except a Reset) */
	protected void processChildElement(Element e) throws Exception {
		super.processChildElement(e);
		if (!e.getName().equals("Reset"))
			update(editor.cloneDocumentToClonedElement(e, myPartialXPath));
	}

	protected abstract String getPartialXPathString();

	protected XPath myPartialXPath;
}

