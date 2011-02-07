package com.carolinarollergirls.scoreboard.xml;

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
//FIXME - update xml-listener so it doesn't keep the remove attr if an element is added back
//FIXME - this won't work until that change!
		update(createXPathElement().setAttribute("remove", "true"));
		update(createXPathElement());
	}

	public void processDocument(Document d) {
		try {
			processElement(editor.cloneDocumentToClonedElement((Element)xPath.selectSingleNode(d)));
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
			} catch ( Exception e ) {
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
		return (Element)xPath.selectSingleNode(xmlScoreBoard.getDocument());
	}
	protected Element createXPathElement() throws JDOMException {
		return editor.cloneDocumentToElement(getXPathElement(), false);
	}

	protected abstract String getXPathString();

	protected XmlDocumentEditor editor = new XmlDocumentEditor();

	protected XmlScoreBoard xmlScoreBoard;
	protected XPath xPath = editor.createXPath(getXPathString());
}

