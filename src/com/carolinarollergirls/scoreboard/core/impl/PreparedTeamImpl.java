package com.carolinarollergirls.scoreboard.core.impl;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */


import com.carolinarollergirls.scoreboard.core.PreparedTeam;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;

public class PreparedTeamImpl extends ScoreBoardEventProviderImpl implements PreparedTeam {
  PreparedTeamImpl(ScoreBoard parent, String id) {
    super(parent, Value.ID, id, ScoreBoard.Child.PREPARED_TEAM, PreparedTeam.class, Value.class, Child.class);
  }

  @Override
  public ValueWithId create(AddRemoveProperty prop, String id) {
    synchronized (coreLock) {
      if (prop == PreparedTeam.Child.SKATER) {
        return new PreparedTeamSkaterImpl(this, id);
      }
      return null;
    }
  }
  public class PreparedTeamSkaterImpl extends ScoreBoardEventProviderImpl implements PreparedTeamSkater {
    PreparedTeamSkaterImpl(PreparedTeam parent, String id) {
      super(parent, Value.ID, id, PreparedTeam.Child.SKATER, PreparedTeamSkater.class, Value.class);
    }
  }
}


