package com.carolinarollergirls.scoreboard.xml;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;

import org.jdom.*;
import org.jdom.xpath.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.xml.*;
import com.carolinarollergirls.scoreboard.model.*;

/**
 * This allows storing team info, e.g. name, logo, roster, etc., separate from the actual teams
 * currently in the scoreboard.	 It also allows copying one of the scoreboard's current teams
 * out into a saved team element here, or loading a team element here into the scoreboard.
 */
public class TeamsXmlDocumentManager extends DefaultXmlDocumentManager implements XmlDocumentManager
{
	public TeamsXmlDocumentManager() { super("Teams"); }

	public void setXmlScoreBoard(XmlScoreBoard xsB) {
		super.setXmlScoreBoard(xsB);
		reset();
	}

	public void reset() {
		super.reset();
		Element e = createXPathElement();
		Element t = editor.addElement(e, "Transfer");
		Element tTo = editor.addElement(t, "To");
		editor.addElement(tTo, "Team1");
		editor.addElement(tTo, "Team2");
		Element tFrom = editor.addElement(t, "From");
		editor.addElement(tFrom, "Team1");
		editor.addElement(tFrom, "Team2");
		Element m = editor.addElement(e, "Merge");
		Element mTo = editor.addElement(m, "To");
		editor.addElement(mTo, "Team1");
		editor.addElement(mTo, "Team2");
		Element mFrom = editor.addElement(m, "From");
		editor.addElement(mFrom, "Team1");
		editor.addElement(mFrom, "Team2");
		update(e);
	}

	protected void processElement(Element e) throws Exception {
		super.processElement(e);
		Iterator<XPath> transferTypes = transferXPaths.iterator();
		while (transferTypes.hasNext()) {
			Iterator elements = transferTypes.next().selectNodes(e).iterator();
			while (elements.hasNext()) {
				Element element = (Element)elements.next();
				String teamId = element.getName();
				if (!teamId.startsWith("Team"))
					continue;
				else
					teamId = teamId.replaceFirst("Team", "");
				Element direction = element.getParentElement();
				Element type = direction.getParentElement();
				processTransfer(type.getName(), direction.getName(), teamId, editor.getText(element));
			}
		}
	}

	protected void processChildElement(Element e) throws Exception {
		super.processChildElement(e);
		if (e.getName().equals("Team"))
			processTeam(e);
	}

	protected void processTeam(Element team) {
		String id = team.getAttributeValue("Id");
		if (null == id || "".equals(id.trim()))
			return; /* Teams MUST have non-empty Id */
		Element newTeam = editor.addElement(createXPathElement(), "Team", id);

		if (editor.hasRemovePI(team)) {
			update(editor.setRemovePI(newTeam));
			return;
		}

		Element name = team.getChild("Name");
		if (null != name)
			editor.addElement(newTeam, "Name", null, editor.getText(name));
		Element logo = team.getChild("Logo");
		if (null != logo)
			editor.addElement(newTeam, "Logo", null, editor.getText(logo));

		Iterator alternateNames = team.getChildren("AlternateName").iterator();
		while (alternateNames.hasNext())
			processAlternateName(newTeam, (Element)alternateNames.next());

		Iterator skaters = team.getChildren("Skater").iterator();
		while (skaters.hasNext())
			processSkater(newTeam, (Element)skaters.next());

		update(newTeam);
	}

	protected void processAlternateName(Element newTeam, Element alternateName) {
		String alternateNameId = alternateName.getAttributeValue("Id");
		if (null == alternateNameId || "".equals(alternateNameId.trim()))
			return;
		Element newAlternateName = editor.addElement(newTeam, "AlternateName", alternateNameId);

		if (editor.hasRemovePI(alternateName)) {
			Element removeAlternateNameTeam = editor.addElement(createXPathElement(), "Team", newTeam.getAttributeValue("Id"));
			update(removeAlternateNameTeam.addContent(editor.setRemovePI((Element)newAlternateName.detach())));
			return;
		}

		Element aName = alternateName.getChild("Name");
		if (null != aName)
			editor.addElement(newAlternateName, "Name", null, editor.getText(aName));
	}

