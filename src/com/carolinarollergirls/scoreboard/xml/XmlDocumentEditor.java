package com.carolinarollergirls.scoreboard.xml;

import java.io.*;
import java.util.*;

import org.jdom.*;
import org.jdom.filter.*;
import org.jdom.input.*;
import org.jdom.output.*;
import org.jdom.xpath.*;

import com.carolinarollergirls.scoreboard.*;

public class XmlDocumentEditor
{
  public XmlDocumentEditor() { }

  public XPath createXPath(String path) {
    try {
      return XPath.newInstance(path);
    } catch ( JDOMException jdE ) {
      jdE.printStackTrace();
      throw new RuntimeException("ERROR: could not create XPath for '"+path+"' : "+jdE.getMessage());
    }
  }

  public String checkId(String id) {
    return id.replaceAll("['\"()]", "");
  }

  public Document createDocument() {
    return createDocument(null, null, null);
  }

  public Document createDocument(String element) {
    return createDocument(element, null, null);
  }

  public Document createDocument(String element, String id) {
    return createDocument(element, id, null);
  }

  public Document createDocument(String element, String id, String value) {
    Element e = new Element("document");
    Document document = new Document(e);
    addVersion(document);
    addSystemTime(document);
    if (null != element)
      addElement(e, element, id, value);
    return document;
  }

  public Element addElement(Element parent, String name) {
    return addElement(parent, name, null, null);
  }

  public Element addElement(Element parent, String name, String id) {
    return addElement(parent, name, id, null);
  }

  public Element addElement(Element parent, String name, String id, String content) {
    Document d = parent.getDocument();
    Element element = new Element(name);
    if (null != id && !"".equals(id))
      element.setAttribute("Id", id);
    synchronized (d) {
      parent.addContent(setContent(element, content));
    }
    return element;
  }

  public Element setElement(Element parent, String name) {
    return setElement(parent, name, null, null);
  }

  public Element setElement(Element parent, String name, String id) {
    return setElement(parent, name, id, null);
  }

  public Element setElement(Element parent, String name, String id, String content) {
    return setContent(getElement(parent, name, id), content);
  }

  public Element getElement(Element parent, String name) {
    return getElement(parent, name, null);
  }

  public Element getElement(Element parent, String name, String id) {
    return getElement(parent, name, id, true);
  }

  public Element getElement(Element parent, String name, String id, boolean create) {
    synchronized (parent.getDocument()) {
      Iterator children = parent.getChildren(name).iterator();
      while (children.hasNext()) {
        Element child = (Element)children.next();
        String childId = child.getAttributeValue("Id");
        if ((null == id && null == childId) || (null != id && id.equals(childId)))
          return child;
      }
    }

    return create ? addElement(parent, name, id, null) : null;
  }

  public void removeElement(Element parent, String name) {
    removeElement(parent, name, null);
  }

  public void removeElement(Element parent, String name, String id) {
    synchronized (parent.getDocument()) {
      Element child = getElement(parent, name, id, false);
      if (null != child)
        parent.getChildren().remove(child);
    }
  }

  public void removeContent(Element e) {
    if (null == e)
      return;

    synchronized (e) {
      e.removeAttribute("empty");
      e.removeContent(new ContentFilter(ContentFilter.TEXT));
    }
  }

  public Element setContent(Element e, String content) {
    if (null == e || null == content)
      return e;

    synchronized (e) {
      removeContent(e);

      if ("".equals(content))
        e.setAttribute("empty", "true");
      else
        e.addContent(content);
    }

    return e;
  }

  public String getContent(Element e) {
    if (null == e)
      return null;

    synchronized (e) {
      String s = e.getText();
      if ("".equals(s))
        return ("true".equals(e.getAttributeValue("empty")) ? "" : null);
      else
        return s;
    }
  }

  public Document addVersion(Document doc) {
    String oldVersion = doc.getRootElement().getAttributeValue("Version");
    if (oldVersion == null || oldVersion.equals(""))
      setVersion(doc);
    return doc;
  }

  public Document setVersion(Document doc) {
    doc.getRootElement().setAttribute("Version", ScoreBoardManager.getVersion());
    return doc;
  }

  public String getVersion(Document doc) {
    return doc.getRootElement().getAttributeValue("Version");
  }

  public Document addSystemTime(Document doc) {
    String oldSystemTime = doc.getRootElement().getAttributeValue("SystemTime");
    if (oldSystemTime == null || oldSystemTime.equals(""))
      setSystemTime(doc);
    return doc;
  }

  public Document setSystemTime(Document doc) {
    doc.getRootElement().setAttribute("SystemTime", Long.toString(new Date().getTime()));
    return doc;
  }

  public long getSystemTime(Document doc) throws NumberFormatException {
    return Long.parseLong(doc.getRootElement().getAttributeValue("SystemTime"));
  }



  public Document toDocument(InputStream stream) throws JDOMException,IOException {
    synchronized (builder) {
      return addSystemTime(builder.build(stream));
    }
  }

  public Document toDocument(Reader reader) throws JDOMException,IOException {
    synchronized (builder) {
      return addSystemTime(builder.build(reader));
    }
  }

