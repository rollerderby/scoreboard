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

public class XmlRealtimeStats extends XmlStats
{
	public XmlRealtimeStats() { super("Realtime"); }

	public void scoreBoardChange(ScoreBoardEvent event) {
		synchronized (lock) {
			try { scoreBoardChange((Clock)event.getProvider(), event); }
			catch ( ClassCastException ccE ) { }
			try { scoreBoardChange((Team)event.getProvider(), event); }
			catch ( ClassCastException ccE ) { }
			try { scoreBoardChange((Skater)event.getProvider(), event); }
			catch ( ClassCastException ccE ) { }
		}
	}

	public void reset() {
		synchronized (lock) {
			super.reset();
		}
	}

	public void scoreBoardChange(Clock c, ScoreBoardEvent event) {
		String p = event.getProperty();
		String v = event.getValue().toString();
		if (p.equals(Clock.EVENT_RUNNING) || p.equals(Clock.EVENT_NUMBER))
			update(editor.addElement(editor.getElement(createXPathElement(), "Clock", c.getId()), p, getStatsTime(), v));
	}

	public void scoreBoardChange(Team t, ScoreBoardEvent event) {
		String p = event.getProperty();
		String v = event.getValue().toString();
		if (p.equals(Team.EVENT_SCORE))
			update(editor.addElement(editor.getElement(createXPathElement(), "Team", t.getId()), p, getStatsTime(), v));
	}

	public void scoreBoardChange(Skater s, ScoreBoardEvent event) {
		String p = event.getProperty();
		String v = event.getValue().toString();
		if (p.equals(Skater.EVENT_POSITION) || p.equals(Skater.EVENT_LEAD_JAMMER) || p.equals(Skater.EVENT_PENALTY_BOX))
			update(editor.addElement(editor.getElement(createXPathElement(), "Skater", s.getId()), p, getStatsTime(), v));
	}

	protected Object lock = new Object();
}
