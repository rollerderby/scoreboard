package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.utils.ValWithId;

// Roster for teams for loading in for games.
public interface PreparedTeam extends ScoreBoardEventProvider {
    public enum Value implements PermanentProperty {
      NAME(String.class, ""),
      LOGO(String.class, "");

      private Value(Class<?> t, Object dv) { type = t; defaultValue = dv; }
      private final Class<?> type;
      private final Object defaultValue;
      @Override
      public Class<?> getType() { return type; }
      @Override
      public Object getDefaultValue() { return defaultValue; }
    }
    public enum Child implements AddRemoveProperty {
      ALTERNATE_NAME(ValWithId.class),
      COLOR(ValWithId.class),
      SKATER(PreparedTeamSkater.class);

      private Child(Class<? extends ValueWithId> t) { type = t; }
      private final Class<? extends ValueWithId> type;
      @Override
      public Class<? extends ValueWithId> getType() { return type; }
    }

  public static interface PreparedTeamSkater extends ScoreBoardEventProvider {

    public enum Value implements PermanentProperty {
      NAME(String.class, ""),
      NUMBER(String.class, ""),
      FLAGS(String.class, "");

      private Value(Class<?> t, Object dv) { type = t; defaultValue = dv; }
      private final Class<?> type;
      private final Object defaultValue;
      @Override
      public Class<?> getType() { return type; }
      @Override
      public Object getDefaultValue() { return defaultValue; }
    }
  }
}

