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
import org.jdom.input.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;

public class XmlScoreBoard
{
  public XmlScoreBoard(ScoreBoardModel sbM) {
    scoreBoardModel = sbM;
    scoreBoardXmlListener = new ScoreBoardXmlListener(sbM) {
        public void scoreBoardChange(ScoreBoardEvent sbE) {
          super.scoreBoardChange(sbE);
          XmlScoreBoard.this.xmlChange(resetDocument());
        }
      };
    xmlChange(converter.toDocument(scoreBoardModel));
    loadXmlDocumentManagers();
  }

  //FIXME - this isn't a good way to do this,
  // but we need to load the autosaved docs after the viewers/controllers
  // are loaded...
  public void load() {
    loadDocuments();
    startAutoSave();
  }

  public ScoreBoardModel getScoreBoardModel() { return scoreBoardModel; }

  public void addXmlScoreBoardListener(XmlScoreBoardListener xsbL) {
    synchronized (documentLock) {
      listeners.addXmlScoreBoardListener(xsbL, document);
    }
  }
  public void removeXmlScoreBoardListener(XmlScoreBoardListener xsbL) {
    listeners.removeXmlScoreBoardListener(xsbL);
  }

  public Document getDocument() {
    synchronized (documentLock) {
      return (Document)document.clone();
    }
  }

  public void addXmlDocumentManager(XmlDocumentManager xdM) { managers.addXmlDocumentManager(xdM); }
  public void removeXmlDocumentManager(XmlDocumentManager xdM) { managers.removeXmlDocumentManager(xdM); }

  public void reset() {
    synchronized (managerLock) {
      if (null == exclusiveDocumentManager) {
        scoreBoardModel.reset();
        managers.reset();
      } else {
//FIXME - would be better to pass "exclusivity" selection on to the real executors instead of this
        final XmlDocumentManager xdM = exclusiveDocumentManager;
        exclusiveExecutor.submit(new Runnable() { public void run() { xdM.reset(); } });
      }
    }
  }

  /* FIXME:
   * This is unfortunately needed until
   * feature req 3483303 allows pages to
   * not need a reload when key elements
   * are removed/replaced
   */
  public void reloadViewers() {
    reloadScoreBoardViewers.reloadViewers();
  }

  public void loadDocument(Document d) {
    reset();
    mergeDocument(d);
    reloadViewers();
  }

  /**
   * This updates the main ScoreBoard and
   * all XmlDocumentManagers with the Document.
   * This should be used for changes from an
   * external source, i.e. not the main ScoreBoard
   * nor any XmlDocumentManager.
   */
  public void mergeDocument(Document doc) {
    synchronized (managerLock) {
      if (null == exclusiveDocumentManager) {
//FIXME - change ScoreBoardXmlConverter into XmlDocumentManager?
        converter.processDocument(scoreBoardModel, doc);
        managers.processDocument(doc);
      } else {
//FIXME - would be better to pass "exclusivity" selection on to the real executors instead of this
        final XmlDocumentManager xdM = exclusiveDocumentManager;
        final Document d = doc;
        exclusiveExecutor.submit(new Runnable() { public void run() { xdM.processDocument(d); } });
      }
    }
  }

  /**
   * This is a convenience method for mergeDocument(Document).
   */
  public void mergeDocument(Element e) { mergeDocument(e.getDocument()); }

  /**
   * This is similar to mergeDocument(Element), but it
   * only merges the provided element (and below),
   * instead of the entire document.
   */
  public void mergeElement(Element e) { mergeDocument(editor.cloneDocumentToClonedElement(e)); }

  /**
   * This updates all XmlScoreBoardListeners with
   * the Document.  This should be called with
   * changes from the main ScoreBoard or changes
   * from a XmlDocumentManager.
   */
  public void xmlChange(Document d) {
    synchronized (managerLock) {
      if (null != exclusiveDocumentManager) {
        /* Ignore updates not from the "exclusive" document manager */
        if (d.getProperty("DocumentManager") != exclusiveDocumentManager)
          return;
      }
    }
    synchronized (documentLock) {
      editor.mergeDocuments(document, d);
      editor.filterRemovePI(document);
      editor.filterOncePI(document);
      editor.removeExceptPI(document, "NoSave");
    }
    listeners.xmlChange(d);
  }

  /**
   * Start this XmlScoreBoard in "exclusive" mode.
   * This first resets, then causes any incoming documents
   * to be sent only to the "exclusive" manager, and any
   * updates from managers to be ignored (except updates
   * from the "exclusive" manager).
   * For now this is used only to "replay" a saved "stream"
   * of XML events.
   * This returns true if the document manager was given
   * exclusive access, false otherwise.
   */
  public boolean startExclusive(XmlDocumentManager manager) {
    synchronized (managerLock) {
      if (null != exclusiveDocumentManager || null == manager)
        return false;
      reset();
      reloadViewers();
      exclusiveDocumentManager = manager;
      return true;
    }
  }

