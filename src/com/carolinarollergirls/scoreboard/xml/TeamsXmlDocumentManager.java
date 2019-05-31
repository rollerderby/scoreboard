package com.carolinarollergirls.scoreboard.xml;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;

/**
 * This allows storing team info, e.g. name, logo, roster, etc., separate from the actual teams
 * currently in the scoreboard.	 It also allows copying one of the scoreboard's current teams
 * out into a saved team element here, or loading a team element here into the scoreboard.
 */
public class TeamsXmlDocumentManager extends DefaultXmlDocumentManager implements XmlDocumentManager {
    public TeamsXmlDocumentManager() { super("Teams"); }

    @Override
    public void setXmlScoreBoard(XmlScoreBoard xsB) {
        super.setXmlScoreBoard(xsB);
        reset();
    }

    @Override
    protected void processChildElement(Element e) throws Exception {
        super.processChildElement(e);
        if (e.getName().equals("Team")) {
            processTeam(e);
        }
    }

    protected void processTeam(Element team) {
        String id = team.getAttributeValue("Id");
        if (null == id || "".equals(id.trim())) {
            return;    /* Teams MUST have non-empty Id */
        }
        Element newTeam = editor.addElement(createXPathElement(), "Team", id);

        if (editor.hasRemovePI(team)) {
            update(editor.setRemovePI(newTeam));
            return;
        }

        Element name = team.getChild("Name");
        if (null != name) {
            editor.addElement(newTeam, "Name", null, editor.getText(name));
        }
        Element logo = team.getChild("Logo");
        if (null != logo) {
            editor.addElement(newTeam, "Logo", null, editor.getText(logo));
        }

        Iterator<?> alternateNames = team.getChildren("AlternateName").iterator();
        while (alternateNames.hasNext()) {
            processAlternateName(newTeam, (Element)alternateNames.next());
        }

        Iterator<?> colors = team.getChildren("Color").iterator();
        while (colors.hasNext()) {
            processColor(newTeam, (Element)colors.next());
        }

        Iterator<?> skaters = team.getChildren("Skater").iterator();
        while (skaters.hasNext()) {
            processSkater(newTeam, (Element)skaters.next());
        }

        update(newTeam);
    }

    protected void processAlternateName(Element newTeam, Element alternateName) {
        String alternateNameId = alternateName.getAttributeValue("Id");
        if (null == alternateNameId || "".equals(alternateNameId.trim())) {
            return;
        }
        Element newAlternateName = editor.addElement(newTeam, "AlternateName", alternateNameId, editor.getText(alternateName));

        if (editor.hasRemovePI(alternateName)) {
            Element removeAlternateNameTeam = editor.addElement(createXPathElement(), "Team", newTeam.getAttributeValue("Id"));
            update(removeAlternateNameTeam.addContent(editor.setRemovePI((Element)newAlternateName.detach())));
        }
    }

    protected void processColor(Element newTeam, Element color) {
        String colorId = color.getAttributeValue("Id");
        if (null == colorId || "".equals(colorId.trim())) {
            return;
        }
        Element newColor = editor.addElement(newTeam, "Color", colorId, editor.getText(color));

        if (editor.hasRemovePI(color)) {
            Element removeColorTeam = editor.addElement(createXPathElement(), "Team", newTeam.getAttributeValue("Id"));
            update(removeColorTeam.addContent(editor.setRemovePI((Element)newColor.detach())));
        }
    }

    protected void processSkater(Element newTeam, Element skater) {
        String skaterId = skater.getAttributeValue("Id");
        if (null == skaterId || "".equals(skaterId.trim())) {
            return;
        }
        Element newSkater = editor.addElement(newTeam, "Skater", skaterId);

        if (editor.hasRemovePI(skater)) {
            Element removeSkaterTeam = editor.addElement(createXPathElement(), "Team", newTeam.getAttributeValue("Id"));
            update(removeSkaterTeam.addContent(editor.setRemovePI((Element)newSkater.detach())));
            return;
        }

        Element sName = skater.getChild("Name");
        if (null != sName) {
            editor.addElement(newSkater, "Name", null, editor.getText(sName));
        }
        Element sNumber = skater.getChild("Number");
        if (null != sNumber) {
            editor.addElement(newSkater, "Number", null, editor.getText(sNumber));
        }
        Element sFlags = skater.getChild("Flags");
        if (null != sFlags) {
            editor.addElement(newSkater, "Flags", null, editor.getText(sFlags));
        }
    }

    public void toScoreBoard(String sbTeamId, String id, boolean reset) throws JDOMException {
        Element newTeam = editor.getElement(getXPathElement(), "Team", id, false);
        if (null == newTeam) {
            return;    /* Ignore if no team info exists for given Id */
        }
        Team team = xmlScoreBoard.getScoreBoard().getTeam(sbTeamId);
        if (reset) {
            team.reset();
        }
        Element name = newTeam.getChild("Name");
        if (null != name) {
            team.setName(editor.getText(name));
        }
        Element logo = newTeam.getChild("Logo");
        if (null != logo) {
            team.setLogo(editor.getText(logo));
        }
        Iterator<?> alternateNames = newTeam.getChildren("AlternateName").iterator();
        while (alternateNames.hasNext()) {
            Element alternateName = (Element)alternateNames.next();
            String aId = alternateName.getAttributeValue("Id");
            if (null == aId || "".equals(aId.trim())) {
                continue;
            }
            String aName = "";
            aName = editor.getText(alternateName);
            team.setAlternateName(aId, aName);
        }
        Iterator<?> colors = newTeam.getChildren("Color").iterator();
        while (colors.hasNext()) {
            Element color = (Element)colors.next();
            String cId = color.getAttributeValue("Id");
            if (null == cId || "".equals(cId.trim())) {
                continue;
            }
            String cColor = "";
            cColor = editor.getText(color);
            team.setColor(cId, cColor);
        }
        Iterator<?> skaters = newTeam.getChildren("Skater").iterator();
        while (skaters.hasNext()) {
            Element skater = (Element)skaters.next();
            String sId = skater.getAttributeValue("Id");
            if (null == sId || "".equals(sId.trim())) {
                continue;
            }
            String sName = "";
            String sNumber = "";
            String sFlags = "";
            sName = editor.getText(skater.getChild("Name"));
            sNumber = editor.getText(skater.getChild("Number"));
            sFlags = editor.getText(skater.getChild("Flags"));
            team.addSkater(sId, sName, sNumber, sFlags);
        }
    }
}

