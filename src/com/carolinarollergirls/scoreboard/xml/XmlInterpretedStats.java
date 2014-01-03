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
import com.carolinarollergirls.scoreboard.event.*;

public class XmlInterpretedStats extends XmlStats
{
	public XmlInterpretedStats() {
		super("Interpreted");
	}

	public void reset() {
		synchronized (lock) {
			super.reset();

			periodNumber = scoreBoard.getClock(Clock.ID_PERIOD).getNumber();
			periodRunning = scoreBoard.getClock(Clock.ID_PERIOD).isRunning();
			jamNumber = scoreBoard.getClock(Clock.ID_JAM).getNumber();
			jamRunning = scoreBoard.getClock(Clock.ID_JAM).isRunning();
			timeoutNumber = scoreBoard.getClock(Clock.ID_TIMEOUT).getNumber();
			timeoutRunning = scoreBoard.getClock(Clock.ID_TIMEOUT).isRunning();
			Iterator<Team> teams = scoreBoard.getTeams().iterator();
			while (teams.hasNext())
				passNumber.put(teams.next().getId(), 0);

			states.put(PERIOD_LISTENER, (periodRunning?periodRunningState:periodNotRunningState));
			states.put(JAM_LISTENER, (jamRunning?jamRunningState:jamNotRunningState));
			states.put(TIMEOUT_LISTENER, (timeoutRunning?timeoutRunningState:timeoutNotRunningState));
			states.put(POSITION_LISTENER, positionListener);
			states.put(PASS_LISTENER, passListener);
			states.put(SCORE_LISTENER, scoreListener);
		}
	}

	public void scoreBoardChange(ScoreBoardEvent event) {
		synchronized (lock) {
			Iterator<String> s = states.keySet().iterator();
			while (s.hasNext())
				try { states.get(s.next()).scoreBoardChange(event); } catch ( Exception e ) { }
		}
	}

	protected Element getPeriodStats() {
		return editor.getElement(createXPathElement(), "Period", String.valueOf(periodNumber));
	}
	protected Element getJamStats() {
		return editor.getElement(getPeriodStats(), "Jam", String.valueOf(jamNumber));
	}
	protected Element getTimeoutStats() {
		return editor.getElement(getPeriodStats(), "Timeout", String.valueOf(timeoutNumber));
	}
	protected Element getTeamStats(Team team) {
		return editor.getElement(getJamStats(), "Team", team.getId());
	}
	protected Element getTeamPassStats(Team team, int pass) {
		return editor.getElement(getTeamStats(team), "Pass", String.valueOf(pass));
	}

	protected int getPassNumber(Team team) {
		try { return ((Integer)passNumber.get(team.getId())).intValue(); }
		catch ( Exception e ) { return 0; }
	}

