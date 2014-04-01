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

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;

/**
 * Converts a ScoreBoardEvent into a representative XML Document or XML String.
 *
 * This class is not synchronized.	Each event method modifies the same document.
 */
public class ScoreBoardXmlListener implements ScoreBoardListener
{
	public ScoreBoardXmlListener() { }
	public ScoreBoardXmlListener(boolean p) {
		setPersistent(p);
	}
	public ScoreBoardXmlListener(ScoreBoard sb, boolean p) {
		setPersistent(p);
		sb.addScoreBoardListener(this);
	}
	public ScoreBoardXmlListener(ScoreBoard sb) {
		sb.addScoreBoardListener(this);
	}

	public boolean isEmpty() { return empty; }

	public Document getDocument() { return document; }

	public Document resetDocument() {
		Document oldDoc = document;
		empty = true;
		document = editor.createDocument("ScoreBoard");
		return oldDoc;
	}

	public void scoreBoardChange(ScoreBoardEvent event) {
		ScoreBoardEventProvider p = event.getProvider();
		String prop = event.getProperty();
		String v = (event.getValue()==null?null:event.getValue().toString());
		if (p.getProviderName().equals("ScoreBoard")) {
			if (prop.equals(ScoreBoard.EVENT_ADD_CLOCK)) {
				converter.toElement(getScoreBoardElement(), (Clock)event.getValue());
			} else if (prop.equals(ScoreBoard.EVENT_REMOVE_CLOCK)) {
				if (isPersistent())
					editor.removeElement(getScoreBoardElement(), "Clock", ((Clock)event.getValue()).getId());
				else
					editor.setRemovePI(converter.toElement(getScoreBoardElement(), (Clock)event.getValue()));
			} else if (prop.equals(ScoreBoard.EVENT_ADD_TEAM)) {
				converter.toElement(getScoreBoardElement(), (Team)event.getValue());
			} else if (prop.equals(ScoreBoard.EVENT_REMOVE_TEAM)) {
				if (isPersistent())
					editor.removeElement(getScoreBoardElement(), "Team", ((Team)event.getValue()).getId());
				else
					editor.setRemovePI(converter.toElement(getScoreBoardElement(), (Team)event.getValue()));
			} else if (prop.equals(ScoreBoard.EVENT_ADD_POLICY)) {
				converter.toElement(getScoreBoardElement(), (Policy)event.getValue());
			} else if (prop.equals(ScoreBoard.EVENT_REMOVE_POLICY)) {
				if (isPersistent())
					editor.removeElement(getScoreBoardElement(), "Policy", ((Policy)event.getValue()).getId());
				else
					editor.setRemovePI(converter.toElement(getScoreBoardElement(), (Policy)event.getValue()));
			} else {
				editor.setElement(getScoreBoardElement(), prop, null, v);
			}
		} else if (p.getProviderName().equals("Team")) {
			if (prop.equals(Team.EVENT_ADD_ALTERNATE_NAME)) {
				Element e = converter.toElement(getTeamElement((Team)p), (Team.AlternateName)event.getValue());
			} else if (prop.equals(Team.EVENT_REMOVE_ALTERNATE_NAME)) {
				if (isPersistent())
					editor.removeElement(getTeamElement((Team)p), "AlternateName", ((Team.AlternateName)event.getValue()).getId());
				else
					editor.setRemovePI(converter.toElement(getTeamElement((Team)p), (Team.AlternateName)event.getValue()));
			} else if (prop.equals(Team.EVENT_ADD_SKATER)) {
				Element e = converter.toElement(getTeamElement((Team)p), (Skater)event.getValue());
			} else if (prop.equals(Team.EVENT_REMOVE_SKATER)) {
				if (isPersistent())
					editor.removeElement(getTeamElement((Team)p), "Skater", ((Skater)event.getValue()).getId());
				else
					editor.setRemovePI(converter.toElement(getTeamElement((Team)p), (Skater)event.getValue()));
			} else {
				editor.setElement(getTeamElement((Team)p), prop, null, v);
			}
		} else if (p.getProviderName().equals("Position")) {
			Element e = getPositionElement((Position)p);
			if (prop.equals(Position.EVENT_SKATER)) {
				Skater s = (Skater)event.getValue();
				editor.setElement(e, "Id", null, (s==null?"":s.getId()));
				editor.setElement(e, "Name", null, (s==null?"":s.getName()));
				editor.setElement(e, "Number", null, (s==null?"":s.getNumber()));
			}
		} else if (p.getProviderName().equals("AlternateName")) {
			editor.setElement(getAlternateNameElement((Team.AlternateName)p), prop, null, v);
		} else if (p.getProviderName().equals("Skater")) {
			editor.setElement(getSkaterElement((Skater)p), prop, null, v);
		} else if (p.getProviderName().equals("Clock")) {
			Element e = editor.setElement(getClockElement((Clock)p), prop, null, v);
			if (prop.equals("Time")) {
				try {
					Clock c = (Clock)p;
					long time = ((Long)event.getValue()).longValue();
					long prevTime = ((Long)event.getPreviousValue()).longValue();
					if (time/1000 != prevTime/1000 || c.isTimeAtStart(time) || c.isTimeAtEnd(time))
						editor.setPI(e, "TimeUpdate", "sec");
					else
						editor.setPI(e, "TimeUpdate", "ms");
				} catch (Exception ee) { }
			}
		} else if (p.getProviderName().equals("Policy")) {
			editor.setElement(getPolicyElement((Policy)p), prop, null, v);
		} else if (p.getProviderName().equals("Parameter")) {
			editor.setElement(getPolicyParameterElement((Policy.Parameter)p), prop, null, v);
		} else {
			return;
		}
		empty = false;
	}

	public boolean isPersistent() { return persistent; }
	public void setPersistent(boolean p) { persistent = p; }

	protected Element getScoreBoardElement() {
		return editor.getElement(document.getRootElement(), "ScoreBoard");
	}

	protected Element getPolicyElement(Policy policy) {
		return editor.getElement(getScoreBoardElement(), "Policy", policy.getId());
	}

	protected Element getPolicyParameterElement(Policy.Parameter parameter) {
		return editor.getElement(getPolicyElement(parameter.getPolicy()), "Parameter", parameter.getName());
	}

	protected Element getClockElement(Clock clock) {
		return editor.getElement(getScoreBoardElement(), "Clock", clock.getId());
	}

	protected Element getTeamElement(Team team) {
		return editor.getElement(getScoreBoardElement(), "Team", team.getId());
	}

	protected Element getPositionElement(Position position) {
		return editor.getElement(getTeamElement(position.getTeam()), "Position", position.getId());
	}

	protected Element getSkaterElement(Skater skater) {
		return editor.getElement(getTeamElement(skater.getTeam()), "Skater", skater.getId());
	}

	protected Element getAlternateNameElement(Team.AlternateName alternateName) {
		return editor.getElement(getTeamElement(alternateName.getTeam()), "AlternateName", alternateName.getId());
	}

	protected XmlDocumentEditor editor = new XmlDocumentEditor();
	protected ScoreBoardXmlConverter converter = new ScoreBoardXmlConverter();

	protected Document document = editor.createDocument("ScoreBoard");
	protected boolean empty = true;
	protected boolean persistent = false;

}