  /**
   * End the "exclusive" mode.
   * If the provided document manager currently does have "exclusive" mode,
   * then it is ended, the current document is reloaded,
   * and normal operation resumes.
   * Otherwise, nothing changes.
   */
  public void endExclusive(XmlDocumentManager manager) {
    synchronized (managerLock) {
      if (null != manager && exclusiveDocumentManager == manager) {
        exclusiveDocumentManager = null;
        loadDocument(getDocument());
      }
    }
  }

  protected void loadDocuments() {
    if (!loadAutoSaveDocument())
      loadDefaultDocuments();
  }

  protected boolean loadAutoSaveDocument() {
    for (int i=0; i <= AutoSaveScoreBoard.AUTOSAVE_FILES; i++) {
      File f = AutoSaveScoreBoard.getFile(i);
      if (!f.exists())
        continue;
      try {
        loadDocument(saxBuilder.build(f));
        ScoreBoardManager.printMessage("Loaded auto-saved scoreboard XML from "+f.getPath());
        return true;
      } catch ( Exception e ) {
        ScoreBoardManager.printMessage("Could not load auto-saved scoreboard XML file "+f.getPath()+" : "+e.getMessage());
      }
    }

    return false;
  }

  protected void loadDefaultDocuments() {
    File initialDocumentDir = new File(ScoreBoardManager.getProperties().getProperty(DOCUMENT_DIR_KEY, DEFAULT_DIRECTORY_NAME));
    if (!initialDocumentDir.isDirectory()) {
      ScoreBoardManager.printMessage("Initial XML document directory '"+initialDocumentDir.getPath()+"' does not exist.");
      return;
    }

    FilenameFilter xmlFilter = new FilenameFilter() {
        public boolean accept(File f, String n) {
          return (n.endsWith(".xml") || n.endsWith(".XML"));
        }
      };
    Comparator<File> fileCompare = new Comparator<File>() {
        public int compare(File a, File b) { return a.getName().compareTo(b.getName()); }
      };
    List<File> unsortedXmlFiles = Arrays.asList(initialDocumentDir.listFiles(xmlFilter));
    Collections.sort(unsortedXmlFiles, fileCompare);
    Iterator<File> xmlFiles = unsortedXmlFiles.iterator();
    while (xmlFiles.hasNext()) {
      File f = xmlFiles.next();
      try {
        mergeDocument(saxBuilder.build(f));
        ScoreBoardManager.printMessage("Loaded settings from "+f.getName());
      } catch ( Exception e ) {
        ScoreBoardManager.printMessage("Could not load initial XML document "+f.getName()+" : "+e.getMessage());
      }
    }
  }

  protected void loadXmlDocumentManagers() {
//FIXME - this isn't the right way to do this!  use properties file, or xml maybe?
    // disable these until they are useful/complete.
    //new XmlRealtimeStats().setXmlScoreBoard(this);
    //new XmlInterpretedStats().setXmlScoreBoard(this);
    //new XmlGoogleDocsStats().setXmlScoreBoard(this);
    new LoadScoreBoard().setXmlScoreBoard(this);
    new SaveScoreBoardStream().setXmlScoreBoard(this);
    new LoadScoreBoardStream().setXmlScoreBoard(this);
    new TeamsXmlDocumentManager().setXmlScoreBoard(this);
    new ResetScoreBoard().setXmlScoreBoard(this);
    reloadScoreBoardViewers = new ReloadScoreBoardViewers();
    reloadScoreBoardViewers.setXmlScoreBoard(this);
    new OpenXmlDocumentManager("Pages").setXmlScoreBoard(this);
    new MediaXmlDocumentManager("Images", "Image").setXmlScoreBoard(this);
    new MediaXmlDocumentManager("Videos", "Video").setXmlScoreBoard(this);
    new MediaXmlDocumentManager("CustomHtml", "Html").setXmlScoreBoard(this);
    new TwitterXmlDocumentManager().setXmlScoreBoard(this);
  }

  protected void startAutoSave() {
    autoSave = new AutoSaveScoreBoard(this);
    autoSave.start();
  }

  protected ScoreBoardModel scoreBoardModel;
  protected ScoreBoardXmlConverter converter = new ScoreBoardXmlConverter();
  protected XmlDocumentEditor editor = new XmlDocumentEditor();
  protected ScoreBoardXmlListener scoreBoardXmlListener;
  protected AutoSaveScoreBoard autoSave;
  protected Document document = editor.createDocument();

  protected Object documentLock = new Object();
  protected Object managerLock = new Object();

  protected SAXBuilder saxBuilder = new SAXBuilder();

  protected ExecutorXmlScoreBoardListener listeners = new ExecutorXmlScoreBoardListener();
  protected ExecutorXmlDocumentManager managers = new ExecutorXmlDocumentManager();

  protected ReloadScoreBoardViewers reloadScoreBoardViewers = null;
  protected XmlDocumentManager exclusiveDocumentManager = null;
  protected ExecutorService exclusiveExecutor = Executors.newSingleThreadExecutor();

  public static final String DOCUMENT_DIR_KEY = XmlScoreBoard.class.getName() + ".InitialDocumentDirectory";
  public static final String DEFAULT_DIRECTORY_NAME = "config/default";
}
