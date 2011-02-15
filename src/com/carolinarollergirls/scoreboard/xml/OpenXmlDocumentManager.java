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
	protected void processElement(Element e) { update(editor.cloneDocumentToClonedElement(e)); }
}

