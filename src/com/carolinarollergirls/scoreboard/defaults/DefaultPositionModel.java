package com.carolinarollergirls.scoreboard.defaults;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;
import java.util.concurrent.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;

public class DefaultPositionModel extends DefaultScoreBoardEventProvider implements PositionModel
{
  public DefaultPositionModel(TeamModel tM, String i) {
    teamModel = tM;
    id = i;
    reset();
  }

  public String getProviderName() { return "Position"; }
  public Class getProviderClass() { return Position.class; }

  public Team getTeam() { return teamModel.getTeam(); }
  public TeamModel getTeamModel() { return teamModel; }

  public String getId() { return id; }

  public Position getPosition() { return this; }

  public void reset() {
    clear();
  }

  public Skater getSkater() {
    try { return getSkaterModel().getSkater(); }
    catch ( NullPointerException npE ) { return null; }
  }
  public SkaterModel getSkaterModel() { return skaterModel; }
  public void setSkaterModel(String skaterId) throws SkaterNotFoundException {
    if (skaterId == null || skaterId.equals(""))
      clear();
    else
      getTeamModel().getSkaterModel(skaterId).setPosition(getId());
  }
  public void _setSkaterModel(String skaterId) throws SkaterNotFoundException {
    synchronized (skaterLock) {
      SkaterModel newSkaterModel = getTeamModel().getSkaterModel(skaterId);
      clear();
      skaterModel = newSkaterModel;
      scoreBoardChange(new ScoreBoardEvent(getPosition(), "Skater", skaterModel));
    }
  }
  public void clear() {
    try { skaterModel.setPosition(ID_BENCH); }
    catch ( NullPointerException npE ) { /* Was no skater in this position */ }
  }
  public void _clear() {
    synchronized (skaterLock) {
      if (null != skaterModel) {
        skaterModel = null;
        scoreBoardChange(new ScoreBoardEvent(getPosition(), "Skater", null));
      }
    }
  }

  protected TeamModel teamModel;

  protected String id;

  protected SkaterModel skaterModel = null;
  protected Object skaterLock = new Object();
  protected boolean settingSkaterPosition = false;
}
