package com.carolinarollergirls.scoreboard.policy;

import java.util.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public abstract class AbstractClockChangePolicy extends DefaultPolicyModel
{
  public AbstractClockChangePolicy() {
    super();
  }
  public AbstractClockChangePolicy(String id) {
    super(id);
  }

  protected void addClockProperty(String id, String p) {
    filterScoreBoardListener.addProperty(Clock.class, id, p);
  }

  public void setScoreBoardModel(ScoreBoardModel sbM) {
    super.setScoreBoardModel(sbM);
    sbM.addScoreBoardListener(filterScoreBoardListener);
  }

  protected abstract void clockChange(Clock clock, Object value);

  protected FilterScoreBoardListener filterScoreBoardListener = new FilterScoreBoardListener() {
      public void filteredScoreBoardChange(ScoreBoardEvent event) {
        if (isEnabled()) {
          synchronized (changeLock) {
            clockChange((Clock)event.getProvider(), event.getValue());
          }
        }
      }
    };

  protected Object changeLock = new Object();
}
