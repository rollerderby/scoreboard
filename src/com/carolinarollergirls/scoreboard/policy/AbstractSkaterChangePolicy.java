package com.carolinarollergirls.scoreboard.policy;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public abstract class AbstractSkaterChangePolicy extends DefaultPolicyModel
{
  public AbstractSkaterChangePolicy(String id, String desc) { super(id, desc); }
  public AbstractSkaterChangePolicy(String id, String name, String desc) { super(id, name, desc); }

  protected void addSkaterProperty(String p) {
    getScoreBoard().addScoreBoardListener(new ConditionalScoreBoardListener(Skater.class, p, scoreBoardListener));
  }

  protected abstract void skaterChange(Skater skater, Object value);

  protected ScoreBoardListener scoreBoardListener = new ScoreBoardListener() {
      public void scoreBoardChange(ScoreBoardEvent event) {
        if (isEnabled()) {
          synchronized (changeLock) {
            skaterChange((Skater)event.getProvider(), event.getValue());
          }
        }
      }
    };

  protected Object changeLock = new Object();
}
