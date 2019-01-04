package com.carolinarollergirls.scoreboard.core.impl;
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
import java.util.HashMap;
import java.util.List;
import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.FloorPosition;
import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Stats;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.utils.PropertyConversion;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;

public class StatsImpl extends DefaultScoreBoardEventProvider implements Stats {
    public StatsImpl(ScoreBoard sb) {
        scoreBoard = sb;
        children.put(Child.PERIOD, new HashMap<String, ValueWithId>());

        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_PERIOD, Clock.Value.NUMBER, periodNumberListener));
        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_JAM, Clock.Value.NUMBER, jamNumberListener));
        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_JAM, Clock.Value.RUNNING, true, jamStartListener));
        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Clock.class, Clock.ID_JAM, Clock.Value.RUNNING, false, jamStopListener));

        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Team.class, Team.Value.SCORE, teamEventListener));
        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Team.class, Team.Value.LAST_SCORE, teamEventListener));
        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Team.class, Team.Value.LEAD_JAMMER, teamEventListener));
        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Team.class, Team.Value.STAR_PASS, teamEventListener));
        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Team.class, Team.Value.NO_PIVOT, teamEventListener));
        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Team.class, Team.Value.TIMEOUTS, teamEventListener));
        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Team.class, Team.Value.OFFICIAL_REVIEWS, teamEventListener));

        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Position.class, Position.Value.SKATER, positionEventListener));
        scoreBoard.addScoreBoardListener(new ConditionalScoreBoardListener(Position.class, Position.Value.PENALTY_BOX, positionEventListener));

        reset();
    }

    public ScoreBoard getScoreBoard() { return scoreBoard; }
    public String getProviderName() { return PropertyConversion.toFrontend(ScoreBoard.Child.STATS); }
    public Class<Stats> getProviderClass() { return Stats.class; }
    public String getProviderId() { return ""; }
    public ScoreBoardEventProvider getParent() { return scoreBoard; }
    public List<Class<? extends Property>> getProperties() { return properties; }
    
    public ValueWithId create(AddRemoveProperty prop, String id) {
	return new PeriodStatsImpl(this, Integer.valueOf(id));
    }

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
                for (FloorPosition fp : FloorPosition.values()) {
                    Skater s = t.getPosition(fp).getSkater();
                    if (s != null) {
                	ts.addSkaterStats(s.getId());
                	SkaterStats ssm = ts.getSkaterStats(s.getId());
                	ssm.setPosition(fp.toString());
                	ssm.setPenaltyBox(s.isPenaltyBox());
                    }
                }
                ts.setTotalScore(t.getScore());
                ts.setJamScore(t.getScore() - t.getLastScore());
                ts.setLeadJammer(t.getLeadJammer());
                ts.setStarPass(t.isStarPass());
                ts.setNoPivot(t.hasNoPivot());
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
                ts.setNoPivot(t.hasNoPivot());
            }
            ts.setTimeouts(t.getTimeouts());
            ts.setOfficialReviews(t.getOfficialReviews());
            requestBatchEnd();
        }
    };

    protected ScoreBoardListener positionEventListener = new ScoreBoardListener() {
        public void scoreBoardChange(ScoreBoardEvent event) {
            Clock jc = scoreBoard.getClock(Clock.ID_JAM);
            Position p = (Position)event.getProvider();
            Property prop = event.getProperty();
            JamStats js = getCurentJam();
            if (js == null) {
                return;
            }
            TeamStats ts = js.getTeamStats(p.getTeam().getId());
            requestBatchStart();
            if (jc.isRunning()) {
                // If the jam is over, any skater changes are for the next jam.
                // We'll catch them when the jam starts.
                if (p.getSkater() != null && prop == Position.Value.PENALTY_BOX) {
                    SkaterStats ss = ts.getSkaterStats(p.getSkater().getId());
                    if (ss != null) {
                	ss.setPenaltyBox((Boolean)event.getValue());
                    }
                } else if (prop == Position.Value.SKATER) {
                    Skater s = (Skater)event.getValue();
                    Skater last = (Skater)event.getPreviousValue();
                    if (last != null) {
                	ts.removeSkaterStats(last.getId());
                    } 
                    if (s != null) {
                	ts.addSkaterStats(s.getId());
                	SkaterStats ss = ts.getSkaterStats(s.getId());
                	ss.setPosition(s.getPosition().getFloorPosition().toString());
                	ss.setPenaltyBox(s.isPenaltyBox());
                    }
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
            requestBatchStart();
            for (int i = getAll(Child.PERIOD).size(); i < n; i++) {
        	get(Child.PERIOD, String.valueOf(i+1), true);
            }
            requestBatchEnd();
        }
    }

    public void truncateAfterNPeriods(int n) {
        synchronized (coreLock) {
            requestBatchStart();
            for (int i = getAll(Child.PERIOD).size(); i > n; i--) {
        	remove(Child.PERIOD, getPeriodStats(i));
            }
            requestBatchEnd();
        }
    }

    public PeriodStats getPeriodStats(int p) { return (PeriodStats)get(Child.PERIOD, String.valueOf(p)); }

    protected ScoreBoard scoreBoard;

    protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	add(Child.class);
    }};


    public class PeriodStatsImpl extends DefaultScoreBoardEventProvider implements PeriodStats {
        public PeriodStatsImpl(Stats s, int p) {
            children.put(Child.JAM, new HashMap<String, ValueWithId>());
            stats = s;
            period = p;
        }

        public String getProviderName() { return PropertyConversion.toFrontend(Stats.Child.PERIOD); }
        public Class<PeriodStats> getProviderClass() { return PeriodStats.class; }
        public String getProviderId() { return String.valueOf(period); }
        public ScoreBoardEventProvider getParent() { return stats; }
        public List<Class<? extends Property>> getProperties() { return properties; }
        
        public ValueWithId create(AddRemoveProperty prop, String id) {
            return new JamStatsImpl(this, Integer.valueOf(id));
        }

        public int getPeriodNumber() { return period; }

        public void ensureAtLeastNJams(int n) {
            synchronized (coreLock) {
        	requestBatchStart();
        	for (int i = getAll(Child.JAM).size(); i < n; i++) {
        	    get(Child.JAM, String.valueOf(i+1), true);
        	}
        	requestBatchEnd();
            }
        }

        public void truncateAfterNJams(int n) {
            synchronized (coreLock) {
                requestBatchStart();
                for (int i = getAll(Child.JAM).size(); i > n; i--) {
                    remove(Child.JAM, getJamStats(i));
                }
                requestBatchEnd();
            }
        }

        public JamStats getJamStats(int j) { return (JamStats)get(Child.JAM, String.valueOf(j)); }

        private Stats stats;
        private int period;

        protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
            add(Child.class);
        }};
    }


    public class JamStatsImpl extends DefaultScoreBoardEventProvider implements JamStats {
        public JamStatsImpl(PeriodStats p, int j) {
            period = p;
            jam = j;
            children.put(Child.TEAM, new HashMap<String, ValueWithId>());
            add(Child.TEAM, new TeamStatsImpl(Team.ID_1, this));
            add(Child.TEAM, new TeamStatsImpl(Team.ID_2, this));
        }

        public String getProviderName() { return PropertyConversion.toFrontend(PeriodStats.Child.JAM); }
        public Class<JamStats> getProviderClass() { return JamStats.class; }
        public String getProviderId() { return String.valueOf(jam); }
        public ScoreBoardEventProvider getParent() { return period; }
        public List<Class<? extends Property>> getProperties() { return properties; }

        public Object valueFromString(PermanentProperty prop, String sValue) {
            return Long.parseLong(sValue);
        }
        
        public int getPeriodNumber() { return period.getPeriodNumber(); }
        public int getJamNumber() { return jam; }

        public long getJamClockElapsedEnd() { return (Long)get(Value.JAM_CLOCK_ELAPSED_END); }
        public void setJamClockElapsedEnd(long t) { set(Value.JAM_CLOCK_ELAPSED_END, t); }

        public long getPeriodClockElapsedStart() { return (Long)get(Value.PERIOD_CLOCK_ELAPSED_START); }
        public void setPeriodClockElapsedStart(long t) { set(Value.PERIOD_CLOCK_ELAPSED_START, t); }

        public long getPeriodClockElapsedEnd() { return (Long)get(Value.PERIOD_CLOCK_ELAPSED_END); }
        public void setPeriodClockElapsedEnd(long t) { set(Value.PERIOD_CLOCK_ELAPSED_END, t); }

        public long getPeriodClockWalltimeStart() { return (Long)get(Value.PERIOD_CLOCK_WALLTIME_START); }
        public void setPeriodClockWalltimeStart(long t) { set(Value.PERIOD_CLOCK_WALLTIME_START, t); }

        public long getPeriodClockWalltimeEnd() { return (Long)get(Value.PERIOD_CLOCK_WALLTIME_END); }
        public void setPeriodClockWalltimeEnd(long t) { set(Value.PERIOD_CLOCK_WALLTIME_END, t); }

        public TeamStats getTeamStats(String id) { return (TeamStats)get(Child.TEAM, id); }

        private PeriodStats period;
        private int jam;

        protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
            add(Value.class);
            add(Child.class);
        }};
    }

    public class TeamStatsImpl extends DefaultScoreBoardEventProvider implements TeamStats {
        public TeamStatsImpl(String team_id, JamStats j) {
            children.put(Child.SKATER, new HashMap<String, ValueWithId>());
            values.put(Value.ID, team_id);
            jam = j;
        }

        public String getProviderName() { return PropertyConversion.toFrontend(JamStats.Child.TEAM); }
        public Class<TeamStats> getProviderClass() { return TeamStats.class; }
        public String getProviderId() { return getTeamId(); }
        public ScoreBoardEventProvider getParent() { return jam; }
        public List<Class<? extends Property>> getProperties() { return properties; }

        public Object valueFromString(PermanentProperty prop, String sValue) {
            if (prop == Value.STAR_PASS) { return Boolean.parseBoolean(sValue); }
            if (prop == Value.JAM_SCORE || prop == Value.TOTAL_SCORE || prop == Value.TIMEOUTS
        	    || prop == Value.OFFICIAL_REVIEWS) { return Integer.parseInt(sValue); }
            return sValue;
        }
        
        public boolean set(PermanentProperty prop, Object value, Flag flag) {
            if (!(prop instanceof Value) || prop == Value.ID) { return false; }
            return super.set(prop, value, flag);
        }
        
        public ValueWithId create(AddRemoveProperty prop, String id) {
            return new SkaterStatsImpl(id, this);
        }

        public String getTeamId() { return (String)get(Value.ID); }
        public int getPeriodNumber() { return jam.getPeriodNumber(); }
        public int getJamNumber() { return jam.getJamNumber(); }

        public int getJamScore() { return (Integer)get(Value.JAM_SCORE); }
        public void setJamScore(int s) { set(Value.JAM_SCORE, s); }

        public int getTotalScore() { return (Integer)get(Value.TOTAL_SCORE); }
        public void setTotalScore(int s) { set(Value.TOTAL_SCORE, s); }

        public String getLeadJammer() { return (String)get(Value.LEAD_JAMMER); }
        public void setLeadJammer(String ls) { set(Value.LEAD_JAMMER, ls); }

        public boolean getStarPass() { return (Boolean)get(Value.STAR_PASS); }
        public void setStarPass(boolean sp) { set(Value.STAR_PASS, sp); }

        public boolean getNoPivot() { return (Boolean)get(Value.NO_PIVOT); }
        public void setNoPivot(boolean np) { set(Value.NO_PIVOT, np); }

        public int getTimeouts() { return (Integer)get(Value.TIMEOUTS); }
        public void setTimeouts(int t) { set(Value.TIMEOUTS, t); }

        public int getOfficialReviews() { return (Integer)get(Value.OFFICIAL_REVIEWS); }
        public void setOfficialReviews(int o) { set(Value.OFFICIAL_REVIEWS, o); }

        public SkaterStats getSkaterStats (String sid) { return (SkaterStats)get(Child.SKATER, sid); }
        public void addSkaterStats(String sid) { get(Child.SKATER, sid, true); }
        public void removeSkaterStats(String sid) { remove(Child.SKATER, sid); }
        public void removeSkaterStats() { removeAll(Child.SKATER); }

        private JamStats jam;

        protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
            add(Value.class);
            add(Child.class);
        }};
    }

    public class SkaterStatsImpl extends DefaultScoreBoardEventProvider implements SkaterStats {
        public SkaterStatsImpl(String skater_id, TeamStats team) {
            values.put(Value.ID, skater_id);
            this.team = team;
            setPenaltyBox(false);
        }

        public String getProviderName() { return PropertyConversion.toFrontend(TeamStats.Child.SKATER); }
        public Class<SkaterStats> getProviderClass() { return SkaterStats.class; }
        public String getProviderId() { return getSkaterId(); }
        public ScoreBoardEventProvider getParent() { return team; }
        public List<Class<? extends Property>> getProperties() { return properties; }
        
        public Object valueFromString(PermanentProperty prop, String sValue) {
            if (prop == Value.PENALTY_BOX) { return Boolean.parseBoolean(sValue); }
            return sValue;
        }
        
        public boolean set(PermanentProperty prop, Object value, Flag flag) {
            if (!(prop instanceof Value) || prop == Value.ID) { return false; }
            return super.set(prop, value, flag);
        }

        public String getSkaterId() { return (String)get(Value.ID); }
        public String getTeamId() { return team.getTeamId(); }
        public int getPeriodNumber() { return team.getPeriodNumber(); }
        public int getJamNumber() { return team.getJamNumber(); }

        public String getPosition() { return (String)get(Value.POSITION); }
        public void setPosition(String p) { set(Value.POSITION, p); }

        public boolean getPenaltyBox() { return (Boolean)get(Value.PENALTY_BOX); }
        public void setPenaltyBox(boolean p) { set(Value.PENALTY_BOX, p); }

        private TeamStats team;

        protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
            add(Value.class);
        }};
    }
}
