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
		documents.offer(d);
	}

	public Document getDocument() {
		return documents.poll();
	}

	public boolean isEmpty() { return (null != documents.peek()); }

	protected Queue<Document> documents = new ConcurrentLinkedQueue<Document>();
}
