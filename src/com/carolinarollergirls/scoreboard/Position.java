package com.carolinarollergirls.scoreboard;

import java.util.*;

import com.carolinarollergirls.scoreboard.event.*;

public interface Position extends ScoreBoardEventProvider
{
	public Team getTeam();

	public String getId();

	public Skater getSkater();

	public static final String ID_BENCH = "Bench";
	public static final String ID_JAMMER = "Jammer";
	public static final String ID_PIVOT = "Pivot";
	public static final String ID_BLOCKER1 = "Blocker1";
	public static final String ID_BLOCKER2 = "Blocker2";
	public static final String ID_BLOCKER3 = "Blocker3";
	public static final List<String> POSITIONS = Arrays.asList(new String[]{ ID_BENCH, ID_JAMMER, ID_PIVOT, ID_BLOCKER1, ID_BLOCKER2, ID_BLOCKER3 });
	public static final List<String> FLOOR_POSITIONS = Arrays.asList(new String[]{ ID_JAMMER, ID_PIVOT, ID_BLOCKER1, ID_BLOCKER2, ID_BLOCKER3 });
}
