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
      event.reflect(this);
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
    if (p.equals("Running") || p.equals("Number"))
      update(editor.addElement(editor.getElement(createXPathElement(), "Clock", c.getId()), p, getStatsTime(), v));
  }

  public void scoreBoardChange(Team t, ScoreBoardEvent event) {
    String p = event.getProperty();
    String v = event.getValue().toString();
    if (p.equals("Score"))
      update(editor.addElement(editor.getElement(createXPathElement(), "Team", t.getId()), p, getStatsTime(), v));
  }

  public void scoreBoardChange(Skater s, ScoreBoardEvent event) {
    String p = event.getProperty();
    String v = event.getValue().toString();
    if (p.equals("Position") || p.equals("LeadJammer") || p.equals("PenaltyBox"))
      update(editor.addElement(editor.getElement(createXPathElement(), "Skater", s.getId()), p, getStatsTime(), v));
  }

  protected Object lock = new Object();
}
