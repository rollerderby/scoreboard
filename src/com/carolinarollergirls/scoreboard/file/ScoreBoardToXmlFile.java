package com.carolinarollergirls.scoreboard.file;

import java.io.*;
import java.util.*;

import org.jdom.*;
import org.jdom.output.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.xml.*;

public class ScoreBoardToXmlFile extends ScoreBoardToFile
{
  public ScoreBoardToXmlFile() { super(); }
  public ScoreBoardToXmlFile(String d) { super(d); }
  public ScoreBoardToXmlFile(String d, String f) { super(d, f); }

  public void save(ScoreBoard sB) throws Exception { save(sB.getXmlScoreBoard()); }
  public void save(XmlScoreBoard xsB) throws Exception {
    synchronized (xmlOutputter) {
      xmlOutputter.output(xsB.getDocument(), new FileOutputStream(getFile()));
    }
  }

  public static void save(ScoreBoard sB, File f) throws Exception { save(sB.getXmlScoreBoard(), f); }
  public static void save(XmlScoreBoard xsB, File f) throws Exception {
    XmlDocumentEditor.getPrettyXmlOutputter().output(xsB.getDocument(), new FileOutputStream(f));
  }

  private XMLOutputter xmlOutputter = XmlDocumentEditor.getPrettyXmlOutputter();
}
