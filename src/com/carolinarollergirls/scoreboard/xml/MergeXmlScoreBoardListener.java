package com.carolinarollergirls.scoreboard.xml;

import org.jdom.*;

public class MergeXmlScoreBoardListener implements XmlScoreBoardListener
{
  public MergeXmlScoreBoardListener() { super(); }
  public MergeXmlScoreBoardListener(XmlScoreBoard sb) {
    super();
    sb.addXmlScoreBoardListener(this);
  }

  public void xmlChange(Document d) {
    synchronized (documentLock) {
      editor.mergeDocuments(document, d);
      empty = false;
    }
  }

  public Document getDocument() {
    synchronized (documentLock) {
      return (Document)document.clone();
    }
  }

  public Document resetDocument() {
    synchronized (documentLock) {
      Document oldDoc = document;
      document = editor.createDocument();
      empty = true;
      return oldDoc;
    }
  }

  public boolean isEmpty() { return empty; }

  protected XmlDocumentEditor editor = new XmlDocumentEditor();

  protected Document document = editor.createDocument();
  protected Object documentLock = new Object();
  protected boolean empty = true;
}
