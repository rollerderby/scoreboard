package com.carolinarollergirls.scoreboard.model;

import java.util.*;

import com.carolinarollergirls.scoreboard.*;

public interface TeamModel extends Team
{
	public ScoreBoardModel getScoreBoardModel();

	public Team getTeam();

	public void reset();

	public void setName(String name);

	public TeamLogoModel getTeamLogoModel();

	public void timeout();

	public void setScore(int score);
	public void changeScore(int change);

	public void setTimeouts(int timeouts);
	public void changeTimeouts(int change);

	public void addSkaterModel(SkaterModel skater);
	public SkaterModel addSkaterModel(String id);
	public SkaterModel addSkaterModel(String id, String name, String number);
	public void removeSkaterModel(String id) throws SkaterNotFoundException;

	public List<SkaterModel> getSkaterModels();
	public SkaterModel getSkaterModel(String id) throws SkaterNotFoundException;

	public List<PositionModel> getPositionModels();
	public PositionModel getPositionModel(String id) throws PositionNotFoundException;

	public void setLeadJammer(boolean lead);
	/* For internal use only */
	public void _setLeadJammer(boolean lead);

	public void setPass(int pass);
	public void changePass(int change);
	/* For internal use only */
	public void _setPass(int pass);
}
