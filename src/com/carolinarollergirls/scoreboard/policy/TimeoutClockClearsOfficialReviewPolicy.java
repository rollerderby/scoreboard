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

public class TimeoutClockClearsOfficialReviewPolicy extends AbstractClockRunningChangePolicy
{
  public TimeoutClockClearsOfficialReviewPolicy() { super(ID, DESCRIPTION); }

  public void setScoreBoardModel(ScoreBoardModel sbm) {
    super.setScoreBoardModel(sbm);
    addClock(Clock.ID_TIMEOUT);
  }

  public void clockRunningChange(Clock clock, boolean running) {
    if (!running)
      getScoreBoardModel().setOfficialReview(false);
  }

  public static final String ID = "Timeout Clock Clears Official Review";
  public static final String DESCRIPTION = "This clears the Official Review flag based on the Timeout clock.  When the Timeout clock stops, the Official Review flag is set to false.";

}
