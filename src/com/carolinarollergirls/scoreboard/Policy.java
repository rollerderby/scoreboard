package com.carolinarollergirls.scoreboard;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.List;

import com.carolinarollergirls.scoreboard.event.*;

public interface Policy extends ScoreBoardEventProvider
{
  public ScoreBoard getScoreBoard();

  public String getId();

  public String getName();

  public String getDescription();

  public boolean isEnabled();

  public List<Policy.Parameter> getParameters();
  public Policy.Parameter getParameter(String name);

  public interface Parameter extends ScoreBoardEventProvider
  {
    public Policy getPolicy();

    public String getName();

    public String getValue();

    /**
     * Indication of the type of parameter.
     *
     * Valid types are String, Boolean, Integer, Long, Short, Byte, etc.
     * If the type is a known Class in the java.lang package, values are checked for validity
     * to the specified type.  If the type is not a known Class the validity is not checked when
     * setting the value.
     */
    public String getType();

    public static final String EVENT_VALUE = "Value";
  }

  public static final String EVENT_NAME = "Name";
  public static final String EVENT_DESCRIPTION = "Description";
  public static final String EVENT_ENABLED = "Enabled";
  public static final String EVENT_ADD_PARAMETER = "AddParameter";
  public static final String EVENT_REMOVE_PARAMETER = "RemoveParameter";
}
