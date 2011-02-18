package com.carolinarollergirls.scoreboard.xml;

import java.util.*;

import org.jdom.*;
import org.jdom.xpath.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.xml.*;

/**
 * This abstract class simply passes any Element under its management back to the XmlScoreBoard,
 * So the subtree managed by this is open for anyone to edit.
 */
public abstract class OpenXmlDocumentManager extends AbstractXmlDocumentManager implements XmlDocumentManager
{
	/* Write back all child elements except a Reset, which will reset/clear this entire tree */
	protected void processChildElement(Element e) throws Exception {
		super.processChildElement(e);
		if (!e.getName().equals("Reset"))
			update(editor.cloneDocumentToClonedElement(e));
	}
}

