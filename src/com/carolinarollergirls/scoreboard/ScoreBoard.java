package com.carolinarollergirls.scoreboard;

import java.util.*;

import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.xml.*;

public interface ScoreBoard extends ScoreBoardEventProvider
{
  /**
   * Id of Team who called Timeout.
   *
   * The Id is as returned from Team.getId().  For Offical Timeouts, this returns an empty string.
   */
  public String getTimeoutOwner();

  /**
   * If this bout is in Overtime.
   */
  public boolean getOvertime();

// FIXME - clock and team getters should either return null or throw exception instead of creating new clock/team...
  public List<Clock> getClocks();
  public Clock getClock(String id);

  public List<Team> getTeams();
  public Team getTeam(String id);

  public List<Policy> getPolicies();
  public Policy getPolicy(String id);

  public XmlScoreBoard getXmlScoreBoard();
}