	protected Element getJam() throws JDOMException { return getJam(jamNumber); }
	protected Element getJam(int jam) throws JDOMException {
		return (Element)XPath.selectSingleNode(getXPathElement(), "Period/Jam[@Id='"+jam+"']");
	}
	protected List<Element> getPreviousJams() throws JDOMException { return getPreviousJams(jamNumber); }
	@SuppressWarnings(value={"unchecked"})
	protected List<Element> getPreviousJams(int jam) throws JDOMException {
		return XPath.selectNodes(getXPathElement(), "Period/Jam[@Id<'"+jam+"']");
	}
	protected Element getPreviousJam() throws JDOMException,NoSuchElementException { return getPreviousJam(jamNumber); }
	protected Element getPreviousJam(int jam) throws JDOMException,NoSuchElementException {
		return (Element)Collections.max(getPreviousJams(jam), idNumberComparator);
	}
	protected List<Element> getNextJams() throws JDOMException { return getPreviousJams(jamNumber); }
	@SuppressWarnings(value={"unchecked"})
	protected List<Element> getNextJams(int jam) throws JDOMException {
		return XPath.selectNodes(getXPathElement(), "Period/Jam[@Id>'"+jam+"']");
	}
	protected Element getNextJam() throws JDOMException,NoSuchElementException { return getNextJam(jamNumber); }
	protected Element getNextJam(int jam) throws JDOMException,NoSuchElementException {
		return (Element)Collections.min(getNextJams(jam), idNumberComparator);
	}
	protected Element getTeam(Team team) throws JDOMException { return getTeam(getJam(), team); }
	protected Element getTeam(Team team, int jam) throws JDOMException { return getTeam(getJam(jam), team); }
	protected Element getTeam(Element jam, Team team) throws JDOMException {
		return (Element)XPath.selectSingleNode(jam, "Team[@Id='"+team.getId()+"']");
	}
	protected Element getPass(Team team, int pass) throws JDOMException { return getPass(getJam(), team, pass); }
	protected Element getPass(Team team, int jam, int pass) throws JDOMException { return getPass(getJam(jam), team, pass); }
	protected Element getPass(Element jam, Team team, int pass) throws JDOMException {
		return (Element)XPath.selectSingleNode(jam, "Team[@Id='"+team.getId()+"']/Pass[@Id='"+pass+"']");
	}
	protected List<Element> getPreviousPasses(Team team) throws JDOMException { return getPreviousPasses(team, getPassNumber(team)); }
	protected List<Element> getPreviousPasses(Team team, int pass) throws JDOMException { return getPreviousPasses(team, jamNumber, pass); }
	@SuppressWarnings(value={"unchecked"})
	protected List<Element> getPreviousPasses(Team team, int jam, int pass) throws JDOMException {
		List passes = XPath.selectNodes(getTeam(getJam(jam), team), "Pass[@Id<'"+pass+"']");
		if (!passes.isEmpty())
			return passes;
		else
			return XPath.selectNodes(getTeam(getPreviousJam(jam), team), "Pass");
	}
	protected Element getPreviousPass(Team team) throws JDOMException,NoSuchElementException { return getPreviousPass(team, getPassNumber(team)); }
	protected Element getPreviousPass(Team team, int pass) throws JDOMException,NoSuchElementException { return getPreviousPass(team, jamNumber, pass); }
	protected Element getPreviousPass(Team team, int jam, int pass) throws JDOMException,NoSuchElementException {
		return (Element)Collections.max(getPreviousPasses(team, jam, pass), idNumberComparator);
	}
	protected List<Element> getNextPasses(Team team) throws JDOMException { return getNextPasses(team, getPassNumber(team)); }
	protected List<Element> getNextPasses(Team team, int pass) throws JDOMException { return getNextPasses(team, jamNumber, pass); }
	@SuppressWarnings(value={"unchecked"})
	protected List<Element> getNextPasses(Team team, int jam, int pass) throws JDOMException {
		List passes = XPath.selectNodes(getTeam(getJam(jam), team), "Pass[@Id>'"+pass+"']");
		if (!passes.isEmpty())
			return passes;
		else
			return XPath.selectNodes(getTeam(getNextJam(jam), team), "Pass");
	}
	protected Element getNextPass(Team team) throws JDOMException,NoSuchElementException { return getNextPass(team, getPassNumber(team)); }
	protected Element getNextPass(Team team, int pass) throws JDOMException,NoSuchElementException { return getNextPass(team, jamNumber, pass); }
	protected Element getNextPass(Team team, int jam, int pass) throws JDOMException,NoSuchElementException {
		return (Element)Collections.max(getNextPasses(team, jam, pass), idNumberComparator);
	}

	protected void addTeamScore(Team team) {
		// From this method, only updateTeamScore if there isn't already a score there.
		try { editor.getText(getTeam(team).getChild("Score")); }
		catch ( Exception e ) { updateTeamScore(team, team.getScore()); }
	}
	protected void updateTeamScore(Team team, int score) { updateTeamScore(team, jamNumber, score); }
	protected void updateTeamScore(Team team, int jam, int score) {
		Element e = getTeamStats(team);
		editor.setElement(e, "Score", null, String.valueOf(score));
		update(e);
		updateTeamPoints(team, jam, score);
		updateTeamPassScore(team, score);
	}
	protected void updateTeamPoints(Team team, int jam, int score) {
		Element e = getTeamStats(team);
		try { score -= Integer.parseInt(editor.getText(getTeam(getPreviousJam(jam), team).getChild("Score"))); }
		catch ( JDOMException jE ) { }
		catch ( NoSuchElementException nseE ) { }
		editor.setElement(e, "Points", null, String.valueOf(score));
		update(e);
	}