  public Document toDocument(String s) throws JDOMException,IOException {
    synchronized (builder) {
      return addSystemTime(builder.build(new StringReader(s)));
    }
  }

  public String toString(Document d) { return toString(d, xmlOutputter.getFormat()); }
  public String toString(Document d, Format f) {
    return (xmlOutputter.getFormat().equals(f) ? xmlOutputter.outputString(d) : new XMLOutputter(f).outputString(d));
  }
  public String toString(Element e) { return toString(e, xmlOutputter.getFormat()); }
  public String toString(Element e, Format f) {
    return (xmlOutputter.getFormat().equals(f) ? xmlOutputter.outputString(e) : new XMLOutputter(f).outputString(e));
  }

  public void sendToOutputStream(Document d, OutputStream os) throws IOException { sendToOutputStream(d, os, xmlOutputter.getFormat()); }
  public void sendToOutputStream(Document d, OutputStream os, Format f) throws IOException {
    if (xmlOutputter.getFormat().equals(f))
      xmlOutputter.output(d, os);
    else
      new XMLOutputter(f).output(d, os);
  }

  public void sendToWriter(Document d, Writer w) throws IOException { sendToWriter(d, w, xmlOutputter.getFormat()); }
  public void sendToWriter(Document d, Writer w, Format f) throws IOException {
    if (xmlOutputter.getFormat().equals(f))
      xmlOutputter.output(d, w);
    else
      new XMLOutputter(f).output(d, w);
  }

  public Document mergeDocuments(Document to, Document from) { return mergeDocuments(to, from, false); }

  public Document mergeDocuments(Document to, Document from, boolean persistent) {
    mergeElements(to.getRootElement(), from.getRootElement(), persistent);
    return to;
  }

  public void mergeElements(Element to, Element from) { mergeElements(to, from, false); }

  public void mergeElements(Element to, Element from, boolean persistent) {
    synchronized (to.getDocument()) {
      /* Remove any nodes with "remove" attribute if document is persistent */
      if (persistent && Boolean.parseBoolean(from.getAttributeValue("remove"))) {
        to.detach();
        return;
      }

      /* If doing a merge to a persistent document and the from element is marked as persistentIgnore, ignore it */
//FIXME - remove this?
      if (persistent && Boolean.parseBoolean(from.getAttributeValue("persistentIgnore")))
        return;

      Iterator attrs = from.getAttributes().iterator();
      while (attrs.hasNext())
        to.setAttribute((Attribute)((Attribute)attrs.next()).clone());

      setContent(to, getContent(from));

      Iterator children = from.getChildren().iterator();
      while (children.hasNext()) {
        Element child = (Element)children.next();
        mergeElements(getElement(to, child.getName(), child.getAttributeValue("Id")), child, persistent);
      }
    }
  }

  public boolean hasElementRemoval(Document d) {
    return hasElementRemoval(d.getRootElement());
  }
  public boolean hasElementRemoval(Element e) {
    if ("true".equals(e.getAttributeValue("remove")))
      return true;

    Iterator i = e.getChildren().iterator();
    while (i.hasNext())
      if (hasElementRemoval((Element)i.next()))
        return true;

    return false;
  }

  public Element cloneDocumentToClonedElement(Element e) {
    return cloneDocumentToElement(e, createDocument(), true, false, false);
  }
  public Element cloneDocumentToElement(Element e) {
    return cloneDocumentToElement(e, true);
  }
  public Element cloneDocumentToElement(Element e, boolean includeTextFirst) {
    return cloneDocumentToElement(e, createDocument(), includeTextFirst);
  }
  public Element cloneDocumentToElement(Element e, Document d, boolean includeTextFirst) {
    return cloneDocumentToElement(e, d, includeTextFirst, false);
  }
  public Element cloneDocumentToElement(Element e, Document d, boolean includeTextFirst, boolean includeTextAll) {
    return cloneDocumentToElement(e, d, false, includeTextFirst, includeTextAll);
  }
  public Element cloneDocumentToElement(Element e, Document d, boolean cloneThisElement, boolean includeTextFirst, boolean includeTextAll) {
    if (e == null)
      return null;
    Element newE;
    if (cloneThisElement)
      newE = (Element)e.clone();
    else {
      newE = new Element(e.getName());
      Iterator attrs = e.getAttributes().iterator();
      while (attrs.hasNext())
        newE.setAttribute((Attribute)((Attribute)attrs.next()).clone());
      if (includeTextFirst)
        newE.setText(e.getText());
    }
    if (e.getParent().equals(e.getDocument().getRootElement()))
      d.getRootElement().addContent(newE);
    else
      cloneDocumentToElement(e.getParentElement(), d, false, includeTextAll, includeTextAll).addContent(newE);
    return newE;
  }

  public static XmlDocumentEditor getInstance() { return xmlDocumentEditor; }

  protected SAXBuilder builder = new SAXBuilder();
  protected XMLOutputter xmlOutputter = new XMLOutputter();

  private static XmlDocumentEditor xmlDocumentEditor = new XmlDocumentEditor();
}
