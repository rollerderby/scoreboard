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

public class ConditionalScoreBoardListener implements ScoreBoardListener
{
  public ConditionalScoreBoardListener(ScoreBoardEvent e, ScoreBoardListener l) {
    event = e;
    listener = l;
  }

  public void scoreBoardChange(ScoreBoardEvent e) {
    if (event.equals(e))
      listener.scoreBoardChange(e);
  }

  protected ScoreBoardEvent event;
  protected ScoreBoardListener listener;
}

