package com.carolinarollergirls.scoreboard.xml;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.OrderedScoreBoardEventProvider.IValue;
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

        toElement(root, scoreBoard, false);

        return d;
    }

    public Element toElement(Element parent, ScoreBoardEventProvider p, Boolean remove) {
        Element e = editor.setElement(parent, p.getProviderName(), p.getProviderId());

        for (Class<? extends Property> type : p.getProperties()) {
            for (Property prop : type.getEnumConstants()) {
                String name = PropertyConversion.toFrontend(prop);
                if (prop instanceof PermanentProperty) {
                    Object v = p.get((PermanentProperty)prop);
                    Element ne = editor.setElement(e, name, null, v == null ? "" : String.valueOf(v));
                    editor.setRemovePI(ne, remove);
                } else if (prop instanceof CommandProperty) {
                    Element ne = editor.setElement(e, name, null, "");
                    editor.setRemovePI(ne, remove);
                } else if (prop instanceof AddRemoveProperty) {
                    for (ValueWithId c : p.getAll((AddRemoveProperty)prop)) {
                        if (c instanceof ScoreBoardEventProvider && ((ScoreBoardEventProvider)c).getParent() == p) {
                            Element ne = toElement(e, (ScoreBoardEventProvider)c, remove);
                            editor.setRemovePI(ne, remove);
                        } else {
                            Element ne = editor.setElement(e, name, c.getId(), c.getValue());
                            editor.setRemovePI(ne, remove);
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
                List<ValueUpdate> postponedUpdates = new ArrayList<ValueUpdate>();
                process(scoreBoard, child, restore, postponedUpdates);
                for (ValueUpdate update : postponedUpdates) {
                    update.process();
                }
            }
        }
    }

    public void process(ScoreBoardEventProvider p, Element element, boolean restore, List<ValueUpdate> postponedUpdates) {
        for (Object c : element.getChildren()) {
            Element child = (Element)c;
            String name = child.getName();
            String id = child.getAttributeValue("Id");
            if (id == null) { id = ""; }
            String value = editor.getText(child);
            Flag flag = null;
            if (Boolean.parseBoolean(child.getAttributeValue("reset"))) { flag = Flag.RESET; }
            if (Boolean.parseBoolean(child.getAttributeValue("change"))) { flag = Flag.CHANGE; }
            if (restore) { flag = Flag.FROM_AUTOSAVE; }
            try {
                Property prop = PropertyConversion.fromFrontend(name, p.getProperties());
                if (prop == null) { throw new IllegalArgumentException("Unknown property"); }

                if (prop == IValue.ID) {
                    p.set((PermanentProperty) prop, p.valueFromString((PermanentProperty) prop, value, flag), flag);
                } else if (prop instanceof PermanentProperty) {
                    // postpone setting PermanentProperties except ID, as they may reference
                    //  elements not yet created when restoring from autosave
                    postponedUpdates.add(new ValueUpdate(p, (PermanentProperty) prop, value, flag));
                } else if (prop instanceof CommandProperty) { 
                    if (Boolean.parseBoolean(value)) {
                        p.execute((CommandProperty)prop);
                    }
                } else if (editor.hasRemovePI(child)) {
                    p.remove((AddRemoveProperty)prop, id);
                } else if (child.getChildren().size() > 0) {
                    process((ScoreBoardEventProvider)p.getOrCreate((AddRemoveProperty)prop, id), child, restore, postponedUpdates);
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
    
    public static class ValueUpdate {
        public ValueUpdate(ScoreBoardEventProvider sbe, PermanentProperty prop, String value, Flag flag) {
            this.sbe = sbe;
            this.prop = prop;
            this.value = value;
            this.flag = flag;
        }
        
        public void process() { sbe.set(prop, sbe.valueFromString(prop, value, flag), flag); }
        
        private ScoreBoardEventProvider sbe;
        private PermanentProperty prop;
        private String value;
        private Flag flag;
    }
}
