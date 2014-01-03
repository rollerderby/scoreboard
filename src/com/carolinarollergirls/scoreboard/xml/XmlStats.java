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
import com.carolinarollergirls.scoreboard.model.*;

public abstract class XmlStats extends SegmentedXmlDocumentManager implements ScoreBoardListener
{
	public XmlStats(String n) { super("Stats", n); }

	public void setXmlScoreBoard(XmlScoreBoard xsB) {
		scoreBoard = xsB.getScoreBoardModel();
		super.setXmlScoreBoard(xsB);
		scoreBoard.addScoreBoardListener(this);
	}

	public void reset() {
		super.reset();
		statsStartTime = new Date().getTime();
		update(createXPathElement().addContent(editor.setText(new Element("Start"), Long.toString(statsStartTime))));
	}

	public abstract void scoreBoardChange(ScoreBoardEvent event);

	protected String getStatsTime() { return Long.toString(new Date().getTime() - statsStartTime); }

	protected ScoreBoard scoreBoard;
	protected long statsStartTime;
}
