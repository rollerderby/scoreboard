package com.carolinarollergirls.scoreboard.xml;

import java.util.*;

import org.jdom.*;
import org.jdom.xpath.*;

import com.carolinarollergirls.scoreboard.*;

public abstract class AbstractXmlDocumentManager implements XmlDocumentManager
{
	public void setXmlScoreBoard(XmlScoreBoard xsB) {
		xmlScoreBoard = xsB;
		xmlScoreBoard.addXmlDocumentManager(this);

		reset();
	}

	public XmlScoreBoard getXmlScoreBoard() { return xmlScoreBoard; }

	public void reset() {
		update(createXPathElement().setAttribute("remove", "true"));
		update(createXPathElement());
	}

	public void processDocument(Document d) {
		try {
			processElement(editor.cloneDocumentToClonedElement((Element)myXPath.selectSingleNode(d)));
		} catch ( Exception e ) {
			/* Ignore exceptions in document processing */
		}
	}

	protected void processElement(Element e) {
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

	protected void processChildElement(Element e) {
		/* By default, ignore */
	}

	protected Document createDocument() {
		return editor.createDocument();
	}

	protected void update(Element e) { update(e.getDocument()); }
	protected void update(Document d) { xmlScoreBoard.xmlChange(d); }

	protected Element getXPathElement() throws JDOMException {
		return (Element)myXPath.selectSingleNode(xmlScoreBoard.getDocument());
	}
	protected Element createXPathElement() {
		Element e = new Element(getManagedElementName());
		createDocument().getRootElement().addContent(e);
		return e;
	}
	protected String getXPathString() { return "/*/"+getManagedElementName(); }
	protected abstract String getManagedElementName();

	protected XmlDocumentEditor editor = new XmlDocumentEditor();

	protected XmlScoreBoard xmlScoreBoard;
	protected XPath myXPath = editor.createXPath(getXPathString());
}

