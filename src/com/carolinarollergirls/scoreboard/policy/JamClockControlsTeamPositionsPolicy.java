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

public class JamClockControlsTeamPositionsPolicy extends AbstractClockRunningChangePolicy
{
  public JamClockControlsTeamPositionsPolicy() {
    super();
  }

  public void setScoreBoardModel(ScoreBoardModel sbm) {
    super.setScoreBoardModel(sbm);
    addClock(Clock.ID_JAM);
  }

  public void reset() {
    super.reset();
    setDescription("This clears all Team Positions (who are not in the Penalty Box) when the Jam clock is stopped, sets all Skaters to Not Lead Jammer, and sets the Team to Not Lead Jammer.");
  }

  public void clockRunningChange(Clock clock, boolean running) {
    if (!running) {
      Iterator<TeamModel> teams = getScoreBoardModel().getTeamModels().iterator();
      while (teams.hasNext()) {
        TeamModel teamModel = teams.next();
        Iterator<PositionModel> positions = teamModel.getPositionModels().iterator();
        while (positions.hasNext()) {
          SkaterModel sM = positions.next().getSkaterModel();
          if (sM != null && !sM.isPenaltyBox())
            sM.setPosition(Position.ID_BENCH);
        }
        Iterator<SkaterModel> skaters = teamModel.getSkaterModels().iterator();
        while (skaters.hasNext())
          skaters.next().setLeadJammer(false);
        teamModel.setLeadJammer(false);
      }
    }
  }
}
