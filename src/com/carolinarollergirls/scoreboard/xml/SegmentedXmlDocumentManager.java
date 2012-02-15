package com.carolinarollergirls.scoreboard.xml;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

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

