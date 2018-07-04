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

public interface ClockModel extends Clock
{
	public ScoreBoardModel getScoreBoardModel();

	public Clock getClock();

	public void reset();

	public void start();
	public void stop();

	public void unstart();
	public void unstop();

	public void setName(String name);

	public void setNumber(int n);
	public void changeNumber(int n);
	public void setMinimumNumber(int n);
	public void changeMinimumNumber(int n);
	public void setMaximumNumber(int n);
	public void changeMaximumNumber(int n);

	public void setTime(long ms);
	public void changeTime(long ms);
	public void elapseTime(long ms);
	public void resetTime();
	public void setMinimumTime(long ms);
	public void changeMinimumTime(long ms);
	public void setMaximumTime(long ms);
	public void changeMaximumTime(long ms);

	public void setCountDirectionDown(boolean down);
}

