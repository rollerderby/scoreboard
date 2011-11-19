package com.carolinarollergirls.scoreboard.policy;

import java.util.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public abstract class AbstractClockTimeChangePolicy extends AbstractClockChangePolicy
{
  public AbstractClockTimeChangePolicy() {
    super();
  }
  public AbstractClockTimeChangePolicy(String id) {
    super(id);
  }

  protected void addClock(String id) {
    addClockProperty(id, "Time");
  }

  protected void clockChange(Clock c, Object v) {
    clockTimeChange(c, ((Long)v).longValue());
  }

  protected abstract void clockTimeChange(Clock clock, long time);
}
