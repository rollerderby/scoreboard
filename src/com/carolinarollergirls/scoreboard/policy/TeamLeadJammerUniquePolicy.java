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

public class TeamLeadJammerUniquePolicy extends AbstractTeamLeadJammerChangePolicy
{
  public TeamLeadJammerUniquePolicy() { super(ID, DESCRIPTION); }

  public void setScoreBoardModel(ScoreBoardModel sbm) {
    super.setScoreBoardModel(sbm);
    addTeam(Team.ID_1);
    addTeam(Team.ID_2);
  }

  protected void teamLeadJammerChange(Team team, boolean lead) {
    if (lead) {
      String otherId = (team.getId().equals(Team.ID_1) ? Team.ID_2 : Team.ID_1);
      TeamModel otherTeam = getScoreBoardModel().getTeamModel(otherId);
      if (otherTeam.isLeadJammer())
        otherTeam.setLeadJammer(false);
    }
  }

  public static final String ID = "Team Lead Jammer Unique";
  public static final String DESCRIPTION = "This allows only one team to have lead jammer status; when either team gains lead jammer, the other team will be forced to lose lead jammer if it has it.";
}