	protected void processSkater(Element newTeam, Element skater) {
		String skaterId = skater.getAttributeValue("Id");
		if (null == skaterId || "".equals(skaterId.trim()))
			return;
		Element newSkater = editor.addElement(newTeam, "Skater", skaterId);

		if (editor.hasRemovePI(skater)) {
			Element removeSkaterTeam = editor.addElement(createXPathElement(), "Team", newTeam.getAttributeValue("Id"));
			update(removeSkaterTeam.addContent(editor.setRemovePI((Element)newSkater.detach())));
			return;
		}

		Element sName = skater.getChild("Name");
		if (null != sName)
			editor.addElement(newSkater, "Name", null, editor.getText(sName));
		Element sNumber = skater.getChild("Number");
		if (null != sNumber)
			editor.addElement(newSkater, "Number", null, editor.getText(sNumber));
	}

	protected void processTransfer(String type, String direction, String sbTeamId, String teamId) throws JDOMException {
		if (!Team.ID_1.equals(sbTeamId) && !Team.ID_2.equals(sbTeamId))
			return; /* Only process Team 1 or 2 transfers... */
		if (null == teamId || "".equals(teamId))
			return; /* Teams.Team elements must have an id */
		if (!"Transfer".equals(type) && !"Merge".equals(type))
			return;
		if ("To".equals(direction)) {
			toScoreBoard(sbTeamId, teamId, "Transfer".equals(type));
		} else if ("From".equals(direction)) {
			fromScoreBoard(sbTeamId, teamId, "Transfer".equals(type));
		}
	}

	protected void toScoreBoard(String sbTeamId, String id, boolean reset) throws JDOMException {
		Element newTeam = editor.getElement(getXPathElement(), "Team", id, false);
		if (null == newTeam)
			return; /* Ignore if no team info exists for given Id */
		TeamModel team = xmlScoreBoard.getScoreBoardModel().getTeamModel(sbTeamId);
		if (reset)
			team.reset();
		Element name = newTeam.getChild("Name");
		if (null != name)
			team.setName(editor.getText(name));
		Element logo = newTeam.getChild("Logo");
		if (null != logo)
			team.setLogo(editor.getText(logo));
		Iterator alternateNames = newTeam.getChildren("AlternateName").iterator();
		while (alternateNames.hasNext()) {
			Element alternateName = (Element)alternateNames.next();
			String aId = alternateName.getAttributeValue("Id");
			if (null == aId || "".equals(aId.trim()))
				continue;
			String aName = "";
			aName = editor.getText(alternateName.getChild("Name"));
			team.setAlternateNameModel(aId, aName);
		}
		Iterator skaters = newTeam.getChildren("Skater").iterator();
		while (skaters.hasNext()) {
			Element skater = (Element)skaters.next();
			String sId = skater.getAttributeValue("Id");
			if (null == sId || "".equals(sId.trim()))
				continue;
			String sName = "";
			String sNumber = "";
			sName = editor.getText(skater.getChild("Name"));
			sNumber = editor.getText(skater.getChild("Number"));
			team.addSkaterModel(sId, sName, sNumber);
		}
	}

	protected void fromScoreBoard(String sbTeamId, String id, boolean clear) throws JDOMException {
		Team team = xmlScoreBoard.getScoreBoardModel().getTeam(sbTeamId);
		Element newTeam = (Element)editor.getElement(getXPathElement(), "Team", id).clone();
		if (clear) {
			Element clearTeam = (Element)newTeam.clone();
			Iterator clearSkaters = clearTeam.getChildren("Skater").iterator();
			while (clearSkaters.hasNext())
				editor.setRemovePI((Element)clearSkaters.next());
			update(createXPathElement().addContent(clearTeam));
		}
		createXPathElement().addContent(newTeam);
		editor.addElement(newTeam, "Name", null, team.getName());
		editor.addElement(newTeam, "Logo", null, team.getLogo());
		Iterator<Team.AlternateName> alternateNames = team.getAlternateNames().iterator();
		while (alternateNames.hasNext()) {
			Team.AlternateName alternateName = alternateNames.next();
			Element newAlternateName = editor.addElement(newTeam, "AlternateName", alternateName.getId());
			editor.addElement(newAlternateName, "Name", null, alternateName.getName());
		}
		Iterator<Skater> skaters = team.getSkaters().iterator();
		while (skaters.hasNext()) {
			Skater skater = skaters.next();
			Element newSkater = editor.addElement(newTeam, "Skater", skater.getId());
			editor.addElement(newSkater, "Name", null, skater.getName());
			editor.addElement(newSkater, "Number", null, skater.getNumber());
		}
		update(newTeam);
	}

	protected List<XPath> transferXPaths = Arrays.asList(new XPath[]
		{ editor.createXPath("Transfer/To/*"),
			editor.createXPath("Merge/To/*"),
			editor.createXPath("Transfer/From/*"),
			editor.createXPath("Merge/From/*") });
}

