package com.carolinarollergirls.scoreboard.core;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;

public interface Stats extends ScoreBoardEventProvider {
    public ScoreBoard getScoreBoard();
    public void reset();

    public void ensureAtLeastNPeriods(int n);
    public void truncateAfterNPeriods(int n);

    public PeriodStats getPeriodStats(int p);

    public enum Child implements AddRemoveProperty {
        PERIOD;
    }

    public static interface PeriodStats extends ScoreBoardEventProvider {
        public void ensureAtLeastNJams(int n);
        public void truncateAfterNJams(int n);

        public int getPeriodNumber();

        public JamStats getJamStats(int j);

        public enum Child implements AddRemoveProperty {
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

        public TeamStats getTeamStats(String id);

        public enum Value implements PermanentProperty {
            JAM_CLOCK_ELAPSED_END,
            PERIOD_CLOCK_ELAPSED_START,
            PERIOD_CLOCK_ELAPSED_END,
            PERIOD_CLOCK_WALLTIME_START,
            PERIOD_CLOCK_WALLTIME_END;
        }
        public enum Child implements AddRemoveProperty {
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

        public SkaterStats getSkaterStats(String sid);
        public void addSkaterStats(String sid);
        public void removeSkaterStats(String sid);
        public void removeSkaterStats();

        public enum Value implements PermanentProperty {
            ID,
            JAM_SCORE,
            TOTAL_SCORE,
            LEAD_JAMMER,
            STAR_PASS,
            NO_PIVOT,
            TIMEOUTS,
            OFFICIAL_REVIEWS;
        }
        public enum Child implements AddRemoveProperty {
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

        public enum Value implements PermanentProperty {
            ID,
            POSITION,
            PENALTY_BOX;
        }
    }
}
