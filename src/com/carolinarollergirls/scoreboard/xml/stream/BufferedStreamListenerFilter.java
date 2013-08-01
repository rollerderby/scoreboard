package com.carolinarollergirls.scoreboard.xml.stream;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;

import org.jdom.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.xml.*;

public class BufferedStreamListenerFilter extends StreamListenerFilter implements StreamListener
{
  public BufferedStreamListenerFilter(StreamListener l) { super(l); }

  public void xmlChange(Document d) {
    addDocument(d);
  }

  public void end() {
    synchronized (processorLock) {
      if (null != processor)
        processor.end();
    }
  }

  public void start() {
    synchronized (processorLock) {
      if (null == processor) {
        processor = new DocumentsProcessor(getListener(), documents, 0);
        processor.start();
      }
    }
  }
  public void stop() {
    synchronized (processorLock) {
      if (null != processor) {
        processor.stop();
        processor = null;
      }
    }
  }

  //FIXME - move this tracking into processor
  public long getStartTime() { return startTime; }
  public long getEndTime() { return endTime; }

  protected void addDocument(Document d) {
    long time = editor.getSystemTime(d);
    if (startTime == -1)
      startTime = time;
    endTime = time;
    synchronized (documents) {
      documents.add(d);
      documents.notifyAll();
    }
  }

  protected long startTime = -1;
  protected long endTime = -1;
  protected XmlDocumentEditor editor = new XmlDocumentEditor();

  protected Object processorLock = new Object();
  protected DocumentsProcessor processor = null;
  protected List<Document> documents = new ArrayList<Document>();

  protected class DocumentsProcessor implements Runnable
  {
    public DocumentsProcessor(StreamListener l, List<Document> d, int start) {
      listener = l;
      documents = d;
      position = start;
      myThread = new Thread(this);
    }

    public void run() {
      while (running) {
        Document d;
        synchronized (documents) {
          try {
            d = documents.get(position);
            position++;
          } catch ( IndexOutOfBoundsException aioobE ) {
            if (ended) {
              running = false;
            } else {
              try { documents.wait(); }
              catch ( InterruptedException iE ) { /* Keep trying */ }
            }
            continue;
          }
        }
        listener.xmlChange(d);
      }
      listener.end();
    }

    public void end() {
      synchronized (documents) {
        ended = true;
        documents.notifyAll();
      }
    }

    public int getPosition() { return position; }

    public void start() {
      synchronized (runLock) {
        running = true;
        myThread.start();
      }
    }
    public void stop() {
      synchronized (runLock) {
        running = false;
        if (!myThread.isAlive())
          return;
        myThread.interrupt();
      }
      try {
        myThread.join();
      } catch ( InterruptedException iE ) {
        /* Not much we can do */
      }
    }

    protected Thread myThread;
    protected boolean running;
    protected Object runLock = new Object();
    protected boolean ended = false;
    protected StreamListener listener;
    protected List<Document> documents;
    protected int position;
  }
}