	protected void addTeamPass(Team team) { addTeamPass(team, getPassNumber(team)); }
	protected void addTeamPass(Team team, int pass) {
		// From this method, only updateTeamPassScore if there isn't already a score there.
		try { editor.getText(getPass(team, pass).getChild("Score")); }
		catch ( Exception e ) { updateTeamPassScore(team, pass, team.getScore()); }
	}
	protected void updateTeamPassScore(Team team, int score) { updateTeamPassScore(team, getPassNumber(team), score); }
	protected void updateTeamPassScore(Team team, int pass, int score) { updateTeamPassScore(team, jamNumber, pass, score); }
	protected void updateTeamPassScore(Team team, int jam, int pass, int score) {
		Element e = getTeamPassStats(team, pass);
		editor.setElement(e, "Score", null, String.valueOf(score));
		update(e);
		updateTeamPassPoints(team, jam, pass, score);
	}
	protected void updateTeamPassPoints(Team team, int jam, int pass, int score) {
		Element e = getTeamPassStats(team, pass);
		try { score -= Integer.parseInt(editor.getText(getPreviousPass(team, jam, pass).getChild("Score"))); }
		catch ( JDOMException jE ) { }
		catch ( NoSuchElementException nseE ) { }
		editor.setElement(e, "Points", null, String.valueOf(score));
		update(e);
	}

	protected void addSkaterPositions() {
		addTeamSkaterPositions(scoreBoard.getTeam(Team.ID_1));
		addTeamSkaterPositions(scoreBoard.getTeam(Team.ID_2));
	}
	protected void addTeamSkaterPositions(Team team) {
		Iterator<Position> positions = team.getPositions().iterator();
		while (positions.hasNext())
			addTeamSkaterPosition(team, positions.next());
	}
	protected void addTeamSkaterPosition(Team team, Position position) {
		if (!scoreBoard.getClock(Clock.ID_JAM).isRunning())
			return;
		Element e = editor.getElement(getTeamStats(team), "Position", position.getId());
		Skater skater = position.getSkater();
		if (null != skater) {
			editor.setElement(e, "Id", null, skater.getId());
			editor.setElement(e, "Name", null, skater.getName());
			editor.setElement(e, "Number", null, skater.getNumber());
		} else if (scoreBoard.getClock(Clock.ID_JAM).isRunning()) {
			editor.setElement(e, "Id", null, "");
			editor.setElement(e, "Name", null, "");
			editor.setElement(e, "Number", null, "");
		}
		update(e);
	}

	protected void processElement(Element e) throws Exception {
		super.processElement(e);
		Iterator i = positionXPath.selectNodes(e).iterator();
		while (i.hasNext())
			try { processPosition((Element)i.next()); } catch ( Exception e2 ) { }
	}

	protected void processPosition(Element position) {
		Team team = scoreBoard.getTeam(position.getParentElement().getAttributeValue("Id"));
		Iterator ids = position.getChildren("Id").iterator();
		while (ids.hasNext()) {
			String id = editor.getText((Element)ids.next());
			if ("".equals(id))
				processPositionSkater(position, null);
			else
				try { processPositionSkater(position, team.getSkater(id)); } catch ( Exception e ) { }
		}
		Iterator names = position.getChildren("Name").iterator();
		while (names.hasNext()) {
			Element nameE = (Element)names.next();
			String name = editor.getText(nameE);
			boolean manual = Boolean.parseBoolean(nameE.getAttributeValue("manual"));
			boolean clearOnEmpty = Boolean.parseBoolean(nameE.getAttributeValue("clearOnEmpty"));
			if (manual) {
				processPositionSkater(position, "", name, null);
			} else if (clearOnEmpty && "".equals(name)) {
				processPositionSkater(position, null);
			} else {
				Iterator<Skater> skaters = team.getSkaters().iterator();
				while (skaters.hasNext()) {
					Skater skater = skaters.next();
					if (name.equals(skater.getName())) {
						processPositionSkater(position, skater);
						break;
					}
				}
			}
		}
		Iterator numbers = position.getChildren("Number").iterator();
		while (numbers.hasNext()) {
			Element numberE = (Element)numbers.next();
			String number = editor.getText(numberE);
			boolean manual = Boolean.parseBoolean(numberE.getAttributeValue("manual"));
			boolean clearOnEmpty = Boolean.parseBoolean(numberE.getAttributeValue("clearOnEmpty"));
			if (manual) {
				processPositionSkater(position, "", null, number);
			} else if (clearOnEmpty && "".equals(number)) {
				processPositionSkater(position, null);
			} else {
				Iterator<Skater> skaters = team.getSkaters().iterator();
				while (skaters.hasNext()) {
					Skater skater = skaters.next();
					if (number.equals(skater.getNumber())) {
						processPositionSkater(position, skater);
						break;
					}
				}
			}
		}
	}

