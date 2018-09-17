package com.carolinarollergirls.scoreboard.base;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.List;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public interface Skater extends ScoreBoardEventProvider
{
	public Team getTeam();
	public String getId();
	public String getName();
	public String getNumber();
	public String getPosition();
	public boolean isPenaltyBox();
	public String getFlags();
  public List<Penalty> getPenalties();
  public Penalty getFOEXPPenalty();

	public static final String EVENT_NAME = "Name";
	public static final String EVENT_NUMBER = "Number";
	public static final String EVENT_POSITION = "Position";
	public static final String EVENT_PENALTY_BOX = "PenaltyBox";
	public static final String EVENT_FLAGS = "Flags";

  public static final String EVENT_PENALTY = "Penalty";
  public static final String EVENT_REMOVE_PENALTY = "RemovePenalty";
  public static final String EVENT_PENALTY_FOEXP = "PenaltyFOEXP";
  public static final String EVENT_PENALTY_REMOVE_FOEXP = "RemovePenaltyFOEXP";
  public static final String EVENT_PENALTY_PERIOD = "Period";
  public static final String EVENT_PENALTY_JAM = "Jam";
  public static final String EVENT_PENALTY_CODE = "Code";

	public static interface Penalty extends ScoreBoardEventProvider {
		public String getId();
		public int getPeriod();
		public int getJam();
		public String getCode();
	}
}
