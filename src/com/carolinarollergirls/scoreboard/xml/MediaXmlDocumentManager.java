package com.carolinarollergirls.scoreboard.xml;

import java.util.*;
import java.io.*;

import org.jdom.*;
import org.jdom.xpath.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.jetty.*;
import com.carolinarollergirls.scoreboard.xml.*;

public class MediaXmlDocumentManager extends OpenXmlDocumentManager implements XmlDocumentManager
{
  public MediaXmlDocumentManager(String n, String m) {
    super(n);
    mediaName = m;
  }

  public void reset() {
    super.reset();
    loadTypes();
  }

  protected void loadTypes() {
    FilenameFilter managedFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return getManagedElementName().equalsIgnoreCase(name);
        }
      };
    FileFilter typeFilter = new FileFilter() {
        public boolean accept(File dir) {
          return (dir.isDirectory());
        }
      };
    File topDir = new File(htmlDirName);
    Iterator<File> managedDirs = Arrays.asList(topDir.listFiles(managedFilter)).iterator();
    while (managedDirs.hasNext()) {
      File managedDir = managedDirs.next();
      Iterator<File> typeDirs = Arrays.asList(managedDir.listFiles(typeFilter)).iterator();
      while (typeDirs.hasNext())
        update(editor.getElement(createXPathElement(), "Type", typeDirs.next().getName()));
    }
  }

  protected String getMediaName() { return mediaName; }

  private String mediaName;

  private String htmlDirName = ScoreBoardManager.getProperties().getProperty(JettyServletScoreBoardController.PROPERTY_HTML_DIR_KEY);
}
