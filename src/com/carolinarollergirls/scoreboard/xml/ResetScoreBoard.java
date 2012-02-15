package com.carolinarollergirls.scoreboard.xml;

import java.util.*;

import org.jdom.*;
import org.jdom.xpath.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.xml.*;

public class ResetScoreBoard extends DefaultXmlDocumentManager implements XmlDocumentManager
{
  public ResetScoreBoard() { super("Reset"); }

  public void reset() {
    update(createXPathElement());
  }

  protected void processElement(Element e) throws Exception {
    if (Boolean.parseBoolean(editor.getText(e))) {
      getXmlScoreBoard().reset();
      getXmlScoreBoard().loadDefaultDocuments();
      update(editor.setText(editor.setRemovePI(createXPathElement()), "true"));
      update(createXPathElement());
    }
  }
}

