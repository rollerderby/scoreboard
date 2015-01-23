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

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.model.*;

public class DefaultSkaterModel extends DefaultScoreBoardEventProvider implements SkaterModel
{
	public DefaultSkaterModel(TeamModel tm, String i, String n, String num) {
		teamModel = tm;
		id = i;
		setName(n);
		setNumber(num);
	}

	public String getProviderName() { return "Skater"; }
	public Class getProviderClass() { return Skater.class; }
	public String getProviderId() { return getId(); }

	public Team getTeam() { return teamModel.getTeam(); }
	public TeamModel getTeamModel() { return teamModel; }

	public String getId() { return id; }

	public Skater getSkater() { return this; }

	public String getName() { return name; }
	public void setName(String n) {
		synchronized (nameLock) {
			String last = name;
			name = n;
			scoreBoardChange(new ScoreBoardEvent(getSkater(), EVENT_NAME, name, last));
		}
	}

	public String getNumber() { return number; }
	public void setNumber(String n) {
		synchronized (numberLock) {
			String last = number;
			number = n;
			scoreBoardChange(new ScoreBoardEvent(getSkater(), EVENT_NUMBER, number, last));
		}
	}

	public String getPosition() { return position; }
	public void setPosition(String p) throws PositionNotFoundException {
		synchronized (positionLock) {
			if (position.equals(p))
				return;

			try { getTeamModel().getPositionModel(position)._clear(); }
			catch ( PositionNotFoundException pnfE ) { /* I was on the Bench. */ }

			try { getTeamModel().getPositionModel(p)._setSkaterModel(this.getId()); }
			catch ( PositionNotFoundException pnfE ) { /* I'm being put on the Bench. */ }

			if (!Position.ID_JAMMER.equals(p)) {
				setLeadJammer(false);
			}
			String last = position;
			position = p;
			scoreBoardChange(new ScoreBoardEvent(getSkater(), EVENT_POSITION, position, last));
		}
	}

	public boolean isLeadJammer() { return leadJammer; }
	public void setLeadJammer(boolean lead) {
		synchronized (positionLock) {
			if ((!Position.ID_JAMMER.equals(position)) && lead)
				return;
			Boolean last = new Boolean(leadJammer);
			leadJammer = lead;
			scoreBoardChange(new ScoreBoardEvent(getSkater(), EVENT_LEAD_JAMMER, new Boolean(leadJammer), last));
		}
	}

	public boolean isPenaltyBox() { return penaltyBox; }
	public void setPenaltyBox(boolean box) {
		synchronized (penaltyBoxLock) {
			Boolean last = new Boolean(penaltyBox);
			penaltyBox = box;
			scoreBoardChange(new ScoreBoardEvent(getSkater(), EVENT_PENALTY_BOX, new Boolean(penaltyBox), last));
		}
	}

	protected TeamModel teamModel;

	protected String id;
	protected String name;
	protected String number;
	protected String position = Position.ID_BENCH;
	protected boolean leadJammer = false;
	protected boolean penaltyBox = false;

	protected Object nameLock = new Object();
	protected Object numberLock = new Object();
	protected Object positionLock = new Object();
	protected Object penaltyBoxLock = new Object();

	protected boolean settingPositionSkater = false;
}
