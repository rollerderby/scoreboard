package com.carolinarollergirls.scoreboard;

import java.util.List;

import com.carolinarollergirls.scoreboard.event.*;

public interface Policy extends ScoreBoardEventProvider
{
  public ScoreBoard getScoreBoard();

  public String getId();

  public String getName();

  public String getDescription();

  public boolean isEnabled();

  public List<Policy.Parameter> getParameters();
  public Policy.Parameter getParameter(String name);

  public interface Parameter extends ScoreBoardEventProvider
  {
    public Policy getPolicy();

    public String getName();

    public String getValue();

    /**
     * Indication of the type of parameter.
     *
     * Valid types are String, Boolean, Integer, Long, Short, Byte, etc.
     * If the type is a known Class in the java.lang package, values are checked for validity
     * to the specified type.  If the type is not a known Class the validity is not checked when
     * setting the value.
     */
    public String getType();
  }
}
