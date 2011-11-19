package com.carolinarollergirls.scoreboard.policy;

import java.util.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public class PenaltyBoxControlsLeadJammerPolicy extends AbstractSkaterPenaltyBoxChangePolicy
{
  public PenaltyBoxControlsLeadJammerPolicy() {
    super();
    setDescription("This removes Lead Jammer from any Skater sent to the Penalty Box.");
  }

  protected void skaterPenaltyBoxChange(Skater skater, boolean penaltyBox) {
    try {
      if (penaltyBox)
        getScoreBoardModel().getTeamModel(skater.getTeam().getId()).getSkaterModel(skater.getId()).setLeadJammer(false);
    } catch ( SkaterNotFoundException snfE ) {
      /* Should not happen - no SkaterModel for specified Skater */
    }
  }
}
