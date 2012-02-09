package com.carolinarollergirls.scoreboard.xml.stream;

import java.io.*;
import java.util.*;
import java.nio.charset.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.xml.*;

import org.jdom.*;
import org.jdom.output.*;

public class ScoreBoardOutputStream
{
  public ScoreBoardOutputStream(File f) {
    file = f;
    xmlOutputter.getFormat().setEncoding(OUTPUT_ENCODING);
    xmlOutputter.getFormat().setOmitDeclaration(true);
  }

  public synchronized void start() throws IllegalStateException,FileNotFoundException {
    if (null != printWriter)
      throw new IllegalStateException("Already started");
    if (finished)
      throw new IllegalStateException("Finished; cannot restart");

    printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Charset.forName(OUTPUT_ENCODING))));
    // I would rather use programmatic means to do this,
    // but I can't find anything that allows manually creating a toplevel element
    // and then streaming in subelements (and then eventually closing the element/doc)
    printWriter.print("<?xml version=\"1.0\" encoding=\""+OUTPUT_ENCODING+"\"?>");
    printWriter.print("<ScoreBoardStream version=\""+ScoreBoardManager.getVersion()+"\">");
  }

  public synchronized void stop() {
    if (null == printWriter)
      return;
    finished = true;

    printWriter.print("</ScoreBoardStream>");
    printWriter.flush();
    printWriter.close();
    printWriter = null;
  }

  public synchronized void write(Document d) throws IOException {
    xmlOutputter.output(d.getRootElement(), printWriter);
  }

  protected File file = null;
  protected PrintWriter printWriter = null;
  protected boolean finished = false;
  protected XMLOutputter xmlOutputter = XmlDocumentEditor.getRawXmlOutputter();

  public static final String OUTPUT_ENCODING = "UTF-8";

}
