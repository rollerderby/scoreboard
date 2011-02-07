package com.carolinarollergirls.scoreboard;

import com.carolinarollergirls.scoreboard.event.*;

public interface Clock extends ScoreBoardEventProvider
{
	public ScoreBoard getScoreBoard();

	public String getId();

	public String getName();

	public int getNumber();
	public int getMinimumNumber();
	public int getMaximumNumber();

	public long getTime();
	public long getMinimumTime();
	public long getMaximumTime();

	public boolean isRunning();

	public boolean isCountDirectionDown();

	public static final String ID_PERIOD = "Period";
	public static final String ID_JAM = "Jam";
	public static final String ID_LINEUP = "Lineup";
	public static final String ID_TIMEOUT = "Timeout";
	public static final String ID_INTERMISSION = "Intermission";
	public static final String ID_COUNTDOWN = "Countdown";
	public static final String ID_PENALTY_TEAM1_B1 = "PenaltyTeam1B1";
	public static final String ID_PENALTY_TEAM1_B2 = "PenaltyTeam1B2";
	public static final String ID_PENALTY_TEAM1_B3 = "PenaltyTeam1B3";
	public static final String ID_PENALTY_TEAM1_J = "PenaltyTeam1J";
	public static final String ID_PENALTY_TEAM2_B1 = "PenaltyTeam2B1";
	public static final String ID_PENALTY_TEAM2_B2 = "PenaltyTeam2B2";
	public static final String ID_PENALTY_TEAM2_B3 = "PenaltyTeam2B3";
	public static final String ID_PENALTY_TEAM2_J = "PenaltyTeam2J";
}
