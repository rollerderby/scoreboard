package com.carolinarollergirls.scoreboard.file;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.nio.charset.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.xml.*;

import org.jdom.*;
import org.jdom.output.*;

public class ScoreBoardStreamToXmlFile extends ScoreBoardStreamToFile implements XmlScoreBoardListener
{
  public ScoreBoardStreamToXmlFile(XmlScoreBoard xsb) {
    super();
    setXmlScoreBoard(xsb);
    init();
  }
  public ScoreBoardStreamToXmlFile(XmlScoreBoard xsb, String d) {
    super(d);
    setXmlScoreBoard(xsb);
    init();
  }
  public ScoreBoardStreamToXmlFile(XmlScoreBoard xsb, String d, String f) {
    super(d, f);
    setXmlScoreBoard(xsb);
    init();
  }

  protected void setXmlScoreBoard(XmlScoreBoard xsb) { xmlScoreBoard = xsb; }

  protected void init() {
    outputter.getFormat().setEncoding(OUTPUT_ENCODING);
    outputter.getFormat().setOmitDeclaration(true);
  }

  public void doStart() throws Exception {
    printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getFile()), Charset.forName(OUTPUT_ENCODING))));
    // I would rather use programmatic means to do this,
    // but I can't find anything that allows manually creating a toplevel element
    // and then streaming in subelements (and then eventually closing the element/doc)
    printWriter.print("<?xml version=\"1.0\" encoding=\""+OUTPUT_ENCODING+"\"?>");
    printWriter.print("<ScoreBoardStream version=\""+ScoreBoardManager.getVersion()+"\">");
    xmlScoreBoard.addXmlScoreBoardListener(this);
  }

  protected void doStop() {
    xmlScoreBoard.removeXmlScoreBoardListener(this);
    synchronized (outputter) {
      printWriter.print("</ScoreBoardStream>");
      printWriter.flush();
      printWriter.close();
      printWriter = null;
    }
  }

  public void xmlChange(Document d) {
    try {
      synchronized (outputter) {
        if (isRunning())
          outputter.output(d.getRootElement(), printWriter);
      }
    } catch ( IOException ioE ) {
      ScoreBoardManager.printMessage("Could not output ScoreBoard element to XML stream : " + ioE.getMessage());
      stop();
    }
  }

  protected XmlScoreBoard xmlScoreBoard = null;
  protected PrintWriter printWriter = null;
  protected XMLOutputter outputter = XmlDocumentEditor.getRawXmlOutputter();

  public static final String OUTPUT_ENCODING = "UTF-8";
}
