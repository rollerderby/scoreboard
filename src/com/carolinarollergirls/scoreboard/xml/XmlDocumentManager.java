package com.carolinarollergirls.scoreboard.xml;

import org.jdom.*;

import com.carolinarollergirls.scoreboard.*;

public interface XmlDocumentManager
{
	/**
	 * Process a Document.
	 *
	 * If this manager is responsible for any parts
	 * of the document, it should return the updated
	 * parts to the XmlScoreBoard.
	 */
	public void processDocument(Document d);

	public void reset();
}

