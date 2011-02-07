package com.carolinarollergirls.scoreboard.model;

import java.util.*;

import com.carolinarollergirls.scoreboard.*;

public interface PositionModel extends Position
{
	public TeamModel getTeamModel();

	public Position getPosition();

	public void reset();

	public SkaterModel getSkaterModel();

	public void setSkaterModel(String skaterId) throws SkaterNotFoundException;
	public void clear();

	/* These methods are for internal use by SkaterModel to coordinate Position */
	public void _setSkaterModel(String skaterId) throws SkaterNotFoundException;
	public void _clear();
}
