package com.carolinarollergirls.scoreboard.policy;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public abstract class AbstractScoreBoardChangePolicy extends DefaultPolicyModel
{
  public AbstractScoreBoardChangePolicy(String id, String desc) { super(id, desc); }
  public AbstractScoreBoardChangePolicy(String id, String name, String desc) { super(id, name, desc); }

  protected void addScoreBoardProperty(String p) {
    getScoreBoard().addScoreBoardListener(new ConditionalScoreBoardListener(getScoreBoard(), p, scoreBoardListener));
  }

  protected abstract void scoreBoardChange(ScoreBoard sb, Object v);

  protected ScoreBoardListener scoreBoardListener = new ScoreBoardListener() {
      public void scoreBoardChange(ScoreBoardEvent event) {
        if (isEnabled()) {
          synchronized (changeLock) {
            AbstractScoreBoardChangePolicy.this.scoreBoardChange((ScoreBoard)event.getProvider(), event.getValue());
          }
        }
      }
    };

  protected Object changeLock = new Object();
}
