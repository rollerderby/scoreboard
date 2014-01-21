package com.carolinarollergirls.scoreboard.xml;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;

import org.jdom.*;
import org.jdom.xpath.*;

import com.carolinarollergirls.scoreboard.*;

public class DefaultXmlDocumentManager implements XmlDocumentManager
{
	public DefaultXmlDocumentManager(String n) {
		managedElementName = n;
	}

	public void setXmlScoreBoard(XmlScoreBoard xsB) {
		myXPath = editor.createXPath(getXPathString());
		xmlScoreBoard = xsB;
		xmlScoreBoard.addXmlDocumentManager(this);

		reset();
	}

	public XmlScoreBoard getXmlScoreBoard() { return xmlScoreBoard; }

	public void reset() {
		update(editor.setRemovePI(createXPathElement()));
		Element reset = new Element("Reset");
		update(createXPathElement().addContent(reset));
	}

	public void processDocument(Document d) {
		try {
			processElement(editor.cloneDocumentToClonedElement((Element)myXPath.selectSingleNode(d)));
		} catch ( Exception e ) {
			/* Ignore exceptions in document processing */
		}
	}

	protected void processElement(Element e) throws Exception {
		/* By default, process all child elements. */
		Iterator i = e.getChildren().iterator();
		while (i.hasNext()) {
			try {
				processChildElement((Element)i.next());
			} catch ( Exception ex ) {
				/* Ignore exceptions in element processing */
			}
		}
	}

	protected void processChildElement(Element e) throws Exception {
		/* By default, ignore unless this is a Reset */
		if (e.getName().equals("Reset") && Boolean.parseBoolean(editor.getText(e)))
			reset();
	}

	protected Document createDocument() {
		return editor.createDocument();
	}

	protected void update(Element e) {
		if (e != null)
			update(e.getDocument());
	}
	protected void update(Document d) {
		if (d != null)
			xmlScoreBoard.xmlChange(d);
	}

	protected Element getXPathElement() throws JDOMException {
		return (Element)myXPath.selectSingleNode(xmlScoreBoard.getDocument());
	}
	protected Element createXPathElement() {
		Element e = new Element(getManagedElementName());
		createDocument().getRootElement().addContent(e);
		return e;
	}
	protected String getXPathString() { return "/*/"+getManagedElementName(); }
	protected String getManagedElementName() { return managedElementName; }

	private String managedElementName;

	protected XmlDocumentEditor editor = new XmlDocumentEditor();

	protected XmlScoreBoard xmlScoreBoard;
	protected XPath myXPath;
}

