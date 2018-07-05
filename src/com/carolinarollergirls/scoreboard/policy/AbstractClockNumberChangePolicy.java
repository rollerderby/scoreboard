package com.carolinarollergirls.scoreboard.policy;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.Clock;

public abstract class AbstractClockNumberChangePolicy extends AbstractClockChangePolicy
{
  public AbstractClockNumberChangePolicy(String id, String desc) { super(id, desc); }
  public AbstractClockNumberChangePolicy(String id, String name, String desc) { super(id, name, desc); }

  protected void addClock(String id) {
    addClockProperty(id, "Number");
  }

  protected void clockChange(Clock c, Object v) {
    clockNumberChange(c, ((Integer)v).intValue());
  }

  protected abstract void clockNumberChange(Clock clock, int number);
}
