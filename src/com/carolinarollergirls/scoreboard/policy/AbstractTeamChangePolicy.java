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

public abstract class AbstractTeamChangePolicy extends DefaultPolicyModel
{
  public AbstractTeamChangePolicy(String id, String desc) { super(id, desc); }
  public AbstractTeamChangePolicy(String id, String name, String desc) { super(id, name, desc); }

  protected void addTeamProperty(String id, String p) {
    getScoreBoard().addScoreBoardListener(new ConditionalScoreBoardListener(Team.class, id, p, scoreBoardListener));
  }

  protected abstract void teamChange(Team team, Object value);

  protected ScoreBoardListener scoreBoardListener = new ScoreBoardListener() {
      public void scoreBoardChange(ScoreBoardEvent event) {
        if (isEnabled()) {
          synchronized (changeLock) {
            teamChange((Team)event.getProvider(), event.getValue());
          }
        }
      }
    };

  protected Object changeLock = new Object();
}
