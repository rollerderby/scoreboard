package com.carolinarollergirls.scoreboard.xml;

import java.util.*;
import java.util.concurrent.*;

import org.jdom.*;

import com.carolinarollergirls.scoreboard.*;

public class ExecutorXmlDocumentManager implements XmlDocumentManager
{
  public void addXmlDocumentManager(XmlDocumentManager m) {
    synchronized (managerLock) {
      if (!managers.containsKey(m))
        managers.put(m, Executors.newSingleThreadExecutor());
    }
  }

  public void removeXmlDocumentManager(XmlDocumentManager m) {
    synchronized (managerLock) {
      managers.remove(m);
    }
  }

  public void processDocument(Document d) {
    synchronized (managerLock) {
      Iterator<XmlDocumentManager> m = managers.keySet().iterator();
      while (m.hasNext())
        submitProcessDocument(m.next(), d);
    }
  }

  public void reset() {
    synchronized (managerLock) {
      Iterator<XmlDocumentManager> m = managers.keySet().iterator();
      while (m.hasNext())
        submitReset(m.next());
    }
  }

  protected void submitProcessDocument(XmlDocumentManager m, Document d) {
    ExecutorService eS = managers.get(m);
    if (eS != null && d != null)
      eS.submit(new ProcessDocumentRunnable(m, (Document)d.clone()));
  }

  protected void submitReset(XmlDocumentManager m) {
    ExecutorService eS = managers.get(m);
    if (eS != null)
      eS.submit(new ResetRunnable(m));
  }

  protected HashMap<XmlDocumentManager,ExecutorService> managers = new LinkedHashMap<XmlDocumentManager,ExecutorService>();
  protected Object managerLock = new Object();

  public class ProcessDocumentRunnable implements Runnable
  {
    public ProcessDocumentRunnable(XmlDocumentManager m, Document d) {
      manager = m;
      document = d;
    }
    public void run() { manager.processDocument(document); }
    public XmlDocumentManager manager;
    public Document document;
  }

  public class ResetRunnable implements Runnable
  {
    public ResetRunnable(XmlDocumentManager m) {
      manager = m;
    }
    public void run() { manager.reset(); }
    public XmlDocumentManager manager;
  }
}

