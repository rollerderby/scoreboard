package com.carolinarollergirls.scoreboard.xml;

import java.util.*;
import java.io.*;

import org.jdom.*;
import org.jdom.xpath.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.xml.*;

public class MediaXmlDocumentManager extends OpenXmlDocumentManager implements XmlDocumentManager,FileFilter
{
  public MediaXmlDocumentManager(String n, String m) {
    super(n);
    mediaName = m;
  }

  public boolean accept(File f) {
    String name = f.getName();
    return (f.isFile() && !name.startsWith(".") && !name.endsWith(".db"));
  }

  public void reset() {
    super.reset();
    loadAllMedia();
  }

  protected void loadAllMedia() {
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
    File topDir = new File("html");
    Iterator<File> managedDirs = Arrays.asList(topDir.listFiles(managedFilter)).iterator();
    while (managedDirs.hasNext()) {
      File managedDir = managedDirs.next();
      Iterator<File> typeDirs = Arrays.asList(managedDir.listFiles(typeFilter)).iterator();
      while (typeDirs.hasNext()) {
        File typeDir = typeDirs.next();
        Iterator<File> files = Arrays.asList(typeDir.listFiles(this)).iterator();
        while (files.hasNext()) {
          addMedia(managedDir.getName(), typeDir.getName(), files.next().getName());
        }
      }
    }
  }

  protected String getMediaName() { return mediaName; }

  protected Element createMediaTypeElement(String type) {
    return editor.getElement(createXPathElement(), "Type", type);
  }

  protected void addMedia(String managedName, String typeName, String fileName) {
    String name = fileName.replaceAll("\\.[^.]*$", "");
    String id = editor.checkId(name);
    String src = "/"+managedName+"/"+typeName+"/"+fileName;
    Element typeE = createMediaTypeElement(typeName);
    Element e = editor.getElement(typeE, getMediaName(), id);
    e.addContent(new Element("Name").setText(name));
    e.addContent(new Element("Src").setText(src));
    update(e);
  }

  private String mediaName;
}
