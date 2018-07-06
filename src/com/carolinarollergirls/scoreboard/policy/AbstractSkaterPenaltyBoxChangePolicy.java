package com.carolinarollergirls.scoreboard.policy;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.Skater;
import com.carolinarollergirls.scoreboard.model.ScoreBoardModel;

public abstract class AbstractSkaterPenaltyBoxChangePolicy extends AbstractSkaterChangePolicy
{
  public AbstractSkaterPenaltyBoxChangePolicy(String id, String desc) { super(id, desc); }
  public AbstractSkaterPenaltyBoxChangePolicy(String id, String name, String desc) { super(id, name, desc); }

  public void setScoreBoardModel(ScoreBoardModel sbm) {
    super.setScoreBoardModel(sbm);
    addSkaterProperty("PenaltyBox");
  }

  protected void skaterChange(Skater s, Object v) {
    skaterPenaltyBoxChange(s, ((Boolean)v).booleanValue());
  }

  protected abstract void skaterPenaltyBoxChange(Skater skater, boolean penaltyBox);
}
