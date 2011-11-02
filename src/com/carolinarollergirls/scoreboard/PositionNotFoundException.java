package com.carolinarollergirls.scoreboard;

public class PositionNotFoundException extends RuntimeException
{
  public PositionNotFoundException(String p) {
    super("Position '"+p+"' not found");
    position = p;
  }

  public String getPosition() { return position; }

  protected String position = "";
}
