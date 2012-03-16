package com.carolinarollergirls.scoreboard.xml.policy;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import org.jdom.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.xml.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.policy.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public class PagePolicy_scoreboardhtml extends AbstractClockRunningChangePolicy
{
	public PagePolicy_scoreboardhtml() {
		super();

		addParameterModel(new DefaultPolicyModel.DefaultParameterModel(this, RESET_INTERMISSION_CONFIRMED, "Boolean", String.valueOf(true)));
	}

  public void setScoreBoardModel(ScoreBoardModel sbm) {
    super.setScoreBoardModel(sbm);
		addClock(Clock.ID_INTERMISSION);
  }

  public void reset() {
    super.reset();
		setName("Page(scoreboard.html) Policy");
		setDescription("This controls elements specific to the scoreboard.html page.");
  }

	public void clockRunningChange(Clock clock, boolean running) {
		boolean isIntermission = clock.getId().equals(Clock.ID_INTERMISSION);
		boolean reset = Boolean.parseBoolean(getParameter(RESET_INTERMISSION_CONFIRMED).getValue());
		if (reset && isIntermission && running) {
			try {
				Element pageE = getPageElement();
				String intermissionN = String.valueOf(clock.getNumber());
				Element intermissionE = editor.getElement(pageE, "Intermission", intermissionN, false);
				Element confirmedE = editor.setText(intermissionE.getChild("Confirmed"), "false");
				getScoreBoardModel().getXmlScoreBoard().mergeElement(confirmedE);
			} catch ( Exception e ) {
				/* Ignore?  probably no existing element for current Intermission... */
			}
		}
	}

	protected Element getPageElement() throws Exception {
		Element pages = getScoreBoardModel().getXmlScoreBoard().getDocument().getRootElement().getChild("Pages");
		return editor.getElement(pages, "Page", "scoreboard.html", false);
	}

	protected XmlDocumentEditor editor = new XmlDocumentEditor();

	public static final String RESET_INTERMISSION_CONFIRMED = "Reset Intermission Confirmed";
}
