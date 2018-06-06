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
import org.jdom.output.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public class ScoreBoardXmlConverter
{
	/*****************************/
	/* ScoreBoard to XML methods */

	public String toString(ScoreBoard scoreBoard) {
		return rawXmlOutputter.outputString(toDocument(scoreBoard));
	}

	public Document toDocument(ScoreBoard scoreBoard) {
		Element sb = new Element("ScoreBoard");
		Document d = new Document(new Element("document").addContent(sb));

		editor.setElement(sb, "Reset", null, "");
		editor.setElement(sb, "StartJam", null, "");
		editor.setElement(sb, "UnStartJam", null, "");
		editor.setElement(sb, "StopJam", null, "");
		editor.setElement(sb, "UnStopJam", null, "");
		editor.setElement(sb, "Timeout", null, "");
		editor.setElement(sb, "UnTimeout", null, "");
		editor.setElement(sb, "StartOvertime", null, "");

		editor.setElement(sb, ScoreBoard.EVENT_TIMEOUT_OWNER, null, scoreBoard.getTimeoutOwner());
		editor.setElement(sb, ScoreBoard.EVENT_OFFICIAL_REVIEW, null, String.valueOf(scoreBoard.isOfficialReview()));
		editor.setElement(sb, ScoreBoard.EVENT_IN_OVERTIME, null, String.valueOf(scoreBoard.isInOvertime()));
		editor.setElement(sb, ScoreBoard.EVENT_IN_PERIOD, null, String.valueOf(scoreBoard.isInPeriod()));
		editor.setElement(sb, ScoreBoard.EVENT_OFFICIAL_SCORE, null, String.valueOf(scoreBoard.isOfficialScore()));
		editor.setElement(sb, ScoreBoard.EVENT_RULESET, null, String.valueOf(scoreBoard.getRuleset()));

		toElement(sb, scoreBoard.getSettings());

		Iterator<Clock> clocks = scoreBoard.getClocks().iterator();
		while (clocks.hasNext())
			toElement(sb, clocks.next());

		Iterator<Team> teams = scoreBoard.getTeams().iterator();
		while (teams.hasNext())
			toElement(sb, teams.next());

		Iterator<Policy> policies = scoreBoard.getPolicies().iterator();
		while (policies.hasNext())
			toElement(sb, policies.next());

		return d;
	}

	public Element toElement(Element p, Settings s) {
		Element e = editor.setElement(p, "Settings");
		Iterator<String> keys = s.getAll().keySet().iterator();
		while (keys.hasNext()) {
			String k = keys.next();
			String v = s.get(k);
			if (v != null)
				editor.setElement(e, Settings.EVENT_SETTING, k, v);
		}
		return e;
	}

	public Element toElement(Element sb, Clock c) {
		Element e = editor.setElement(sb, "Clock", c.getId());

		editor.setElement(e, "Start", null, "");
		editor.setElement(e, "UnStart", null, "");
		editor.setElement(e, "Stop", null, "");
		editor.setElement(e, "UnStop", null, "");
		editor.setElement(e, "ResetTime", null, "");

		editor.setElement(e, Clock.EVENT_NAME, null, c.getName());
		editor.setElement(e, Clock.EVENT_NUMBER, null, String.valueOf(c.getNumber()));
		editor.setElement(e, Clock.EVENT_MINIMUM_NUMBER, null, String.valueOf(c.getMinimumNumber()));
		editor.setElement(e, Clock.EVENT_MAXIMUM_NUMBER, null, String.valueOf(c.getMaximumNumber()));
		editor.setElement(e, Clock.EVENT_TIME, null, String.valueOf(c.getTime()));
		editor.setElement(e, Clock.EVENT_INVERTED_TIME, null, String.valueOf(c.getInvertedTime()));
		editor.setElement(e, Clock.EVENT_MINIMUM_TIME, null, String.valueOf(c.getMinimumTime()));
		editor.setElement(e, Clock.EVENT_MAXIMUM_TIME, null, String.valueOf(c.getMaximumTime()));
		editor.setElement(e, Clock.EVENT_RUNNING, null, String.valueOf(c.isRunning()));
		editor.setElement(e, Clock.EVENT_DIRECTION, null, String.valueOf(c.isCountDirectionDown()));
		return e;
	}

	public Element toElement(Element sb, Team t) {
		Element e = editor.setElement(sb, "Team", t.getId());

		editor.setElement(e, "Timeout", null, "");
		editor.setElement(e, "OfficialReview", null, "");

		editor.setElement(e, Team.EVENT_NAME, null, t.getName());
		editor.setElement(e, Team.EVENT_LOGO, null, t.getLogo());
		editor.setElement(e, Team.EVENT_SCORE, null, String.valueOf(t.getScore()));
		editor.setElement(e, Team.EVENT_LAST_SCORE, null, String.valueOf(t.getLastScore()));
		editor.setElement(e, Team.EVENT_TIMEOUTS, null, String.valueOf(t.getTimeouts()));
		editor.setElement(e, Team.EVENT_OFFICIAL_REVIEWS, null, String.valueOf(t.getOfficialReviews()));
		editor.setElement(e, Team.EVENT_IN_TIMEOUT, null, String.valueOf(t.inTimeout()));
		editor.setElement(e, Team.EVENT_IN_OFFICIAL_REVIEW, null, String.valueOf(t.inOfficialReview()));
		editor.setElement(e, Team.EVENT_RETAINED_OFFICIAL_REVIEW, null, String.valueOf(t.retainedOfficialReview()));
		editor.setElement(e, Team.EVENT_LEAD_JAMMER, null, t.getLeadJammer());
		editor.setElement(e, Team.EVENT_STAR_PASS, null, String.valueOf(t.isStarPass()));

		Iterator<Team.AlternateName> alternateNames = t.getAlternateNames().iterator();
		while (alternateNames.hasNext())
			toElement(e, alternateNames.next());

		Iterator<Team.Color> colors = t.getColors().iterator();
		while (colors.hasNext())
			toElement(e, colors.next());

		Iterator<Position> positions = t.getPositions().iterator();
		while (positions.hasNext())
			toElement(e, positions.next());

		Iterator<Skater> skaters = t.getSkaters().iterator();
		while (skaters.hasNext())
			toElement(e, skaters.next());

		return e;
	}

	public Element toElement(Element team, Team.AlternateName n) {
		Element e = editor.setElement(team, "AlternateName", n.getId());

		editor.setElement(e, Team.AlternateName.EVENT_NAME, null, n.getName());

		return e;
	}

	public Element toElement(Element team, Team.Color c) {
		Element e = editor.setElement(team, "Color", c.getId());

		editor.setElement(e, Team.Color.EVENT_COLOR, null, c.getColor());

		return e;
	}

	public Element toElement(Element team, Position p) {
		Element e = editor.setElement(team, "Position", p.getId());

		editor.setElement(e, "Clear", null, "");

		Skater s = p.getSkater();
		editor.setElement(e, "Id", null, (s==null?"":s.getId()));
		editor.setElement(e, Skater.EVENT_NAME, null, (s==null?"":s.getName()));
		editor.setElement(e, Skater.EVENT_NUMBER, null, (s==null?"":s.getNumber()));
		editor.setElement(e, Skater.EVENT_PENALTY_BOX, null, String.valueOf(s==null?false:s.isPenaltyBox()));
		editor.setElement(e, Skater.EVENT_FLAGS, null, (s==null?"":s.getFlags()));

		return e;
	}

	public Element toElement(Element sb, Policy p) {
		Element e = editor.setElement(sb, "Policy", p.getId());
		editor.setElement(e, Policy.EVENT_NAME, null, p.getName());
		editor.setElement(e, Policy.EVENT_DESCRIPTION, null, p.getDescription());
		editor.setElement(e, Policy.EVENT_ENABLED, null, String.valueOf(p.isEnabled()));

		Iterator<Policy.Parameter> parameters = p.getParameters().iterator();
		while (parameters.hasNext())
			toElement(e, parameters.next());

		return e;
	}

	public Element toElement(Element p, Policy.Parameter pp) {
		Element e = editor.setElement(p, "Parameter", pp.getName());
		editor.setElement(e, "Name", null, pp.getName());
		editor.setElement(e, "Type", null, pp.getType());
		editor.setElement(e, Policy.Parameter.EVENT_VALUE, null, pp.getValue());
		return e;
	}

	public Element toElement(Element t, Skater s) {
		Element e = editor.setElement(t, "Skater", s.getId());
		editor.setElement(e, Skater.EVENT_NAME, null, s.getName());
		editor.setElement(e, Skater.EVENT_NUMBER, null, s.getNumber());
		editor.setElement(e, Skater.EVENT_POSITION, null, s.getPosition());
		editor.setElement(e, Skater.EVENT_PENALTY_BOX, null, String.valueOf(s.isPenaltyBox()));
		editor.setElement(e, Skater.EVENT_FLAGS, null, s.getFlags());

    for (Skater.Penalty p: s.getPenalties()) {
			toElement(e, p);
    }

    if (s.getFOEXPPenalty() != null) {
      Element fe = editor.setElement(e, Skater.EVENT_PENALTY_FOEXP, s.getFOEXPPenalty().getId());
      toElement(fe, s.getFOEXPPenalty());
    }

		return e;
	}

	public Element toElement(Element s, Skater.Penalty p) {
		Element e = editor.setElement(s, "Penalty", p.getId());
		editor.setElement(e, Skater.EVENT_PENALTY_PERIOD, null, String.valueOf(p.getPeriod()));
		editor.setElement(e, Skater.EVENT_PENALTY_JAM, null, String.valueOf(p.getJam()));
		editor.setElement(e, Skater.EVENT_PENALTY_CODE, null, p.getCode());
    return e;
  }

	/*****************************/
	/* XML to ScoreBoard methods */

	public void processDocument(ScoreBoardModel scoreBoardModel, Document document) {
		Iterator children = document.getRootElement().getChildren().iterator();
		while (children.hasNext()) {
			Element element = (Element)children.next();
			if (element.getName().equals("ScoreBoard"))
				processScoreBoard(scoreBoardModel, element);
		}
	}

	public void processScoreBoard(ScoreBoardModel scoreBoardModel, Element scoreBoard) {
		Iterator children = scoreBoard.getChildren().iterator();
		while (children.hasNext()) {
			Element element = (Element)children.next();
			try {
				String name = element.getName();
				String value = editor.getText(element);
				boolean bVal = Boolean.parseBoolean(value);

				if (name.equals("Clock"))
					processClock(scoreBoardModel, element);
				else if (name.equals("Team"))
					processTeam(scoreBoardModel, element);
				else if (name.equals("Policy"))
					processPolicy(scoreBoardModel, element);
				else if (name.equals("Settings"))
					processSettings(scoreBoardModel, element);
				else if (null == value)
					continue;
				else if (name.equals(ScoreBoard.EVENT_TIMEOUT_OWNER))
					scoreBoardModel.setTimeoutOwner(value);
				else if (name.equals(ScoreBoard.EVENT_OFFICIAL_REVIEW))
					scoreBoardModel.setOfficialReview(bVal);
				else if (name.equals(ScoreBoard.EVENT_IN_OVERTIME))
					scoreBoardModel.setInOvertime(bVal);
				else if (name.equals(ScoreBoard.EVENT_IN_PERIOD))
					scoreBoardModel.setInPeriod(bVal);
				else if (name.equals(ScoreBoard.EVENT_OFFICIAL_SCORE))
					scoreBoardModel.setOfficialScore(bVal);
				else if (name.equals(ScoreBoard.EVENT_RULESET))
					scoreBoardModel.setRuleset(value);
				else if (bVal) {
					if (name.equals("Reset"))
						scoreBoardModel.reset();
					else if (name.equals("StartJam"))
						scoreBoardModel.startJam();
					else if (name.equals("StopJam"))
						scoreBoardModel.stopJam();
					else if (name.equals("Timeout"))
						scoreBoardModel.timeout();
					else if (name.equals("UnStartJam"))
						scoreBoardModel.unStartJam();
					else if (name.equals("UnStopJam"))
						scoreBoardModel.unStopJam();
					else if (name.equals("UnTimeout"))
						scoreBoardModel.unTimeout();
					else if (name.equals("StartOvertime"))
						scoreBoardModel.startOvertime();
				}
			} catch ( Exception e ) {
			}
		}
	}

	public void processSettings(ScoreBoardModel scoreBoardModel, Element settings) {
		SettingsModel sm = scoreBoardModel.getSettingsModel();
		Iterator children = settings.getChildren().iterator();
		while (children.hasNext()) {
			Element element = (Element)children.next();
			try {
				String k = element.getAttributeValue("Id");
				String v = editor.getText(element);
				if (v == null)
					v = "";
				sm.set(k, v);
			} catch ( Exception e ) {
			}
		}
	}

	public void processClock(ScoreBoardModel scoreBoardModel, Element clock) {
		String id = clock.getAttributeValue("Id");
		ClockModel clockModel = scoreBoardModel.getClockModel(id);
		boolean requestStart = false;
		boolean requestStop = false;
		boolean requestUnStart = false;
		boolean requestUnStop = false;

		Iterator children = clock.getChildren().iterator();
		while (children.hasNext()) {
			Element element = (Element)children.next();
			try {
				String name = element.getName();
				String value = editor.getText(element);

				boolean isChange = Boolean.parseBoolean(element.getAttributeValue("change"));
				boolean isReset = Boolean.parseBoolean(element.getAttributeValue("reset"));

//FIXME - might be better way to handle changes/resets than an attribute...
				if ((null == value) && !isReset)
					continue;
				else if (name.equals("Start") && Boolean.parseBoolean(value))
					requestStart = true;
				else if (name.equals("Stop") && Boolean.parseBoolean(value))
					requestStop = true;
				else if (name.equals("UnStart") && Boolean.parseBoolean(value))
					requestUnStart = true;
				else if (name.equals("UnStop") && Boolean.parseBoolean(value))
					requestUnStop = true;
				else if (name.equals("ResetTime") && Boolean.parseBoolean(value))
					clockModel.resetTime();
				else if (name.equals(Clock.EVENT_NAME))
					clockModel.setName(value);
				else if (name.equals(Clock.EVENT_NUMBER) && isChange)
					clockModel.changeNumber(Integer.parseInt(value));
				else if (name.equals(Clock.EVENT_NUMBER) && !isChange)
					clockModel.setNumber(Integer.parseInt(value));
				else if (name.equals(Clock.EVENT_MINIMUM_NUMBER))
					clockModel.setMinimumNumber(Integer.parseInt(value));
				else if (name.equals(Clock.EVENT_MAXIMUM_NUMBER))
					clockModel.setMaximumNumber(Integer.parseInt(value));
				else if (name.equals(Clock.EVENT_TIME) && isChange)
					clockModel.changeTime(Long.parseLong(value));
				else if (name.equals(Clock.EVENT_TIME) && isReset)
					clockModel.resetTime();
				else if (name.equals(Clock.EVENT_TIME) && !isChange && !isReset)
					clockModel.setTime(Long.parseLong(value));
				else if (name.equals(Clock.EVENT_MINIMUM_TIME) && isChange)
					clockModel.changeMinimumTime(Long.parseLong(value));
				else if (name.equals(Clock.EVENT_MINIMUM_TIME))
					clockModel.setMinimumTime(Long.parseLong(value));
				else if (name.equals(Clock.EVENT_MAXIMUM_TIME) && isChange)
					clockModel.changeMaximumTime(Long.parseLong(value));
				else if (name.equals(Clock.EVENT_MAXIMUM_TIME))
					clockModel.setMaximumTime(Long.parseLong(value));
				else if (name.equals(Clock.EVENT_RUNNING) && Boolean.parseBoolean(value))
					requestStart = true;
				else if (name.equals(Clock.EVENT_RUNNING) && !Boolean.parseBoolean(value))
					requestStop = true;
				else if (name.equals(Clock.EVENT_DIRECTION))
					clockModel.setCountDirectionDown(Boolean.parseBoolean(value));
			} catch ( Exception e ) {
			}
		}
		// Process start/stops at the end to allow setting of options (direction/min/max/etc) on load
		if (requestStart) clockModel.start();
		if (requestStop) clockModel.stop();
		if (requestUnStart) clockModel.unstart();
		if (requestUnStop) clockModel.unstop();
	}

	public void processTeam(ScoreBoardModel scoreBoardModel, Element team) {
		String id = team.getAttributeValue("Id");
		TeamModel teamModel = scoreBoardModel.getTeamModel(id);

		Iterator children = team.getChildren().iterator();
		while (children.hasNext()) {
			Element element = (Element)children.next();
			try {
				String name = element.getName();
				String eId = element.getAttributeValue("Id");
				String value = editor.getText(element);

				boolean isChange = Boolean.parseBoolean(element.getAttributeValue("change"));

				if (name.equals("AlternateName"))
					processAlternateName(teamModel, element);
				else if (name.equals("Color"))
					processColor(teamModel, element);
				else if (name.equals("Skater"))
					processSkater(teamModel, element);
				else if (name.equals("Position"))
					processPosition(teamModel, element);
				else if (null == value)
					continue;
				else if (name.equals("Timeout") && Boolean.parseBoolean(value))
					teamModel.timeout();
				else if (name.equals("OfficialReview") && Boolean.parseBoolean(value))
					teamModel.officialReview();
				else if (name.equals(Team.EVENT_NAME))
					teamModel.setName(value);
				else if (name.equals(Team.EVENT_LOGO))
					teamModel.setLogo(value);
				else if (name.equals(Team.EVENT_SCORE) && isChange)
					teamModel.changeScore(Integer.parseInt(value));
				else if (name.equals(Team.EVENT_LAST_SCORE) && isChange)
					teamModel.changeLastScore(Integer.parseInt(value));
				else if (name.equals(Team.EVENT_SCORE) && !isChange)
					teamModel.setScore(Integer.parseInt(value));
				else if (name.equals(Team.EVENT_LAST_SCORE) && !isChange)
					teamModel.setLastScore(Integer.parseInt(value));
				else if (name.equals(Team.EVENT_TIMEOUTS) && isChange)
					teamModel.changeTimeouts(Integer.parseInt(value));
				else if (name.equals(Team.EVENT_TIMEOUTS) && !isChange)
					teamModel.setTimeouts(Integer.parseInt(value));
				else if (name.equals(Team.EVENT_OFFICIAL_REVIEWS) && isChange)
					teamModel.changeOfficialReviews(Integer.parseInt(value));
				else if (name.equals(Team.EVENT_OFFICIAL_REVIEWS) && !isChange)
					teamModel.setOfficialReviews(Integer.parseInt(value));
				else if (name.equals(Team.EVENT_IN_TIMEOUT))
					teamModel.setInTimeout(Boolean.parseBoolean(value));
				else if (name.equals(Team.EVENT_IN_OFFICIAL_REVIEW))
					teamModel.setInOfficialReview(Boolean.parseBoolean(value));
				else if (name.equals(Team.EVENT_RETAINED_OFFICIAL_REVIEW))
					teamModel.setRetainedOfficialReview(Boolean.parseBoolean(value));
				else if (name.equals(Team.EVENT_LEAD_JAMMER))
					teamModel.setLeadJammer(value);
				else if (name.equals(Team.EVENT_STAR_PASS))
					teamModel.setStarPass(Boolean.parseBoolean(value));
			} catch ( Exception e ) {
			}
		}
	}

	public void processAlternateName(TeamModel teamModel, Element alternateName) {
		String id = alternateName.getAttributeValue("Id");
		TeamModel.AlternateNameModel alternateNameModel = teamModel.getAlternateNameModel(id);

		if (editor.hasRemovePI(alternateName)) {
			teamModel.removeAlternateNameModel(id);
			return;
		}

		if (null == alternateNameModel) {
			teamModel.setAlternateNameModel(id, "");
			alternateNameModel = teamModel.getAlternateNameModel(id);
		}

		Iterator children = alternateName.getChildren().iterator();
		while (children.hasNext()) {
			Element element = (Element)children.next();
			try {
				String name = element.getName();
				String value = editor.getText(element);

				if (null == value)
					continue;
				else if (name.equals(Team.AlternateName.EVENT_NAME))
					alternateNameModel.setName(value);
			} catch ( Exception e ) {
			}
		}
	}

	public void processColor(TeamModel teamModel, Element color) {
		String id = color.getAttributeValue("Id");
		TeamModel.ColorModel colorModel = teamModel.getColorModel(id);

		if (editor.hasRemovePI(color)) {
			teamModel.removeColorModel(id);
			return;
		}

		if (null == colorModel) {
			teamModel.setColorModel(id, "");
			colorModel = teamModel.getColorModel(id);
		}

		Iterator children = color.getChildren().iterator();
		while (children.hasNext()) {
			Element element = (Element)children.next();
			try {
				String name = element.getName();
				String value = editor.getText(element);

				if (null == value)
					continue;
				else if (name.equals(Team.Color.EVENT_COLOR))
					colorModel.setColor(value);
			} catch ( Exception e ) {
			}
		}
	}

	public void processPosition(TeamModel teamModel, Element position) {
		String id = position.getAttributeValue("Id");
		PositionModel positionModel = teamModel.getPositionModel(id);

		Iterator children = position.getChildren().iterator();
		while (children.hasNext()) {
			Element element = (Element)children.next();
			try {
				String name = element.getName();
				String value = editor.getText(element);

				if (null == value)
					continue;
				else if (name.equals("Clear") && Boolean.parseBoolean(value))
					positionModel.clear();
				else if (name.equals("Id"))
					positionModel.setSkaterModel(value);
				else if (name.equals(Position.EVENT_PENALTY_BOX))
					positionModel.setPenaltyBox(Boolean.parseBoolean(value));
			} catch ( Exception e ) {
			}
		}
	}

	public void processPolicy(ScoreBoardModel scoreBoardModel, Element policy) throws NoSuchElementException {
		String id = policy.getAttributeValue("Id");
		PolicyModel policyModel = scoreBoardModel.getPolicyModel(id);

		Iterator children = policy.getChildren().iterator();
		while (children.hasNext()) {
			Element element= (Element)children.next();
			try {
				String name = element.getName();
				String value = editor.getText(element);

				if (name.equals("Parameter"))
					processPolicyParameter(policyModel, element);
				else if (null == value)
					continue;
				else if (name.equals(Policy.EVENT_ENABLED))
					policyModel.setEnabled(Boolean.parseBoolean(value));
			} catch ( Exception e ) {
			}
		}
	}

	public void processPolicyParameter(PolicyModel policyModel, Element parameter) throws NoSuchElementException {
		String id = parameter.getAttributeValue("Id");
		PolicyModel.ParameterModel parameterModel = policyModel.getParameterModel(id);

		Iterator children = parameter.getChildren().iterator();
		while (children.hasNext()) {
			Element element = (Element)children.next();
			try {
				String name = element.getName();
				String value = editor.getText(element);

				if (null == value)
					continue;
				else if (name.equals(Policy.Parameter.EVENT_VALUE))
					parameterModel.setValue(value);
			} catch ( Exception e ) {
			}
		}
	}

	public void processSkater(TeamModel teamModel, Element skater) {
		String id = skater.getAttributeValue("Id");
		SkaterModel skaterModel;

		if (editor.hasRemovePI(skater)) {
			teamModel.removeSkaterModel(id);
			return;
		}

		try {
			skaterModel = teamModel.getSkaterModel(id);
		} catch ( SkaterNotFoundException snfE ) {
			Element nameE = skater.getChild(Skater.EVENT_NAME);
			String name = (nameE == null ? "" : editor.getText(nameE));
			Element numberE = skater.getChild(Skater.EVENT_NUMBER);
			String number = (numberE == null ? "" : editor.getText(numberE));
			Element flagsE = skater.getChild(Skater.EVENT_FLAGS);
			String flags = (flagsE == null ? "" : editor.getText(flagsE));
			teamModel.addSkaterModel(id, name, number, flags);
			skaterModel = teamModel.getSkaterModel(id);
		}

		Iterator children = skater.getChildren().iterator();
		while (children.hasNext()) {
			Element element = (Element)children.next();
			try {
				String name = element.getName();
				String value = editor.getText(element);

				boolean isChange = Boolean.parseBoolean(element.getAttributeValue("change"));

				if (name.equals(Skater.EVENT_PENALTY))
					processPenalty(skaterModel, element, false);
				else if (name.equals(Skater.EVENT_PENALTY_FOEXP))
					processPenalty(skaterModel, element.getChild(Skater.EVENT_PENALTY), true);
        else if (null == value)
					continue;
				else if (name.equals(Skater.EVENT_NAME))
					skaterModel.setName(value);
				else if (name.equals(Skater.EVENT_NUMBER))
					skaterModel.setNumber(value);
				else if (name.equals(Skater.EVENT_POSITION))
					skaterModel.setPosition(value);
				else if (name.equals(Skater.EVENT_PENALTY_BOX))
					skaterModel.setPenaltyBox(Boolean.parseBoolean(value));
				else if (name.equals(Skater.EVENT_FLAGS))
					skaterModel.setFlags(value);
			} catch ( Exception e ) {
			}
		}
	}

	public void processPenalty(SkaterModel skaterModel, Element penalty, boolean foulout_exp) {
		String id = penalty.getAttributeValue("Id");
    int period = 0;
    int jam = 0;
    String code = "";

		Iterator children = penalty.getChildren().iterator();
		while (children.hasNext()) {
			Element element = (Element)children.next();
      try {
				String name = element.getName();
				String value = editor.getText(element);

				if (null == value)
					continue;
				else if (name.equals(Skater.EVENT_PENALTY_PERIOD))
					period = Integer.parseInt(value);
				else if (name.equals(Skater.EVENT_PENALTY_JAM))
					jam = Integer.parseInt(value);
				else if (name.equals(Skater.EVENT_PENALTY_CODE))
					code = value;
			} catch ( Exception e ) {
			}
    }
    skaterModel.AddPenaltyModel(null, foulout_exp, period, jam, code);
  }

	public static ScoreBoardXmlConverter getInstance() { return scoreBoardXmlConverter; }

	protected XmlDocumentEditor editor = new XmlDocumentEditor();
	protected XMLOutputter rawXmlOutputter = XmlDocumentEditor.getRawXmlOutputter();

	private static ScoreBoardXmlConverter scoreBoardXmlConverter = new ScoreBoardXmlConverter();
}
