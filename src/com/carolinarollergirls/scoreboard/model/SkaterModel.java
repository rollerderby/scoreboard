package com.carolinarollergirls.scoreboard.model;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

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
}
