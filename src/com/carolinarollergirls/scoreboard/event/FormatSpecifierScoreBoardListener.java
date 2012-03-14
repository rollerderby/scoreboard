package com.carolinarollergirls.scoreboard.event;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;

import com.carolinarollergirls.scoreboard.viewer.*;

public class FormatSpecifierScoreBoardListener extends ConditionalScoreBoardListener implements ScoreBoardListener
{
  public FormatSpecifierScoreBoardListener(FormatSpecifierViewer v, String f, ScoreBoardListener l) throws IllegalArgumentException {
    super(v.getScoreBoardEvent(f), l);
    formatSpecifierViewer = v;
    format = f;
  }

  public String getFormat() { return format; }

  protected boolean checkScoreBoardEvent(ScoreBoardEvent e) {
    return (super.checkScoreBoardEvent(e) && formatSpecifierViewer.checkCondition(getFormat(), e));
  }

  protected FormatSpecifierViewer formatSpecifierViewer;
  protected String format;
}
