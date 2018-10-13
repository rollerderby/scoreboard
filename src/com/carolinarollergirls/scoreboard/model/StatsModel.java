package com.carolinarollergirls.scoreboard.model;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.view.ScoreBoard;
import com.carolinarollergirls.scoreboard.view.Stats;

public interface StatsModel extends Stats {
    public ScoreBoard getScoreBoard();
    public void reset();

    public void ensureAtLeastNPeriods(int n);
    public void truncateAfterNPeriods(int n);
    public PeriodStatsModel getPeriodStatsModel(int p);

    public static interface PeriodStatsModel extends PeriodStats {
        public void ensureAtLeastNJams(int n);
        public void truncateAfterNJams(int n);

        public JamStatsModel getJamStatsModel(int j);
    }

    public static interface JamStatsModel extends JamStats {
        public TeamStatsModel getTeamStatsModel(String id);

        public void setJamClockElapsedEnd(long t);
        public void setPeriodClockElapsedStart(long t);
        public void setPeriodClockElapsedEnd(long t);
        public void setPeriodClockWalltimeStart(long t);
        public void setPeriodClockWalltimeEnd(long t);
    }

    public static interface TeamStatsModel extends TeamStats {
        public void setJamScore(int s);
        public void setTotalScore(int s);
        public void setLeadJammer(String ls);
        public void setStarPass(boolean sp);
        public void setTimeouts(int t);
        public void setOfficialReviews(int o);

        public SkaterStatsModel getSkaterStatsModel(String sid);
        public void addSkaterStatsModel(String sid);
        public void removeSkaterStatsModel(String sid);
        public void removeSkaterStatsModels();
    }

    public static interface SkaterStatsModel extends SkaterStats {
        public void setPenaltyBox(boolean p);
        public void setPosition(String p);
    }
}
