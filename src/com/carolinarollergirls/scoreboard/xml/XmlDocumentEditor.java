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

  public Element addElement(Element parent, String name, String id, String text) {
    Document d = parent.getDocument();
    Element element = new Element(name);
    if (null != id && !"".equals(id))
      element.setAttribute("Id", id);
    synchronized (d) {
      parent.addContent(setText(element, text));
    }
    return element;
  }

  public Element setElement(Element parent, String name) {
    return setElement(parent, name, null, null);
  }

  public Element setElement(Element parent, String name, String id) {
    return setElement(parent, name, id, null);
  }

  public Element setElement(Element parent, String name, String id, String text) {
    return setText(getElement(parent, name, id), text);
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

  public void removeText(Element e) {
    if (null == e)
      return;

    synchronized (e) {
      e.setAttribute("empty", "true");
      e.removeContent(cdataTextFilter);
    }
  }

  public Element setText(Element e, String text) {
    if (null == text)
      return e;
    return setText(e, new CDATA(text));
  }
  public Element setText(Element e, CDATA text) {
    if (null == e || null == text || null == text.getText())
      return e;

    synchronized (e) {
      removeText(e);

      if (!"".equals(text.getText()))
        e.removeAttribute("empty");
      e.addContent(text);
    }

    return e;
  }

  public String getText(Element e) {
    if (null == e)
      return null;

    synchronized (e) {
      if ("true".equals(e.getAttributeValue("empty")))
        return "";
      List l = e.getContent(cdataFilter);
      if (l.size() == 0)
        return null;
      StringBuffer sbuf = new StringBuffer();
      Iterator cdata = l.iterator();
      while (cdata.hasNext())
        sbuf.append(((CDATA)cdata.next()).getText());
      String s = sbuf.toString();
      if ("".equals(s))
        return null;
      else
        return s;
    }
  }

  public boolean isTrue(Element e) {
    return Boolean.parseBoolean(getText(e));
  }

  public Element removePI(Element e, final String target) {
    if (null == e || null == target || "".equals(target))
      return e;

    synchronized (e) {
      e.removeContent(new NamedProcessingInstructionFilter(target));
      return e;
    }
  }

  public Element setRemovePI(Element e) { return setPI(e, "Remove"); }
  public boolean hasRemovePI(Element e) { return hasPI(e, "Remove"); }
  public Element setNoSavePI(Element e) { return setPI(e, "NoSave"); }

  public Element addPI(Element e, String target) { return setPI(e, target); }
  public Element addPI(Element e, ProcessingInstruction pi) { return setPI(e, pi); }
  public Element setPI(Element e, String target) { return setPI(e, new ProcessingInstruction(target, "")); }
  public Element setPI(Element e, ProcessingInstruction pi) {
    if (null == e || null == pi || "".equals(pi.getTarget()))
      return e;

    synchronized (e) {
      return removePI(e, pi.getTarget()).addContent((ProcessingInstruction)pi.clone());
    }
  }

  public ProcessingInstruction getPI(Element e, String target) {
    if (null == e || null == target || "".equals(target))
      return null;

    synchronized (e) {
      List matches = e.getContent(new NamedProcessingInstructionFilter(target));
      if (matches.size() > 0)
        return (ProcessingInstruction)matches.get(0);
      else
        return null;
    }
  }

  public boolean hasPI(Element e, String target) {
    return (null != getPI(e, target));
  }

  public boolean hasAnyPI(Document d) { return d.getDescendants(piFilter).hasNext(); }

  public Document filterRemovePI(Document d) { return filterPI(d, "Remove"); }
  public Document filterNoSavePI(Document d) { return filterPI(d, "NoSave"); }
  public Document filterPI(Document d, String target) {
    synchronized (d) {
      filterPI(d.getRootElement(), target);
    }
    return d;
  }
  protected boolean filterPI(Element e, String target) {
    if (null == e)
      return false;

    synchronized (e) {
      if (hasPI(e, target)) {
        return true;
      } else {
        ListIterator children = e.getChildren().listIterator();
        while (children.hasNext())
          if (filterPI((Element)children.next(), target))
            children.remove();
      }
    }
    return false;
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

  public Document mergeDocuments(Document to, Document from) {
    mergeElements(to.getRootElement(), from.getRootElement());
    return to;
  }

  public void mergeElements(Element to, Element from) {
    synchronized (to.getDocument()) {
      Iterator pis = from.getContent(piFilter).iterator();
      while (pis.hasNext())
        setPI(to, (ProcessingInstruction)pis.next());

      Iterator attrs = from.getAttributes().iterator();
      while (attrs.hasNext())
        to.setAttribute((Attribute)((Attribute)attrs.next()).clone());

      setText(to, getText(from));

      Iterator children = from.getChildren().iterator();
      while (children.hasNext()) {
        Element child = (Element)children.next();
        mergeElements(getElement(to, child.getName(), child.getAttributeValue("Id")), child);
      }
    }
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
      Iterator pis = e.getContent(piFilter).iterator();
      while (pis.hasNext())
        setPI(newE, (ProcessingInstruction)pis.next());
      if (includeTextFirst)
        setText(newE, getText(e));
    }
    if (e.getParent().equals(e.getDocument().getRootElement()))
      d.getRootElement().addContent(newE);
    else
      cloneDocumentToElement(e.getParentElement(), d, false, includeTextAll, includeTextAll).addContent(newE);
    return newE;
  }

  public static XMLOutputter getPrettyXmlOutputter() { return new XMLOutputter(Format.getPrettyFormat()); }
  public static XMLOutputter getRawXmlOutputter() { return new XMLOutputter(Format.getRawFormat()); }

  public static XmlDocumentEditor getInstance() { return xmlDocumentEditor; }

  protected SAXBuilder builder = new SAXBuilder();

  private static XmlDocumentEditor xmlDocumentEditor = new XmlDocumentEditor();
  private static ContentFilter cdataFilter = new ContentFilter(ContentFilter.CDATA);
  private static ContentFilter cdataTextFilter = new ContentFilter(ContentFilter.CDATA|ContentFilter.TEXT);
  private static ContentFilter piFilter = new ContentFilter(ContentFilter.PI);

  protected static class NamedProcessingInstructionFilter extends ContentFilter
  {
    public NamedProcessingInstructionFilter(String t) {
      super(ContentFilter.PI);
      target = t;
    }
    public boolean matches(Object o) {
      try {
        return (super.matches(o) && ((ProcessingInstruction)o).getTarget().equals(target));
      } catch ( Exception e ) {
        return false;
      }
    }
    private String target;
  }

}
