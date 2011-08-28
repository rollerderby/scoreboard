package com.carolinarollergirls.scoreboard.xml;

import org.jdom.*;

/**
 * This abstract class simply divides a top-level element name into different
 * XmlDocumentManagers for each sub-element.
 */
public class SegmentedXmlDocumentManager extends DefaultXmlDocumentManager implements XmlDocumentManager
{
  public SegmentedXmlDocumentManager(String a, String b) {
    super(a);
    managedSubElementName = b;
  }

  protected Element createXPathElement() {
    Element e = new Element(getManagedSubElementName());
    super.createXPathElement().addContent(e);
    return e;
  }
  protected String getXPathString() { return super.getXPathString()+"/"+getManagedSubElementName(); }
  protected String getManagedSubElementName() { return managedSubElementName; }

  private String managedSubElementName;
}

