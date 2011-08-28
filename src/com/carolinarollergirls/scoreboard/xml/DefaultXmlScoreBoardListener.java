package com.carolinarollergirls.scoreboard.xml;

import org.jdom.*;

public class DefaultXmlScoreBoardListener implements XmlScoreBoardListener
{
  public DefaultXmlScoreBoardListener() { }
  public DefaultXmlScoreBoardListener(XmlScoreBoard sb) {
    sb.addXmlScoreBoardListener(this);
  }
  public DefaultXmlScoreBoardListener(boolean p) {
    setPersistent(p);
  }
  public DefaultXmlScoreBoardListener(XmlScoreBoard sb, boolean p) {
    setPersistent(p);
    sb.addXmlScoreBoardListener(this);
  }

  public void xmlChange(Document d) {
    synchronized (documentLock) {
      editor.mergeDocuments(document, d, isPersistent());
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

  public boolean isPersistent() { return persistent; }
  public void setPersistent(boolean p) { persistent = p; }

  protected XmlDocumentEditor editor = new XmlDocumentEditor();

  protected Document document = editor.createDocument();
  protected Object documentLock = new Object();
  protected boolean empty = true;
  protected boolean persistent = false;

}
