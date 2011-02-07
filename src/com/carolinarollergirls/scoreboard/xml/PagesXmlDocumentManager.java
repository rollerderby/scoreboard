package com.carolinarollergirls.scoreboard.xml;

import org.jdom.*;
import org.jdom.xpath.*;

import com.carolinarollergirls.scoreboard.*;

public class PagesXmlDocumentManager extends OpenXmlDocumentManager implements XmlDocumentManager
{
	protected String getTopLevelElementName() { return "Pages"; }
}

