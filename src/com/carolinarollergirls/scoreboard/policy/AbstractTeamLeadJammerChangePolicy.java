package com.carolinarollergirls.scoreboard.policy;

import java.util.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.defaults.*;

public abstract class AbstractTeamLeadJammerChangePolicy extends AbstractTeamChangePolicy
{
  public AbstractTeamLeadJammerChangePolicy() {
    super();
  }
  public AbstractTeamLeadJammerChangePolicy(String id) {
    super(id);
  }

  protected void addTeam(String id) {
    addTeamProperty(id, "LeadJammer");
  }

  protected void teamChange(Team t, Object v) {
    teamLeadJammerChange(t, ((Boolean)v).booleanValue());
  }

  protected abstract void teamLeadJammerChange(Team team, boolean lead);
}
