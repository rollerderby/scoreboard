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
import com.carolinarollergirls.scoreboard.defaults.*;

public abstract class AbstractClockRunningChangePolicy extends AbstractClockChangePolicy
{
  public AbstractClockRunningChangePolicy(String id, String desc) { super(id, desc); }
  public AbstractClockRunningChangePolicy(String id, String name, String desc) { super(id, name, desc); }

  protected void addClock(String id) {
    addClockProperty(id, "Running");
  }

  protected void clockChange(Clock c, Object v) {
    clockRunningChange(c, ((Boolean)v).booleanValue());
  }

  protected abstract void clockRunningChange(Clock clock, boolean running);
}
