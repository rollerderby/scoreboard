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

public class PenaltyBoxControlsLeadJammerPolicy extends AbstractSkaterPenaltyBoxChangePolicy
{
  public PenaltyBoxControlsLeadJammerPolicy() { super(ID, DESCRIPTION); }

  protected void skaterPenaltyBoxChange(Skater skater, boolean penaltyBox) {
    try {
      if (penaltyBox)
        getScoreBoardModel().getTeamModel(skater.getTeam().getId()).getSkaterModel(skater.getId()).setLeadJammer(false);
    } catch ( SkaterNotFoundException snfE ) {
      /* Should not happen - no SkaterModel for specified Skater */
    }
  }

  public static final String ID = "Penalty Box Controls Lead Jammer";
  public static final String DESCRIPTION = "This removes Lead Jammer from any Skater sent to the Penalty Box.";
}
