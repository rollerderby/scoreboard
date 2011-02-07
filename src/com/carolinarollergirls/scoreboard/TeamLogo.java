package com.carolinarollergirls.scoreboard;

import java.awt.image.*;

import com.carolinarollergirls.scoreboard.event.*;

public interface TeamLogo extends ScoreBoardEventProvider
{
	public Team getTeam();

	public String getId();

	public String getType();

	public String getName();

	public String getDirectory();
	public String getFilename();

	public BufferedImage getImage();
}
