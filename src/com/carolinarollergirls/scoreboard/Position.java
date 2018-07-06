package com.carolinarollergirls.scoreboard;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.Arrays;
import java.util.List;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public interface Position extends ScoreBoardEventProvider
{
	public Team getTeam();
	public String getId();
	public Skater getSkater();
	public boolean getPenaltyBox();

	public static final String ID_BENCH = "Bench";
	public static final String ID_JAMMER = "Jammer";
	public static final String ID_PIVOT = "Pivot";
	public static final String ID_BLOCKER1 = "Blocker1";
	public static final String ID_BLOCKER2 = "Blocker2";
	public static final String ID_BLOCKER3 = "Blocker3";
	public static final List<String> POSITIONS = Arrays.asList(new String[]{ ID_BENCH, ID_JAMMER, ID_PIVOT, ID_BLOCKER1, ID_BLOCKER2, ID_BLOCKER3 });
	public static final List<String> FLOOR_POSITIONS = Arrays.asList(new String[]{ ID_JAMMER, ID_PIVOT, ID_BLOCKER1, ID_BLOCKER2, ID_BLOCKER3 });

	public static final String EVENT_SKATER = "Skater";
	public static final String EVENT_PENALTY_BOX = "PenaltyBox";
}
