package com.carolinarollergirls.scoreboard.xml;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.jdom.*;
import org.jdom.input.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;

public class LoadScoreBoard extends SegmentedXmlDocumentManager
{
  public LoadScoreBoard() { super("SaveLoad", "Load"); }

  public void setXmlScoreBoard(XmlScoreBoard xsB) {
    super.setXmlScoreBoard(xsB);

    Element e = createXPathElement();
    editor.addElement(e, "LoadFile");
    editor.addElement(e, "MergeFile");
    update(e);
  }

  public void reset() {
    /* Don't reset anything, as this controls loading. */
  }

  protected void processChildElement(Element e) throws Exception {
    super.processChildElement(e);
    Document d = saxBuilder.build(new File(DIRECTORY_NAME, editor.getText(e)));
    if (e.getName().equals("LoadFile"))
      xmlScoreBoard.loadDocument(d);
    else if (e.getName().equals("MergeFile"))
      xmlScoreBoard.mergeDocument(d);
  }

  protected Element createXPathElement() {
    return editor.setNoSavePI(super.createXPathElement());
  }

  protected SAXBuilder saxBuilder = new SAXBuilder();

  public static final String DIRECTORY_NAME = "html/save";
}
