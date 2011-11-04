package com.carolinarollergirls.scoreboard.xml;

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

  /* If possible, get rid of this...not many classes really need this currently.
   * It's possible to subclass Document so any changes to any elements come back as events,
   * which would let us give out the actual document to anyone to modify as they like...
   */
  public Document getRealDocument() {
    return document;
  }

  public void addXmlDocumentManager(XmlDocumentManager xdM) { managers.addXmlDocumentManager(xdM); }
  public void removeXmlDocumentManager(XmlDocumentManager xdM) { managers.removeXmlDocumentManager(xdM); }

  public void reset() {
    scoreBoardModel.reset();
    managers.reset();
  }

  public void loadDocument(Document d) {
    reset();
    mergeDocument(d);
  }

  /**
   * This updates the main ScoreBoard and
   * all XmlDocumentManagers with the Document.
   * This should be used for changes from an
   * external source, i.e. not the main ScoreBoard
   * nor any XmlDocumentManager.
   */
  public void mergeDocument(Document d) {
//FIXME - change ScoreBoardXmlConverter into XmlDocumentManager?
    converter.processDocument(scoreBoardModel, d);
    managers.processDocument(d);
//_mergeDocument(d);
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
    synchronized (documentLock) {
      editor.mergeDocuments(document, d, true);
    }
    listeners.xmlChange(d);
  }

  protected void loadDocuments() {
    if (!loadAutoSaveDocument())
      loadDefaultDocuments();
  }

  protected boolean loadAutoSaveDocument() {
    File autoSaveDir = new File(AutoSaveScoreBoard.DIRECTORY_NAME);
    if (!autoSaveDir.exists())
      return false;

    for (int i=0; i <= AutoSaveScoreBoard.AUTOSAVE_FILES; i++) {
      File f = new File(autoSaveDir, AutoSaveScoreBoard.getName(i));
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
    new XmlRealtimeStats().setXmlScoreBoard(this);
    new XmlInterpretedStats().setXmlScoreBoard(this);
    //new XmlGoogleDocsStats().setXmlScoreBoard(this);
    new LoadScoreBoard().setXmlScoreBoard(this);
    new TeamsXmlDocumentManager().setXmlScoreBoard(this);
    new ResetScoreBoard().setXmlScoreBoard(this);
    new OpenXmlDocumentManager("Pages").setXmlScoreBoard(this);
    new MediaXmlDocumentManager("Images", "Image").setXmlScoreBoard(this);
    new MediaXmlDocumentManager("Videos", "Video").setXmlScoreBoard(this);
    new MediaXmlDocumentManager("CustomHtml", "Html").setXmlScoreBoard(this);
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

  protected SAXBuilder saxBuilder = new SAXBuilder();

  protected ExecutorXmlScoreBoardListener listeners = new ExecutorXmlScoreBoardListener();
  protected ExecutorXmlDocumentManager managers = new ExecutorXmlDocumentManager();

  public static final String DOCUMENT_DIR_KEY = XmlScoreBoard.class.getName() + ".InitialDocumentDirectory";
  public static final String DEFAULT_DIRECTORY_NAME = "config/default";
}
