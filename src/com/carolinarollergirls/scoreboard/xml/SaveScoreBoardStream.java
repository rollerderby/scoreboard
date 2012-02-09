package com.carolinarollergirls.scoreboard.xml;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.jdom.*;
import org.jdom.output.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.file.*;

public class SaveScoreBoardStream extends SegmentedXmlDocumentManager
{
  public SaveScoreBoardStream() {
    super("SaveLoad", "SaveStream");
  }

  public void setXmlScoreBoard(XmlScoreBoard xsb) {
    streamToFile = new ScoreBoardStreamToXmlFile(xsb);
    super.setXmlScoreBoard(xsb);
  }

  public void reset() {
    super.reset();
    if (streamToFile.isRunning())
      streamToFile.stop();
    Element e = createXPathElement();
    e.addContent(new Element("Filename"));
    e.addContent(editor.setText(new Element("Running"), "false"));
    e.addContent(editor.setText(new Element("Error"), "false"));
    e.addContent(new Element("Message"));
    update(e);
  }

  protected void processChildElement(Element e) throws JDOMException {
    if (e.getName() == "Filename")
      setFilename(e);
    else if (e.getName() == "Start" && editor.isTrue(e))
      start();
    else if (e.getName() == "Stop" && editor.isTrue(e))
      stop();
  }

  protected void setFilename(Element e) throws JDOMException {
    String filename = editor.getText(e);
    if (null == filename)
      return;
    Element msg = editor.setText(new Element("Message"), "");
    Element error = editor.setText(new Element("Error"), "false");
    Element updateE = createXPathElement().addContent(msg).addContent(error);
    if (editor.isTrue(getXPathElement().getChild("Running"))) {
      editor.setText(msg, "Cannot change Filename while Running");
      editor.setText(error, "true");
      return;
    }
    updateE.addContent(editor.setText(new Element("Filename"), filename));
    editor.setText(msg, "Filename changed to '"+filename+"'");
    update(updateE);
  }

  protected void start() throws JDOMException {
    if (editor.isTrue(getXPathElement().getChild("Running")))
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
      streamToFile.setDirectory(directory);
      streamToFile.setFile(filename);
      streamToFile.start();
      editor.setText(msg, "Started streaming to file");
      updateE.addContent(editor.setText(new Element("Running"), "true"));
    } catch ( Exception e ) {
      editor.setText(msg, "Could not start streaming to file '"+filename+"' : "+e.getMessage());
      editor.setText(error, "true");
    } finally {
      update(updateE);
    }
  }

  protected void stop() {
    try {
      if (!editor.isTrue(getXPathElement().getChild("Running")))
        return;
    } catch ( JDOMException jE ) {
      // If XML isn't setup for some reason, check streamToFile directly
      if (!streamToFile.isRunning())
        return;
    }
    streamToFile.stop();
    Element updateE = createXPathElement();
    updateE.addContent(editor.setText(new Element("Running"), "false"));
    updateE.addContent(editor.setText(new Element("Message"), ""));
    updateE.addContent(editor.setText(new Element("Error"), "false"));
    update(updateE);
  }

  protected ScoreBoardStreamToXmlFile streamToFile;

  public static final String DIRECTORY_KEY = SaveScoreBoardStream.class.getName() + ".dir";
}
