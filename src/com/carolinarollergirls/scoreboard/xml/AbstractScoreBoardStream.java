package com.carolinarollergirls.scoreboard.xml;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.jdom.*;
import org.jdom.output.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.xml.stream.*;

public abstract class AbstractScoreBoardStream extends SegmentedXmlDocumentManager implements XmlScoreBoardListener
{
  public AbstractScoreBoardStream(String name) {
    super("SaveLoad", name);
  }

  public void reset() {
    synchronized (lock) {
      stop();
    }
    super.reset();
    Element e = createXPathElement();
    e.addContent(new Element("Filename"));
    e.addContent(editor.setText(new Element("Running"), "false"));
    e.addContent(editor.setText(new Element("Error"), "false"));
    e.addContent(new Element("Message"));
    update(e);
  }

  protected void processChildElement(Element e) throws JDOMException {
    synchronized (lock) {
      if (e.getName() == "Filename")
        setFilename(e);
      else if (e.getName() == "Start" && editor.isTrue(e))
        start();
      else if (e.getName() == "Stop" && editor.isTrue(e))
        stop();
    }
  }

  protected void setFilename(Element e) throws JDOMException {
    String filename = editor.getText(e);
    if (null == filename)
      return;
    Element msg = editor.setText(new Element("Message"), "");
    Element error = editor.setText(new Element("Error"), "false");
    Element updateE = createXPathElement().addContent(msg).addContent(error);
    if (running) {
      editor.setText(msg, "Cannot change Filename while Running");
      editor.setText(error, "true");
      return;
    }
    updateE.addContent(editor.setText(new Element("Filename"), filename));
    update(updateE);
  }

  protected void start() throws JDOMException {
    if (running)
      return;
    Element msg = editor.setText(new Element("Message"), "");
    Element error = editor.setText(new Element("Error"), "false");
    Element updateE = createXPathElement().addContent(msg).addContent(error);
    String filename = "";
    String directory = ScoreBoardManager.getProperties().getProperty(DIRECTORY_KEY);
    try {
      if (null == directory || "".equals(directory)) {
        editor.setText(msg, "No directory set for stream files");
        editor.setText(error, "true");
        return;
      }
      filename = editor.getText(getXPathElement().getChild("Filename"));
      if ("".equals(filename)) {
        editor.setText(msg, "Filename must be set before starting");
        editor.setText(error, "true");
        return;
      }
      running = true;
      doStart(new File(directory, filename));
      updateE.addContent(editor.setText(new Element("Running"), "true"));
    } catch ( IOException ioE ) {
//FIXME - need generic text
      editor.setText(msg, "Could not start streaming to file '"+filename+"' : "+ioE.getMessage());
      editor.setText(error, "true");
    } finally {
      update(updateE);
    }
  }

  protected void stop() {
    if (!running)
      return;
    running = false;
    doStop();
    Element updateE = createXPathElement();
    updateE.addContent(editor.setText(new Element("Running"), "false"));
    updateE.addContent(editor.setText(new Element("Message"), ""));
    updateE.addContent(editor.setText(new Element("Error"), "false"));
    updateE.addContent(editor.setText(new Element("Filename"), ""));
    update(updateE);
  }

  public void xmlChange(Document d) {
    synchronized (lock) {
      try {
        if (running)
          doXmlChange(d);
      } catch ( IOException ioE ) {
        Element updateE = createXPathElement();
        updateE.addContent(editor.setText(new Element("Message"), ioE.getMessage()));
        updateE.addContent(editor.setText(new Element("Error"), "true"));
        update(updateE);
      }
    }
  }

  protected abstract void doStart(File f) throws IOException;
  protected abstract void doStop();
  protected abstract void doXmlChange(Document d) throws IOException;

  protected Object lock = new Object();
  protected boolean running = false;

  public static final String DIRECTORY_KEY = AbstractScoreBoardStream.class.getName() + ".dir";
}
