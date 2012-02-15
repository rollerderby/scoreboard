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

public abstract class AbstractTeamLeadJammerChangePolicy extends AbstractTeamChangePolicy
{
  public AbstractTeamLeadJammerChangePolicy() {
    super();
  }
  public AbstractTeamLeadJammerChangePolicy(String id) {
    super(id);
  }

  protected void addTeam(String id) {
    addTeamProperty(id, "LeadJammer");
  }

  protected void teamChange(Team t, Object v) {
    teamLeadJammerChange(t, ((Boolean)v).booleanValue());
  }

  protected abstract void teamLeadJammerChange(Team team, boolean lead);
}
