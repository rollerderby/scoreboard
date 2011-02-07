package com.carolinarollergirls.scoreboard;

import java.awt.image.*;

import java.util.*;

import com.carolinarollergirls.scoreboard.event.*;

public interface ScoreBoardImage extends ScoreBoardEventProvider
{
	public ScoreBoard getScoreBoard();

	public String getId();

	public String getType();

	public String getName();

	public String getDirectory();
	public String getFilename();

	public BufferedImage getImage();

	public static final String TYPE_AD = "ad";
	public static final String TYPE_TEAM_LOGO = "teamlogo";
	public static final String TYPE_FULL_SCREEN = "fullscreen";
	public static final String TYPE_OTHER = "other";
}
