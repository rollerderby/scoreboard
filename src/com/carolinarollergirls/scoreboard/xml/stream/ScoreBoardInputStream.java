package com.carolinarollergirls.scoreboard.xml.stream;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.io.*;
import java.util.*;
import java.nio.charset.*;

import javax.xml.parsers.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.xml.*;

import org.jdom.*;
import org.jdom.input.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class ScoreBoardInputStream
{
  public ScoreBoardInputStream(File f, StreamListener l) throws FileNotFoundException {
    filterRunnable = new StreamFilterRunnable(new FileInputStream(f), l);
  }

  public void start() throws IllegalStateException {
    synchronized (lock) {
      if (finished)
        throw new IllegalStateException("Finished; cannot restart");
      if (running)
        throw new IllegalStateException("Already started");
      running = true;
      filterRunnable.start();
    }
  }

  public void stop() {
    synchronized (lock) {
      if (!running)
        return;
      finished = true;
      running = false;
      filterRunnable.stop();
    }
  }

  protected Object lock = new Object();
  protected boolean finished = false;
  protected boolean running = false;
  protected StreamFilterRunnable filterRunnable;

  public static final String OUTPUT_ENCODING = "UTF-8";

  protected static class StreamFilterRunnable extends XMLFilterImpl implements XMLFilter,Runnable
  {
    public StreamFilterRunnable(FileInputStream fos, StreamListener l) {
      fileInputStream = fos;
      listener = l;
    }
    public void run() {
      try {
        setParent(SAXParserFactory.newInstance().newSAXParser().getXMLReader());
        parse(new InputSource(fileInputStream));
      } catch ( Exception e ) {
        if (!stopped)
          exception = e;
      } finally {
        try { fileInputStream.close(); } catch ( IOException ioE ) { }
        listener.end();
      }
    }
    public Exception getException() { return exception; }
    public void start() {
      myThread = new Thread(this);
      myThread.start();
    }
    public void stop() {
      stopped = true;
      try { myThread.join(); }
      catch ( InterruptedException iE ) { /* Can only hope myThread is stopped */ }
    }
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
      if (stopped)
        throw new SAXException("Parsing stopped");
      if (qName.equals("document")) {
        if (0 == docLevel++)
          startHandling();
      }
      super.startElement(uri, localName, qName, atts);
    }
    public void endElement(String uri, String localName, String qName) throws SAXException {
      super.endElement(uri, localName, qName);
      if (qName.equals("document")) {
        if (0 == --docLevel)
          stopHandling();
      }
    }
    protected void startHandling() throws SAXException {
      if (stopped)
        return;
      handler = new SAXHandler();
      setContentHandler(handler);
      setDTDHandler(handler);
      setEntityResolver(handler);
      setErrorHandler(handler);
      try { setProperty("http://xml.org/sax/properties/lexical-handler", handler); }
      catch ( Exception e ) {
        try { setProperty("http://xml.org/sax/handlers/LexicalHandler", handler); }
        catch ( Exception ee ) { }
      }
      getContentHandler().startDocument();
    }
    protected void stopHandling() throws SAXException {
      getContentHandler().endDocument();
      setContentHandler(defaultHandler);
      setDTDHandler(defaultHandler);
      setEntityResolver(defaultHandler);
      setErrorHandler(defaultHandler);
      try { setProperty("http://xml.org/sax/properties/lexical-handler", defaultHandler); }
      catch ( Exception e ) {
        try { setProperty("http://xml.org/sax/handlers/LexicalHandler", defaultHandler); }
        catch ( Exception ee ) { }
      }
      if (!stopped)
        listener.xmlChange(handler.getDocument());
      handler = null;
    }
    protected Thread myThread;
    protected FileInputStream fileInputStream;
    protected StreamListener listener;
    protected DefaultHandler defaultHandler = new DefaultHandler();
    protected SAXHandler handler = null;
    protected int docLevel = 0;
    protected boolean stopped = false;
    protected Exception exception = null;
  }
}
