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
		if (id == null)
			return null;
		else
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

	public boolean isEmptyDocument(Document d) {
		return !(d.hasRootElement() && (d.getRootElement().getChildren().size() > 0));
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
		id = checkId(id);
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
		id = checkId(id);
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

	public String getId(Element e) {
		if (null == e)
			return null;
		return e.getAttributeValue("Id");
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

	public Element setPI(Element e, String target) { return setPI(e, target, ""); }
	public Element setPI(Element e, String target, String data) { return setPI(e, new ProcessingInstruction(target, data)); }
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
	public boolean hasRemovePI(Document d) { return d.getDescendants(removePIFilter).hasNext(); }
	public Document removeAllPI(Document d, List targets, boolean inclusive) {
		LinkedList<ProcessingInstruction> list = new LinkedList<ProcessingInstruction>();
		Iterator pis = d.getDescendants(piFilter);
		while (pis.hasNext()) {
			ProcessingInstruction pi = (ProcessingInstruction)pis.next();
			if (inclusive == targets.contains(pi.getTarget()))
				list.add(pi);
		}
		ListIterator<ProcessingInstruction> li = list.listIterator();
		while (li.hasNext())
			li.next().detach();
		return d;
	}
	/* Remove all PIs */
	public Document removeAllPI(Document d) { return removeAllPI(d, Collections.emptyList(), false); }
	/* Remove all PIs with targets specified in the List */
	public Document removeAllPI(Document d, List targets) { return removeAllPI(d, targets, true); }
	public Document removeAllPI(Document d, String target) { return removeAllPI(d, Collections.singletonList(target)); }
	/* Remove all PIs with targets not specified in the List */
	public Document removeExceptPI(Document d, List targets) { return removeAllPI(d, targets, false); }
	public Document removeExceptPI(Document d, String target) { return removeExceptPI(d, Collections.singletonList(target)); }

	/* These methods remove any element that contains a matched PI */
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

	/**
	 * ProcessingInstructions indicate the owning element should be processed in some way.
	 *
	 * XmlScoreBoard merge into the main Document
	 *	 Remove will be processed
	 *	 Once will be processed
	 *	 NoSave will be preserved
	 *	 All other PIs will be removed
	 * XmlScoreBoardListeners that receive Document changes from the XmlScoreBoard
	 *	 Remove must be processed
	 *	 Once must be processed
	 *	 NoSave must be processed by any that save to disc
	 *	 All other PIs must be removed by any that save to disc
	 *	 All other PIs should be processed or ignored/removed
	 * XmlDocumentManagers that receive Document changes from the XmlScoreBoard
	 *	 Remove must be processed
	 *	 Once must be processed
	 *	 All other PIs should be processed or ignored/removed
	 *
	 * Currently used ProcessingInstructions:
	 *	 Remove
	 *		 This indicates the owning element should be removed immediately.
	 *	 Once
	 *		 This indicates the owning element should be removed after processing it.
	 *	 NoSave
	 *		 This indicates the owning element should not be saved to disc.
	 *
	 * Special ProcessingInstructions:
	 *	 Reload
	 *		 This indicates the listener/viewer should reload
	 */
	public Element setRemovePI(Element e) { return setPI(e, "Remove"); }
	public boolean hasRemovePI(Element e) { return hasPI(e, "Remove"); }
	public Element setOncePI(Element e) { return setPI(e, "Once"); }
	public boolean hasOncePI(Element e) { return hasPI(e, "Once"); }
	public Element setNoSavePI(Element e) { return setPI(e, "NoSave"); }
	public boolean hasNoSavePI(Element e) { return hasPI(e, "NoSave"); }

	public Document filterRemovePI(Document d) { return filterPI(d, "Remove"); }
	public Document filterOncePI(Document d) { return filterPI(d, "Once"); }
	public Document filterNoSavePI(Document d) { return filterPI(d, "NoSave"); }

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
		return setSystemTime(doc, new Date().getTime());
	}
	public Document setSystemTime(Document doc, long time) {
		doc.getRootElement().setAttribute("SystemTime", Long.toString(time));
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

	public void filterOutDocumentXPath(Document d, XPath filter) throws JDOMException {
		filterOutElementXPath(d.getRootElement(), filter);
	}
	public void filterOutElementXPath(Element e, XPath filter) throws JDOMException {
		if (e == null || filter == null)
			return;
		List nodes = filter.selectNodes(e);
		Iterator i = nodes.iterator();
		while (i.hasNext()) {
			Element n = (Element)i.next();
			Element p = n.getParentElement();
			n.detach();
			while (p != e && p != null && p.getChildren().size() == 0 && getText(p) == null && !nodes.contains(p)) {
				Element nextP = p.getParentElement();
				p.detach();
				p = nextP;
			}
		}
	}
	public void filterElementXPath(Element e, XPath filter) throws JDOMException {
		if (e == null || filter == null)
			return;
		filterElementList(e, filter.selectNodes(e));
	}
	public void filterElementList(Element e, List keep) {
		if (e == null || keep == null)
			return;
		// Need to copy list first to avoid ConcurrentModificationException
		// Suppress unchecked warnings because JDOM 1.x doesn't support Java generics
		@SuppressWarnings("unchecked") Iterator<Element> children = new ArrayList(e.getChildren()).iterator();
		while (children.hasNext())
			filterElementList(children.next(), keep);
		if (e.getChildren().size() == 0 && !keep.contains(e))
			e.detach();
	}

	public Element cloneDocumentToClonedElement(Element e) {
		return cloneDocumentToElement(e, createDocument(), true, false, false, null);
	}
	public Element cloneDocumentToClonedElement(Element e, XPath filter) {
		return cloneDocumentToElement(e, createDocument(), true, false, false, filter);
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
		return cloneDocumentToElement(e, d, false, includeTextFirst, includeTextAll, null);
	}
	/*
	 * Clone a specific portion of a document with optional filtering.
	 *
	 * This clones a document from the root down to the specified element,
	 * optionally copying the contained text (if any) in the parent elements
	 * to the target cloned element.  If a XPath filter is used, only
	 * elements that match the filter (and their parent elements up to the
	 * document root) are included in the new document.
	 *
	 * The XPath filter should be an absolute XPath (starting with /)
	 * because JDOM 1.x doesn't appear to work correctly with relative XPaths.
	 *
	 * The returned element is the element in the new cloned document that
	 * corresponds to the initially provided element.
	 *
	 * If no elements match the XPath filter, then null is returned.
	 */
	public Element cloneDocumentToElement(Element e, Document d, boolean cloneThisElement, boolean includeTextFirst, boolean includeTextAll, XPath filter) {
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
			cloneDocumentToElement(e.getParentElement(), d, false, includeTextAll, includeTextAll, null).addContent(newE);
		try { filterElementXPath(newE, filter); } catch ( JDOMException jE ) { /* Nothing we can do, ignore */ }
		return ( newE.getDocument() == null ? null : newE );
	}

	public static XMLOutputter getPrettyXmlOutputter() { return new XMLOutputter(Format.getPrettyFormat()); }
	public static XMLOutputter getRawXmlOutputter() { return new XMLOutputter(Format.getRawFormat()); }

	public static XmlDocumentEditor getInstance() { return xmlDocumentEditor; }

	protected SAXBuilder builder = new SAXBuilder();

	private static XmlDocumentEditor xmlDocumentEditor = new XmlDocumentEditor();
	private static ContentFilter cdataFilter = new ContentFilter(ContentFilter.CDATA);
	private static ContentFilter cdataTextFilter = new ContentFilter(ContentFilter.CDATA|ContentFilter.TEXT);
	private static ContentFilter piFilter = new ContentFilter(ContentFilter.PI);
	private static ContentFilter removePIFilter = new NamedProcessingInstructionFilter("Remove");

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
