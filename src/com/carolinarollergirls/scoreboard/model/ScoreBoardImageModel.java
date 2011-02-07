package com.carolinarollergirls.scoreboard.model;

import java.awt.image.*;

import java.util.*;

import com.carolinarollergirls.scoreboard.*;

public interface ScoreBoardImageModel extends ScoreBoardImage
{
	public ScoreBoardModel getScoreBoardModel();

	public ScoreBoardImage getScoreBoardImage();

	public void setName(String name);
}
