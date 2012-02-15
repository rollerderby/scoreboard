package com.carolinarollergirls.scoreboard.model;
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

public interface PositionModel extends Position
{
  public TeamModel getTeamModel();

  public Position getPosition();

  public void reset();

  public SkaterModel getSkaterModel();

  public void setSkaterModel(String skaterId) throws SkaterNotFoundException;
  public void clear();

  /* These methods are for internal use by SkaterModel to coordinate Position */
  public void _setSkaterModel(String skaterId) throws SkaterNotFoundException;
  public void _clear();
}
