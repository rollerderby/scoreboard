package com.carolinarollergirls.scoreboard.xml;

import java.util.*;

import org.jdom.*;
import org.jdom.xpath.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.xml.*;

public class ResetScoreBoard extends DefaultXmlDocumentManager implements XmlDocumentManager
{
  public ResetScoreBoard() { super("Reset"); }

  protected void processElement(Element e) throws Exception {
    if (Boolean.parseBoolean(e.getText()))
      getXmlScoreBoard().loadDefaultDocuments();
  }
}

