package com.carolinarollergirls.scoreboard.model;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.view.PositionNotFoundException;
import com.carolinarollergirls.scoreboard.view.Skater;

public interface SkaterModel extends Skater {
    public TeamModel getTeamModel();
    public Skater getSkater();

    public void bench();
    public SkaterSnapshotModel snapshot();
    public void restoreSnapshot(SkaterSnapshotModel s);

    public void setName(String id);
    public void setNumber(String number);
    public void setPosition(String position) throws PositionNotFoundException;
    public void setPenaltyBox(boolean box);
    public void setFlags(String flags);

    // A null code removes the penalty.
    public void AddPenaltyModel(String id, boolean foulout_explusion, int period, int jam, String code);

    public static interface PenaltyModel extends Penalty {
    }

    public static interface SkaterSnapshotModel	{
        public String getId();
        public String getPosition();
        public boolean isPenaltyBox();
    }
}
