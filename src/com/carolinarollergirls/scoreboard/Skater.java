package com.carolinarollergirls.scoreboard;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;

import com.carolinarollergirls.scoreboard.event.*;

public interface Skater extends ScoreBoardEventProvider
{
  public Team getTeam();

  public String getId();

  public String getName();

  public String getNumber();

  public String getPosition();

  public boolean isLeadJammer();

  public boolean isPenaltyBox();

  public int getPass();
}
