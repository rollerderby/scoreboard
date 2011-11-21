package com.carolinarollergirls.scoreboard.policy;

import java.util.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public abstract class AbstractTeamChangePolicy extends DefaultPolicyModel
{
  public AbstractTeamChangePolicy() {
    super();
  }
  public AbstractTeamChangePolicy(String id) {
    super(id);
  }

  protected void addTeamProperty(String id, String p) {
    filterScoreBoardListener.addProperty(Team.class, id, p);
  }

  public void setScoreBoardModel(ScoreBoardModel sbM) {
    super.setScoreBoardModel(sbM);
    sbM.addScoreBoardListener(filterScoreBoardListener);
  }

  protected abstract void teamChange(Team team, Object value);

  protected FilterScoreBoardListener filterScoreBoardListener = new FilterScoreBoardListener() {
      public void filteredScoreBoardChange(ScoreBoardEvent event) {
        if (isEnabled()) {
          synchronized (changeLock) {
            teamChange((Team)event.getProvider(), event.getValue());
          }
        }
      }
    };

  protected Object changeLock = new Object();
}
