package com.carolinarollergirls.scoreboard.xml;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import org.jdom.Document;
import org.jdom.Element;

import com.carolinarollergirls.scoreboard.Clock;
import com.carolinarollergirls.scoreboard.Policy;
import com.carolinarollergirls.scoreboard.Position;
import com.carolinarollergirls.scoreboard.ScoreBoard;
import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.Skater;
import com.carolinarollergirls.scoreboard.Team;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;
import com.carolinarollergirls.scoreboard.model.SettingsModel;

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

	private void batchStart() {
		Element root = document.getRootElement();
		String b = root.getAttributeValue("BATCH_START");
		if (b == null)
			b = "";
		b = b + "X";
		root.setAttribute("BATCH_START", b);
	}

	private void batchEnd() {
		Element root = document.getRootElement();
		String b = root.getAttributeValue("BATCH_END");
		if (b == null)
			b = "";
		b = b + "X";
		root.setAttribute("BATCH_END", b);
	}

	public void scoreBoardChange(ScoreBoardEvent event) {
		ScoreBoardEventProvider p = event.getProvider();
		String prop = event.getProperty();
		String v = (event.getValue()==null?null:event.getValue().toString());
		if (prop.equals(ScoreBoardEvent.BATCH_START)) {
			batchStart();
		} else if (prop.equals(ScoreBoardEvent.BATCH_END)) {
			batchEnd();
		} else if (p.getProviderName().equals("Settings")) {
			SettingsModel settings = (SettingsModel)p;
			Element e = editor.setElement(getSettingsElement(settings), "Settings");
			if (e != null) {
				if (v == null) {
					if (isPersistent())
						editor.removeElement(e, "Setting", prop);
					else
						editor.setRemovePI(editor.setElement(e, "Setting", prop));
				} else
					editor.setElement(e, "Setting", prop, v);
			} else
				ScoreBoardManager.printMessage("************ ADD SUPPORT FOR SETTINGS TO ScoreBoardXmlListener FOR " + settings.getParent().getProviderName());
		} else if (p.getProviderName().equals("ScoreBoard")) {
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
			} else if (prop.equals(Team.EVENT_ADD_COLOR)) {
				Element e = converter.toElement(getTeamElement((Team)p), (Team.Color)event.getValue());
			} else if (prop.equals(Team.EVENT_REMOVE_COLOR)) {
				if (isPersistent())
					editor.removeElement(getTeamElement((Team)p), "Color", ((Team.Color)event.getValue()).getId());
				else
					editor.setRemovePI(converter.toElement(getTeamElement((Team)p), (Team.Color)event.getValue()));
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
				editor.setElement(e, "PenaltyBox", null, String.valueOf(s==null?false:s.isPenaltyBox()));
				editor.setElement(e, "Flags", null, (s==null?"":s.getFlags()));
			} else if (prop.equals(Position.EVENT_PENALTY_BOX)) {
				editor.setElement(e, "PenaltyBox", null, String.valueOf(event.getValue()));
			}
		} else if (p.getProviderName().equals("AlternateName")) {
			editor.setElement(getAlternateNameElement((Team.AlternateName)p), prop, null, v);
		} else if (p.getProviderName().equals("Color")) {
			editor.setElement(getColorElement((Team.Color)p), prop, null, v);
		} else if (p.getProviderName().equals("Skater")) {
      if (prop.equals(Skater.EVENT_PENALTY) || prop.equals(Skater.EVENT_PENALTY_FOEXP)) {
        // Replace whole skater.
				converter.toElement(getTeamElement(((Skater)p).getTeam()), (Skater)p);
      } else if (prop.equals(Skater.EVENT_REMOVE_PENALTY)) {
        Skater.Penalty prev = (Skater.Penalty)(event.getPreviousValue());
        if (prev != null) {
          if (isPersistent()) {
            editor.removeElement(getSkaterElement((Skater)p), Skater.EVENT_PENALTY, prev.getId());
          } else {
            editor.setRemovePI(editor.addElement(getSkaterElement((Skater)p), Skater.EVENT_PENALTY, prev.getId()));
          }
        }
      } else if (prop.equals(Skater.EVENT_PENALTY_REMOVE_FOEXP)) {
				if (isPersistent()) {
					editor.removeElement(getSkaterElement((Skater)p), Skater.EVENT_PENALTY_FOEXP);
        } else {
          Skater.Penalty prev = (Skater.Penalty)(event.getPreviousValue());
          if (prev != null) {
            editor.setRemovePI(editor.addElement(getSkaterElement((Skater)p), Skater.EVENT_PENALTY_FOEXP, prev.getId()));
          }
        }
      } else {
        editor.setElement(getSkaterElement((Skater)p), prop, null, v);
      }
		} else if (p.getProviderName().equals("Clock")) {
			Element e = editor.setElement(getClockElement((Clock)p), prop, null, v);
			if (prop.equals("Time")) {
				try {
					Clock c = (Clock)p;
					long time = ((Long)event.getValue()).longValue();
					long prevTime = ((Long)event.getPreviousValue()).longValue();
					if (time % 1000 == 0 || Math.abs(prevTime - time) >= 1000)
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

	private Element getSettingsElement(SettingsModel settings) {
		if (settings.getParent().getProviderName().equals("ScoreBoard"))
			return getScoreBoardElement();
		return null;
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

	protected Element getColorElement(Team.Color color) {
		return editor.getElement(getTeamElement(color.getTeam()), "Color", color.getId());
	}

	protected XmlDocumentEditor editor = new XmlDocumentEditor();
	protected ScoreBoardXmlConverter converter = new ScoreBoardXmlConverter();

	protected Document document = editor.createDocument("ScoreBoard");
	protected boolean empty = true;
	protected boolean persistent = false;
}
