package com.carolinarollergirls.scoreboard.defaults;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
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
	public Class<Skater> getProviderClass() { return Skater.class; }
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

	public List<Penalty> getPenalties() { return Collections.unmodifiableList(new ArrayList<Penalty>(penalties)); }
	public Penalty getFOEXPPenalty() { return foexp_penalty; }
	
	public void AddPenaltyModel(String id, boolean foulout_explusion, int period, int jam, String code) {
		synchronized (penaltiesLock) {
			if (foulout_explusion && code != null) {
					Penalty prev = foexp_penalty;
					id = UUID.randomUUID().toString();
					if (prev != null) {
						id = prev.getId();
					}
					foexp_penalty = new DefaultPenaltyModel(id, period, jam, code);
					scoreBoardChange(new ScoreBoardEvent(getSkater(), EVENT_PENALTY_FOEXP, foexp_penalty, null));
			} else if (foulout_explusion && code == null) {
				Penalty prev = foexp_penalty;
				foexp_penalty = null;
				scoreBoardChange(new ScoreBoardEvent(getSkater(), EVENT_PENALTY_REMOVE_FOEXP, null, prev));
			} else if (id == null ) {
				id = UUID.randomUUID().toString();
				// Non FO/Exp, make sure skater has 9 or less regular penalties before adding another
				if (penalties.size() < 9) {
					DefaultPenaltyModel dpm = new DefaultPenaltyModel(id, period, jam, code);
					penalties.add(dpm);
					sortPenalties();
					scoreBoardChange(new ScoreBoardEvent(getSkater(), EVENT_PENALTY, getPenalties(), null));
				}
			} else {
				// Updating/Deleting existing Penalty.	Find it and process
				for (DefaultPenaltyModel p2 : penalties) {
					if (p2.getId().equals(id)) {
						if (code != null) {
							p2.period = period;
							p2.jam = jam;
							p2.code = code;
							sortPenalties();
							scoreBoardChange(new ScoreBoardEvent(getSkater(), EVENT_PENALTY, getPenalties(), null));
						} else {
							penalties.remove(p2);
							scoreBoardChange(new ScoreBoardEvent(getSkater(), EVENT_REMOVE_PENALTY, null, p2));
						}
						return;
					}
				}
				// Penalty has an ID we don't have likely from the autosave, add it.
				DefaultPenaltyModel dpm = new DefaultPenaltyModel(id, period, jam, code);
				penalties.add(dpm);
				sortPenalties();
				scoreBoardChange(new ScoreBoardEvent(getSkater(), EVENT_PENALTY, getPenalties(), null));
			}
		}
	}
	
	private void sortPenalties() {
		Collections.sort(penalties, new Comparator<DefaultPenaltyModel>() {

		@Override
		public int compare(DefaultPenaltyModel a, DefaultPenaltyModel b) {
			int periodSort = Integer.valueOf(a.period).compareTo(b.period);
			
			if(periodSort != 0) {
				return periodSort;
			} else {
				return Integer.valueOf(a.jam).compareTo(b.jam);
			}
		}
			
		});
	}


	public void bench() {
		synchronized (positionLock) {
	
			if (!penaltyBox)
				setPosition(Position.ID_BENCH);
			else if (position.equals(Position.ID_PIVOT) && teamModel.isStarPass())
				setPosition(Position.ID_JAMMER);
		}
	}
	public SkaterSnapshotModel snapshot() {
		return new DefaultSkaterSnapshotModel(this);
	}
	public void restoreSnapshot(SkaterSnapshotModel s) {
		if (s.getId() != getId()) {	return; }
		setPosition(s.getPosition());
		setPenaltyBox(s.isPenaltyBox());
	}

	protected TeamModel teamModel;

	protected String id;
	protected String name;
	protected String number;
	protected String position = Position.ID_BENCH;
	protected boolean penaltyBox = false;
	protected String flags;
	protected List<DefaultPenaltyModel> penalties = new LinkedList<DefaultPenaltyModel>();
	protected PenaltyModel foexp_penalty;

	protected Object nameLock = new Object();
	protected Object numberLock = new Object();
	protected Object positionLock = new Object();
	protected Object flagsLock = new Object();
	protected Object penaltiesLock = new Object();

	protected boolean settingPositionSkater = false;

	public class DefaultPenaltyModel extends DefaultScoreBoardEventProvider implements PenaltyModel
	{ 
		public DefaultPenaltyModel(String i, int p, int j, String c) {
			id = i;
			period = p;
			jam = j;
			code = c;
		}
		public String getId() { return id; }
		public int getPeriod() { return period; }
		public int getJam() { return jam; }
		public String getCode() { return code; }

		public String getProviderName() { return "Penalty"; }
		public Class<Penalty> getProviderClass() { return Penalty.class; }
		public String getProviderId() { return getId(); }

		protected String id;
		protected int period;
		protected int jam;
		protected String code;
	}

	public static class DefaultSkaterSnapshotModel implements SkaterSnapshotModel {
		private DefaultSkaterSnapshotModel(SkaterModel skater) {
			id = skater.getId();
			position = skater.getPosition();
			box = skater.isPenaltyBox();
		}

		public String getId( ) { return id; }
		public String getPosition() { return position; }
		public boolean isPenaltyBox() { return box; }

		protected String id;
		protected String position;
		protected boolean box;

	}

}
