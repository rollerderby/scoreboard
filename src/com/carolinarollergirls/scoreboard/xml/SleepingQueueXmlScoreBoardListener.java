package com.carolinarollergirls.scoreboard.xml;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;
import java.util.concurrent.*;

import org.jdom.*;

public class SleepingQueueXmlScoreBoardListener extends FilterXmlScoreBoardListener implements XmlScoreBoardListener
{
	public SleepingQueueXmlScoreBoardListener() { super(); }
	public SleepingQueueXmlScoreBoardListener(XmlScoreBoard sb) {
		sb.addXmlScoreBoardListener(this);
	}

	public void xmlChange(Document d) {
		super.xmlChange(d);
		if (editor.isEmptyDocument(d))
			return;
		synchronized (documentsLock) {
			if (queueNextDocument || documents.isEmpty()) {
				documents.addLast(d);
				// Wake any sleeping threads
				ListIterator itr = sleepingThreads.listIterator();
				while(itr.hasNext()) {
					((Thread)itr.next()).interrupt();
				}
			} else
				editor.mergeDocuments(documents.getLast(), d);

			queueNextDocument = editor.hasRemovePI(d);
		}
	}

	public Document getNextDocument() { return getNextDocument(0); }
	public Document getNextDocument(int timeout) {
		synchronized (documentsLock) {
			Document ret = documents.poll();
			if (null == ret) {
				// No results.  Add to sleeping thread list and sleep for timeout / 1000 seconds
				sleepingThreads.addLast(Thread.currentThread());
			} else
				return ret;
		}
		try {
			Thread.sleep(timeout);
		} catch (InterruptedException e) {}
		try {
			Thread.sleep(20);
		} catch (InterruptedException e) {}

		synchronized (documentsLock) {
			// Remove from sleeping list and return the next document (if any)
			sleepingThreads.remove(Thread.currentThread());
			return documents.poll();
		}
	}

	public boolean isEmpty() { return (null == documents.peek()); }

	protected boolean queueNextDocument = false;
	protected LinkedList<Thread> sleepingThreads = new LinkedList<Thread>();
	protected LinkedList<Document> documents = new LinkedList<Document>();
	protected Object documentsLock = new Object();
}
