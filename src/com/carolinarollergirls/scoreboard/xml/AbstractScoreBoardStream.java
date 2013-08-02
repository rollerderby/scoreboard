package com.carolinarollergirls.scoreboard.xml;
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

  public void setXmlScoreBoard(XmlScoreBoard xsB) {
    super.setXmlScoreBoard(xsB);

    Element e = createXPathElement();
    e.addContent(new Element("Filename"));
    e.addContent(editor.setText(new Element("Running"), "false"));
    e.addContent(editor.setText(new Element("Error"), "false"));
    e.addContent(new Element("Message"));
    update(e);
  }

  public void reset() {
    /* Don't reset anything, as this controls loading. */
  }

  protected void processChildElement(Element e) throws JDOMException {
    synchronized (processLock) {
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
    } else {
      try {
        if (!"".equals(filename))
          getFile(filename);
      } catch ( IllegalArgumentException iaE ) {
        editor.setText(msg, iaE.getMessage());
        editor.setText(error, "true");
      } catch ( FileNotFoundException fnfE ) {
        editor.setText(msg, fnfE.getMessage());
        editor.setText(error, "true");
      }

      updateE.addContent(editor.setText(new Element("Filename"), filename));
    }
    update(updateE);
  }

  protected void start() throws JDOMException {
    if (running)
      return;
    Element msg = editor.setText(new Element("Message"), "");
    Element error = editor.setText(new Element("Error"), "false");
    Element updateE = createXPathElement().addContent(msg).addContent(error);
    try {
      running = true;
      doStart(getFile(editor.getText(getXPathElement().getChild("Filename"))));
      updateE.addContent(editor.setText(new Element("Running"), "true"));
    } catch ( FileNotFoundException fnfE ) {
      running = false;
      editor.setText(msg, fnfE.getMessage());
      editor.setText(error, "true");
    } catch ( IOException ioE ) {
      running = false;
      editor.setText(msg, ioE.getMessage());
      editor.setText(error, "true");
    } catch ( IllegalArgumentException iaE ) {
      running = false;
      editor.setText(msg, iaE.getMessage());
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

  protected File getFile(String filename) throws IllegalArgumentException,FileNotFoundException {
    String dirname = ScoreBoardManager.getProperty(DIRECTORY_KEY);
    if (null == dirname || "".equals(dirname))
      throw new IllegalArgumentException("No directory set for stream files");
    File directory = new File(dirname);
    if (!directory.exists() && !directory.mkdirs())
      throw new FileNotFoundException("Could not create directory '"+dirname+"' for stream files");
    if ("".equals(filename))
      throw new IllegalArgumentException("No filename provided");
    File file = new File(directory, filename);
    if (!directory.equals(file.getParentFile()))
      throw new IllegalArgumentException("Please specify only the filename, without any leading path");
    return file;
  }

  public void xmlChange(Document d) {
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

  protected Element createXPathElement() {
    return editor.setNoSavePI(super.createXPathElement());
  }

  protected abstract void doStart(File f) throws IOException,FileNotFoundException;
  protected abstract void doStop();
  protected abstract void doXmlChange(Document d) throws IOException;

  protected Object processLock = new Object();
  protected boolean running = false;

  public static final String DIRECTORY_KEY = AbstractScoreBoardStream.class.getName() + ".dir";
}
