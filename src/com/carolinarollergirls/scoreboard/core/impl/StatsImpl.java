package com.carolinarollergirls.scoreboard.core.impl;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.Fielding;
import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Period;
import com.carolinarollergirls.scoreboard.core.Position;
import com.carolinarollergirls.scoreboard.core.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.core.Stats;
import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.event.ConditionalScoreBoardListener;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.Property;
import com.carolinarollergirls.scoreboard.event.ScoreBoardListener;

public class StatsImpl extends ScoreBoardEventProviderImpl implements Stats {
    public StatsImpl(ScoreBoard sb) {
	super(sb, ScoreBoard.Child.STATS, Stats.class);

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

    public String getId() { return ""; }
    
    public void reset() {
    }

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
        return getPeriod(p).getJam(j);
    }

    public Period getPeriod(int p) { return scoreBoard.getPeriod(p); }
}
