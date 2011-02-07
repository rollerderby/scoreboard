package com.carolinarollergirls.scoreboard.model;

import com.carolinarollergirls.scoreboard.*;

public interface SkaterModel extends Skater
{
	public TeamModel getTeamModel();

	public Skater getSkater();

	public void setName(String id);

	public void setNumber(String number);

	public void setPosition(String position) throws PositionNotFoundException;

	public void setLeadJammer(boolean lead);

	public void setPenaltyBox(boolean box);

	public void setPass(int pass);
	public void changePass(int change);
}
