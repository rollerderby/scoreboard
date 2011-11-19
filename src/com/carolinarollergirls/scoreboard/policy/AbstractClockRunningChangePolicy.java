package com.carolinarollergirls.scoreboard.policy;

import java.util.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public abstract class AbstractClockRunningChangePolicy extends AbstractClockChangePolicy
{
  public AbstractClockRunningChangePolicy() {
    super();
  }
  public AbstractClockRunningChangePolicy(String id) {
    super(id);
  }

  protected void addClock(String id) {
    addClockProperty(id, "Running");
  }

  protected void clockChange(Clock c, Object v) {
    clockRunningChange(c, ((Boolean)v).booleanValue());
  }

  protected abstract void clockRunningChange(Clock clock, boolean running);
}
