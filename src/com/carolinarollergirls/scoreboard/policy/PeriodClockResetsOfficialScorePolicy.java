package com.carolinarollergirls.scoreboard.policy;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public class PeriodClockResetsOfficialScorePolicy extends AbstractScoreBoardInPeriodChangePolicy
{
  public PeriodClockResetsOfficialScorePolicy() { super(ID, DESCRIPTION); }

  public void scoreBoardInPeriodChange(ScoreBoard scoreBoard, boolean inPeriod) {
    if (!inPeriod)
      getScoreBoardModel().setOfficialScore(false);
  }

  public static final String ID = "Period Clock Resets Official Score";
  public static final String DESCRIPTION = "When the Period ends, as reported by ScoreBoard's InPeriod event, this resets the ScoreBoard OfficialScore to false.";
}
