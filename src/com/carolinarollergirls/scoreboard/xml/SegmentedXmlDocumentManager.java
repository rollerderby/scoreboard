package com.carolinarollergirls.scoreboard.xml;

import org.jdom.*;

/**
 * This abstract class simply divides a top-level element name into different
 * XmlDocumentManagers for each sub-element.
 */
public abstract class SegmentedXmlDocumentManager extends AbstractXmlDocumentManager implements XmlDocumentManager
{
	protected Element createXPathElement() {
		Element e = new Element(getManagedSubElementName());
		super.createXPathElement().addContent(e);
		return e;
	}
	protected abstract String getManagedSubElementName();
}

