package com.carolinarollergirls.scoreboard.model;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;

import com.carolinarollergirls.scoreboard.*;

public interface TeamModel extends Team
{
	public ScoreBoardModel getScoreBoardModel();

	public Team getTeam();

	public void reset();

	public void setName(String name);

	public void startJam();
	public void unStartJam();
	public void stopJam();
	public void unStopJam();
	public void benchSkaters();

	public List<AlternateNameModel> getAlternateNameModels();
	public AlternateNameModel getAlternateNameModel(String id);
	public void setAlternateNameModel(String id, String name);
	public void removeAlternateNameModel(String id);
	public void removeAlternateNameModels();

	public List<ColorModel> getColorModels();
	public ColorModel getColorModel(String id);
	public void setColorModel(String id, String color);
	public void removeColorModel(String id);
	public void removeColorModels();

	public void setLogo(String logo);

	public void timeout();
	public void officialReview();

	public void setScore(int score);
	public void changeScore(int change);

	public void setLastScore(int score);
	public void changeLastScore(int change);

	public void setTimeouts(int timeouts);
	public void changeTimeouts(int change);
	public void setOfficialReviews(int reviews);
	public void changeOfficialReviews(int reviews);

	public void setInTimeout(boolean in_timeouts);
	public void setInOfficialReview(boolean in_official_review);
	public void setRetainedOfficialReview(boolean retained_official_review);

	public void addSkaterModel(SkaterModel skater);
	public SkaterModel addSkaterModel(String id);
	public SkaterModel addSkaterModel(String id, String name, String number, String flag);
	public void removeSkaterModel(String id) throws SkaterNotFoundException;

	public List<SkaterModel> getSkaterModels();
	public SkaterModel getSkaterModel(String id) throws SkaterNotFoundException;

	public List<PositionModel> getPositionModels();
	public PositionModel getPositionModel(String id) throws PositionNotFoundException;

	public void setLeadJammer(String lead);

	public void setStarPass(boolean starPass);

  public void penalty(String skaterId, String penaltyId, boolean fo_exp, int period, int jam, String code);

	public static interface AlternateNameModel extends AlternateName {
		public void setName(String n);

		public TeamModel getTeamModel();
	}

	public static interface ColorModel extends Color {
		public void setColor(String c);

		public TeamModel getTeamModel();
	}
}
