package com.carolinarollergirls.scoreboard.xml.policy;

import org.jdom.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.xml.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.policy.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public class PagePolicy_scoreboardhtml extends AbstractClockRunningChangePolicy
{
	public PagePolicy_scoreboardhtml() {
		super("Page(scoreboard.html) Policy");
		setDescription("This controls elements specific to the scoreboard.html page.");

		addParameterModel(new DefaultPolicyModel.DefaultParameterModel(this, RESET_INTERMISSION_CONFIRMED, "Boolean", String.valueOf(true)));

		addClock(Clock.ID_INTERMISSION);
	}

	public void clockRunningChange(Clock clock, boolean running) {
		boolean isIntermission = clock.getId().equals(Clock.ID_INTERMISSION);
		boolean reset = Boolean.parseBoolean(getParameter(RESET_INTERMISSION_CONFIRMED).getValue());
		if (reset && isIntermission && running) {
			try {
				Element pageE = getPageElement();
				String intermissionN = String.valueOf(clock.getNumber());
				Element intermissionE = editor.getElement(pageE, "Intermission", intermissionN, false);
				Element confirmedE = intermissionE.getChild("Confirmed").setText("true");
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
