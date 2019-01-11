package com.carolinarollergirls.scoreboard.core.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import com.carolinarollergirls.scoreboard.core.Team;
import com.carolinarollergirls.scoreboard.core.TeamJam;
import com.carolinarollergirls.scoreboard.core.Clock;
import com.carolinarollergirls.scoreboard.core.Fielding;
import com.carolinarollergirls.scoreboard.core.FloorPosition;
import com.carolinarollergirls.scoreboard.core.Jam;
import com.carolinarollergirls.scoreboard.core.Period;
import com.carolinarollergirls.scoreboard.core.Skater;
import com.carolinarollergirls.scoreboard.event.NumberedScoreBoardEventProviderImpl;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.PermanentProperty;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEvent.ValueWithId;
import com.carolinarollergirls.scoreboard.utils.ScoreBoardClock;

public class JamImpl extends NumberedScoreBoardEventProviderImpl<Jam> implements Jam {
    public JamImpl(Period p, String j) {
	super(p, Period.NChild.JAM, Jam.class, j, 0, Value.class, Child.class);
	values.put(Value.ID, UUID.randomUUID().toString());
        children.put(Child.TEAM_JAM, new HashMap<String, ValueWithId>());
        add(Child.TEAM_JAM, new TeamJamImpl(Team.ID_1, this));
        add(Child.TEAM_JAM, new TeamJamImpl(Team.ID_2, this));
    }

    public String getId() { return (String)get(Value.ID); }

    public Object valueFromString(PermanentProperty prop, String sValue) {
	synchronized (coreLock) {
            if (sValue == null || prop == Value.ID) return sValue;
            return Long.parseLong(sValue);
	}
    }
    public boolean set(PermanentProperty prop, Object value, Flag flag) {
	synchronized (coreLock) {
	    if (prop == Value.ID && flag != Flag.FORCE) { return false; }
	    return super.set(prop, value, flag);
	}
    }
    
    public Period getPeriod() { return period; }
    public int getPeriodNumber() { return period.getNumber(); }

    public long getDuration() { return (Long)get(Value.DURATION); }
    public void setDuration(long t) { set(Value.DURATION, t); }

    public long getPeriodClockElapsedStart() { return (Long)get(Value.PERIOD_CLOCK_ELAPSED_START); }
    public void setPeriodClockElapsedStart(long t) { set(Value.PERIOD_CLOCK_ELAPSED_START, t); }

    public long getPeriodClockElapsedEnd() { return (Long)get(Value.PERIOD_CLOCK_ELAPSED_END); }
    public void setPeriodClockElapsedEnd(long t) { set(Value.PERIOD_CLOCK_ELAPSED_END, t); }

    public long getWalltimeStart() { return (Long)get(Value.WALLTIME_START); }
    public void setWalltimeStart(long t) { set(Value.WALLTIME_START, t); }

    public long getWalltimeEnd() { return (Long)get(Value.WALLTIME_END); }
    public void setWalltimeEnd(long t) { set(Value.WALLTIME_END, t); }

    public TeamJam getTeamJam(String id) { return (TeamJam)get(Child.TEAM_JAM, id); }

    public void start() {
	synchronized (coreLock) {
	    requestBatchStart();
            setPeriodClockElapsedStart(scoreBoard.getClock(Clock.ID_PERIOD).getTimeElapsed());
            setWalltimeStart(ScoreBoardClock.getInstance().getCurrentWalltime());

            // Update all skater position, as they may have changed since
            // the previous jam ended. Also initalise other settings.
            for(String tid : Arrays.asList(Team.ID_1, Team.ID_2)) {
                TeamJam tj = getTeamJam(tid);
                Team t = scoreBoard.getTeam(tid);
                tj.removeFielding();
                for (FloorPosition fp : FloorPosition.values()) {
                    Skater s = t.getPosition(fp).getSkater();
                    if (s != null) {
                	tj.addFielding(s.getId());
                	Fielding ssm = tj.getFielding(s.getId());
                	ssm.setPosition(fp.toString());
                	ssm.setPenaltyBox(s.isPenaltyBox());
                    }
                }
                tj.setTotalScore(t.getScore());
                tj.setJamScore(t.getScore() - t.getLastScore());
                tj.setLeadJammer(t.getLeadJammer());
                tj.setStarPass(t.isStarPass());
                tj.setNoPivot(t.hasNoPivot());
                tj.setTimeouts(t.getTimeouts());
                tj.setOfficialReviews(t.getOfficialReviews());
            }
	    requestBatchEnd();
	}
    }
    public void stop() {
	synchronized (coreLock) {
	    requestBatchStart();
            set(Value.DURATION, scoreBoard.getClock(Clock.ID_JAM).getTimeElapsed());
            set(Value.PERIOD_CLOCK_ELAPSED_END, scoreBoard.getClock(Clock.ID_PERIOD).getTimeElapsed());
            set(Value.WALLTIME_END, ScoreBoardClock.getInstance().getCurrentWalltime());
	    requestBatchEnd();
	}
    }

    private Period period;
}
