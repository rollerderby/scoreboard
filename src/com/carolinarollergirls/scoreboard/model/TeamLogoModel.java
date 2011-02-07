package com.carolinarollergirls.scoreboard.model;

import java.awt.image.*;

import com.carolinarollergirls.scoreboard.*;

public interface TeamLogoModel extends TeamLogo
{
	public TeamModel getTeamModel();

	public TeamLogo getTeamLogo();

	public void reset();

	public void setId(String id);
}
