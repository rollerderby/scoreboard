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
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public class IntermissionClockStopPolicy extends AbstractClockRunningChangePolicy
{
  public IntermissionClockStopPolicy() {
    super(ID, DESCRIPTION);

    PolicyModel.ParameterModel periodIncrementNumber = new DefaultPolicyModel.DefaultParameterModel(this, PERIOD_INCREMENT_NUMBER, "Boolean", String.valueOf(true));
    PolicyModel.ParameterModel periodResetNumber = new DefaultPolicyModel.DefaultParameterModel(this, PERIOD_RESET_NUMBER, "Boolean", String.valueOf(false));
    PolicyModel.ParameterModel periodResetTime = new DefaultPolicyModel.DefaultParameterModel(this, PERIOD_RESET_TIME, "Boolean", String.valueOf(true));
    PolicyModel.ParameterModel jamIncrementNumber = new DefaultPolicyModel.DefaultParameterModel(this, JAM_INCREMENT_NUMBER, "Boolean", String.valueOf(false));
    PolicyModel.ParameterModel jamResetNumber = new DefaultPolicyModel.DefaultParameterModel(this, JAM_RESET_NUMBER, "Boolean", String.valueOf(true));
    PolicyModel.ParameterModel jamResetTime = new DefaultPolicyModel.DefaultParameterModel(this, JAM_RESET_TIME, "Boolean", String.valueOf(true));
    PolicyModel.ParameterModel periodResetOR = new DefaultPolicyModel.DefaultParameterModel(this, PERIOD_RESET_OR, "Boolean", String.valueOf(true));

    addParameterModel(periodIncrementNumber);
    periodIncrementNumber.addScoreBoardListener(new ConditionalScoreBoardListener(periodIncrementNumber, "Value", String.valueOf(true), new ScoreBoardListener() {
        public void scoreBoardChange(ScoreBoardEvent event) {
          if (Boolean.parseBoolean(getParameter(PERIOD_RESET_NUMBER).getValue()))
            getParameterModel(PERIOD_RESET_NUMBER).setValue(String.valueOf(false));
        }
      }));

    addParameterModel(periodResetNumber);
    periodResetNumber.addScoreBoardListener(new ConditionalScoreBoardListener(periodResetNumber, "Value", String.valueOf(true), new ScoreBoardListener() {
        public void scoreBoardChange(ScoreBoardEvent event) {
          if (Boolean.parseBoolean(getParameter(PERIOD_INCREMENT_NUMBER).getValue()))
            getParameterModel(PERIOD_INCREMENT_NUMBER).setValue(String.valueOf(false));
        }
      }));

    addParameterModel(periodResetTime);

    addParameterModel(jamIncrementNumber);
    jamIncrementNumber.addScoreBoardListener(new ConditionalScoreBoardListener(jamIncrementNumber, "Value", String.valueOf(true), new ScoreBoardListener() {
        public void scoreBoardChange(ScoreBoardEvent event) {
          if (Boolean.parseBoolean(getParameter(JAM_RESET_NUMBER).getValue()))
            getParameterModel(JAM_RESET_NUMBER).setValue(String.valueOf(false));
        }
      }));

    addParameterModel(jamResetNumber);
    jamResetNumber.addScoreBoardListener(new ConditionalScoreBoardListener(jamResetNumber, "Value", String.valueOf(true), new ScoreBoardListener() {
        public void scoreBoardChange(ScoreBoardEvent event) {
          if (Boolean.parseBoolean(getParameter(JAM_INCREMENT_NUMBER).getValue()))
            getParameterModel(JAM_INCREMENT_NUMBER).setValue(String.valueOf(false));
        }
      }));

    addParameterModel(jamResetTime);
    addParameterModel(periodResetOR);
    
  }

  public void setScoreBoardModel(ScoreBoardModel sbm) {
    super.setScoreBoardModel(sbm);
    addClock(Clock.ID_INTERMISSION);
  }

  public void clockRunningChange(Clock clock, boolean running) {
    ClockModel pc = getScoreBoardModel().getClockModel(Clock.ID_PERIOD);
    ClockModel jc = getScoreBoardModel().getClockModel(Clock.ID_JAM);
    boolean atIntermissionEnd = (clock.getTime() == (clock.isCountDirectionDown() ? clock.getMinimumTime() : clock.getMaximumTime()));
    if (!running && atIntermissionEnd) {
      boolean atPeriodEnd = (pc.getTime() == (pc.isCountDirectionDown() ? pc.getMinimumTime() : pc.getMaximumTime()));
      if (!pc.isRunning() && atPeriodEnd && !jc.isRunning()) {
        if (Boolean.parseBoolean(getParameter(PERIOD_INCREMENT_NUMBER).getValue()))
          pc.changeNumber(1);
        else if (Boolean.parseBoolean(getParameter(PERIOD_RESET_NUMBER).getValue()))
          pc.setNumber(pc.getMinimumNumber());
        if (Boolean.parseBoolean(getParameter(PERIOD_RESET_TIME).getValue()))
          pc.resetTime();
        if (Boolean.parseBoolean(getParameter(JAM_INCREMENT_NUMBER).getValue()))
          jc.changeNumber(1);
        else if (Boolean.parseBoolean(getParameter(JAM_RESET_NUMBER).getValue()))
          jc.setNumber(jc.getMinimumNumber());
        if (Boolean.parseBoolean(getParameter(JAM_RESET_TIME).getValue()))
          jc.resetTime();
        if (Boolean.parseBoolean(getParameter(PERIOD_RESET_OR).getValue())) {
        	TeamModel team1 = getScoreBoardModel().getTeamModel("1");
        	TeamModel team2 = getScoreBoardModel().getTeamModel("2");
        	team1.setOfficialReviews(1);
        	team2.setOfficialReviews(1);
        	team1.setRetainedOfficialReview(false);
        	team2.setRetainedOfficialReview(false);
        }	
      }
    }
  }

  public static final String PERIOD_INCREMENT_NUMBER = "Period Increment Number";
  public static final String PERIOD_RESET_NUMBER = "Period Reset Number";
  public static final String PERIOD_RESET_TIME = "Period Reset Time";
  public static final String JAM_INCREMENT_NUMBER = "Jam Increment Number";
  public static final String JAM_RESET_NUMBER = "Jam Reset Number";
  public static final String JAM_RESET_TIME = "Jam Reset Time";
  
  public static final String PERIOD_RESET_OR = "Reset Official Reviews";

  public static final String ID = "Intermission Clock Stop";
  public static final String DESCRIPTION = "When the Intermission clock is over, this resets the Period and Jam clock times, and (optionally) increments both of their numbers.  This will only modify the clocks if both are stopped and the Period clock's time is at its end (if counting down, its minimum; if counting up, its maximum).";
}
