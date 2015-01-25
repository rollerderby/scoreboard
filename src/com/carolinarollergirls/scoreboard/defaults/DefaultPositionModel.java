package com.carolinarollergirls.scoreboard.defaults;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;
import java.util.concurrent.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;

public class DefaultPositionModel extends DefaultScoreBoardEventProvider implements PositionModel
{
	public DefaultPositionModel(TeamModel tM, String i) {
		teamModel = tM;
		id = i;
		reset();
	}

	public String getProviderName() { return "Position"; }
	public Class getProviderClass() { return Position.class; }
	public String getProviderId() { return getId(); }

	public Team getTeam() { return teamModel.getTeam(); }
	public TeamModel getTeamModel() { return teamModel; }

	public String getId() { return id; }

	public Position getPosition() { return this; }

	public void reset() {
		clear();
	}

	public Skater getSkater() {
		try { return getSkaterModel().getSkater(); }
		catch ( NullPointerException npE ) { return null; }
	}
	public SkaterModel getSkaterModel() { return skaterModel; }
	public void setSkaterModel(String skaterId) throws SkaterNotFoundException {
		if (skaterId == null || skaterId.equals(""))
			clear();
		else
			getTeamModel().getSkaterModel(skaterId).setPosition(getId());
	}
	public void _setSkaterModel(String skaterId) throws SkaterNotFoundException {
		synchronized (skaterLock) {
			SkaterModel newSkaterModel = getTeamModel().getSkaterModel(skaterId);
			clear();
			SkaterModel last = skaterModel;
			skaterModel = newSkaterModel;
			scoreBoardChange(new ScoreBoardEvent(getPosition(), EVENT_SKATER, skaterModel, last));
		}
	}
	public void clear() {
		try { skaterModel.setPosition(ID_BENCH); }
		catch ( NullPointerException npE ) { /* Was no skater in this position */ }
	}
	public void _clear() {
		synchronized (skaterLock) {
			if (null != skaterModel) {
				SkaterModel last = skaterModel;
				skaterModel = null;
				scoreBoardChange(new ScoreBoardEvent(getPosition(), EVENT_SKATER, skaterModel, last));
			}
			_setPenaltyBox(false);
		}
	}
	public boolean getPenaltyBox() {
		return penaltyBox;
	}
	public void setPenaltyBox(boolean box) {
		try { skaterModel.setPenaltyBox(box); }
		catch ( NullPointerException npE ) { /* Was no skater in this position */ }
	}
	public void _setPenaltyBox(boolean box) {
		synchronized (skaterLock) {
			if (box != penaltyBox) {
				Boolean last = new Boolean(penaltyBox);
				penaltyBox = box;
				scoreBoardChange(new ScoreBoardEvent(getPosition(), EVENT_PENALTY_BOX, new Boolean(penaltyBox), last));
			}
		}
	}

	protected TeamModel teamModel;

	protected String id;

	protected SkaterModel skaterModel = null;
	protected boolean penaltyBox = false;
	protected Object skaterLock = new Object();
	protected boolean settingSkaterPosition = false;
}
