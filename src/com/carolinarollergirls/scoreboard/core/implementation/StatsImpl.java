package com.carolinarollergirls.scoreboard.core.implementation;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;

import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Stats;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;

public class StatsImpl extends DefaultScoreBoardEventProvider implements Stats {
    public StatsImpl(ScoreBoard sb) {
        scoreBoard = sb;

        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_PERIOD, Clock.EVENT_NUMBER, periodNumberListener));
        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_JAM, Clock.EVENT_NUMBER, jamNumberListener));
        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_JAM, Clock.EVENT_RUNNING, true, jamStartListener));
        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_JAM, Clock.EVENT_RUNNING, false, jamStopListener));

        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Team.class, Team.EVENT_SCORE, teamEventListener));
        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Team.class, Team.EVENT_LAST_SCORE, teamEventListener));
        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Team.class, Team.EVENT_LEAD_JAMMER, teamEventListener));
        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Team.class, Team.EVENT_STAR_PASS, teamEventListener));
        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Team.class, Team.EVENT_TIMEOUTS, teamEventListener));
        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Team.class, Team.EVENT_OFFICIAL_REVIEWS, teamEventListener));

        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Skater.class, Skater.EVENT_POSITION, skaterEventListener));
        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Skater.class, Skater.EVENT_PENALTY_BOX, skaterEventListener));

        reset();
    }

    public ScoreBoard getScoreBoard() { return scoreBoard; }
    public String getProviderName() { return "Stats"; }
    public Class<Stats> getProviderClass() { return Stats.class; }
    public String getProviderId() { return "Stats"; }

    public void reset() {
        synchronized (coreLock) {
            truncateAfterNPeriods(0);
        }
    }

    protected ScoreBoardListener periodNumberListener = new ScoreBoardListener() {
        public void scoreBoardChange(ScoreBoardEvent event) {
            // If the period number has dropped, we need to delete periods.
            Clock pc = scoreBoard.getClock(Clock.ID_PERIOD);
            truncateAfterNPeriods(pc.getNumber());
        }
    };

    protected ScoreBoardListener jamNumberListener = new ScoreBoardListener() {
        public void scoreBoardChange(ScoreBoardEvent event) {
            // If the jam number has dropped, we need to delete jams.
            int p = scoreBoard.getClock(Clock.ID_PERIOD).getNumber();
            int j = scoreBoard.getClock(Clock.ID_JAM).getNumber();
            ensureAtLeastNPeriods(p);
            PeriodStats period = getPeriodStats(p);
            period.truncateAfterNJams(j);
        }
    };

    protected ScoreBoardListener jamStartListener = new ScoreBoardListener() {
        public void scoreBoardChange(ScoreBoardEvent event) {
            Clock pc = scoreBoard.getClock(Clock.ID_PERIOD);
            JamStats js = getCurentJam();
            if (js == null) {
                return;
            }
            requestBatchStart();
            js.setPeriodClockElapsedStart(pc.getTimeElapsed());
            js.setPeriodClockWalltimeStart(System.currentTimeMillis());

            // Update all skater position, as they may have changed since
            // the previous jam ended. Also initalise other settings.
            for(String tid : Arrays.asList(Team.ID_1, Team.ID_2)) {
                TeamStats ts = js.getTeamStats(tid);
                Team t = scoreBoard.getTeam(tid);
                ts.removeSkaterStats();
                for (Position p : t.getPositions()) {
                    String pos = p.getId();
                    if (Position.FLOOR_POSITIONS.contains(pos)) {
                        Skater s = p.getSkater();
                        if (s != null) {
                            ts.addSkaterStats(s.getId());
                            SkaterStats ssm = ts.getSkaterStats(s.getId());
                            ssm.setPosition(s.getPosition());
                            ssm.setPenaltyBox(s.isPenaltyBox());
                        }
                    }
                }
                ts.setTotalScore(t.getScore());
                ts.setJamScore(t.getScore() - t.getLastScore());
                ts.setLeadJammer(t.getLeadJammer());
                ts.setStarPass(t.isStarPass());
                ts.setTimeouts(t.getTimeouts());
                ts.setOfficialReviews(t.getOfficialReviews());
            }

            requestBatchEnd();
        }
    };

    protected ScoreBoardListener jamStopListener = new ScoreBoardListener() {
        public void scoreBoardChange(ScoreBoardEvent event) {
            Clock pc = scoreBoard.getClock(Clock.ID_PERIOD);
            Clock jc = scoreBoard.getClock(Clock.ID_JAM);
            JamStats js = getCurentJam();
            if (js == null) {
                return;
            }
            requestBatchStart();
            js.setJamClockElapsedEnd(jc.getTimeElapsed());
            js.setPeriodClockElapsedEnd(pc.getTimeElapsed());
            js.setPeriodClockWalltimeEnd(System.currentTimeMillis());
            requestBatchEnd();
        }
    };

    protected ScoreBoardListener teamEventListener = new ScoreBoardListener() {
        public void scoreBoardChange(ScoreBoardEvent event) {
            Clock jc = scoreBoard.getClock(Clock.ID_JAM);
            Team t = (Team)event.getProvider();
            JamStats js = getCurentJam();
            if (js == null) {
                return;
            }
            TeamStats ts = js.getTeamStats(t.getId());

            requestBatchStart();
            ts.setTotalScore(t.getScore());
            ts.setJamScore(t.getScore() - t.getLastScore());
            if (jc.isRunning()) {
                // Only set lead/star pass during a jam, to avoid
                // resetting it at the end of a jam.
                ts.setLeadJammer(t.getLeadJammer());
                ts.setStarPass(t.isStarPass());
            }
            ts.setTimeouts(t.getTimeouts());
            ts.setOfficialReviews(t.getOfficialReviews());
            requestBatchEnd();
        }
    };

    protected ScoreBoardListener skaterEventListener = new ScoreBoardListener() {
        public void scoreBoardChange(ScoreBoardEvent event) {
            Clock jc = scoreBoard.getClock(Clock.ID_JAM);
            Skater s = (Skater)event.getProvider();
            String prop = event.getProperty();
            JamStats js = getCurentJam();
            if (js == null) {
                return;
            }
            TeamStats ts = js.getTeamStats(s.getTeam().getId());
            requestBatchStart();
            if (jc.isRunning()) {
                // If the jam is over, any skater changes are for the next jam.
                // We'll catch them when the jam starts.
                if (s.getPosition().equals(Position.ID_BENCH)
                        || prop.equals(Team.EVENT_REMOVE_SKATER)) {
                    ts.removeSkaterStats(s.getId());
                } else {
                    ts.addSkaterStats(s.getId());
                    SkaterStats ssm = ts.getSkaterStats(s.getId());
                    ssm.setPosition(s.getPosition());
                    ssm.setPenaltyBox(s.isPenaltyBox());
                }
            }
            requestBatchEnd();
        }
    };

    protected JamStats getCurentJam() {
        int p = scoreBoard.getClock(Clock.ID_PERIOD).getNumber();
        int j = scoreBoard.getClock(Clock.ID_JAM).getNumber();
        if (j == 0) {
            return null;
        }
        ensureAtLeastNPeriods(p);
        PeriodStats period = getPeriodStats(p);
        period.ensureAtLeastNJams(j);
        return period.getJamStats(j);
    }

    public void ensureAtLeastNPeriods(int n) {
        synchronized (coreLock) {
            while (periods.size() < n) {
                PeriodStats psm = new PeriodStatsImpl(periods.size() + 1);
                psm.addScoreBoardListener(this);
                periods.add(psm);
                scoreBoardChange(new ScoreBoardEvent(this, Stats.EVENT_ADD_PERIOD, psm, null));
            }
        }
    }

    public void truncateAfterNPeriods(int n) {
        synchronized (coreLock) {
            requestBatchStart();
            while (periods.size() > n) {
                PeriodStats psm = periods.get(periods.size() - 1);
                psm.removeScoreBoardListener(this);
                periods.remove(psm);
                scoreBoardChange(new ScoreBoardEvent(this, Stats.EVENT_REMOVE_PERIOD, psm, null));
            }
            requestBatchEnd();
        }
    }

    public List<PeriodStats> getPeriodStats() {
        synchronized (coreLock) {
            return Collections.unmodifiableList(new ArrayList<PeriodStats>(periods));
        }
    }
    public PeriodStats getPeriodStats(int p) {
        synchronized (coreLock) {
            return periods.get(p - 1);
        }
    }

    protected ScoreBoard scoreBoard;

    protected static Object coreLock = ScoreBoardImpl.getCoreLock();

    protected List<PeriodStats> periods = new ArrayList<PeriodStats>();


    public class PeriodStatsImpl extends DefaultScoreBoardEventProvider implements PeriodStats {
        public PeriodStatsImpl(int p) {
            period = p;
        }

        public String getProviderName() { return "PeriodStats"; }
        public Class<PeriodStats> getProviderClass() { return PeriodStats.class; }
        public String getProviderId() { return "PeriodStats"; }

        public int getPeriodNumber() { return period; }

        public void ensureAtLeastNJams(int n) {
            synchronized (coreLock) {
                while (jams.size() < n) {
                    JamStats jsm = new JamStatsImpl(getPeriodNumber(), jams.size() + 1);
                    jsm.addScoreBoardListener(this);
                    jams.add(jsm);
                    scoreBoardChange(new ScoreBoardEvent(this, EVENT_ADD_JAM, jsm, null));
                }
            }
        }

        public void truncateAfterNJams(int n) {
            synchronized (coreLock) {
                requestBatchStart();
                while (jams.size() > n) {
                    JamStats jsm = jams.get(jams.size() - 1);
                    jsm.removeScoreBoardListener(this);
                    jams.remove(jams.size() - 1);
                    scoreBoardChange(new ScoreBoardEvent(this, EVENT_REMOVE_JAM, jsm, null));
                }
                requestBatchEnd();
            }
        }

        public List<JamStats> getJamStats() {
            synchronized (coreLock) {
                return Collections.unmodifiableList(new ArrayList<JamStats>(jams));
            }
        }
        public JamStats getJamStats(int j) {
            synchronized (coreLock) {
                return jams.get(j - 1);
            }
        }

        private int period;
        protected List<JamStats> jams = new ArrayList<JamStats>();
    }


    public class JamStatsImpl extends DefaultScoreBoardEventProvider implements JamStats {
        public JamStatsImpl(int p, int j) {
            period = p;
            jam = j;
            teams = new TeamStatsImpl[2];
            teams[0] = new TeamStatsImpl(Team.ID_1, period, jam);
            teams[1] = new TeamStatsImpl(Team.ID_2, period, jam);
            teams[0].addScoreBoardListener(this);
            teams[1].addScoreBoardListener(this);
        }

        public String getProviderName() { return "JamStats"; }
        public Class<JamStats> getProviderClass() { return JamStats.class; }
        public String getProviderId() { return "JamStats"; }

        public int getPeriodNumber() { return period; }
        public int getJamNumber() { return jam; }

        public long getJamClockElapsedEnd() { return jamClockElapsedEnd; }
        public void setJamClockElapsedEnd(long t) {
            synchronized (coreLock) {
                jamClockElapsedEnd = t;
                scoreBoardChange(new ScoreBoardEvent(this, JamStats.EVENT_STATS, this, null));
            }
        }

        public long getPeriodClockElapsedStart() { return periodClockElapsedStart; }
        public void setPeriodClockElapsedStart(long t) {
            synchronized (coreLock) {
                periodClockElapsedStart = t;
                scoreBoardChange(new ScoreBoardEvent(this, JamStats.EVENT_STATS, this, null));
            }
        }

        public long getPeriodClockElapsedEnd() { return periodClockElapsedEnd; }
        public void setPeriodClockElapsedEnd(long t) {
            synchronized (coreLock) {
                periodClockElapsedEnd = t;
                scoreBoardChange(new ScoreBoardEvent(this, JamStats.EVENT_STATS, this, null));
            }
        }

        public long getPeriodClockWalltimeStart() { return periodClockWalltimeStart; }
        public void setPeriodClockWalltimeStart(long t) {
            synchronized (coreLock) {
                periodClockWalltimeStart = t;
                scoreBoardChange(new ScoreBoardEvent(this, JamStats.EVENT_STATS, this, null));
            }
        }

        public long getPeriodClockWalltimeEnd() { return periodClockWalltimeEnd; }
        public void setPeriodClockWalltimeEnd(long t) {
            synchronized (coreLock) {
                periodClockWalltimeEnd = t;
                scoreBoardChange(new ScoreBoardEvent(this, JamStats.EVENT_STATS, this, null));
            }
        }

        public List<TeamStats> getTeamStats() {
            synchronized (coreLock) {
                return Collections.unmodifiableList(new ArrayList<TeamStats>(Arrays.asList(teams)));
            }
        }

        public TeamStats getTeamStats(String id) {
            synchronized (coreLock) {
                if (id.equals(Team.ID_1)) {
                    return teams[0];
                } else {
                    return teams[1];
                }
            }
        }

        private int period;
        private int jam;
        private long jamClockElapsedEnd;
        private long periodClockElapsedStart;
        private long periodClockElapsedEnd;
        private long periodClockWalltimeStart;
        private long periodClockWalltimeEnd;
        protected TeamStats teams[];
    }

    public class TeamStatsImpl extends DefaultScoreBoardEventProvider implements TeamStats {
        public TeamStatsImpl(String team_id, int p, int j) {
            id = team_id;
            period = p;
            jam = j;
        }

        public String getProviderName() { return "TeamStats"; }
        public Class<TeamStats> getProviderClass() { return TeamStats.class; }
        public String getProviderId() { return "TeamStats"; }

        public String getTeamId() { return id; }
        public int getPeriodNumber() { return period; }
        public int getJamNumber() { return jam; }

        public int getJamScore() { return jamScore; }
        public void setJamScore(int s) {
            synchronized (coreLock) {
                jamScore = s;
                scoreBoardChange(new ScoreBoardEvent(this, TeamStats.EVENT_STATS, this, null));
            }
        }

        public int getTotalScore() { return totalScore; }
        public void setTotalScore(int s) {
            synchronized (coreLock) {
                totalScore = s;
                scoreBoardChange(new ScoreBoardEvent(this, TeamStats.EVENT_STATS, this, null));
            }
        }

        public String getLeadJammer() { return leadStatus; }
        public void setLeadJammer(String ls) {
            synchronized (coreLock) {
                leadStatus = ls;
                scoreBoardChange(new ScoreBoardEvent(this, TeamStats.EVENT_STATS, this, null));
            }
        }

        public boolean getStarPass() { return starPass; }
        public void setStarPass(boolean sp) {
            synchronized (coreLock) {
                starPass = sp;
                scoreBoardChange(new ScoreBoardEvent(this, TeamStats.EVENT_STATS, this, null));
            }
        }

        public int getTimeouts() { return timeouts; }
        public void setTimeouts(int t) {
            synchronized (coreLock) {
                timeouts = t;
                scoreBoardChange(new ScoreBoardEvent(this, TeamStats.EVENT_STATS, this, null));
            }
        }

        public int getOfficialReviews() { return officialReviews; }
        public void setOfficialReviews(int o) {
            synchronized (coreLock) {
                officialReviews = o;
                scoreBoardChange(new ScoreBoardEvent(this, TeamStats.EVENT_STATS, this, null));
            }
        }

        public List<SkaterStats> getSkaterStats() {
            synchronized (coreLock) {
                return Collections.unmodifiableList(new ArrayList<SkaterStats>(skaters.values()));
            }
        }
        public SkaterStats getSkaterStats (String sid) {
            synchronized (coreLock) {
                return skaters.get(sid);
            }
        }
        public void addSkaterStats(String sid) {
            synchronized (coreLock) {
                if (skaters.get(sid) == null) {
                    SkaterStats ssm = new SkaterStatsImpl(sid, id, period, jam);
                    ssm.addScoreBoardListener(this);
                    skaters.put(sid, ssm);
                }
            }
        }
        public void removeSkaterStats(String sid) {
            synchronized (coreLock) {
                SkaterStats ssm = skaters.get(sid);
                if (ssm != null) {
                    ssm.removeScoreBoardListener(this);
                    skaters.remove(sid);
                    scoreBoardChange(new ScoreBoardEvent(this, TeamStats.EVENT_REMOVE_SKATER, ssm, null));
                }
            }
        }
        public void removeSkaterStats() {
            synchronized (coreLock) {
                for (SkaterStats ssm : skaters.values()) {
                    ssm.removeScoreBoardListener(this);
                    skaters.remove(ssm.getSkaterId());
                    scoreBoardChange(new ScoreBoardEvent(this, TeamStats.EVENT_REMOVE_SKATER, ssm, null));
                }
            }
        }


        private String id;
        private int period;
        private int jam;
        private int jamScore;
        private int totalScore;
        private String leadStatus;
        private boolean starPass;
        private int timeouts;
        private int officialReviews;
        private Map<String, SkaterStats>skaters = new ConcurrentHashMap<String, SkaterStats>();
    }

    public class SkaterStatsImpl extends DefaultScoreBoardEventProvider implements SkaterStats {
        public SkaterStatsImpl(String skater_id, String team_id, int p, int j) {
            id = skater_id;
            tid = team_id;
            period = p;
            jam = j;
        }

        public String getProviderName() { return "SkaterStats"; }
        public Class<SkaterStats> getProviderClass() { return SkaterStats.class; }
        public String getProviderId() { return "SkaterStats"; }

        public String getSkaterId() { return id; }
        public String getTeamId() { return tid; }
        public int getPeriodNumber() { return period; }
        public int getJamNumber() { return jam; }

        public String getPosition() { return position; }
        public void setPosition(String p) {
            synchronized (coreLock) {
                position = p;
                scoreBoardChange(new ScoreBoardEvent(this, SkaterStats.EVENT_STATS, this, null));
            }
        }

        public boolean getPenaltyBox() { return penaltyBox; }
        public void setPenaltyBox(boolean p) {
            synchronized (coreLock) {
                penaltyBox = p;
                scoreBoardChange(new ScoreBoardEvent(this, SkaterStats.EVENT_STATS, this, null));
            }
        }
        private String id;
        private String tid;
        private int period;
        private int jam;

        private boolean penaltyBox;
        private String position;
    }

}
