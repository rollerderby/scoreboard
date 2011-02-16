package com.carolinarollergirls.scoreboard.xml;

import java.util.*;

import org.jdom.*;
import org.jdom.xpath.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.xml.*;
import com.carolinarollergirls.scoreboard.model.*;

/**
 * This allows storing team info, e.g. name, logo, roster, etc., separate from the actual teams
 * currently in the scoreboard.  It also allows copying one of the scoreboard's current teams
 * out into a saved team element here, or loading a team element here into the scoreboard.
 */
public class TeamsXmlDocumentManager extends AbstractXmlDocumentManager implements XmlDocumentManager,ScoreBoardController
{
	public TeamsXmlDocumentManager() {
		ScoreBoardManager.registerScoreBoardController(this);
		reset();
	}
	public void setScoreBoardModel(ScoreBoardModel sbM) { scoreBoardModel = sbM; }

	public void reset() {
		Element e = createXPathElement();
		Element t = editor.addElement(e, "Transfer");
		Element tTo = editor.addElement(t, "To");
		editor.addElement(tTo, Team.ID_1);
		editor.addElement(tTo, Team.ID_2);
		Element tFrom = editor.addElement(t, "From");
		editor.addElement(tFrom, Team.ID_1);
		editor.addElement(tFrom, Team.ID_2);
		Element m = editor.addElement(e, "Merge");
		Element mTo = editor.addElement(m, "To");
		editor.addElement(mTo, Team.ID_1);
		editor.addElement(mTo, Team.ID_2);
		Element mFrom = editor.addElement(m, "From");
		editor.addElement(mFrom, Team.ID_1);
		editor.addElement(mFrom, Team.ID_2);
		update(e);
	}

	protected void processElement(Element e) {
		Iterator teams = e.getChildren("Team").iterator();
		while (teams.hasNext())
			processTeam((Element)teams.next());

		try {
			Iterator i = transferTo.selectNodes(teams).iterator();
			while (i.hasNext()) {
				Element t = (Element)i.next();
				transferToScoreBoard(t.getText(), t.getName(), true);
			}
		} catch ( JDOMException jE ) { }
		try {
			Iterator i = mergeTo.selectNodes(teams).iterator();
			while (i.hasNext()) {
				Element t = (Element)i.next();
				transferToScoreBoard(t.getText(), t.getName(), false);
			}
		} catch ( JDOMException jE ) { }
		try {
			Iterator i = transferFrom.selectNodes(teams).iterator();
			while (i.hasNext()) {
				Element t = (Element)i.next();
				transferFromScoreBoard(t.getText(), t.getName(), true);
			}
		} catch ( JDOMException jE ) { }
		try {
			Iterator i = mergeFrom.selectNodes(teams).iterator();
			while (i.hasNext()) {
				Element t = (Element)i.next();
				transferFromScoreBoard(t.getText(), t.getName(), false);
			}
		} catch ( JDOMException jE ) { }
	}

	protected void processTeam(Element team) {
		String id = team.getAttributeValue("Id");
		if (null == id || "".equals(id.trim()))
			return; /* Teams MUST have non-empty Id */
		Element newTeam = editor.addElement(createXPathElement(), "Team", id);

		Element name = team.getChild("Name");
		if (null != name)
			editor.addElement(newTeam, "Name", null, name.getText());
		Element logo = team.getChild("Logo");
		if (null != logo)
			editor.addElement(newTeam, "Logo", null, logo.getText());

		Iterator skaters = team.getChildren("Skater").iterator();
		while (skaters.hasNext()) {
			Element skater = (Element)skaters.next();
			String skaterId = skater.getAttributeValue("Id");
			if (null == skaterId || "".equals(skaterId.trim()))
				continue;
			Element newSkater = editor.addElement(newTeam, "Skater", skaterId);

			Element sName = skater.getChild("Name");
			if (null != sName)
				editor.addElement(newSkater, "Name", null, sName.getText());
			Element sNumber = skater.getChild("Number");
			if (null != sNumber)
				editor.addElement(newSkater, "Number", null, sNumber.getText());
		}

		update(newTeam);
	}

	protected void transferToScoreBoard(String id, String sbTeamId, boolean reset) throws JDOMException {
		if (!Team.ID_1.equals(sbTeamId) && !Team.ID_2.equals(sbTeamId))
			return; /* Only process Team 1 or 2 transfers... */
		Element newTeam = editor.getElement(getXPathElement(), "Team", id, false);
		if (null == newTeam)
			return; /* Ignore if no team info exists for given Id */
		TeamModel team = scoreBoardModel.getTeamModel(sbTeamId);
		if (reset)
			team.reset();
		Element name = newTeam.getChild("Name");
		if (null != name)
			team.setName(name.getText());
		Element logo = newTeam.getChild("Logo");
		if (null != logo)
			team.getTeamLogoModel().setId(logo.getText());
		Iterator skaters = newTeam.getChildren("Skater").iterator();
		while (skaters.hasNext()) {
			Element skater = (Element)skaters.next();
			String sId = skater.getAttributeValue("Id");
			if (null == sId || "".equals(sId.trim()))
				continue;
			String sName = "";
			String sNumber = "";
			try { sName = skater.getChild("Name").getText(); } catch ( Exception e ) { }
			try { sNumber = skater.getChild("Number").getText(); } catch ( Exception e ) { }
			team.addSkaterModel(sId, sName, sNumber);
		}
	}

	protected void transferFromScoreBoard(String id, String sbTeamId, boolean clear) throws JDOMException {
		if (!Team.ID_1.equals(sbTeamId) && !Team.ID_2.equals(sbTeamId))
			return; /* Only process Team 1 or 2 transfers... */
		Team team = scoreBoardModel.getTeam(sbTeamId);
		if (clear)
			update(editor.addElement(createXPathElement(), "Team", id).setAttribute("remove", "true"));
		Element newTeam = (Element)editor.getElement(getXPathElement(), "Team", id).clone();
		createXPathElement().addContent(newTeam);
		editor.addElement(newTeam, "Name", null, team.getName());
		editor.addElement(newTeam, "Logo", null, team.getTeamLogo().getId());
		Iterator<Skater> skaters = team.getSkaters().iterator();
		while (skaters.hasNext()) {
			Skater skater = skaters.next();
			Element newSkater = editor.addElement(newTeam, "Skater", skater.getId());
			editor.addElement(newSkater, "Name", null, skater.getName());
			editor.addElement(newSkater, "Number", null, skater.getNumber());
		}
		update(newTeam);
	}

	protected String getManagedElementName() { return "Teams"; }

	protected ScoreBoardModel scoreBoardModel;

	protected XPath transferTo = editor.createXPath("Transfer/To/Team/*");
	protected XPath mergeTo = editor.createXPath("Merge/From/Team/*");
	protected XPath transferFrom = editor.createXPath("Transfer/To/Team/*");
	protected XPath mergeFrom = editor.createXPath("Merge/From/Team/*");
}

