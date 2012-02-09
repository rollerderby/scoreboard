package com.carolinarollergirls.scoreboard.xml;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.jdom.*;
import org.jdom.output.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.xml.stream.*;

public class SaveScoreBoardStream extends SegmentedXmlDocumentManager implements XmlScoreBoardListener
{
  public SaveScoreBoardStream() {
    super("SaveLoad", "SaveStream");
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
    try {
      String directory = ScoreBoardManager.getProperties().getProperty(DIRECTORY_KEY);
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
      outputStream = new ScoreBoardOutputStream(new File(directory, filename));
      outputStream.start();
      outputStream.write(getXmlScoreBoard().getDocument());
      running = true;
      getXmlScoreBoard().addXmlScoreBoardListener(this);
      updateE.addContent(editor.setText(new Element("Running"), "true"));
    } catch ( IOException ioE ) {
      outputStream.stop();
      outputStream = null;
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
    getXmlScoreBoard().removeXmlScoreBoardListener(this);
    outputStream.stop();
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
          outputStream.write(d);
      } catch ( IOException ioE ) {
        outputStream.stop();
        Element updateE = createXPathElement();
        updateE.addContent(editor.setText(new Element("Message"), "Error while streaming to file : "+ioE.getMessage()));
        updateE.addContent(editor.setText(new Element("Error"), "true"));
        update(updateE);
      }
    }
  }

  protected Object lock = new Object();
  protected boolean running = false;
  protected ScoreBoardOutputStream outputStream = null;

  public static final String DIRECTORY_KEY = SaveScoreBoardStream.class.getName() + ".dir";
}
