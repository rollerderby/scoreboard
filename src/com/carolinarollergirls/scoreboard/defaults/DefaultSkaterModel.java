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

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;

public class DefaultSkaterModel extends DefaultScoreBoardEventProvider implements SkaterModel
{
  public DefaultSkaterModel(TeamModel tm, String i, String n, String num) {
    teamModel = tm;
    id = i;
    setName(n);
    setNumber(num);
  }

  public String getProviderName() { return "Skater"; }
  public Class getProviderClass() { return Skater.class; }

  public Team getTeam() { return teamModel.getTeam(); }
  public TeamModel getTeamModel() { return teamModel; }

  public String getId() { return id; }

  public Skater getSkater() { return this; }

  public String getName() { return name; }
  public void setName(String n) {
    synchronized (nameLock) {
      name = n;
      scoreBoardChange(new ScoreBoardEvent(getSkater(), "Name", name));
    }
  }

  public String getNumber() { return number; }
  public void setNumber(String n) {
    synchronized (numberLock) {
      number = n;
      scoreBoardChange(new ScoreBoardEvent(getSkater(), "Number", number));
    }
  }

  public String getPosition() { return position; }
  public void setPosition(String p) throws PositionNotFoundException {
    synchronized (positionLock) {
      if (position.equals(p))
        return;

      try { getTeamModel().getPositionModel(position)._clear(); }
      catch ( PositionNotFoundException pnfE ) { /* I was on the Bench. */ }

      try { getTeamModel().getPositionModel(p)._setSkaterModel(this.getId()); }
      catch ( PositionNotFoundException pnfE ) { /* I'm being put on the Bench. */ }

      if (!Position.ID_JAMMER.equals(p)) {
        setLeadJammer(false);
        setPass(0);
      }
      position = p;
      scoreBoardChange(new ScoreBoardEvent(getSkater(), "Position", position));
    }
  }

  public boolean isLeadJammer() { return leadJammer; }
  public void setLeadJammer(boolean lead) {
    synchronized (positionLock) {
      if ((!Position.ID_JAMMER.equals(position)) && lead)
        return;
      leadJammer = lead;
      scoreBoardChange(new ScoreBoardEvent(getSkater(), "LeadJammer", new Boolean(leadJammer)));

      getTeamModel()._setLeadJammer(leadJammer);
    }
  }

  public boolean isPenaltyBox() { return penaltyBox; }
  public void setPenaltyBox(boolean box) {
    synchronized (penaltyBoxLock) {
      penaltyBox = box;
      scoreBoardChange(new ScoreBoardEvent(getSkater(), "PenaltyBox", new Boolean(penaltyBox)));
    }
  }

  public int getPass() { return pass; }
  public void setPass(int p) {
    synchronized (passLock) {
//FIXME - need to remove hardcoded min/max like this.
      if (p < 0)
        p = 0;
      pass = p;
      scoreBoardChange(new ScoreBoardEvent(getSkater(), "Pass", new Integer(pass)));

      getTeamModel()._setPass(pass);
    }
  }
  public void changePass(int c) {
    synchronized (passLock) {
      setPass(getPass() + c);
    }
  }

  protected TeamModel teamModel;

  protected String id;
  protected String name;
  protected String number;
  protected String position = Position.ID_BENCH;
  protected boolean leadJammer = false;
  protected boolean penaltyBox = false;
  protected int pass = 0;

  protected Object nameLock = new Object();
  protected Object numberLock = new Object();
  protected Object positionLock = new Object();
  protected Object penaltyBoxLock = new Object();
  protected Object passLock = new Object();

  protected boolean settingPositionSkater = false;
}
