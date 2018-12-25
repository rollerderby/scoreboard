package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.List;

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.MultiProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.SingleProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public interface Stats extends ScoreBoardEventProvider {
    public ScoreBoard getScoreBoard();
    public void reset();

    public void ensureAtLeastNPeriods(int n);
    public void truncateAfterNPeriods(int n);

    public List<PeriodStats> getPeriodStats();
    public PeriodStats getPeriodStats(int p);

    public static interface PeriodStats extends ScoreBoardEventProvider {
        public void ensureAtLeastNJams(int n);
        public void truncateAfterNJams(int n);

        public int getPeriodNumber();

        public List<JamStats> getJamStats();
        public JamStats getJamStats(int j);

        public enum Child implements MultiProperty {
            JAM;
        }
    }

    public static interface JamStats extends ScoreBoardEventProvider {
        public int getPeriodNumber();
        public int getJamNumber();

        public long getJamClockElapsedEnd();
        public void setJamClockElapsedEnd(long t);
        public long getPeriodClockElapsedStart();
        public void setPeriodClockElapsedStart(long t);
        public long getPeriodClockElapsedEnd();
        public void setPeriodClockElapsedEnd(long t);
        public long getPeriodClockWalltimeStart();
        public void setPeriodClockWalltimeStart(long t);
        public long getPeriodClockWalltimeEnd();
        public void setPeriodClockWalltimeEnd(long t);

        public List<TeamStats> getTeamStats();
        public TeamStats getTeamStats(String id);

        public enum Value implements SingleProperty {
            STATS;
        }
        public enum Child implements MultiProperty {
            TEAM;
        }
    }

    public static interface TeamStats extends ScoreBoardEventProvider {
        public int getPeriodNumber();
        public int getJamNumber();
        public String getTeamId();

        public int getJamScore();
        public void setJamScore(int s);
        public int getTotalScore();
        public void setTotalScore(int s);
        public String getLeadJammer();
        public void setLeadJammer(String ls);
        public boolean getStarPass();
        public void setStarPass(boolean sp);
        public boolean getNoPivot();
        public void setNoPivot(boolean np);
        public int getTimeouts();
        public void setTimeouts(int t);
        public int getOfficialReviews();
        public void setOfficialReviews(int o);

        public List<SkaterStats> getSkaterStats();
        public SkaterStats getSkaterStats(String sid);
        public void addSkaterStats(String sid);
        public void removeSkaterStats(String sid);
        public void removeSkaterStats();

        public enum Value implements SingleProperty {
            STATS;
        }
        public enum Child implements MultiProperty {
            SKATER;
        }
    }

    public static interface SkaterStats extends ScoreBoardEventProvider {
        public int getPeriodNumber();
        public int getJamNumber();
        public String getTeamId();
        public String getSkaterId();

        public boolean getPenaltyBox();
        public void setPenaltyBox(boolean p);
        public String getPosition();
        public void setPosition(String p);

        public enum Value implements SingleProperty {
            STATS;
        }
    }

    public enum Child implements MultiProperty {
        PERIOD;
    }
}
