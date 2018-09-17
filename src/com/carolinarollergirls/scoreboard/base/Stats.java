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

public interface Stats extends ScoreBoardEventProvider
{
	public ScoreBoard getScoreBoard();

	public List<PeriodStats> getPeriodStats();

	public static interface PeriodStats extends ScoreBoardEventProvider {
		public int getPeriodNumber();

		public List<JamStats> getJamStats();

		public static final String EVENT_ADD_JAM = "AddJam";
		public static final String EVENT_REMOVE_JAM = "RemoveJam";
	}

	public static interface JamStats extends ScoreBoardEventProvider {
		public int getPeriodNumber();
		public int getJamNumber();

		public long getJamClockElapsedEnd();
		public long getPeriodClockElapsedStart();
		public long getPeriodClockElapsedEnd();
		public long getPeriodClockWalltimeStart();
		public long getPeriodClockWalltimeEnd();

		public List<TeamStats> getTeamStats();

		public static final String EVENT_STATS = "Stats";
	}

	public static interface TeamStats extends ScoreBoardEventProvider {
		public int getPeriodNumber();
		public int getJamNumber();
		public String getTeamId();

		public int getJamScore();
		public int getTotalScore();
		public String getLeadJammer();
		public boolean getStarPass();
		public int getTimeouts();
		public int getOfficialReviews();

		public List<SkaterStats> getSkaterStats();

		public static final String EVENT_STATS = "Stats";
		public static final String EVENT_REMOVE_SKATER = "RemoveSkater";
	}

	public static interface SkaterStats extends ScoreBoardEventProvider {
		public int getPeriodNumber();
		public int getJamNumber();
		public String getTeamId();
		public String getSkaterId();

		public boolean getPenaltyBox();
		public String getPosition();

		public static final String EVENT_STATS = "Stats";
	}

	public static final String EVENT_ADD_PERIOD = "AddPeriod";
	public static final String EVENT_REMOVE_PERIOD = "RemovePeriod";
}
