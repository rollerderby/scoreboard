package com.carolinarollergirls.scoreboard.policy;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public class IntermissionClockStopPolicy extends AbstractClockRunningChangePolicy
{
  public IntermissionClockStopPolicy() {
    super();

    PolicyModel.ParameterModel periodIncrementNumber = new DefaultPolicyModel.DefaultParameterModel(this, PERIOD_INCREMENT_NUMBER, "Boolean", String.valueOf(true));
    PolicyModel.ParameterModel periodResetNumber = new DefaultPolicyModel.DefaultParameterModel(this, PERIOD_RESET_NUMBER, "Boolean", String.valueOf(false));
    PolicyModel.ParameterModel periodResetTime = new DefaultPolicyModel.DefaultParameterModel(this, PERIOD_RESET_TIME, "Boolean", String.valueOf(true));
    PolicyModel.ParameterModel jamIncrementNumber = new DefaultPolicyModel.DefaultParameterModel(this, JAM_INCREMENT_NUMBER, "Boolean", String.valueOf(true));
    PolicyModel.ParameterModel jamResetNumber = new DefaultPolicyModel.DefaultParameterModel(this, JAM_RESET_NUMBER, "Boolean", String.valueOf(false));
    PolicyModel.ParameterModel jamResetTime = new DefaultPolicyModel.DefaultParameterModel(this, JAM_RESET_TIME, "Boolean", String.valueOf(true));

    addParameterModel(periodIncrementNumber);
    new FilterScoreBoardListener(periodIncrementNumber, "Value", Boolean.TRUE.toString()) {
      public void filteredScoreBoardChange(ScoreBoardEvent event) {
        if (Boolean.parseBoolean(getParameter(PERIOD_RESET_NUMBER).getValue()))
          getParameterModel(PERIOD_RESET_NUMBER).setValue(String.valueOf(false));
      }
    };
    addParameterModel(periodResetNumber);
    new FilterScoreBoardListener(periodResetNumber, "Value", Boolean.TRUE.toString()) {
      public void filteredScoreBoardChange(ScoreBoardEvent event) {
        if (Boolean.parseBoolean(getParameter(PERIOD_INCREMENT_NUMBER).getValue()))
          getParameterModel(PERIOD_INCREMENT_NUMBER).setValue(String.valueOf(false));
      }
    };
    addParameterModel(periodResetTime);
    addParameterModel(jamIncrementNumber);
    new FilterScoreBoardListener(jamIncrementNumber, "Value", Boolean.TRUE.toString()) {
      public void filteredScoreBoardChange(ScoreBoardEvent event) {
        if (Boolean.parseBoolean(getParameter(JAM_RESET_NUMBER).getValue()))
          getParameterModel(JAM_RESET_NUMBER).setValue(String.valueOf(false));
      }
    };
    addParameterModel(jamResetNumber);
    new FilterScoreBoardListener(jamResetNumber, "Value", Boolean.TRUE.toString()) {
      public void scoreBoardChange(ScoreBoardEvent event) {
        if (Boolean.parseBoolean(getParameter(JAM_INCREMENT_NUMBER).getValue()))
          getParameterModel(JAM_INCREMENT_NUMBER).setValue(String.valueOf(false));
      }
    };
    addParameterModel(jamResetTime);

    addClock(Clock.ID_INTERMISSION);
  }

  public void reset() {
    super.reset();
    setDescription("When the Intermission clock is over, this resets the Period and Jam clock times, and (optionally) increments both of their numbers.  This will only modify the clocks if both are stopped and the Period clock's time is at its end (if counting down, its minimum; if counting up, its maximum).");
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
          jc.setNumber(pc.getMinimumNumber());
        if (Boolean.parseBoolean(getParameter(JAM_RESET_TIME).getValue()))
          jc.resetTime();
      }
    }
  }

  public static final String PERIOD_INCREMENT_NUMBER = "Period Increment Number";
  public static final String PERIOD_RESET_NUMBER = "Period Reset Number";
  public static final String PERIOD_RESET_TIME = "Period Reset Time";
  public static final String JAM_INCREMENT_NUMBER = "Jam Increment Number";
  public static final String JAM_RESET_NUMBER = "Jam Reset Number";
  public static final String JAM_RESET_TIME = "Jam Reset Time";
}
