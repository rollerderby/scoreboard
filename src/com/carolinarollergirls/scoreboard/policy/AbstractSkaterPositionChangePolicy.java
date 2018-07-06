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

public abstract class AbstractSkaterPositionChangePolicy extends AbstractSkaterChangePolicy
{
  public AbstractSkaterPositionChangePolicy(String id, String desc) { super(id, desc); }
  public AbstractSkaterPositionChangePolicy(String id, String name, String desc) { super(id, name, desc); }

  public void setScoreBoardModel(ScoreBoardModel sbm) {
    super.setScoreBoardModel(sbm);
    addSkaterProperty("Position");
  }

  protected void skaterChange(Skater s, Object v) {
    skaterPositionChange(s, v.toString());
  }

  protected abstract void skaterPositionChange(Skater skater, String position);
}
