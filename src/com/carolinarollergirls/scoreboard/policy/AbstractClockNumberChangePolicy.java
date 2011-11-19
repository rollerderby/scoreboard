package com.carolinarollergirls.scoreboard.policy;

import java.util.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public abstract class AbstractClockNumberChangePolicy extends AbstractClockChangePolicy
{
  public AbstractClockNumberChangePolicy() {
    super();
  }
  public AbstractClockNumberChangePolicy(String id) {
    super(id);
  }

  protected void addClock(String id) {
    addClockProperty(id, "Number");
  }

  protected void clockChange(Clock c, Object v) {
    clockNumberChange(c, ((Integer)v).intValue());
  }

  protected abstract void clockNumberChange(Clock clock, int number);
}
