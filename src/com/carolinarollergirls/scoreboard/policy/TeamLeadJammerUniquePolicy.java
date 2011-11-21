package com.carolinarollergirls.scoreboard.policy;

import java.util.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public class TeamLeadJammerUniquePolicy extends AbstractTeamLeadJammerChangePolicy
{
  public TeamLeadJammerUniquePolicy() {
    super();

    addTeam(Team.ID_1);
    addTeam(Team.ID_2);
  }

  public void reset() {
    super.reset();
    setDescription("This allows only one team to have lead jammer status; when either team gains lead jammer, the other team will be forced to lose lead jammer if it has it.");
  }

  protected void teamLeadJammerChange(Team team, boolean lead) {
    if (lead) {
      String otherId = (team.getId().equals(Team.ID_1) ? Team.ID_2 : Team.ID_1);
      TeamModel otherTeam = getScoreBoardModel().getTeamModel(otherId);
      if (otherTeam.isLeadJammer())
        otherTeam.setLeadJammer(false);
    }
  }
}
