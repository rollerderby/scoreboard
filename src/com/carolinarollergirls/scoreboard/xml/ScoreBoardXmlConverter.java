package com.carolinarollergirls.scoreboard.xml;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Iterator;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.CommandProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider.Flag;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;

public class ScoreBoardXmlConverter {
    /*****************************/
    /* ScoreBoard to XML methods */

    public String toString(ScoreBoard scoreBoard) {
        return rawXmlOutputter.outputString(toDocument(scoreBoard));
    }

    public Document toDocument(ScoreBoard scoreBoard) {
	Element root = new Element("document");
	Document d = new Document(root);
	
	toElement(root, scoreBoard);
	
	return d;
    }
	
    public Element toElement(Element parent, ScoreBoardEventProvider p) {
        Element e = editor.setElement(parent, p.getProviderName(), p.getProviderId());

        for (Class<? extends Property> type : p.getProperties()) {
	    for (Property prop : type.getEnumConstants()) {
		String name = PropertyConversion.toFrontend(prop);
		if (prop instanceof PermanentProperty) {
		    Object v = p.get((PermanentProperty)prop);
	            editor.setElement(e, name, null, v == null ? "" : String.valueOf(v));
		} else if (prop instanceof CommandProperty) {
		    editor.setElement(e, name, null, "");
		} else if (prop instanceof AddRemoveProperty) {
		    for (ValueWithId c : p.getAll((AddRemoveProperty)prop)) {
			if (c instanceof ScoreBoardEventProvider && ((ScoreBoardEventProvider)c).getParent() == p) {
			    toElement(e, (ScoreBoardEventProvider)c);
			} else {
			    editor.setElement(e, name, c.getId(), c.getValue());
			}
		    }
		}
	    }
        }
        return e;
    }

    /*****************************/
    /* XML to ScoreBoard methods */

    public void processDocument(ScoreBoard scoreBoard, Document document, boolean restore) {
        Iterator<?> children = document.getRootElement().getChildren().iterator();
        while (children.hasNext()) {
            Element child = (Element)children.next();
            if (child.getName().equals(scoreBoard.getProviderName())) {
                process(scoreBoard, child, restore);
            }
        }
    }
    
    public void process(ScoreBoardEventProvider p, Element element, boolean restore) {
	for (Object c : element.getChildren()) {
	    Element child = (Element)c;
            String name = child.getName();
            String id = child.getAttributeValue("Id");
            if (id == null) { id = ""; }
            String value = editor.getText(child);
	    try {
                Property prop = PropertyConversion.fromFrontend(name, p.getProperties());

                if (prop instanceof PermanentProperty) {
                    p.set((PermanentProperty)prop, p.valueFromString((PermanentProperty)prop, value), restore ? Flag.FROM_AUTOSAVE : null);
                } else if (prop instanceof CommandProperty) { 
                    if (Boolean.parseBoolean(value)) {
                        p.execute((CommandProperty)prop);
                    }
                } else if (editor.hasRemovePI(child)) {
                    p.remove((AddRemoveProperty)prop, id);
                } else if (child.getChildren().size() > 0) {
                    process((ScoreBoardEventProvider)p.get((AddRemoveProperty)prop, id, true), child, restore);
                } else {
                    p.add((AddRemoveProperty)prop, p.childFromString((AddRemoveProperty)prop, id, value));
                }
	    } catch (Exception e) {
		ScoreBoardManager.printMessage("Exception parsing XML for " + p.getProviderName() +
			"(" + p.getProviderId() + ")." + name + "(" + id + ") - " + value + ": " + e.toString());
		e.printStackTrace();
	    }
	}
    }

    public static ScoreBoardXmlConverter getInstance() { return scoreBoardXmlConverter; }

    protected XmlDocumentEditor editor = new XmlDocumentEditor();
    protected XMLOutputter rawXmlOutputter = XmlDocumentEditor.getRawXmlOutputter();

    private static ScoreBoardXmlConverter scoreBoardXmlConverter = new ScoreBoardXmlConverter();
}