	protected void processPositionSkater(Element position, Skater skater) {
		if (skater == null)
			processPositionSkater(position, "", "", "");
		else
			processPositionSkater(position, skater.getId(), skater.getName(), skater.getNumber());
	}

	protected void processPositionSkater(Element e, String id, String name, String number) {
		String position = e.getAttributeValue("Id");
		Element teamE = e.getParentElement();
		String team = teamE.getAttributeValue("Id");
		Element jamE = teamE.getParentElement();
		String jam = jamE.getAttributeValue("Id");
		Element periodE = jamE.getParentElement();
		String period = periodE.getAttributeValue("Id");
		Element newPeriod = editor.setElement(createXPathElement(), "Period", period);
		Element newJam = editor.setElement(newPeriod, "Jam", jam);
		Element newTeam = editor.setElement(newJam, "Team", team);
		Element newPosition = editor.setElement(newTeam, "Position", position);
		if (null != id)
			editor.setElement(newPosition, "Id", null, id);
		if (null != name)
			editor.setElement(newPosition, "Name", null, name);
		if (null != number)
			editor.setElement(newPosition, "Number", null, number);
		update(newPosition);
	}

	protected XPath positionXPath = editor.createXPath("Period/Jam/Team/Position");

	protected Map<String,ScoreBoardListener> states = new LinkedHashMap<String,ScoreBoardListener>();

	/* Period state */
	protected static final String PERIOD_LISTENER = "periodListener";
	protected int periodNumber;
	protected boolean periodRunning;
	protected ScoreBoardListener periodNotRunningState =
		new ConditionalScoreBoardListener(ScoreBoard.class, "", ScoreBoard.EVENT_IN_PERIOD, Boolean.TRUE, null) {
			public void matchedScoreBoardChange(ScoreBoardEvent event) {
				periodNumber = scoreBoard.getClock(Clock.ID_PERIOD).getNumber();
				periodRunning = true;
				update(editor.setElement(getPeriodStats(), "Start", null, getStatsTime()));
				states.put(PERIOD_LISTENER, periodRunningState);
			}
		};
	protected ScoreBoardListener periodRunningState =
		new ConditionalScoreBoardListener(ScoreBoard.class, "", ScoreBoard.EVENT_IN_PERIOD, Boolean.FALSE, null) {
			public void matchedScoreBoardChange(ScoreBoardEvent event) {
				periodRunning = false;
				update(editor.setElement(getPeriodStats(), "Stop", null, getStatsTime()));
				states.put(PERIOD_LISTENER, periodNotRunningState);
			}
		};

