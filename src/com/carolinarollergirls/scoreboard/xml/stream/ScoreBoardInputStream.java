package com.carolinarollergirls.scoreboard.xml.stream;

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
  public ScoreBoardInputStream(File f, XmlScoreBoardListener l) {
    filterRunnable = new StreamFilterRunnable(f, l);
  }

  public void start() throws IllegalStateException,FileNotFoundException {
    synchronized (lock) {
      if (finished)
        throw new IllegalStateException("Finished; cannot restart");
      if (running)
        throw new IllegalStateException("Already started");
      running = true;
      new Thread(filterRunnable).start();
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
    public StreamFilterRunnable(File f, XmlScoreBoardListener l) {
      file = f;
      listener = l;
    }
    public void run() {
      FileInputStream fos = null;
      try {
        fos = new FileInputStream(file);
        setParent(SAXParserFactory.newInstance().newSAXParser().getXMLReader());
        parse(new InputSource(fos));
      } catch ( Exception e ) {
        exception = e;
      } finally {
        try {
          if (null != fos)
            fos.close();
        } catch ( IOException ioE ) { /* ignore err on close */ }
      }
    }
    public Exception getException() { return exception; }
    public void stop() { stopped = true; }
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
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
      getContentHandler().startDocument();
    }
    protected void stopHandling() throws SAXException {
      getContentHandler().endDocument();
      setContentHandler(defaultHandler);
      setDTDHandler(defaultHandler);
      setEntityResolver(defaultHandler);
      setErrorHandler(defaultHandler);
      if (!stopped)
        listener.xmlChange(handler.getDocument());
      handler = null;
    }
    protected File file;
    protected XmlScoreBoardListener listener;
    protected DefaultHandler defaultHandler = new DefaultHandler();
    protected SAXHandler handler = null;
    protected int docLevel = 0;
    protected boolean stopped = false;
    protected Exception exception = null;
  }
}
