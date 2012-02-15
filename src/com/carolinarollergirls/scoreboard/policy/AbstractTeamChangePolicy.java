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
  public AbstractTeamChangePolicy() {
    super();
  }
  public AbstractTeamChangePolicy(String id) {
    super(id);
  }

  protected void addTeamProperty(String id, String p) {
    filterScoreBoardListener.addProperty(Team.class, id, p);
  }

  public void setScoreBoardModel(ScoreBoardModel sbM) {
    super.setScoreBoardModel(sbM);
    sbM.addScoreBoardListener(filterScoreBoardListener);
  }

  protected abstract void teamChange(Team team, Object value);

  protected FilterScoreBoardListener filterScoreBoardListener = new FilterScoreBoardListener() {
      public void filteredScoreBoardChange(ScoreBoardEvent event) {
        if (isEnabled()) {
          synchronized (changeLock) {
            teamChange((Team)event.getProvider(), event.getValue());
          }
        }
      }
    };

  protected Object changeLock = new Object();
}
