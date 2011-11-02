package com.carolinarollergirls.scoreboard;

import java.util.*;

import com.carolinarollergirls.scoreboard.event.*;

public interface Skater extends ScoreBoardEventProvider
{
  public Team getTeam();

  public String getId();

  public String getName();

  public String getNumber();

  public String getPosition();

  public boolean isLeadJammer();

  public boolean isPenaltyBox();

  public int getPass();
}
