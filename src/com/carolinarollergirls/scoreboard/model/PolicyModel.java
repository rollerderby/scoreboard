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

public interface PolicyModel extends Policy,ScoreBoardController
{
  public ScoreBoardModel getScoreBoardModel();

  public Policy getPolicy();

  public void reset();

  public void setName(String name);

  public void setDescription(String description);

  public void setEnabled(boolean enabled);

  public List<PolicyModel.ParameterModel> getParameterModels();
  public PolicyModel.ParameterModel getParameterModel(String name);

  public interface ParameterModel extends Policy.Parameter
  {
    public PolicyModel getPolicyModel();

    public Policy.Parameter getParameter();

    public void reset();

    /**
     * Set the value.
     * @exception IllegalArgumentException If the Parameter type is a known java.lang Class and
     * the provided value is invalid for that Class.
     */
    public void setValue(String value) throws IllegalArgumentException;
  }
}
