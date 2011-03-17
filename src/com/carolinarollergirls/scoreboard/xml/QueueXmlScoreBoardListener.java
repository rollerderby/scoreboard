package com.carolinarollergirls.scoreboard.xml;

import java.util.*;
import java.util.concurrent.*;

import org.jdom.*;

public class QueueXmlScoreBoardListener implements XmlScoreBoardListener
{
	public QueueXmlScoreBoardListener() { }
	public QueueXmlScoreBoardListener(XmlScoreBoard sb) {
		sb.addXmlScoreBoardListener(this);
	}

	public void xmlChange(Document d) {
		synchronized (documentsLock) {
			if (queueNextDocument || documents.isEmpty())
				documents.addLast(d);
			else
				editor.mergeDocuments(documents.getLast(), d);

			queueNextDocument = (isQueueOnlyRemovals() && editor.hasElementRemoval(d));
		}
	}

	public Document getNextDocument() {
		synchronized (documentsLock) {
			return documents.poll();
		}
	}

	public boolean isEmpty() { return (null == documents.peek()); }

	public boolean isQueueOnlyRemovals() { return queueOnlyRemovals; }
	public void setQueueOnlyRemovals(boolean q) { queueOnlyRemovals = q; }

	protected XmlDocumentEditor editor = new XmlDocumentEditor();

	protected boolean queueOnlyRemovals = true;
	protected boolean queueNextDocument = false;
	protected LinkedList<Document> documents = new LinkedList<Document>();
	protected Object documentsLock = new Object();
}