	/* Jam state */
	protected static final String JAM_LISTENER = "jamListener";
	protected int jamNumber;
	protected boolean jamRunning;
	protected ScoreBoardListener jamNotRunningState =
		new ConditionalScoreBoardListener(Clock.class, Clock.ID_JAM, Clock.EVENT_RUNNING, Boolean.TRUE, null) {
			public void matchedScoreBoardChange(ScoreBoardEvent event) { 
				Clock c = (Clock)event.getProvider();
				jamNumber = c.getNumber();
				jamRunning = true;
				update(editor.setElement(getJamStats(), "Start", null, getStatsTime()));
				addSkaterPositions();
				Iterator<Team> teams = scoreBoard.getTeams().iterator();
				while (teams.hasNext()) {
					Team team = teams.next();
					passNumber.put(team.getId(), 0);
					addTeamScore(team);
				}
				states.put(JAM_LISTENER, jamRunningState);
			}
		};
	protected ScoreBoardListener jamRunningState =
		new ConditionalScoreBoardListener(Clock.class, Clock.ID_JAM, Clock.EVENT_RUNNING, Boolean.FALSE, null) {
			public void matchedScoreBoardChange(ScoreBoardEvent event) { 
				Clock c = (Clock)event.getProvider();
				jamRunning = false;
				update(editor.setElement(getJamStats(), "Stop", null, getStatsTime()));
				states.put(JAM_LISTENER, jamNotRunningState);
			}
		};

	/* Timeout state */
	protected static final String TIMEOUT_LISTENER = "timeoutListener";
	protected int timeoutNumber;
	protected boolean timeoutRunning;
	protected ScoreBoardListener timeoutNotRunningState =
		new ConditionalScoreBoardListener(Clock.class, Clock.ID_TIMEOUT, Clock.EVENT_RUNNING, Boolean.TRUE, null) {
			public void matchedScoreBoardChange(ScoreBoardEvent event) { 
				Clock c = (Clock)event.getProvider();
				timeoutNumber = c.getNumber();
				timeoutRunning = true;
				update(editor.setElement(getTimeoutStats(), "Start", null, getStatsTime()));
				states.put(TIMEOUT_LISTENER, timeoutRunningState);
			}
		};
	protected ScoreBoardListener timeoutRunningState =
		new ConditionalScoreBoardListener(Clock.class, Clock.ID_TIMEOUT, Clock.EVENT_RUNNING, Boolean.FALSE, null) {
			public void matchedScoreBoardChange(ScoreBoardEvent event) { 
				Clock c = (Clock)event.getProvider();
				timeoutRunning = false;
				update(editor.setElement(getTimeoutStats(), "Stop", null, getStatsTime()));
				states.put(TIMEOUT_LISTENER, timeoutNotRunningState);
			}
		};

	protected static final String POSITION_LISTENER = "positionListener";
	protected ScoreBoardListener positionListener = 
		new ConditionalScoreBoardListener(Position.class, ScoreBoardCondition.ANY_ID, Position.EVENT_SKATER, null) {
			public void matchedScoreBoardChange(ScoreBoardEvent event) {
				Position position = (Position)event.getProvider();
				addTeamSkaterPosition(position.getTeam(), position);
			}
		};

	protected static final String PASS_LISTENER = "passListener";
	protected Map<String,Integer> passNumber = new HashMap<String,Integer>();
	protected ScoreBoardListener passListener = 
		new ConditionalScoreBoardListener(Team.class, ScoreBoardCondition.ANY_ID, Team.EVENT_PASS, null) {
			public void matchedScoreBoardChange(ScoreBoardEvent event) {
				if (jamRunning) {
					Team team = (Team)event.getProvider();
					Integer pass = (Integer)event.getValue();
					passNumber.put(team.getId(), pass);
					addTeamPass(team, pass.intValue());
				}
			}
		};

	protected static final String SCORE_LISTENER = "scoreListener";
	protected ScoreBoardListener scoreListener = 
		new ConditionalScoreBoardListener(Team.class, ScoreBoardCondition.ANY_ID, Team.EVENT_SCORE, null) {
			public void matchedScoreBoardChange(ScoreBoardEvent event) {
				Team team = (Team)event.getProvider();
				int score = ((Integer)event.getValue()).intValue();
				updateTeamScore(team, score);
			}
		};


	public static final Comparator<Element> idNumberComparator = new Comparator<Element>() {
		public int compare(Element e1, Element e2) {
			return (Integer.parseInt(e1.getAttributeValue("Id")) - Integer.parseInt(e2.getAttributeValue("Id")));
		}
	};

	protected Object lock = new Object();
}
