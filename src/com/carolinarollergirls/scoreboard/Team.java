package com.carolinarollergirls.scoreboard;

import java.util.*;

import com.carolinarollergirls.scoreboard.event.*;

public interface Team extends ScoreBoardEventProvider
{
	public ScoreBoard getScoreBoard();

	public String getId();

	public String getName();

	public TeamLogo getTeamLogo();

	public int getScore();

	public int getTimeouts();

	public List<Skater> getSkaters();
	public Skater getSkater(String id) throws SkaterNotFoundException;

	public List<Position> getPositions();
	public Position getPosition(String id) throws PositionNotFoundException;

	public boolean isLeadJammer();

	public int getPass();

	public static final String ID_1 = "1";
	public static final String ID_2 = "2";
}
