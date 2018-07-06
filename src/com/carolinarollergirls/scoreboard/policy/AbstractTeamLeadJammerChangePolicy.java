package com.carolinarollergirls.scoreboard.policy;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.Team;

public abstract class AbstractTeamLeadJammerChangePolicy extends AbstractTeamChangePolicy
{
  public AbstractTeamLeadJammerChangePolicy(String id, String desc) { super(id, desc); }
  public AbstractTeamLeadJammerChangePolicy(String id, String name, String desc) { super(id, name, desc); }

  protected void addTeam(String id) {
    addTeamProperty(id, "LeadJammer");
  }

  protected void teamChange(Team t, Object v) {
    teamLeadJammerChange(t, ((Boolean)v).booleanValue());
  }

  protected abstract void teamLeadJammerChange(Team team, boolean lead);
}
