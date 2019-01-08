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
import com.carolinarollergirls.scoreboard.core.Fielding;
import com.carolinarollergirls.scoreboard.core.FloorPosition;
import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Period;
import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Stats;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.DefaultScoreBoardEventProvider;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.AddRemoveProperty;
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
    public String getId() { return ""; }
    public ScoreBoardEventProvider getParent() { return scoreBoard; }
    public List<Class<? extends Property>> getProperties() { return properties; }
    
    public ValueWithId create(AddRemoveProperty prop, String id) {
	return new PeriodImpl(this, Integer.valueOf(id));
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
            Period period = getPeriod(p);
            period.truncateAfterNJams(j);
        }
    };

    protected ScoreBoardListener jamStartListener = new ScoreBoardListener() {
        public void scoreBoardChange(ScoreBoardEvent event) {
            Clock pc = scoreBoard.getClock(Clock.ID_PERIOD);
            Jam js = getCurentJam();
            if (js == null) {
                return;
            }
            requestBatchStart();
            js.setPeriodClockElapsedStart(pc.getTimeElapsed());
            js.setPeriodClockWalltimeStart(System.currentTimeMillis());

            // Update all skater position, as they may have changed since
            // the previous jam ended. Also initalise other settings.
            for(String tid : Arrays.asList(Team.ID_1, Team.ID_2)) {
                TeamJam ts = js.getTeamJam(tid);
                Team t = scoreBoard.getTeam(tid);
                ts.removeFielding();
                for (FloorPosition fp : FloorPosition.values()) {
                    Skater s = t.getPosition(fp).getSkater();
                    if (s != null) {
                	ts.addFielding(s.getId());
                	Fielding ssm = ts.getFielding(s.getId());
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
            Jam js = getCurentJam();
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
            Jam js = getCurentJam();
            if (js == null) {
                return;
            }
            TeamJam ts = js.getTeamJam(t.getId());

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
            Jam js = getCurentJam();
            if (js == null) {
                return;
            }
            TeamJam ts = js.getTeamJam(p.getTeam().getId());
            requestBatchStart();
            if (jc.isRunning()) {
                // If the jam is over, any skater changes are for the next jam.
                // We'll catch them when the jam starts.
                if (p.getSkater() != null && prop == Position.Value.PENALTY_BOX) {
                    Fielding ss = ts.getFielding(p.getSkater().getId());
                    if (ss != null) {
                	ss.setPenaltyBox((Boolean)event.getValue());
                    }
                } else if (prop == Position.Value.SKATER) {
                    Skater s = (Skater)event.getValue();
                    Skater last = (Skater)event.getPreviousValue();
                    if (last != null) {
                	ts.removeFielding(last.getId());
                    } 
                    if (s != null) {
                	ts.addFielding(s.getId());
                	Fielding ss = ts.getFielding(s.getId());
                	ss.setPosition(s.getPosition().getFloorPosition().toString());
                	ss.setPenaltyBox(s.isPenaltyBox());
                    }
                }
            }
            requestBatchEnd();
        }
    };

    protected Jam getCurentJam() {
        int p = scoreBoard.getClock(Clock.ID_PERIOD).getNumber();
        int j = scoreBoard.getClock(Clock.ID_JAM).getNumber();
        if (j == 0) {
            return null;
        }
        ensureAtLeastNPeriods(p);
        Period period = getPeriod(p);
        period.ensureAtLeastNJams(j);
        return period.getJam(j);
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
        	remove(Child.PERIOD, getPeriod(i));
            }
            requestBatchEnd();
        }
    }

    public Period getPeriod(int p) { return (Period)get(Child.PERIOD, String.valueOf(p)); }

    protected ScoreBoard scoreBoard;

    protected List<Class<? extends Property>> properties = new ArrayList<Class<? extends Property>>() {{
	add(Child.class);
    }};
}
