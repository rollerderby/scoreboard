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
public class TeamsXmlDocumentManager extends AbstractXmlDocumentManager implements XmlDocumentManager
{
	public void setXmlScoreBoard(XmlScoreBoard xsB) {
		super.setXmlScoreBoard(xsB);
		reset();
	}

	public void reset() {
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

	protected void processElement(Element e) throws JDOMException {
		Iterator teams = e.getChildren("Team").iterator();
		while (teams.hasNext())
			processTeam((Element)teams.next());

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
				processTransfer(type.getName(), direction.getName(), teamId, element.getText());
			}
		}
	}

	protected void processTeam(Element team) {
		String id = team.getAttributeValue("Id");
		if (null == id || "".equals(id.trim()))
			return; /* Teams MUST have non-empty Id */
		Element newTeam = editor.addElement(createXPathElement(), "Team", id);

		String remove = team.getAttributeValue("remove");
		if (null != remove && Boolean.parseBoolean(remove)) {
			update(newTeam.setAttribute("remove", "true"));
			return;
		}

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

	protected void processTransfer(String type, String direction, String sbTeamId, String teamId) throws JDOMException {
		if (!Team.ID_1.equals(sbTeamId) && !Team.ID_2.equals(sbTeamId))
			return; /* Only process Team 1 or 2 transfers... */
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

	protected void fromScoreBoard(String sbTeamId, String id, boolean clear) throws JDOMException {
		Team team = xmlScoreBoard.getScoreBoardModel().getTeam(sbTeamId);
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

	protected List<XPath> transferXPaths = Arrays.asList(new XPath[]
		{ editor.createXPath("Transfer/To/*"),
		  editor.createXPath("Merge/To/*"),
		  editor.createXPath("Transfer/From/*"),
		  editor.createXPath("Merge/From/*") });
}

