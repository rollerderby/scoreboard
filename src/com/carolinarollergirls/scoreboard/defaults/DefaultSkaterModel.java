package com.carolinarollergirls.scoreboard.defaults;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.UUID;

import com.carolinarollergirls.scoreboard.Position;
import com.carolinarollergirls.scoreboard.PositionNotFoundException;
import com.carolinarollergirls.scoreboard.ScoreBoardManager;
import com.carolinarollergirls.scoreboard.Skater;
import com.carolinarollergirls.scoreboard.Team;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.model.SkaterModel;
import com.carolinarollergirls.scoreboard.model.TeamModel;

public class DefaultSkaterModel extends DefaultScoreBoardEventProvider implements SkaterModel
{
	public DefaultSkaterModel(TeamModel tm, String i, String n, String num, String flags) {
		teamModel = tm;
		setId(i);
		setName(n);
		setNumber(num);
		setFlags(flags);
	}

	public String getProviderName() { return "Skater"; }
	public Class<?> getProviderClass() { return Skater.class; }
	public String getProviderId() { return getId(); }

	public Team getTeam() { return teamModel.getTeam(); }
	public TeamModel getTeamModel() { return teamModel; }

	public String getId() { return id; }
	public void setId(String i) {
		UUID uuid;
		try {
			uuid = UUID.fromString(i);
		} catch (IllegalArgumentException iae) {
			uuid = UUID.randomUUID();
		}
		id = uuid.toString();
	}

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

			try { teamModel.getPositionModel(position)._clear(); }
			catch ( PositionNotFoundException pnfE ) { /* I was on the Bench. */ }

			try { teamModel.getPositionModel(p)._setSkaterModel(this.getId()); }
			catch ( PositionNotFoundException pnfE ) { /* I'm being put on the Bench. */ }

			String last = position;
			position = p;
			scoreBoardChange(new ScoreBoardEvent(getSkater(), EVENT_POSITION, position, last));
			ScoreBoardManager.gameSnapshot();
		}
	}

	public boolean isPenaltyBox() { return penaltyBox; }
	public void setPenaltyBox(boolean box) {
		synchronized (positionLock) {
			if (box == penaltyBox)
				return;

			requestBatchStart();

			Boolean last = new Boolean(penaltyBox);
			penaltyBox = box;
			scoreBoardChange(new ScoreBoardEvent(getSkater(), EVENT_PENALTY_BOX, new Boolean(penaltyBox), last));

			if (box && position.equals(Position.ID_JAMMER) && teamModel.getLeadJammer().equals(Team.LEAD_LEAD))
				teamModel.setLeadJammer(Team.LEAD_LOST_LEAD);

			if (position.equals(Position.ID_JAMMER) || position.equals(Position.ID_PIVOT)) {
				// Update Position Model if Jammer or Pivot
				try { teamModel.getPositionModel(position)._setPenaltyBox(box); }
				catch ( PositionNotFoundException pnfE ) { }
			}

			requestBatchEnd();
			ScoreBoardManager.gameSnapshot();
		}
	}

	public String getFlags() { return flags; }
	public void setFlags(String f) {
		synchronized (flagsLock) {
			String last = flags;
			flags = f;
			scoreBoardChange(new ScoreBoardEvent(getSkater(), EVENT_FLAGS, flags, last));
			ScoreBoardManager.gameSnapshot();
		}
	}

	public void bench() {
		synchronized (positionLock) {
			saved_position = position;

			if (!penaltyBox)
				setPosition(Position.ID_BENCH);
			else if (position.equals(Position.ID_PIVOT) && teamModel.isStarPass())
				setPosition(Position.ID_JAMMER);
		}
	}
	public void unBench() {
		synchronized (positionLock) {
			setPosition(saved_position);
		}
	}

	protected TeamModel teamModel;

	protected String id;
	protected String name;
	protected String number;
	protected String position = Position.ID_BENCH;
	protected boolean penaltyBox = false;
	protected String flags;

	private String saved_position = Position.ID_BENCH;

	protected Object nameLock = new Object();
	protected Object numberLock = new Object();
	protected Object positionLock = new Object();
	protected Object flagsLock = new Object();

	protected boolean settingPositionSkater = false;
}
